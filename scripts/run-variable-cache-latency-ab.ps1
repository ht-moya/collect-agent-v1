param(
    [string]$BaseUrl = "http://127.0.0.1:18080",
    [int]$FixedIterations = 8,
    [int]$ChangeCycles = 12,
    [long]$RequestIntervalMs = 600,
    [int]$VariableChangeDelayMs = 1200,
    [string]$UserMessage = "你好",
    [string]$RunTag = (Get-Date -Format "yyyyMMddHHmmss")
)

$ErrorActionPreference = "Stop"
$api = $BaseUrl.TrimEnd("/") + "/api/dynamic-variables"
$benchmarkUrl = $BaseUrl.TrimEnd("/") + "/api/debug/variable-cache-benchmark"
$resultDir = Join-Path $PSScriptRoot "../cache-benchmark-results"
New-Item -ItemType Directory -Path $resultDir -Force | Out-Null

function Invoke-Json {
    param([string]$Method, [string]$Uri, [object]$Body)
    $json = if ($null -eq $Body) { $null } else { $Body | ConvertTo-Json -Depth 20 }
    $params = @{ Method = $Method; Uri = $Uri; ContentType = "application/json; charset=utf-8" }
    if ($null -ne $json) { $params.Body = [System.Text.Encoding]::UTF8.GetBytes($json) }
    return Invoke-RestMethod @params
}

function To-DefinitionBody {
    param([object]$Definition)
    return @{
        variableKey = $Definition.variableKey
        displayName = $Definition.displayName
        description = $Definition.description
        extractionHint = $Definition.extractionHint
        enabled = $Definition.enabled
        sortOrder = $Definition.sortOrder
    }
}

function Invoke-BenchmarkOnce {
    param([string]$Variant, [string]$RunId)
    return Invoke-Json -Method POST -Uri $benchmarkUrl -Body @{
        variant = $Variant
        iterations = 1
        runId = $RunId
        intervalMs = 0
        userMessage = $UserMessage
    }
}

function Get-Stats {
    param([object[]]$Values)
    $sorted = @($Values | Sort-Object)
    $average = ($sorted | Measure-Object -Average).Average
    $median = if ($sorted.Count -eq 0) { 0 } elseif ($sorted.Count % 2 -eq 1) { $sorted[[int][math]::Floor($sorted.Count / 2)] } else { ($sorted[$sorted.Count / 2 - 1] + $sorted[$sorted.Count / 2]) / 2 }
    $index = [math]::Min($sorted.Count - 1, [math]::Ceiling($sorted.Count * 0.95) - 1)
    return @{ Average = $average; Median = $median; P95 = $sorted[$index] }
}

$definitionResponse = Invoke-Json -Method GET -Uri "$api/definitions"
$definitions = @($definitionResponse | ForEach-Object { $_ })
$target = $definitions | Where-Object { $_.enabled } | Select-Object -First 1
if ($null -eq $target) { throw "没有启用的动态变量定义" }
$original = To-DefinitionBody $target
$rows = New-Object System.Collections.Generic.List[object]

try {
    # 固定变量场景：第一轮预热，后续请求均为稳定缓存延迟。
    foreach ($variant in @("A", "B")) {
        $fixed = Invoke-Json -Method POST -Uri $benchmarkUrl -Body @{
            variant = $variant
            iterations = $FixedIterations
            runId = "$variant-fixed-$RunTag"
            intervalMs = $RequestIntervalMs
            userMessage = $UserMessage
        }
        $index = 0
        foreach ($sample in $fixed.samples) {
            $index++
            if ($index -gt 1) {
                $rows.Add([pscustomobject]@{
                    Scenario = "FIXED"; Cycle = 0; Variant = $variant
                    PromptTokens = $sample.promptTokens; CachedTokens = $sample.cachedTokens
                    CacheHitRate = $sample.cacheHitRate; LatencyMs = $sample.latencyMs
                })
            }
        }
    }

    # 会话变量变化场景：每轮修改变量后只取第一次响应。
    for ($cycle = 1; $cycle -le $ChangeCycles; $cycle++) {
        $stageDefinition = To-DefinitionBody $original
        $stageDefinition.description = "$($original.description) | latency-stage-$cycle-$RunTag"
        Invoke-Json -Method PUT -Uri "$api/definitions/$($target.id)" -Body $stageDefinition | Out-Null
        Start-Sleep -Milliseconds $VariableChangeDelayMs

        $order = if ($cycle % 2 -eq 1) { @("A", "B") } else { @("B", "A") }
        foreach ($variant in $order) {
            $result = Invoke-BenchmarkOnce -Variant $variant -RunId "$variant-mutation-$RunTag"
            $sample = $result.samples[0]
            $rows.Add([pscustomobject]@{
                Scenario = "MUTATED"; Cycle = $cycle; Variant = $variant
                PromptTokens = $sample.promptTokens; CachedTokens = $sample.cachedTokens
                CacheHitRate = $sample.cacheHitRate; LatencyMs = $sample.latencyMs
            })
        }
    }
}
finally {
    Invoke-Json -Method PUT -Uri "$api/definitions/$($target.id)" -Body $original | Out-Null
}

$csvPath = Join-Path $resultDir "variable-cache-latency-ab-$RunTag.csv"
$jsonPath = Join-Path $resultDir "variable-cache-latency-ab-$RunTag.json"
$rows | Export-Csv -LiteralPath $csvPath -NoTypeInformation -Encoding utf8
$rows | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding utf8

$summaryRows = foreach ($scenario in @("FIXED", "MUTATED")) {
    foreach ($variant in @("A", "B")) {
        $groupRows = @($rows | Where-Object {
            $_.Scenario -eq $scenario -and $_.Variant -eq $variant -and [int]$_.Cycle -ne 1
        } | Where-Object { $scenario -eq "FIXED" -or [int]$_.Cycle -gt 1 })
        if ($groupRows.Count -eq 0) { continue }
        $latencyStats = Get-Stats -Values @($groupRows | ForEach-Object { [double]$_.LatencyMs })
        $cacheStats = Get-Stats -Values @($groupRows | ForEach-Object { [double]$_.CacheHitRate })
        [pscustomobject]@{
            Scenario = $scenario
            Variant = $variant
            Samples = $groupRows.Count
            AvgLatencyMs = [math]::Round($latencyStats.Average, 1)
            MedianLatencyMs = [math]::Round($latencyStats.Median, 1)
            P95LatencyMs = [math]::Round($latencyStats.P95, 1)
            AvgCacheHitRate = [math]::Round($cacheStats.Average, 4)
        }
    }
}
$summaryRows | Format-Table -AutoSize

Write-Output "CSV=$csvPath"
Write-Output "JSON=$jsonPath"
