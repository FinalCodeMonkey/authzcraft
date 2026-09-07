-- AuthzCraft 数据权限 Schema —— MVP 子集
-- MySQL 8.0.16+
-- UTF-8 无 BOM
-- 本文件是回路 2（数据权限）的物理数据模型唯一权威，不再有 v2 作为上游。
--
-- 范围：17 张表，按外键依赖自顶向下排序。
-- 所有外键均在本文件内解析，无悬空引用。
--
-- 延期（2 张表，本文件内无任何表引用它们）：
--   authzcraft_catalog_resource_snapshot  -> 无结构漂移检测
--   authzcraft_decision_evidence          -> 已知决策结果，未记录命中的策略

SET NAMES utf8mb4;

-- =============================================================================
-- 主体域 Principal domain (8 张表)
-- =============================================================================

CREATE TABLE authzcraft_principal (
    id BIGINT NOT NULL COMMENT '内部雪花标识',
    tenant_key VARCHAR(64) NOT NULL COMMENT '外部租户稳定键',
    principal_kind VARCHAR(24) NOT NULL COMMENT 'USER用户/ORGANIZATION组织/POSITION岗位/ROLE角色/GROUP主体组',
    principal_key VARCHAR(160) NOT NULL COMMENT '来自权威源的稳定键；USER 取 user_id（逻辑主键，如 fanlaihua）而非 staff_no；ORGANIZATION/POSITION 取等价源列；ROLE/GROUP 由产品定义，应用作用域时带应用前缀',
    display_name VARCHAR(200) NOT NULL,
    lifecycle_state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    record_version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_authzcraft_principal_key (tenant_key, principal_kind, principal_key),
    UNIQUE KEY uk_authzcraft_principal_tenant_ref (id, tenant_key),
    UNIQUE KEY uk_authzcraft_principal_kind_ref (id, principal_kind),
    KEY idx_authzcraft_principal_lookup (tenant_key, principal_kind, lifecycle_state),
    CONSTRAINT ck_authzcraft_principal_kind CHECK (
        principal_kind IN ('USER', 'ORGANIZATION', 'POSITION', 'ROLE', 'GROUP')
    ),
    CONSTRAINT ck_authzcraft_principal_state CHECK (lifecycle_state IN ('ACTIVE', 'INACTIVE', 'RETIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='主体注册表，为用户、组织、岗位、角色和主体组提供统一身份与生命周期';

CREATE TABLE authzcraft_principal_organization (
    principal_id BIGINT NOT NULL,
    principal_kind VARCHAR(24) NOT NULL DEFAULT 'ORGANIZATION' COMMENT '通过复合外键锁定子类型',
    tenant_key VARCHAR(64) NOT NULL,
    department_code VARCHAR(128) NOT NULL COMMENT '部门编码，与 authzcraft_principal.principal_key 同值',
    department_name VARCHAR(200) DEFAULT NULL COMMENT '部门名称',
    department_level BIGINT DEFAULT NULL COMMENT '部门等级',
    department_type_code VARCHAR(50) DEFAULT NULL COMMENT '部门类型编码',
    department_category VARCHAR(100) DEFAULT NULL COMMENT '部门类别',
    parent_department_code VARCHAR(128) DEFAULT NULL COMMENT '父部门编码，组织树依据',
    parent_department_name VARCHAR(200) DEFAULT NULL COMMENT '父部门名称',
    manage_user_id VARCHAR(100) DEFAULT NULL COMMENT '正职负责人 user_id',
    manage_staff_no VARCHAR(100) DEFAULT NULL COMMENT '正职负责人工号',
    manage_name VARCHAR(200) DEFAULT NULL COMMENT '正职负责人姓名',
    portion_manage_user_id VARCHAR(100) DEFAULT NULL COMMENT '分管负责人 user_id',
    portion_manage_staff_no VARCHAR(100) DEFAULT NULL COMMENT '分管负责人工号',
    portion_manage_name VARCHAR(200) DEFAULT NULL COMMENT '分管负责人姓名',
    is_enable BIGINT DEFAULT NULL COMMENT '是否启用',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    department_hrbp_list TEXT COMMENT '部门 HRBP 列表',
    isenable_code VARCHAR(64) DEFAULT NULL COMMENT '启用状态编码',
    created_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (principal_id),
    UNIQUE KEY uk_authzcraft_org_tenant_ref (principal_id, tenant_key),
    UNIQUE KEY uk_authzcraft_org_department_code (department_code, tenant_key),
    KEY idx_authzcraft_org_parent_code (tenant_key, parent_department_code),
    KEY idx_authzcraft_org_manager (tenant_key, manage_user_id),
    KEY idx_authzcraft_org_portion_manager (tenant_key, portion_manage_user_id),
    CONSTRAINT fk_authzcraft_org_principal FOREIGN KEY (principal_id, principal_kind)
        REFERENCES authzcraft_principal (id, principal_kind) ON DELETE CASCADE,
    CONSTRAINT fk_authzcraft_org_tenant FOREIGN KEY (principal_id, tenant_key)
        REFERENCES authzcraft_principal (id, tenant_key) ON DELETE CASCADE,
    CONSTRAINT ck_authzcraft_org_kind CHECK (principal_kind = 'ORGANIZATION')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='组织主体表，从外部权威系统同步组织树、管理关系和同步水位';

CREATE TABLE authzcraft_organization_closure (
    ancestor_department_code VARCHAR(128) NOT NULL COMMENT '祖先部门编码，引用 authzcraft_principal_organization.department_code',
    descendant_department_code VARCHAR(128) NOT NULL COMMENT '后代部门编码，引用 authzcraft_principal_organization.department_code',
    tenant_key VARCHAR(64) NOT NULL,
    depth INT NOT NULL COMMENT '自身关系深度为 0',
    created_by VARCHAR(128) NOT NULL COMMENT '闭包重建任务身份，非人工操作者',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL COMMENT '闭包重建任务身份，非人工操作者',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (tenant_key, ancestor_department_code, descendant_department_code),
    KEY idx_authzcraft_closure_descendant (tenant_key, descendant_department_code, depth),
    KEY idx_authzcraft_closure_subtree (tenant_key, ancestor_department_code, depth),
    CONSTRAINT fk_authzcraft_closure_ancestor FOREIGN KEY (ancestor_department_code, tenant_key)
        REFERENCES authzcraft_principal_organization (department_code, tenant_key) ON DELETE CASCADE,
    CONSTRAINT fk_authzcraft_closure_descendant FOREIGN KEY (descendant_department_code, tenant_key)
        REFERENCES authzcraft_principal_organization (department_code, tenant_key) ON DELETE CASCADE,
    CONSTRAINT ck_authzcraft_closure_depth CHECK (depth >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='组织闭包表，将本部门及下级数据范围解析为单次索引查询';

CREATE TABLE authzcraft_principal_position (
    principal_id BIGINT NOT NULL,
    principal_kind VARCHAR(24) NOT NULL DEFAULT 'POSITION',
    tenant_key VARCHAR(64) NOT NULL,
    post_code VARCHAR(128) NOT NULL COMMENT '岗位编码，与 authzcraft_principal.principal_key 同值',
    post_name VARCHAR(128) DEFAULT NULL COMMENT '岗位名称',
    post_en_name VARCHAR(200) DEFAULT NULL COMMENT '岗位英文名称',
    post_type_code VARCHAR(128) DEFAULT NULL COMMENT '岗位类型编码',
    post_type VARCHAR(128) DEFAULT NULL COMMENT '岗位类型名称',
    department_code VARCHAR(128) DEFAULT NULL COMMENT '所属部门编码',
    department_name VARCHAR(128) DEFAULT NULL COMMENT '所属部门名称',
    position_grade_code VARCHAR(128) DEFAULT NULL COMMENT '职级编码',
    position_grade_name VARCHAR(256) DEFAULT NULL COMMENT '职级名称',
    is_enable BIGINT DEFAULT NULL COMMENT '是否启用',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    isenable_code VARCHAR(64) DEFAULT NULL COMMENT '启用状态编码',
    created_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (principal_id),
    UNIQUE KEY uk_authzcraft_position_tenant_ref (principal_id, tenant_key),
    UNIQUE KEY uk_authzcraft_position_post_code (post_code, tenant_key),
    KEY idx_authzcraft_position_org (tenant_key, department_code),
    CONSTRAINT fk_authzcraft_position_principal FOREIGN KEY (principal_id, principal_kind)
        REFERENCES authzcraft_principal (id, principal_kind) ON DELETE CASCADE,
    CONSTRAINT fk_authzcraft_position_tenant FOREIGN KEY (principal_id, tenant_key)
        REFERENCES authzcraft_principal (id, tenant_key) ON DELETE CASCADE,
    CONSTRAINT ck_authzcraft_position_kind CHECK (principal_kind = 'POSITION')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='岗位主体表，从外部权威系统同步岗位及其组织归属';

CREATE TABLE authzcraft_principal_user (
    principal_id BIGINT NOT NULL,
    principal_kind VARCHAR(24) NOT NULL DEFAULT 'USER' COMMENT '通过复合外键锁定子类型',
    tenant_key VARCHAR(64) NOT NULL,
    user_id VARCHAR(100) NOT NULL COMMENT '逻辑主键，如 fanlaihua，与 authzcraft_principal.principal_key 同值',
    staff_no VARCHAR(100) DEFAULT NULL COMMENT '工号；兼容匹配的次级标识符，不是 principal_key 来源。PSP 现网 buildAssignmentSubjectIdBuckets/loadManagedRootOrgs 证实历史授权配置同时按 staffNo 和 userId 书写，二者不可互相替代',
    staff_name VARCHAR(200) DEFAULT NULL COMMENT '员工姓名',
    department_code VARCHAR(128) DEFAULT NULL COMMENT '主挂部门编码',
    department_name VARCHAR(200) DEFAULT NULL COMMENT '主挂部门名称',
    post_code VARCHAR(128) DEFAULT NULL COMMENT '主挂岗位编码',
    post_name VARCHAR(200) DEFAULT NULL COMMENT '主挂岗位名称',
    manage_user_id VARCHAR(100) DEFAULT NULL COMMENT '直属上级 user_id',
    manage_staff_no VARCHAR(100) DEFAULT NULL COMMENT '直属上级工号',
    manage_staff_name VARCHAR(200) DEFAULT NULL COMMENT '直属上级姓名',
    staff_status VARCHAR(50) DEFAULT NULL COMMENT '在职状态',
    staff_status_code VARCHAR(50) DEFAULT NULL COMMENT '在职状态编码',
    is_dept_manage TINYINT DEFAULT NULL COMMENT '是否部门主管',
    is_dept_portion_manage TINYINT DEFAULT NULL COMMENT '是否部门分管',
    created_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (principal_id),
    UNIQUE KEY uk_authzcraft_user_tenant_ref (principal_id, tenant_key),
    UNIQUE KEY uk_authzcraft_user_id (tenant_key, user_id),
    UNIQUE KEY uk_authzcraft_user_staff_no (tenant_key, staff_no),
    KEY idx_authzcraft_user_department (tenant_key, department_code),
    KEY idx_authzcraft_user_post (tenant_key, post_code),
    KEY idx_authzcraft_user_manager (tenant_key, manage_user_id),
    CONSTRAINT fk_authzcraft_user_principal FOREIGN KEY (principal_id, principal_kind)
        REFERENCES authzcraft_principal (id, principal_kind) ON DELETE CASCADE,
    CONSTRAINT fk_authzcraft_user_tenant FOREIGN KEY (principal_id, tenant_key)
        REFERENCES authzcraft_principal (id, tenant_key) ON DELETE CASCADE,
    CONSTRAINT ck_authzcraft_user_kind CHECK (principal_kind = 'USER')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='员工主体表，从外部权威系统同步员工身份及主挂任职（主部门、主岗位、直属上级）属性';

CREATE TABLE authzcraft_principal_role (
    principal_id BIGINT NOT NULL,
    principal_kind VARCHAR(24) NOT NULL DEFAULT 'ROLE',
    tenant_key VARCHAR(64) NOT NULL,
    role_code VARCHAR(128) NOT NULL COMMENT '角色编码（逻辑主键，与 authzcraft_principal.principal_key 同值，产品定义）',
    role_name VARCHAR(200) DEFAULT NULL COMMENT '角色名称（与 authzcraft_principal.display_name 同值）',
    app_key VARCHAR(64) DEFAULT NULL COMMENT '为空表示角色跨应用共享',
    description VARCHAR(500) DEFAULT NULL,
    created_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (principal_id),
    UNIQUE KEY uk_authzcraft_role_tenant_ref (principal_id, tenant_key),
    UNIQUE KEY uk_authzcraft_role_code (tenant_key, role_code),
    KEY idx_authzcraft_role_app (tenant_key, app_key),
    CONSTRAINT fk_authzcraft_role_principal FOREIGN KEY (principal_id, principal_kind)
        REFERENCES authzcraft_principal (id, principal_kind) ON DELETE CASCADE,
    CONSTRAINT fk_authzcraft_role_tenant FOREIGN KEY (principal_id, tenant_key)
        REFERENCES authzcraft_principal (id, tenant_key) ON DELETE CASCADE,
    CONSTRAINT ck_authzcraft_role_kind CHECK (principal_kind = 'ROLE')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='角色主体表，从回路 1 业务侧权威系统同步角色定义，AuthzCraft 不创建角色';

CREATE TABLE authzcraft_principal_group (
    principal_id BIGINT NOT NULL,
    principal_kind VARCHAR(24) NOT NULL DEFAULT 'GROUP',
    tenant_key VARCHAR(64) NOT NULL,
    group_code VARCHAR(128) NOT NULL COMMENT '主体组编码（逻辑主键，与 authzcraft_principal.principal_key 同值，产品定义）',
    group_name VARCHAR(200) DEFAULT NULL COMMENT '主体组名称（与 authzcraft_principal.display_name 同值）',
    app_key VARCHAR(64) DEFAULT NULL COMMENT '为空表示主体组跨应用共享',
    description VARCHAR(500) DEFAULT NULL,
    created_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (principal_id),
    UNIQUE KEY uk_authzcraft_group_tenant_ref (principal_id, tenant_key),
    UNIQUE KEY uk_authzcraft_group_code (tenant_key, group_code),
    KEY idx_authzcraft_group_app (tenant_key, app_key),
    CONSTRAINT fk_authzcraft_group_principal FOREIGN KEY (principal_id, principal_kind)
        REFERENCES authzcraft_principal (id, principal_kind) ON DELETE CASCADE,
    CONSTRAINT fk_authzcraft_group_tenant FOREIGN KEY (principal_id, tenant_key)
        REFERENCES authzcraft_principal (id, tenant_key) ON DELETE CASCADE,
    CONSTRAINT ck_authzcraft_group_kind CHECK (principal_kind = 'GROUP')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='主体组表，从回路 1 业务侧权威系统同步用户组定义，AuthzCraft 不创建用户组';

CREATE TABLE authzcraft_principal_membership (
    id BIGINT NOT NULL,
    tenant_key VARCHAR(64) NOT NULL,
    container_principal_id BIGINT NOT NULL,
    container_kind VARCHAR(24) NOT NULL COMMENT 'ROLE角色/GROUP主体组',
    member_principal_id BIGINT NOT NULL,
    member_kind VARCHAR(24) NOT NULL DEFAULT 'USER',
    valid_from DATETIME(3) NOT NULL,
    valid_until DATETIME(3) DEFAULT NULL,
    open_marker TINYINT GENERATED ALWAYS AS (
        CASE WHEN valid_until IS NULL THEN 1 ELSE NULL END
    ) STORED COMMENT '强制每对（容器,成员）至多一个开放区间',
    created_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL COMMENT '同步任务身份，非人工操作者',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_authzcraft_membership_open (container_principal_id, member_principal_id, open_marker),
    KEY idx_authzcraft_membership_member (member_principal_id, valid_until),
    KEY idx_authzcraft_membership_container (container_principal_id, valid_until),
    CONSTRAINT fk_authzcraft_membership_container FOREIGN KEY (container_principal_id, container_kind)
        REFERENCES authzcraft_principal (id, principal_kind) ON DELETE CASCADE,
    CONSTRAINT fk_authzcraft_membership_container_tenant FOREIGN KEY (container_principal_id, tenant_key)
        REFERENCES authzcraft_principal (id, tenant_key) ON DELETE CASCADE,
    CONSTRAINT fk_authzcraft_membership_member FOREIGN KEY (member_principal_id, member_kind)
        REFERENCES authzcraft_principal (id, principal_kind) ON DELETE RESTRICT,
    CONSTRAINT fk_authzcraft_membership_member_tenant FOREIGN KEY (member_principal_id, tenant_key)
        REFERENCES authzcraft_principal (id, tenant_key) ON DELETE RESTRICT,
    CONSTRAINT ck_authzcraft_membership_container_kind CHECK (container_kind IN ('ROLE', 'GROUP')),
    CONSTRAINT ck_authzcraft_membership_member_kind CHECK (member_kind = 'USER'),
    CONSTRAINT ck_authzcraft_membership_period CHECK (valid_until IS NULL OR valid_until > valid_from),
    CONSTRAINT ck_authzcraft_membership_not_self CHECK (container_principal_id <> member_principal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='主体成员表，从回路 1 业务侧权威系统同步用户在角色或主体组中的有效成员关系';

-- =============================================================================
-- 数据目录域·稳定身份 Data catalog domain: stable identity (3 张表)
-- =============================================================================

CREATE TABLE authzcraft_relation_resource (
    id BIGINT NOT NULL,
    tenant_key VARCHAR(64) NOT NULL,
    app_key VARCHAR(64) NOT NULL,
    resource_key VARCHAR(128) NOT NULL COMMENT '面向策略的稳定键；策略绑定到该身份',
    display_name VARCHAR(160) NOT NULL,
    namespace_name VARCHAR(128) NOT NULL,
    physical_name VARCHAR(128) NOT NULL,
    relation_kind VARCHAR(16) NOT NULL COMMENT 'TABLE表/VIEW视图',
    protection_mode VARCHAR(16) NOT NULL DEFAULT 'SHADOW' COMMENT 'ENFORCE强制/SHADOW影子/DISABLED禁用',
    structure_digest CHAR(64) NOT NULL COMMENT '当前字段与键结构的摘要',
    lifecycle_state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(500) DEFAULT NULL,
    created_by VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    record_version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_authzcraft_relation_key (tenant_key, app_key, resource_key),
    UNIQUE KEY uk_authzcraft_relation_scope_ref (id, tenant_key, app_key),
    UNIQUE KEY uk_authzcraft_relation_physical (tenant_key, app_key, namespace_name, physical_name),
    KEY idx_authzcraft_relation_protection (tenant_key, app_key, protection_mode, lifecycle_state),
    CONSTRAINT ck_authzcraft_relation_kind CHECK (relation_kind IN ('TABLE', 'VIEW')),
    CONSTRAINT ck_authzcraft_relation_protection CHECK (protection_mode IN ('ENFORCE', 'SHADOW', 'DISABLED')),
    CONSTRAINT ck_authzcraft_relation_state CHECK (lifecycle_state IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='关系资源表，登记可实施数据权限控制的物理表或视图的稳定身份';

CREATE TABLE authzcraft_resource_field (
    id BIGINT NOT NULL,
    relation_resource_id BIGINT NOT NULL,
    field_key VARCHAR(128) NOT NULL COMMENT '面向策略的稳定字段键',
    display_name VARCHAR(160) NOT NULL,
    physical_name VARCHAR(128) NOT NULL,
    ordinal_position INT NOT NULL,
    logical_type VARCHAR(24) NOT NULL COMMENT 'STRING字符串/INTEGER整数/DECIMAL小数/BOOLEAN布尔/DATE日期/DATETIME日期时间/UUID通用唯一标识/JSON对象',
    native_type VARCHAR(128) NOT NULL,
    nullable_flag TINYINT NOT NULL DEFAULT 1,
    primary_key_ordinal INT DEFAULT NULL,
    filterable_flag TINYINT NOT NULL DEFAULT 0,
    joinable_flag TINYINT NOT NULL DEFAULT 0,
    principal_ref_kind VARCHAR(24) DEFAULT NULL COMMENT '当本列存储需与同步后的主体域标识空间对齐的主体键时取值 USER用户/ORGANIZATION组织/POSITION岗位；普通数据列为空',
    sensitivity_level VARCHAR(16) NOT NULL DEFAULT 'INTERNAL',
    lifecycle_state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    active_marker TINYINT GENERATED ALWAYS AS (
        CASE WHEN lifecycle_state = 'ACTIVE' THEN 1 ELSE NULL END
    ) STORED COMMENT '退休字段不再占用其键或列名',
    created_by VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_authzcraft_field_key_active (relation_resource_id, field_key, active_marker),
    UNIQUE KEY uk_authzcraft_field_physical_active (relation_resource_id, physical_name, active_marker),
    UNIQUE KEY uk_authzcraft_field_ordinal (relation_resource_id, ordinal_position),
    KEY idx_authzcraft_field_capability (relation_resource_id, filterable_flag, joinable_flag),
    KEY idx_authzcraft_field_principal_ref (relation_resource_id, principal_ref_kind),
    CONSTRAINT fk_authzcraft_field_relation FOREIGN KEY (relation_resource_id)
        REFERENCES authzcraft_relation_resource (id) ON DELETE CASCADE,
    CONSTRAINT ck_authzcraft_field_ordinal CHECK (ordinal_position > 0),
    CONSTRAINT ck_authzcraft_field_logical_type CHECK (
        logical_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME', 'UUID', 'JSON')
    ),
    CONSTRAINT ck_authzcraft_field_pk_ordinal CHECK (primary_key_ordinal IS NULL OR primary_key_ordinal > 0),
    CONSTRAINT ck_authzcraft_field_flags CHECK (
        nullable_flag IN (0, 1) AND filterable_flag IN (0, 1) AND joinable_flag IN (0, 1)
    ),
    CONSTRAINT ck_authzcraft_field_state CHECK (lifecycle_state IN ('ACTIVE', 'RETIRED')),
    CONSTRAINT ck_authzcraft_field_sensitivity CHECK (
        sensitivity_level IN ('PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED')
    ),
    CONSTRAINT ck_authzcraft_field_principal_ref CHECK (
        principal_ref_kind IS NULL OR principal_ref_kind IN ('USER', 'ORGANIZATION', 'POSITION')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='资源字段表，登记关系资源字段及其过滤、关联、敏感级别和主体键引用能力';

CREATE TABLE authzcraft_access_path (
    id BIGINT NOT NULL,
    tenant_key VARCHAR(64) NOT NULL,
    app_key VARCHAR(64) NOT NULL,
    path_key VARCHAR(128) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    root_relation_id BIGINT NOT NULL,
    destination_relation_id BIGINT NOT NULL,
    execution_mode VARCHAR(24) NOT NULL COMMENT '路径编译成的 SQL 形态：EXISTS 为半连接，JOIN 会倍增行',
    traversal_steps JSON NOT NULL COMMENT '有序步骤 [{step_no, source_field_key, target_resource_key, target_field_key, cardinality_kind, traversal_direction}]；第 1 步的源资源为根，此后源为前一步的目标',
    lifecycle_state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_authzcraft_path_key (tenant_key, app_key, path_key),
    KEY idx_authzcraft_path_root (root_relation_id, lifecycle_state),
    CONSTRAINT fk_authzcraft_path_root FOREIGN KEY (root_relation_id, tenant_key, app_key)
        REFERENCES authzcraft_relation_resource (id, tenant_key, app_key) ON DELETE RESTRICT,
    CONSTRAINT fk_authzcraft_path_destination FOREIGN KEY (destination_relation_id, tenant_key, app_key)
        REFERENCES authzcraft_relation_resource (id, tenant_key, app_key) ON DELETE RESTRICT,
    CONSTRAINT ck_authzcraft_path_execution CHECK (execution_mode IN ('EXISTS', 'JOIN')),
    CONSTRAINT ck_authzcraft_path_steps CHECK (
        JSON_TYPE(traversal_steps) = 'ARRAY' AND JSON_LENGTH(traversal_steps) BETWEEN 1 AND 5
    ),
    CONSTRAINT ck_authzcraft_path_state CHECK (lifecycle_state IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='访问路径表，以有序步骤白名单定义行级过滤允许遍历的资源关联路径';

-- =============================================================================
-- 策略域 Policy domain (3 张表)
-- =============================================================================

CREATE TABLE authzcraft_rule_blueprint (
    id BIGINT NOT NULL,
    tenant_key VARCHAR(64) NOT NULL COMMENT '星号表示平台级蓝图；写入方必须显式传入 tenant_key，以免漏写时静默创建对全部租户可见的平台级蓝图',
    blueprint_key VARCHAR(96) NOT NULL,
    blueprint_kind VARCHAR(32) NOT NULL DEFAULT 'CUSTOM_BLUEPRINT' COMMENT 'STANDARD_BLUEPRINT标准蓝图/CUSTOM_BLUEPRINT自定义蓝图',
    blueprint_version INT NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    predicate_template JSON NOT NULL COMMENT '受限谓词 AST 模板',
    input_schema JSON NOT NULL COMMENT '带类型的蓝图输入 schema',
    lifecycle_state VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    active_marker TINYINT GENERATED ALWAYS AS (
        CASE WHEN lifecycle_state = 'PUBLISHED' THEN 1 ELSE NULL END
    ) STORED COMMENT '生效蓝图版本的唯一事实来源',
    content_digest CHAR(64) NOT NULL COMMENT 'predicate_template + input_schema 的哈希；定义何为一次新版本',
    published_by VARCHAR(128) DEFAULT NULL,
    published_at DATETIME(3) DEFAULT NULL,
    created_by VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_authzcraft_blueprint_version (tenant_key, blueprint_key, blueprint_version),
    UNIQUE KEY uk_authzcraft_blueprint_active (tenant_key, blueprint_key, active_marker),
    UNIQUE KEY uk_authzcraft_blueprint_digest (tenant_key, blueprint_key, content_digest),
    KEY idx_authzcraft_blueprint_lookup (tenant_key, blueprint_key, lifecycle_state, blueprint_version),
    CONSTRAINT ck_authzcraft_blueprint_version CHECK (blueprint_version > 0),
    CONSTRAINT ck_authzcraft_blueprint_kind CHECK (blueprint_kind IN ('STANDARD_BLUEPRINT', 'CUSTOM_BLUEPRINT')),
    CONSTRAINT ck_authzcraft_blueprint_state CHECK (lifecycle_state IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    CONSTRAINT ck_authzcraft_blueprint_publish CHECK (
        (lifecycle_state = 'DRAFT' AND published_at IS NULL)
        OR (lifecycle_state IN ('PUBLISHED', 'RETIRED') AND published_at IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='规则蓝图表，管理可复用的结构化权限条件模板及其输入规范';

CREATE TABLE authzcraft_access_policy (
    id BIGINT NOT NULL,
    tenant_key VARCHAR(64) NOT NULL,
    app_key VARCHAR(64) NOT NULL,
    policy_key VARCHAR(128) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    target_relation_id BIGINT NOT NULL COMMENT '稳定资源身份；永不版本化作用域',
    operation_code VARCHAR(24) NOT NULL COMMENT 'READ读/UPDATE更新/DELETE删除',
    effect_kind VARCHAR(8) NOT NULL COMMENT 'ALLOW允许/DENY拒绝；决策时拒绝始终优先',
    lifecycle_state VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    archived_by VARCHAR(128) DEFAULT NULL,
    archived_at DATETIME(3) DEFAULT NULL,
    record_version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_authzcraft_policy_key (tenant_key, app_key, policy_key),
    UNIQUE KEY uk_authzcraft_policy_scope_ref (id, tenant_key, app_key),
    KEY idx_authzcraft_policy_target (target_relation_id, operation_code, lifecycle_state),
    CONSTRAINT fk_authzcraft_policy_target FOREIGN KEY (target_relation_id, tenant_key, app_key)
        REFERENCES authzcraft_relation_resource (id, tenant_key, app_key) ON DELETE RESTRICT,
    CONSTRAINT ck_authzcraft_policy_operation CHECK (operation_code IN ('READ', 'UPDATE', 'DELETE')),
    CONSTRAINT ck_authzcraft_policy_effect CHECK (effect_kind IN ('ALLOW', 'DENY')),
    CONSTRAINT ck_authzcraft_policy_state CHECK (lifecycle_state IN ('DRAFT', 'ACTIVE', 'SUSPENDED', 'ARCHIVED')),
    CONSTRAINT ck_authzcraft_policy_archive CHECK (
        (lifecycle_state = 'ARCHIVED' AND archived_at IS NOT NULL)
        OR (lifecycle_state <> 'ARCHIVED' AND archived_at IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='访问策略表，管理行级数据访问策略的稳定标识、目标资源和生命周期';

CREATE TABLE authzcraft_policy_revision (
    id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    revision_no INT NOT NULL,
    blueprint_id BIGINT DEFAULT NULL COMMENT '非空时 predicate_ast 必须由该蓝图实例化；为空表示自由形式谓词 AST',
    predicate_ast JSON NOT NULL COMMENT '经过校验的引擎无关谓词 AST',
    argument_schema JSON NOT NULL COMMENT '带类型的授权参数 schema',
    attribute_references JSON NOT NULL COMMENT '谓词读取的主体事实；全部在主体域内解析',
    content_digest CHAR(64) NOT NULL COMMENT 'predicate_ast + argument_schema + attribute_references 的哈希；定义何为一次新修订',
    revision_state VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'ACTIVE生效/RETIRED退休 版本不可变（由应用层保证）；数据库不阻止对 predicate_ast 的 UPDATE',
    active_marker TINYINT GENERATED ALWAYS AS (
        CASE WHEN revision_state = 'ACTIVE' THEN 1 ELSE NULL END
    ) STORED COMMENT '生效版本的唯一事实来源',
    change_summary VARCHAR(500) DEFAULT NULL,
    published_by VARCHAR(128) DEFAULT NULL,
    published_at DATETIME(3) DEFAULT NULL,
    created_by VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_authzcraft_policy_revision_no (policy_id, revision_no),
    UNIQUE KEY uk_authzcraft_policy_revision_digest (policy_id, content_digest),
    UNIQUE KEY uk_authzcraft_policy_active (policy_id, active_marker),
    CONSTRAINT fk_authzcraft_policy_revision_policy FOREIGN KEY (policy_id)
        REFERENCES authzcraft_access_policy (id) ON DELETE RESTRICT,
    CONSTRAINT fk_authzcraft_policy_revision_blueprint FOREIGN KEY (blueprint_id)
        REFERENCES authzcraft_rule_blueprint (id) ON DELETE RESTRICT,
    CONSTRAINT ck_authzcraft_policy_revision_no CHECK (revision_no > 0),
    CONSTRAINT ck_authzcraft_policy_revision_state CHECK (revision_state IN ('DRAFT', 'ACTIVE', 'RETIRED')),
    CONSTRAINT ck_authzcraft_policy_revision_publish CHECK (
        (revision_state = 'DRAFT' AND published_at IS NULL)
        OR (revision_state IN ('ACTIVE', 'RETIRED') AND published_at IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='策略版本表，保存访问策略不可变的结构化条件、参数规范和发布版本';

-- =============================================================================
-- 授权域 Grant domain (2 张表)
-- =============================================================================

CREATE TABLE authzcraft_access_grant (
    id BIGINT NOT NULL,
    tenant_key VARCHAR(64) NOT NULL,
    app_key VARCHAR(64) NOT NULL,
    grant_key VARCHAR(128) NOT NULL,
    principal_id BIGINT NOT NULL COMMENT '对主体注册表的强引用',
    policy_id BIGINT NOT NULL,
    valid_from DATETIME(3) NOT NULL,
    valid_until DATETIME(3) DEFAULT NULL,
    open_marker TINYINT GENERATED ALWAYS AS (
        CASE WHEN valid_until IS NULL AND lifecycle_state NOT IN ('REVOKED', 'EXPIRED') THEN 1 ELSE NULL END
    ) STORED COMMENT '防止同一对（主体,策略）出现重叠的开放授权；REVOKED撤销/EXPIRED过期会释放槽位，使同一（主体,策略）可被重新授权',
    lifecycle_state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    grant_source VARCHAR(24) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL手动/IMPORT导入/API接口/AI_ASSISTED智能辅助',
    reason VARCHAR(500) DEFAULT NULL,
    revoked_by VARCHAR(128) DEFAULT NULL,
    revoked_at DATETIME(3) DEFAULT NULL,
    created_by VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    record_version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_authzcraft_grant_key (tenant_key, app_key, grant_key),
    UNIQUE KEY uk_authzcraft_grant_open (principal_id, policy_id, open_marker),
    KEY idx_authzcraft_grant_principal (principal_id, lifecycle_state, valid_until),
    KEY idx_authzcraft_grant_policy (policy_id, lifecycle_state, valid_until),
    CONSTRAINT fk_authzcraft_grant_principal FOREIGN KEY (principal_id, tenant_key)
        REFERENCES authzcraft_principal (id, tenant_key) ON DELETE RESTRICT,
    CONSTRAINT fk_authzcraft_grant_policy FOREIGN KEY (policy_id, tenant_key, app_key)
        REFERENCES authzcraft_access_policy (id, tenant_key, app_key) ON DELETE RESTRICT,
    CONSTRAINT ck_authzcraft_grant_state CHECK (lifecycle_state IN ('ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_authzcraft_grant_source CHECK (grant_source IN ('MANUAL', 'IMPORT', 'API', 'AI_ASSISTED')),
    CONSTRAINT ck_authzcraft_grant_period CHECK (valid_until IS NULL OR valid_until > valid_from),
    CONSTRAINT ck_authzcraft_grant_revoke CHECK (
        (lifecycle_state = 'REVOKED' AND revoked_at IS NOT NULL)
        OR (lifecycle_state <> 'REVOKED' AND revoked_at IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='访问授权表，将注册主体绑定到访问策略并管理生效区间';

CREATE TABLE authzcraft_grant_argument (
    id BIGINT NOT NULL,
    access_grant_id BIGINT NOT NULL,
    argument_key VARCHAR(96) NOT NULL COMMENT '决策时按 ACTIVE 策略版本的 argument_schema 校验；schema 变更可能遗留静默悬空的旧参数键',
    value_kind VARCHAR(16) NOT NULL COMMENT 'STRING字符串/INTEGER整数/DECIMAL小数/BOOLEAN布尔/DATE日期/DATETIME日期时间/STRING_SET字符串集合/NUMBER_SET数字集合',
    argument_value JSON NOT NULL COMMENT '应用层必须保持 JSON 结构与 value_kind 一致；数据库无法校验自由形式的 JSON 载荷',
    sensitive_flag TINYINT NOT NULL DEFAULT 0,
    created_by VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_by VARCHAR(128) NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_authzcraft_grant_argument_key (access_grant_id, argument_key),
    CONSTRAINT fk_authzcraft_grant_argument_grant FOREIGN KEY (access_grant_id)
        REFERENCES authzcraft_access_grant (id) ON DELETE CASCADE,
    CONSTRAINT ck_authzcraft_grant_argument_kind CHECK (
        value_kind IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE', 'DATETIME', 'STRING_SET', 'NUMBER_SET')
    ),
    CONSTRAINT ck_authzcraft_grant_argument_sensitive CHECK (sensitive_flag IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='授权参数表，保存访问授权向策略条件提供的类型化参数值';

-- =============================================================================
-- 审计域 Audit domain (1 张表)
-- 有意省略外键，使该表保持只追加、可按时间分区，并与治理对象生命周期解耦。
-- =============================================================================

CREATE TABLE authzcraft_decision_record (
    id BIGINT NOT NULL,
    decided_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '主键的一部分以支持范围分区',
    decision_key CHAR(36) NOT NULL COMMENT '对外可见的决策 UUID',
    request_key VARCHAR(128) NOT NULL COMMENT '跨回路关联键；回路 1 功能权限审计记录相同值',
    tenant_key VARCHAR(64) NOT NULL,
    app_key VARCHAR(64) NOT NULL,
    requester_kind VARCHAR(24) NOT NULL COMMENT 'USER用户/SERVICE服务；调用方身份，可能不存在于主体注册表',
    requester_key VARCHAR(160) NOT NULL,
    operation_code VARCHAR(24) NOT NULL,
    target_resource_key VARCHAR(128) NOT NULL COMMENT '快照值；非外键',
    plan_decision VARCHAR(16) NOT NULL COMMENT 'ALLOW_ALL全部允许/DENY_ALL全部拒绝/FILTER过滤/INDETERMINATE不确定',
    planner_kind VARCHAR(24) NOT NULL COMMENT 'NATIVE原生/CERBOS/OPA',
    plan_digest CHAR(64) DEFAULT NULL,
    attribute_digest CHAR(64) DEFAULT NULL COMMENT '不可逆摘要；不含属性明文',
    failure_code VARCHAR(64) DEFAULT NULL,
    planning_cost_ms INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id, decided_at),
    UNIQUE KEY uk_authzcraft_decision_key (decision_key, decided_at),
    KEY idx_authzcraft_decision_request (tenant_key, app_key, request_key, decided_at),
    KEY idx_authzcraft_decision_requester (tenant_key, app_key, requester_key, decided_at),
    KEY idx_authzcraft_decision_resource (tenant_key, app_key, target_resource_key, operation_code, decided_at),
    KEY idx_authzcraft_decision_result (tenant_key, app_key, plan_decision, decided_at),
    CONSTRAINT ck_authzcraft_decision_requester CHECK (requester_kind IN ('USER', 'SERVICE')),
    CONSTRAINT ck_authzcraft_decision_operation CHECK (operation_code IN ('READ', 'UPDATE', 'DELETE')),
    CONSTRAINT ck_authzcraft_decision_result CHECK (
        plan_decision IN ('ALLOW_ALL', 'DENY_ALL', 'FILTER', 'INDETERMINATE')
    ),
    CONSTRAINT ck_authzcraft_decision_planner CHECK (planner_kind IN ('NATIVE', 'CERBOS', 'OPA')),
    CONSTRAINT ck_authzcraft_decision_cost CHECK (planning_cost_ms >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='决策记录表，以追加方式记录每次行级过滤规划的请求、结果和性能信息';
