-- Long-term memory schema for MySQL 9.0.1.
-- Vector dimension is aligned with DashScope text-embedding-v4 at 1024.

USE collect_agent;

CREATE TABLE IF NOT EXISTS agent_long_term_memory (
  memory_id VARCHAR(64) NOT NULL COMMENT '记忆ID（ULID）',
  tenant_id VARCHAR(64) NOT NULL COMMENT '租户ID',
  user_id VARCHAR(64) NOT NULL COMMENT '用户/债务人ID',
  scope VARCHAR(16) NOT NULL DEFAULT 'user' COMMENT '作用域：user/session/global',
  memory_type VARCHAR(32) NOT NULL COMMENT 'preference/fact/episode/procedure',
  content TEXT NOT NULL COMMENT '自然语言记忆内容',
  content_hash CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'UTF-8内容MD5',
  structured JSON NULL COMMENT '结构化字段',
  source_event_ids JSON NOT NULL COMMENT '来源event_id列表',
  confidence DECIMAL(3,2) NOT NULL COMMENT '置信度0~1',
  status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/superseded/deleted',
  valid_from DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '生效时间',
  valid_to DATETIME(6) NULL COMMENT '失效时间',
  last_accessed_at DATETIME(6) NULL COMMENT '最后召回时间',
  access_count INT NOT NULL DEFAULT 0 COMMENT '召回次数',
  embedding VECTOR(1024) NULL COMMENT '1024维向量',
  embedding_model VARCHAR(64) NULL COMMENT '向量模型',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (memory_id),
  UNIQUE KEY uk_ltm_dedup (tenant_id, user_id, memory_type, content_hash),
  KEY idx_ltm_tenant_user_status (tenant_id, user_id, status),
  KEY idx_ltm_tenant_user_type (tenant_id, user_id, memory_type),
  FULLTEXT KEY ft_content (content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent长期记忆';

CREATE TABLE IF NOT EXISTS agent_memory_pending (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id VARCHAR(64) NOT NULL COMMENT '租户ID',
  user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
  scope VARCHAR(16) NOT NULL DEFAULT 'user' COMMENT '作用域',
  memory_type VARCHAR(32) NOT NULL COMMENT '记忆类型',
  content TEXT NOT NULL COMMENT '候选内容',
  content_hash CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '内容MD5',
  structured JSON NULL COMMENT '结构化字段',
  source_event_ids JSON NOT NULL COMMENT '来源event_id列表',
  confidence DECIMAL(3,2) NOT NULL COMMENT '置信度',
  status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  reviewed_at DATETIME(6) NULL COMMENT '审核时间',
  reviewer VARCHAR(64) NULL COMMENT '审核人',
  review_note VARCHAR(500) NULL COMMENT '审核说明',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_pending_dedup (tenant_id, user_id, memory_type, content_hash),
  KEY idx_pending_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待人工确认的长期记忆';

CREATE TABLE IF NOT EXISTS agent_memory_extraction_checkpoint (
  session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
  tenant_id VARCHAR(64) NOT NULL COMMENT '租户ID',
  user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
  extracted_seq BIGINT NOT NULL DEFAULT 0 COMMENT '已抽取到的session_seq',
  last_extraction_at DATETIME(6) NULL COMMENT '最后一次抽取时间',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (session_id),
  KEY idx_memory_checkpoint_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='长期记忆抽取位点';

DROP TABLE IF EXISTS agent_memory_vector;
DROP TABLE IF EXISTS ltm_layer;
