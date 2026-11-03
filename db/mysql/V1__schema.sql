-- Collect Agent V1 MySQL 9.0 schema.
-- MySQL 9.0 does not enforce CHECK constraints, so enum-like fields use VARCHAR plus application validation.

CREATE DATABASE IF NOT EXISTS collect_agent
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE collect_agent;

CREATE TABLE IF NOT EXISTS dynamic_variable_definition (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  variable_key VARCHAR(64) NOT NULL COMMENT '变量键',
  display_name VARCHAR(128) NOT NULL COMMENT '展示名称',
  description LONGTEXT NOT NULL COMMENT '变量用途说明',
  extraction_hint LONGTEXT NULL COMMENT '抽取提示',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  created_at DATETIME(6) NOT NULL COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dynamic_variable_key (variable_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态变量定义';

CREATE TABLE IF NOT EXISTS dynamic_variable_tool_config (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  tool_name VARCHAR(128) NOT NULL COMMENT '工具名称',
  description LONGTEXT NOT NULL COMMENT '工具描述',
  force_call TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否强制调用',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME(6) NOT NULL COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dynamic_tool_name (tool_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态变量工具配置';

CREATE TABLE IF NOT EXISTS dynamic_variable_value (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id VARCHAR(64) NOT NULL COMMENT '会话标识',
  customer_id VARCHAR(64) NOT NULL COMMENT '客户标识',
  variable_key VARCHAR(64) NOT NULL COMMENT '变量键',
  variable_value LONGTEXT NOT NULL COMMENT '变量值',
  source_event_id BIGINT NULL COMMENT '来源事件主键',
  trace_id VARCHAR(64) NULL COMMENT '链路标识',
  confidence DOUBLE NULL COMMENT '置信度',
  created_at DATETIME(6) NOT NULL COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dynamic_value_session_key (session_id, variable_key),
  KEY idx_dynamic_value_customer (customer_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话动态变量值';

CREATE TABLE IF NOT EXISTS event_layer (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据库主键',
  event_id VARCHAR(64) NOT NULL COMMENT '对外事件标识',
  trace_id VARCHAR(64) NOT NULL COMMENT '请求链路标识',
  session_id VARCHAR(64) NOT NULL COMMENT '会话标识',
  customer_id VARCHAR(64) NOT NULL COMMENT '客户标识',
  session_seq BIGINT NULL COMMENT '会话内单调序号',
  event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
  payload_json LONGTEXT NOT NULL COMMENT '事件载荷 JSON',
  occurred_at DATETIME(6) NOT NULL COMMENT '业务发生时间',
  created_at DATETIME(6) NOT NULL COMMENT '入库时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_event_layer_event_id (event_id),
  UNIQUE KEY uk_event_layer_session_seq (session_id, session_seq),
  KEY idx_event_layer_session_time (session_id, occurred_at),
  KEY idx_event_layer_customer (customer_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原始事件事实层';

CREATE TABLE IF NOT EXISTS outbox_event (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据库主键',
  event_id VARCHAR(64) NOT NULL COMMENT '原始事件标识',
  aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合类型',
  aggregate_id VARCHAR(64) NOT NULL COMMENT '聚合标识',
  event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
  payload_json LONGTEXT NOT NULL COMMENT '事件载荷',
  status VARCHAR(32) NOT NULL COMMENT '处理状态',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  next_retry_at DATETIME(6) NOT NULL COMMENT '下一次重试时间',
  created_at DATETIME(6) NOT NULL COMMENT '创建时间',
  processed_at DATETIME(6) NULL COMMENT '处理完成时间',
  last_error LONGTEXT NULL COMMENT '最近错误',
  PRIMARY KEY (id),
  UNIQUE KEY uk_outbox_event_id (event_id),
  KEY idx_outbox_status_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件 Outbox';

CREATE TABLE IF NOT EXISTS agent_session_state (
  session_id VARCHAR(64) NOT NULL COMMENT '会话标识',
  tenant_id VARCHAR(64) NOT NULL COMMENT '租户标识',
  user_id VARCHAR(64) NOT NULL COMMENT '用户标识',
  task VARCHAR(32) NOT NULL COMMENT '任务类型',
  stage VARCHAR(32) NOT NULL COMMENT '会话阶段',
  state_json LONGTEXT NOT NULL COMMENT '状态 JSON',
  version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  last_seq BIGINT NOT NULL DEFAULT 0 COMMENT '已处理到的会话内序号',
  schema_version INT NOT NULL DEFAULT 1 COMMENT '状态结构版本',
  expire_at DATETIME(6) NULL COMMENT '过期时间',
  created_at DATETIME(6) NOT NULL COMMENT '创建时间',
  updated_at DATETIME(6) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (session_id),
  KEY idx_session_state_tenant_user (tenant_id, user_id),
  KEY idx_session_state_expire (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 会话状态';

CREATE TABLE IF NOT EXISTS agent_summary (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id VARCHAR(64) NOT NULL COMMENT '租户标识',
  session_id VARCHAR(64) NOT NULL COMMENT '会话标识',
  summary LONGTEXT NOT NULL COMMENT '摘要内容',
  covers_seq_from BIGINT NOT NULL COMMENT '覆盖起始序号',
  covers_seq_to BIGINT NOT NULL COMMENT '覆盖结束序号',
  model VARCHAR(64) NULL COMMENT '生成模型',
  prompt_version VARCHAR(32) NULL COMMENT '提示词版本',
  token_count INT NULL COMMENT 'Token 估算',
  status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '摘要状态',
  created_at DATETIME(6) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_agent_summary_session_seq (tenant_id, session_id, covers_seq_to),
  KEY idx_agent_summary_latest (tenant_id, session_id, status, covers_seq_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 会话摘要';


