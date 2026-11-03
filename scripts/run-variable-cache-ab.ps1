param(
    [string]$BaseUrl = "http://127.0.0.1:18080",
    [int]$Iterations = 6,
    [int]$Stages = 3,
    [long]$RequestIntervalMs = 600,
    [int]$VariableChangeDelayMs = 1200,
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

function Convert-ToDefinitionBody {
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

$definitionResponse = Invoke-Json -Method GET -Uri "$api/definitions"
$definitions = @($definitionResponse | ForEach-Object { $_ })
$createdVariableId = $null
if ($definitions.Count -eq 0) {
    $created = Invoke-Json -Method POST -Uri "$api/definitions" -Body @{
        variableKey = "cache_benchmark_var"
        displayName = "缓存基准变量"
        description = "用于动态变量缓存命中测试"
        extractionHint = "从用户消息中提取该变量的明确值"
        enabled = $true
        sortOrder = 999
    }
    $createdVariableId = $created.id
    $definitions = @($created)
}
$target = $definitions | Where-Object { $_.enabled } | Select-Object -First 1
if ($null -eq $target) { throw "没有启用的动态变量定义，无法执行基准测试" }
$original = Convert-ToDefinitionBody $target
$rows = New-Object System.Collections.Generic.List[object]

try {
    for ($stage = 1; $stage -le $Stages; $stage++) {
        $stageDefinition = Convert-ToDefinitionBody $original
        $stageDefinition.description = "$($original.description) | cache-stage-$stage-$RunTag"
        Invoke-Json -Method PUT -Uri "$api/definitions/$($target.id)" -Body $stageDefinition | Out-Null
        Start-Sleep -Milliseconds $VariableChangeDelayMs

        foreach ($variant in @("A", "B")) {
            $request = @{
                variant = $variant
                iterations = $Iterations
                runId = "$variant-$RunTag"
                intervalMs = $RequestIntervalMs
            }
            $result = Invoke-Json -Method POST -Uri $benchmarkUrl -Body $request
            foreach ($sample in $result.samples) {
                $rows.Add([pscustomobject]@{
                    Stage = $stage
                    Variant = $variant
                    Iteration = $sample.iteration
                    PromptTokens = $sample.promptTokens
                    CachedTokens = $sample.cachedTokens
                    CacheMissTokens = $sample.cacheMissTokens
                    CacheHitRate = [math]::Round($sample.cacheHitRate, 4)
                    LatencyMs = $sample.latencyMs
                    VariableFingerprint = $result.variableFingerprint
                })
            }
        }
    }
}
finally {
    Invoke-Json -Method PUT -Uri "$api/definitions/$($target.id)" -Body $original | Out-Null
    if ($null -ne $createdVariableId) {
        Invoke-Json -Method DELETE -Uri "$api/definitions/$createdVariableId" -Body $null | Out-Null
    }
}

$jsonPath = Join-Path $resultDir "variable-cache-ab-$RunTag.json"
$csvPath = Join-Path $resultDir "variable-cache-ab-$RunTag.csv"
$rows | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $jsonPath -Encoding utf8
$rows | Export-Csv -LiteralPath $csvPath -NoTypeInformation -Encoding utf8

$summary = $rows | Group-Object Stage, Variant | ForEach-Object {
    $groupRows = @($_.Group)
    $stage = [int]$groupRows[0].Stage
    $variant = [string]$groupRows[0].Variant
    $steadyRows = @($groupRows | Where-Object { [int]$_.Iteration -gt 1 })
    $steadyRate = [math]::Round(($steadyRows | Measure-Object CacheHitRate -Average).Average, 4)
    $firstRate = [double]$groupRows[0].CacheHitRate
    $previousRows = @($rows | Where-Object { [int]$_.Stage -eq ($stage - 1) -and $_.Variant -eq $variant -and [int]$_.Iteration -gt 1 })
    $previousSteady = if ($previousRows.Count -eq 0) { $null } else { [math]::Round(($previousRows | Measure-Object CacheHitRate -Average).Average, 4) }
    $dropPp = if ($null -eq $previousSteady) { $null } else { [math]::Round(($previousSteady - $firstRate) * 100, 2) }
    $recoveryIndex = 0
    if ($null -ne $previousSteady -and $firstRate -lt ($previousSteady * 0.95)) {
        $recoveryRow = $groupRows | Where-Object {
            [int]$_.Iteration -gt 1 -and [double]$_.CacheHitRate -ge ($previousSteady * 0.95)
        } | Select-Object -First 1
        if ($null -ne $recoveryRow) { $recoveryIndex = [int]$recoveryRow.Iteration - 1 }
    }
    $recoveryMs = 0
    if ($recoveryIndex -gt 0) {
        $recoveryRows = @($groupRows | Where-Object { [int]$_.Iteration -le ($recoveryIndex + 1) })
        $recoveryMs = [math]::Round((($recoveryRows | Measure-Object LatencyMs -Sum).Sum) + ($recoveryIndex * $RequestIntervalMs), 0)
    }
    [pscustomobject]@{
        Stage = $stage
        Variant = $variant
        SteadyCacheHitRate = $steadyRate
        FirstRequestHitRate = $firstRate
        DropVsPreviousSteadyPp = $dropPp
        RecoveryRequests = $recoveryIndex
        RecoveryMs = $recoveryMs
        AvgLatencyMs = [math]::Round(($groupRows | Measure-Object LatencyMs -Average).Average, 1)
    }
}
$summary | Format-Table -AutoSize

Write-Output "JSON=$jsonPath"
Write-Output "CSV=$csvPath"
