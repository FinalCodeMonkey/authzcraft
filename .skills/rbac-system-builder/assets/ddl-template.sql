-- RBAC 权限系统 DDL 模板
-- 技术栈无关：按目标数据库方言调整类型/语法（MySQL / PostgreSQL / SQLite 等）
-- 使用前 MUST 替换占位符：<schema>、<table_prefix>（可选）
--
-- ============================================================
-- 双模式基线（NON-NEGOTIABLE）
-- ============================================================
-- 默认运行模式：AUTHZCRAFT
--   角色、用户组、成员关系的权威读写在 AuthzCraft 主体域。
--   运行时代码必须是单权威路径；本地表不得作为默认回退源。
--
-- 备选运行模式：LOCAL
--   显式切换配置后，仅 Local* 适配器读取本地 users / roles /
--   user_roles / rbac_groups / user_groups，以保证离线或本地存储场景可运行。
--
-- 因此 DDL MUST 始终保留以下本地主体/成员表：
--   users、roles、user_roles、rbac_groups、user_groups
--
-- 功能/字段授权两种模式 MUST 统一使用 code 绑定表：
--   role_permissions.role_code / role_field_permissions.role_code
--   group_permissions.group_code / group_field_permissions.group_code
-- 默认 AUTHZCRAFT 模式引用 AuthzCraft 角色/用户组编码；LOCAL 模式引用 roles.code / rbac_groups.code。
-- 禁止生成旧 role_id 版权限绑定表：
--   role_permissions(role_id, permission_id)
--   role_field_permissions(role_id, field_permission_id)
-- 禁止默认生成同步 outbox；确需异步同步时必须单独确认和设计。
-- ============================================================


-- ============================================================
-- 本地主体/成员表：默认 AUTHZCRAFT 模式下不作为权威源，
-- 也不得作为 AuthzCraft 主体投影缺失时的静默回退；仅 LOCAL 模式读取/写入。
-- ============================================================
CREATE TABLE users (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_key      VARCHAR(128) NOT NULL UNIQUE,        -- 统一身份 user_id / 外部主体键 / 自建登录主体键
    username      VARCHAR(64)  UNIQUE,                 -- 登录名或展示账号（外部身份场景可为空）
    password_hash VARCHAR(255) NULL,                   -- 仅自建登录使用；外部身份场景必须为空
    display_name  VARCHAR(128),
    status        TINYINT      NOT NULL DEFAULT 1,     -- 1=启用 0=禁用
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(64)  NOT NULL UNIQUE,          -- 角色编码；需与 AuthzCraft ROLE principal_key 同域
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id    BIGINT NOT NULL,
    role_id    BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE TABLE rbac_groups (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(64)  NOT NULL UNIQUE,          -- 用户组编码；需与 AuthzCraft GROUP principal_key 同域
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_groups (
    user_id    BIGINT NOT NULL,
    group_id   BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, group_id),
    CONSTRAINT fk_user_groups_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_groups_group FOREIGN KEY (group_id) REFERENCES rbac_groups (id) ON DELETE CASCADE
);


-- ============================================================
-- 授权层：功能权限点
-- ============================================================
CREATE TABLE permissions (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(128) NOT NULL UNIQUE,          -- 权限点编码（应用内唯一，三段式 app:resource:operation，如 app:user:create）
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 授权层：字段权限定义（两维模型）
-- 字段权限必须由后端真实执行：读接口负责隐藏/脱敏/投影，写接口负责拒绝无权字段。
--
-- security_requirement（字段固有属性）：
--   权威来源是 AuthzCraft 回路 2 字段目录 authzcraft_resource_field；
--   本表默认不保存该值；运行时从字段目录读取。若确需本地快照，必须同步调整 Mapper/Repository。
--   PLAIN（明文）/ MASKED（脱敏）/ HIDDEN（隐藏）
-- access_level（授权可变属性，不同角色可获授不同级别）：
--   NONE（无权限）/ READ（只读，受 security_requirement 约束）/ PLAIN_READ（明文只读）/ WRITE（可写，隐含明文）
-- code 格式：resource_key.field_key:access_level（不含 security_requirement，因为它是字段固有属性）
-- 唯一约束：(resource_key, field_key, access_level) 三元组
-- ============================================================
CREATE TABLE field_permissions (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    code                VARCHAR(160) NOT NULL UNIQUE,         -- 字段权限编码，如 user.mobile:read
    resource_key        VARCHAR(128) NOT NULL,
    field_key           VARCHAR(128) NOT NULL,
    access_level        VARCHAR(32)  NOT NULL,               -- NONE / READ / PLAIN_READ / WRITE
    name                VARCHAR(128) NOT NULL,               -- 字段业务名，不拼接访问级别
    description         VARCHAR(255),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_field_permissions_scope (resource_key, field_key, access_level)
);

-- ============================================================
-- 授权层：角色-权限关联
-- role_code 是唯一绑定键。默认 AUTHZCRAFT 模式引用 AuthzCraft ROLE principal_key；
-- LOCAL 模式引用本地 roles.code。不使用 role_id 外键。
-- ============================================================
CREATE TABLE role_permissions (
    role_code     VARCHAR(64) NOT NULL,
    permission_id BIGINT      NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_code, permission_id),
    CONSTRAINT fk_role_permissions_perm FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE TABLE group_permissions (
    group_code    VARCHAR(64) NOT NULL,
    permission_id BIGINT      NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_code, permission_id),
    CONSTRAINT fk_group_permissions_perm FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

-- ============================================================
-- 授权层：角色/用户组-字段权限关联
-- role_code / group_code 是唯一绑定键。默认 AUTHZCRAFT 模式引用 AuthzCraft principal_key；
-- LOCAL 模式引用本地 roles.code / rbac_groups.code。不使用 role_id / group_id 外键。
-- ============================================================
CREATE TABLE role_field_permissions (
    role_code           VARCHAR(64) NOT NULL,
    field_permission_id BIGINT      NOT NULL,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_code, field_permission_id),
    CONSTRAINT fk_role_field_permissions_field FOREIGN KEY (field_permission_id) REFERENCES field_permissions (id) ON DELETE CASCADE
);

CREATE TABLE group_field_permissions (
    group_code          VARCHAR(64) NOT NULL,
    field_permission_id BIGINT      NOT NULL,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_code, field_permission_id),
    CONSTRAINT fk_group_field_permissions_field FOREIGN KEY (field_permission_id) REFERENCES field_permissions (id) ON DELETE CASCADE
);

-- replaceRoleFieldPermissions / replaceGroupFieldPermissions 必须在应用层校验：
-- 同一主体同一 resource_key + field_key 只能保存一个 access_level code。
-- 如果项目希望在 DB 层强约束，可冗余 resource_key / field_key 到绑定表并增加唯一索引，
-- 但必须同步调整 Mapper insert，不能只改 DDL。


-- ============================================================
-- 种子数据：示例角色、用户组、权限点和 role_code 绑定
-- 实际生成时按业务角色、用户组和权限点替换。
-- 默认 AUTHZCRAFT 模式下，roles/rbac_groups 种子仅供 LOCAL 使用；
-- ROLE/GROUP 主体仍应通过 AuthzCraft API 创建或回读。
-- ============================================================
INSERT INTO roles (code, name, description) VALUES
    ('admin', '管理员', '拥有全部权限'),
    ('manager', '管理者', '拥有管理权限'),
    ('user', '普通用户', '拥有基础权限');

INSERT INTO rbac_groups (code, name, description) VALUES
    ('managers_group', '管理者组', '管理者用户组');

-- 权限点编码命名约定：appKey:resourceKey:operation（三段式）
-- operation 是面向业务的动作（read/create/update/delete/manage/submit/audit 等），由探查确定，不是固定 CRUD 枚举
INSERT INTO permissions (code, name, description) VALUES
    ('app:user:read',   '查看用户', '查看用户'),
    ('app:user:create', '创建用户', '创建用户'),
    ('app:user:update', '更新用户', '更新用户'),
    ('app:user:delete', '删除用户', '删除用户'),
    ('app:role:read',   '查看角色', '查看角色'),
    ('app:role:manage', '管理角色', '创建/更新/删除角色并绑定权限和成员');

INSERT INTO role_permissions (role_code, permission_id)
SELECT 'admin', p.id FROM permissions p;

-- 字段权限种子：security_requirement 是来自字段目录的字段固有属性，不在本表落列。
-- NONE 必须显式生成和绑定；缺少绑定记录表示使用 security_requirement 默认访问级别，不表示无权限。
-- 字段 name 只存业务名，不拼接 access_level。
INSERT INTO field_permissions (code, resource_key, field_key, access_level, name, description) VALUES
    ('USER.mobile:NONE',      'USER', 'mobile', 'NONE',       '手机号', '不返回手机号'),
    ('USER.mobile:READ',      'USER', 'mobile', 'READ',       '手机号', '查看脱敏手机号'),
    ('USER.mobile:PLAIN_READ','USER', 'mobile', 'PLAIN_READ', '手机号', '查看明文手机号'),
    ('USER.mobile:WRITE',     'USER', 'mobile', 'WRITE',      '手机号', '查看和编辑手机号');

INSERT INTO role_field_permissions (role_code, field_permission_id)
SELECT 'admin', fp.id FROM field_permissions fp;


-- ============================================================
-- 回滚脚本（按需执行，顺序与建表相反）
-- ============================================================
-- DROP TABLE IF EXISTS group_field_permissions;
-- DROP TABLE IF EXISTS role_field_permissions;
-- DROP TABLE IF EXISTS group_permissions;
-- DROP TABLE IF EXISTS role_permissions;
-- DROP TABLE IF EXISTS field_permissions;
-- DROP TABLE IF EXISTS permissions;
-- DROP TABLE IF EXISTS user_groups;
-- DROP TABLE IF EXISTS rbac_groups;
-- DROP TABLE IF EXISTS user_roles;
-- DROP TABLE IF EXISTS roles;
-- DROP TABLE IF EXISTS users;
