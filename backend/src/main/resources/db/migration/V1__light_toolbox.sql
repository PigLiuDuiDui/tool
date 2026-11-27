-- 轻工具箱一期表结构
CREATE TABLE wx_user (
    id           BIGINT PRIMARY KEY,
    open_id      VARCHAR(64)  NOT NULL,
    nick_name    VARCHAR(64),
    avatar_url   VARCHAR(512),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted      SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_wx_user_open_id ON wx_user (open_id) WHERE deleted = 0;

CREATE TABLE tool_job (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    tool_code       VARCHAR(64)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    params_json     TEXT,
    input_path      VARCHAR(512),
    input_name      VARCHAR(256),
    input_size      BIGINT,
    output_path     VARCHAR(512),
    output_name     VARCHAR(256),
    output_size     BIGINT,
    estimated_size  BIGINT,
    error_message   VARCHAR(512),
    expire_at       TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted         SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_tool_job_user ON tool_job (user_id, created_at DESC) WHERE deleted = 0;

CREATE TABLE user_recent_tool (
    id          BIGINT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    tool_code   VARCHAR(64) NOT NULL,
    used_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    deleted     SMALLINT    NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_recent_user_tool ON user_recent_tool (user_id, tool_code) WHERE deleted = 0;
