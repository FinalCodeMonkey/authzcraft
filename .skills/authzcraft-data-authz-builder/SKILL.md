---
name: authzcraft-data-authz-builder
description: '在 AuthzCraft 回路 2 中心产品上完成数据权限（行数据权限）配置：登记物理表资源、配置行过滤策略、绑定主体授权、模拟验证。Use when 用户需要配置数据权限、行数据范围、谁能看哪些行、数据过滤、物理表资源登记、或回路 1 RBAC 构建完成后需要配置数据范围。'
argument-hint: '描述数据权限需求，例如：管理员可查看全部商机，销售经理只能查看本部门及下级部门的商机。'
---

# AuthzCraft 数据权限配置

在 AuthzCraft 回路 2 中心产品上完成数据权限（行数据权限）配置。本 skill 只负责回路 2 数据权限配置，不负责回路 1 功能/字段权限（那是 `rbac-system-builder` 的职责）。

本 skill 面向 AuthzCraft 中心产品（`authzcraft` Maven 多模块工程），通过 REST API / MCP 工具完成治理期配置。所有接口统一返回 `ApiResponse<T>`，复杂查询使用 `POST /search`，部分更新使用 `POST /{id}/update`，不使用 PATCH。所有 Snowflake ID 以字符串传输。

## 与 rbac-system-builder 的关系

- `rbac-system-builder`：回路 1 功能/字段权限 + 角色/用户组/成员关系管理
- 本 skill：回路 2 数据权限配置（目录 + 策略 + 授权 + 模拟）
- 两者并存，通过"衔接指引"自动切换，用户无需手动指定

## 与 fcm-permission-integration 的关系

- `fcm-permission-integration`：面向 PSP（存量产品）
- 本 skill：面向 AuthzCraft（新产品）
- 两者并存，不互相替代

## 权威源边界（NON-NEGOTIABLE）

物理存储位置 ≠ 权威源。本 skill 配置数据权限时，必须遵守以下权威源边界：

| 主体类型 | 权威源 | AuthzCraft 的角色 |
|---|---|---|
| 用户、组织、岗位 | HR / 统一身份 | 投影副本（从 HR 同步来） |
| 角色、用户组 | 回路 1 业务侧 | 存储载体（回路 1 通过 API 写入） |

AuthzCraft 不创造任何身份，只投影身份。本 skill 在授权环节需要"查主体 principalId"时，只读查询，不创建/修改主体。

### 回路 1 双模式兼容边界

当目标应用由 `rbac-system-builder` 生成或改造时，必须兼容回路 1 的双模式基线：

- 默认 `AUTHZCRAFT` 模式：角色、用户组、成员关系的权威读写在 AuthzCraft 主体域；业务应用本地只以 `role_code` 绑定功能/字段权限。
- `LOCAL` 模式：业务应用可以保留并读取本地 `xx_users`、`xx_roles`、`xx_user_roles`、`xx_groups`、`xx_user_groups` 表，以保证配置切换后可运行；功能/字段权限仍通过 `role_code` 绑定，不使用旧 `role_id` 权限绑定表。
- 本 skill 做数据权限授权时只读查询 AuthzCraft 主体域中的 `principalId`。如果目标应用启用 `LOCAL` 且本地角色/用户组要参与数据策略，必须先确认这些本地主体和成员关系已投影/同步到 AuthzCraft PIP；本 skill 不创建、不修改这些主体，也不要求删除本地 local 表。
- 生成数据权限缺口清单时，不得把 `xx_users`、`xx_roles`、`xx_user_roles`、`xx_groups`、`xx_user_groups` 判定为应删除对象；它们是回路 1 双模式 DDL 的保留表。数据权限规则仍不得写入这些表。

## 能力范围

本 skill 覆盖五块能力：

1. **主体查询（只读）**：授权时查主体 principalId
  - 首选 `POST /authzcraft/api/v1/principals/search`（按 `tenantKey/principalKind/principalKey/keyword/lifecycleState` 查 USER/ORGANIZATION/POSITION/ROLE/GROUP，返回可用于授权的主体 `id`）
  - `POST /authzcraft/api/v1/principals/users/search`（用户投影查询，支持 userId/staffNo/departmentCode/postCode/keyword/limit/offset）
  - `POST /authzcraft/api/v1/principals/organizations/search`（组织投影查询）
  - `POST /authzcraft/api/v1/principals/positions/search`（岗位投影查询）
  - `POST /authzcraft/api/v1/principals/roles/search`、`POST /authzcraft/api/v1/principals/groups/search`（回路 1 主体域的角色/用户组只读查询；若用于授权，取响应中的 `principalId`）

2. **目录治理**：登记物理表资源
   - `POST /authzcraft/api/v1/catalog/relation-resources`（登记表/视图）
  - `POST /authzcraft/api/v1/catalog/relation-resources/search`（查询表/视图目录）
  - `POST /authzcraft/api/v1/catalog/relation-resources/{id}/update`（更新目录元数据、生命周期和保护模式）
   - `POST /authzcraft/api/v1/catalog/relation-resources/{id}/fields/import`（导入字段）
  - `POST /authzcraft/api/v1/catalog/relation-resources/{id}/fields/search`（查询资源字段，供字段权限页和数据权限向导使用）
  - `POST /authzcraft/api/v1/catalog/relation-resources/{id}/fields/{fieldId}/update`（更新字段过滤/关联/敏感标记/字段安全要求）
   - `POST /authzcraft/api/v1/catalog/access-paths`（登记访问路径）
  - `POST /authzcraft/api/v1/catalog/access-paths/search`（查询访问路径）
  - `POST /authzcraft/api/v1/catalog/access-paths/{id}/update`（更新访问路径）

3. **策略治理**：配置行过滤规则
   - `POST /authzcraft/api/v1/policies/rule-blueprints`（规则蓝图）
  - `POST /authzcraft/api/v1/policies/rule-blueprints/search`（查询规则蓝图）
  - `POST /authzcraft/api/v1/policies/rule-blueprints/{id}/update`（更新 DRAFT 蓝图）
  - `POST /authzcraft/api/v1/policies/rule-blueprints/{id}/publish`（发布蓝图）
  - `POST /authzcraft/api/v1/policies/rule-blueprints/{id}/retire`（退休蓝图）
   - `POST /authzcraft/api/v1/policies/access-policies`（访问策略）
  - `POST /authzcraft/api/v1/policies/access-policies/search`（查询访问策略）
  - `POST /authzcraft/api/v1/policies/access-policies/{id}/update`（更新访问策略）
  - `POST /authzcraft/api/v1/policies/access-policies/{id}/activate`（激活访问策略）
   - `POST /authzcraft/api/v1/policies/access-policies/{id}/revisions`（策略修订）
  - `POST /authzcraft/api/v1/policies/access-policies/{id}/revisions/search`（查询策略修订）
  - `POST /authzcraft/api/v1/policies/revisions/{id}/activate`（激活策略修订）
  - `POST /authzcraft/api/v1/policies/revisions/{id}/retire`（退休策略修订）

4. **授权治理**：主体绑定策略 + 授权参数
   - `POST /authzcraft/api/v1/grants/access-grants`（创建授权）
  - `POST /authzcraft/api/v1/grants/access-grants/search`（查询授权）
  - `POST /authzcraft/api/v1/grants/access-grants/{id}/update`（更新有效期、来源、原因）
  - `POST /authzcraft/api/v1/grants/access-grants/{id}/suspend`（停用授权）
  - `POST /authzcraft/api/v1/grants/access-grants/{id}/resume`（恢复授权）
  - `POST /authzcraft/api/v1/grants/access-grants/{id}/revoke`（作废授权）
  - `POST /authzcraft/api/v1/grants/access-grants/{id}/arguments/search`（查询授权参数）
   - `POST /authzcraft/api/v1/grants/access-grants/{id}/arguments/replace`（授权参数）

5. **计划、模拟与审计**：上线前断言 + 生产决策追踪
  - `POST /authzcraft/api/v1/data-authz/plans`（生产计划，记录决策审计）
   - `POST /authzcraft/api/v1/data-authz/simulations`（模拟 + 断言 + Explain）
  - `POST /authzcraft/api/v1/audit/decision-records/search`（查询生产决策记录）
  - `POST /authzcraft/api/v1/audit/decision-records/{decisionKey}`（按 decisionKey 查询最近一次生产决策）

**数据 PEP 覆盖 READ/UPDATE/DELETE**：数据权限的 `operationCode` 覆盖 `READ` / `UPDATE` / `DELETE` 三类。PEP 拦截器同时拦截 `Executor.query`（查询）与 `Executor.update`（更新/删除），按行过滤改写 SQL。语义：修改/删除的数据范围同样由回路 2 行过滤决定（如"只能修改/删除本人的数据"）。

## 核心处理逻辑

### 字段目录与回路 1 字段权限边界

`authzcraft_resource_field` 是字段目录权威源，不只服务行过滤，也为回路 1 字段权限提供字段固有安全要求。配置数据权限或导入字段目录时，必须同时维护以下字段治理属性：

| 属性 | 含义 | 用途 |
|---|---|---|
| `display_name` | 字段业务中文名 | 前端表头、字段授权页字段名称、数据权限向导 |
| `filterable` / 等价标记 | 是否可作为行过滤字段 | 数据权限模板的数据归属字段候选 |
| `sensitivity_level` | 敏感级别/分类 | 审计、目录治理、辅助风险提示 |
| `security_requirement` | 字段固有安全要求：PLAIN/MASKED/HIDDEN | 回路 1 字段权限默认访问级别与可选 access_level 生成依据 |

`security_requirement` 是字段固有属性，MUST 在目录治理/字段导入阶段设置：PLAIN 表示默认明文可读，MASKED 表示默认脱敏可见，HIDDEN 表示默认不可见。它不是授权绑定，不随角色/用户组变化；运行期 RBAC 管理页不得暴露修改入口。若业务方要调整字段固有安全要求，必须作为目录治理变更处理，并说明会影响所有未显式绑定字段权限的主体。

回路 1 `rbac-system-builder` 负责根据该目录属性生成/维护字段权限 option：`NONE/READ/PLAIN_READ/WRITE` 等 access_level 是授权可变属性，角色/用户组绑定在业务系统字段授权表中完成。缺少绑定记录时回路 1 必须回退到 `security_requirement` 默认访问级别；如需让 MASKED 字段对某角色/用户组完全不返回，应显式绑定 `NONE`，不能通过删除绑定表达。

字段中文名、表中文名、模板中文名等治理数据写入时必须使用 UTF-8 without BOM。Windows PowerShell 内联 `python -c` 或 here-string 写中文到 MySQL 容易把中文写成 `?`，应改用 UTF-8 `.py`/JSON 文件或正常 REST API 提交。

### 资源目录保护模式

关系资源默认 `protectionMode=SHADOW`，支持 `ENFORCE`、`SHADOW`、`DISABLED` 三类。配置完成前不得直接切换到 `ENFORCE`；必须先在 `SHADOW` 下完成策略、授权、模拟验证和生产审计观察。运行时语义如下：

| `protectionMode` | PDP 语义 | PEP 要求 |
|---|---|---|
| `DISABLED` | 直接返回 `ALLOW_ALL` | 不改写 SQL |
| `SHADOW` | 正常生成计划并记录决策 | 可观测优先，业务侧可按配置决定是否强执行 |
| `ENFORCE` | 正常生成计划并记录决策 | 必须按 `ALLOW_ALL`/`DENY_ALL`/`FILTER` 强制执行 |

### 策略治理模型

策略域三张表：`authzcraft_rule_blueprint`（规则蓝图）、`authzcraft_access_policy`（访问策略）、`authzcraft_policy_revision`（策略修订）。PAP 只保存受限 Predicate AST、输入 Schema、授权参数 Schema 和主体属性引用声明，不保存 SpEL、SQL 片段或任意脚本字符串。

**规则蓝图 `predicate_template`** 保存模板骨架（占位符表达字段、主体属性、授权参数）：

```json
{
  "operator": "AND",
  "operands": [
    {
      "operator": "OR",
      "operands": [
        {"operator": "EQ", "left": {"fieldInput": "ownerField"}, "right": {"attributeInput": "currentUser"}},
        {"operator": "IN", "left": {"fieldInput": "departmentField"}, "right": {"attributeInput": "managedDepartments"}}
      ]
    },
    {"operator": "IN", "left": {"fieldInput": "stageField"}, "right": {"argumentInput": "allowedStages"}}
  ]
}
```

**规则蓝图 `input_schema`** 声明占位如何填写：

```json
{
  "fieldInputs": [
    {"key": "ownerField", "label": "负责人字段", "candidate": "FILTERABLE_FIELD", "required": true},
    {"key": "departmentField", "label": "部门字段", "candidate": "FILTERABLE_FIELD", "required": true},
    {"key": "stageField", "label": "阶段字段", "candidate": "FILTERABLE_FIELD", "required": true}
  ],
  "attributeInputs": [
    {"key": "currentUser", "attributeKey": "currentUserId", "valueKind": "STRING"},
    {"key": "managedDepartments", "attributeKey": "managedDepartmentCodes", "valueKind": "STRING_SET"}
  ],
  "argumentInputs": [
    {"key": "allowedStages", "argumentKey": "allowedStageCodes", "valueKind": "STRING_SET", "required": true}
  ]
}
```

**策略修订 `predicate_ast`** 保存已绑定字段的可执行条件：

```json
{
  "operator": "AND",
  "operands": [
    {
      "operator": "OR",
      "operands": [
        {"operator": "EQ", "left": {"fieldKey": "owner_id"}, "right": {"attributeKey": "currentUserId"}},
        {"operator": "IN", "left": {"fieldKey": "dept_code"}, "right": {"attributeKey": "managedDepartmentCodes"}}
      ]
    },
    {"operator": "IN", "left": {"fieldKey": "stage_code"}, "right": {"argumentKey": "allowedStageCodes"}}
  ]
}
```

**策略修订 `argument_schema`** 声明授权侧必须填写的参数：

```json
{
  "allowedStageCodes": {
    "valueKind": "STRING_SET",
    "required": true,
    "label": "允许访问的商机阶段"
  }
}
```

**策略修订 `attribute_references`** 声明运行时需要的主体上下文：

```json
[
  {"attributeKey": "currentUserId", "valueKind": "STRING", "resolveKind": "REQUESTER_USER_ID", "required": true},
  {"attributeKey": "managedDepartmentCodes", "valueKind": "STRING_SET", "resolveKind": "MANAGED_DEPARTMENT_CODES", "required": false}
]
```

**策略修订创建三种输入路径**（PAP 应用层统一编译为 `predicate_ast` 入库）：

1. `STANDARD_BLUEPRINT` 蓝图绑定：传 `blueprintId` + `templateBinding.conditions[]`，每项含 `fieldKey/operator/valueType/value`，可选 `valueKind/accessPathKey`，支持 `AND/OR` 组合。**空 conditions 数组允许**，编译为 `{"operator":"TRUE"}` 节点，对应"全量数据/无额外过滤"语义（ALL_DATA 模板）。不使用占位符，`predicate_template`/`input_schema` 为 `{}`。
2. `CUSTOM_BLUEPRINT` 蓝图绑定：传 `blueprintId` + `templateBinding.arguments`（占位符替换值），替换蓝图 `predicateTemplate` 中的 `FIELD_PLACEHOLDER`、`ATTRIBUTE_PLACEHOLDER`、`ARGUMENT_PLACEHOLDER`、`BINDING_PLACEHOLDER`、`LITERAL_PLACEHOLDER` 或 `${name}` 占位符。若 `templateBinding` 携带 `accessPathKey`，PAP 在占位符替换后用 `EXISTS_PATH` 包裹编译后的谓词。蓝图 `input_schema` 声明占位符如何填写（`fieldInputs`/`attributeInputs`/`argumentInputs`），前端可读取 `input_schema` 动态渲染配置表单。
3. `CUSTOM_AST` 自定义路径：不传 `blueprintId`，直接携带 `customAst`，系统包装为 `CUSTOM_AST` 谓词节点。用于框架六类受控解析器无法表达、需要用户自定义函数计算过滤条件的场景。

策略保存规则：

- 访问策略必须绑定单一目标 `resourceKey`、单一 `operationCode`（READ/UPDATE/DELETE）和明确 `effect_kind`（ALLOW/DENY）。
- 访问策略 `effect_kind` 只允许 `ALLOW`/`DENY`，`operationCode` 只允许 `READ`/`UPDATE`/`DELETE`。
- 规则蓝图创建后为 `DRAFT`，只有 `DRAFT` 蓝图可更新并发布；发布时同 key 已发布版本会被退休。
- 策略修订引用蓝图时，`blueprintId` 必须指向 `PUBLISHED` 规则蓝图。
- 访问策略必须先 `activate` 为 `ACTIVE`，才能激活其策略修订。
- 同一访问策略最终只能有一个 ACTIVE 修订；激活新修订时旧 ACTIVE 修订退出生效。
- 策略修订激活前必须完成字段、类型、操作符、访问路径、授权参数 Schema 和主体属性引用校验。
- **数据权限模板应创建独立规则蓝图**：业务侧数据权限向导的每个通用模板（如"我的数据""我管理部门的数据""指定部门数据""全部数据"）MUST 各自对应一条 `authzcraft_rule_blueprint` 记录（`blueprint_key` 用裸模板名如 `MY_DATA`/`ALL_DATA`，`display_name` 用模板业务名如"我的数据模板"），策略修订关联对应模板蓝图，不得多个模板共用单一"通用模板"蓝图——否则前端"数据规则模板"列无法区分模板。`access-policies/search` 响应 MUST 通过 ACTIVE 修订关联返回 `ruleBlueprintDisplayName`（蓝图 `display_name`），供前端列表展示"数据规则模板"。

**规则蓝图按 `blueprint_kind` 分两类**，判定边界为**是否使用占位符骨架**：

| `blueprint_kind` | 判定标准 | `predicate_template` | `input_schema` | 编译方式 |
|---|---|---|---|---|
| `STANDARD_BLUEPRINT` | 不使用占位符 | `{}`（空） | `{}`（空） | `templateBinding.conditions[]` 结构化条件（EQ/IN/AND/OR/EXISTS_PATH），空 conditions 编译为 `TRUE` |
| `CUSTOM_BLUEPRINT` | 使用占位符骨架 | 含 `FIELD_PLACEHOLDER`/`ATTRIBUTE_PLACEHOLDER`/`ARGUMENT_PLACEHOLDER` 等占位符 | 声明占位符如何填写（`fieldInputs`/`attributeInputs`/`argumentInputs`） | `templateBinding.arguments` 替换占位符，替换后若携带 `accessPathKey` 则用 `EXISTS_PATH` 包裹 |

**五项数据范围规则的蓝图类型分配**：

| 模板 | `blueprint_key` | `blueprint_kind` | 占位符 | 说明 |
|---|---|---|---|---|
| 全部数据 | `ALL_DATA` | `STANDARD_BLUEPRINT` | 无 | 无条件放行（`TRUE`），不需要占位符 |
| 我的数据 | `MY_DATA` | `CUSTOM_BLUEPRINT` | `anchorField`(FIELD) + `currentUserId`(ATTRIBUTE) | 字段=当前用户 |
| 我管理部门的数据 | `MANAGED_DEPARTMENTS` | `CUSTOM_BLUEPRINT` | `anchorField`(FIELD) + `managedDepartmentCodes`(ATTRIBUTE) | 字段 IN 当前用户管理部门 |
| 指定部门的数据 | `SPECIFIED_DEPARTMENTS` | `CUSTOM_BLUEPRINT` | `anchorField`(FIELD) + `specifiedDepartmentCodes`(ARGUMENT) | 字段 IN 授权参数指定部门 |
| 分享某人的数据 | `PERSON_SHARE` | `CUSTOM_BLUEPRINT` | `anchorField`(FIELD) + `sharedOwnerUserKey`(ARGUMENT) | 字段=授权参数指定归属人 |

**CUSTOM_BLUEPRINT 三层拆分模型**（蓝图骨架 → 修订绑定 → 授权参数）：

| 层 | 表 | 职责 | 示例（SPECIFIED_DEPARTMENTS） |
|---|---|---|---|
| `rule_blueprint` | `authzcraft_rule_blueprint` | 定义占位符骨架 + `input_schema` 声明占位符如何填写 | `predicate_template`: `{"operator":"IN","left":{"kind":"FIELD_PLACEHOLDER","name":"anchorField"},"right":{"kind":"ARGUMENT_PLACEHOLDER","name":"specifiedDepartmentCodes"}}` |
| `policy_revision` | `authzcraft_policy_revision` | 绑定到具体数据表：占位符替换值通过 `templateBinding.arguments` 传入，PAP 编译为 `predicate_ast` | `templateBinding`: `{"arguments":{"anchorField":"dept_code","specifiedDepartmentCodes":"specifiedDepartmentCodes"}}` |
| `grant_argument` | `authzcraft_grant_argument` | 授权时指定运行时参数值 | `argument_key=specifiedDepartmentCodes`, `argument_value=["D001","D002"]` |

**`input_schema` 前端动态渲染**：`CUSTOM_BLUEPRINT` 蓝图的 `input_schema` 声明了配置表单需要哪些输入（`fieldInputs`/`attributeInputs`/`argumentInputs`），前端 SHOULD 从 PAP `POST /policies/rule-blueprints/search` 查询蓝图 `input_schema`，根据 `fieldInputs` 判断是否显示"数据归属字段"选择器、根据 `argumentInputs` 判断是否显示"部门范围"/"指定部门"/"数据归属人"等参数输入控件。`STANDARD_BLUEPRINT` 的 `input_schema` 为 `{}`，前端不显示额外配置字段。

**过滤条件值来源 5 类**（`STANDARD_BLUEPRINT` 的 `conditions[].valueType` 和 `CUSTOM_BLUEPRINT` 的占位符类型均覆盖前 4 类，第 5 类需要 `CUSTOM_AST` 路径）：

| # | 值来源 | `conditions[].valueType` | 占位符类型 | 适用路径 |
|---|---|---|---|---|
| ① | 固定值 | `LITERAL` | `LITERAL_PLACEHOLDER` | STANDARD / CUSTOM |
| ② | 当前用户静态属性 | `ATTRIBUTE` | `ATTRIBUTE_PLACEHOLDER` | STANDARD / CUSTOM |
| ③ | 当前用户计算/扩展属性 | `ATTRIBUTE` | `ATTRIBUTE_PLACEHOLDER` | STANDARD / CUSTOM |
| ④ | 当前数据表其它字段值 | `FIELD`（编译器待支持） | `FIELD_PLACEHOLDER` | STANDARD / CUSTOM |
| ⑤ | 用户自定义函数计算结果 | 无法用 valueType 表达 | — | **CUSTOM_AST** |

两个类型都关联到 `authzcraft_rule_blueprint.blueprint_kind`，前端"数据规则模板"列的 `ruleBlueprintDisplayName` 与 `blueprint_kind` 无关，只取 `display_name`。

### 授权参数模型（valueKind 八类枚举）

`STRING`、`INTEGER`、`DECIMAL`、`BOOLEAN`、`DATE`、`DATETIME`、`STRING_SET`、`NUMBER_SET`

授权参数 `GrantArgument` 字段：`argumentKey`（必须出现在生效策略修订的 `argument_schema` 中）、`valueKind`、`argumentValue`（JSON 值）、`sensitiveFlag`（敏感值不得进入日志/Explain 明文）。

授权生命周期规则：

- 创建授权前必须确认主体存在、策略属于同一 `tenantKey/appKey` 且策略已 `ACTIVE`。
- `grantSource` 只允许 `MANUAL`、`IMPORT`、`API`、`AI_ASSISTED`；业务侧数据权限向导生成的授权 SHOULD 使用 `AI_ASSISTED`。
- `validUntil` 必须晚于 `validFrom`；过期授权不得恢复。
- `ACTIVE` 授权可 `suspend` 为 `SUSPENDED`，`SUSPENDED` 授权在策略仍为 `ACTIVE` 且未过期时可 `resume`。
- `revoke` 会进入 `REVOKED` 并记录作废人/时间；`REVOKED`、`EXPIRED` 授权不可再修改。
- 编辑数据权限规则时，若新配置对应的 policy/grant 与旧 grant 不同，旧 grant 必须显式 `revoke`，不能只创建新授权后遗留旧 ACTIVE 授权。

授权参数运行时校验规则：

- 授权参数必须全部是合法 JSON 值，且 `valueKind` 只能取八类枚举。
- PDP 命中授权后必须按生效修订的 `argument_schema` 校验：必填参数不得缺失，`valueKind` 必须一致，JSON shape 必须匹配（标量/集合/数字/布尔/日期时间），`sensitiveFlag` 必须符合 schema 要求。
- 禁止携带 `argument_schema` 未声明的授权参数；违反时返回 `INDETERMINATE + GRANT_ARGUMENT_INVALID`。

### 受控解析器（resolveKind 六类枚举）

| `resolveKind` | 输出属性示例 | 解析规则 |
|---|---|---|
| `REQUESTER_USER_ID` | `currentUserId` | 取请求主体 `requesterKey`，按 `authzcraft_principal_user.user_id` 校验或归一化 |
| `REQUESTER_DEPARTMENT_CODE` | `currentDepartmentCode` | 从 `authzcraft_principal_user.department_code` 读取主挂部门 |
| `REQUESTER_POSITION_CODE` | `currentPositionCode` | 从 `authzcraft_principal_user.post_code` 读取主挂岗位 |
| `MANAGED_DEPARTMENT_CODES` | `managedDepartmentCodes` | 默认查 `manage_user_id = 当前用户` 的组织节点，用组织闭包展开全部下级部门并去重；当授权参数携带 `departmentScopes/subDepartmentDepth` 时，必须按下方参数化规则解析 |
| `ROLE_KEYS` | `roleKeys` | 通过成员关系展开当前用户所属角色，读 `authzcraft_principal_role.role_code` |
| `GROUP_KEYS` | `groupKeys` | 通过成员关系展开当前用户所属主体组，读 `authzcraft_principal_group.group_code` |

`MANAGED_DEPARTMENT_CODES` 参数化解析规则（权威规则，业务侧不得另起口径）：

- `DIRECT_MANAGED`：查 `authzcraft_principal_organization.manage_user_id = requesterKey` 的组织节点，并通过 `authzcraft_organization_closure.depth = 0` 返回直接管理部门。
- `SUB_MANAGED`：以上述直接管理部门为祖先，通过 `authzcraft_organization_closure.depth > 0` 返回下级部门；`subDepartmentDepth=1/2/3` 时限制最大 depth，`ALL` 时不限制最大 depth。
- `PORTION_MANAGED`：查 `authzcraft_principal_organization.portion_manage_user_id = requesterKey` 的组织节点，并通过 `depth = 0` 返回分管部门。
- 未携带授权参数或 `departmentScopes=["ALL_MANAGED"]` 时保持历史兼容：返回 `manage_user_id = requesterKey` 的直接管理部门及全部下级部门。
- PIP 解析必须基于主体域投影表和组织闭包表完成，不能在业务系统、前端或回路 1 RBAC 表中自行计算部门集合。

### PDP 计划生成与 PEP 执行语义

- PDP 候选主体集合必须包含请求用户主体，以及通过属性快照解析出的当前部门、岗位、角色和用户组主体；角色/用户组来自 `ROLE_KEYS`/`GROUP_KEYS` 成员关系展开。
- 资源未找到时返回 `INDETERMINATE + RESOURCE_UNAVAILABLE`；资源 `DISABLED` 时返回 `ALLOW_ALL`。
- 无已发布策略、无有效授权、无 ALLOW 谓词时返回 `DENY_ALL`。
- 同一请求命中 ALLOW 与 DENY 时，DENY 优先，最终返回 `DENY_ALL`，不返回过滤谓词和绑定参数。
- 多个 ALLOW 谓词使用 `OR` 合并；合并结果为 `TRUE` 时返回 `ALLOW_ALL`，否则返回 `FILTER` 并附带 predicate、bindings 和 requiredAccessPathKeys。
- `POST /data-authz/plans` 是生产计划接口，会写入 `authzcraft_decision_record`；`POST /data-authz/simulations` 复用同一计划逻辑但 `productionDecisionRecorded=false`，不得写生产决策记录。
- simulation 请求可携带 `expectations[]`，支持断言 `expectedPlanDecision`、`expectedFailureCode`、`expectedBindingKeys`、`expectedAccessPathKeys`；响应必须返回 `assertionPassed`、`assertions[]`、`explains[]`。
- 审计查询只读生产决策：`decision-records/search` 支持 requestKey/decisionKey/requester/operation/resource/decision/failureCode/limit 过滤，`decision-records/{decisionKey}` 返回最近一次生产决策。
- PEP SQL 编译当前只支持 `SELECT`、`UPDATE`、`DELETE`；`ALLOW_ALL` 不改写 SQL，`DENY_ALL` 注入 `1 = 0`，`FILTER` 编译受限 Predicate AST。
- PEP 必须使用参数化 SQL 绑定值，字段名、表名、访问路径映射必须做 identifier 白名单校验；`EXISTS_PATH` 必须在本地 `ResourceMapping.accessPaths` 中声明 targetTable/sourceField/targetField/targetFieldMappings，否则 fail-close。

### 失败语义（failureCode 分类）

完整 `failureCode` 枚举必须与 `authzcraft-api` 保持一致：

| 分类 | failureCode |
|---|---|
| 请求与主体 | `REQUEST_INVALID`、`SUBJECT_UNAVAILABLE` |
| 属性与 PIP | `ATTRIBUTE_MISSING`、`SOURCE_STALE`、`UNTRUSTED_CONTEXT`、`PIP_UNAVAILABLE` |
| PDP 与目录策略 | `PDP_UNAVAILABLE`、`RESOURCE_UNAVAILABLE`、`POLICY_UNAVAILABLE`、`CATALOG_DRIFT` |
| 授权参数与访问路径 | `GRANT_ARGUMENT_INVALID`、`ACCESS_PATH_UNAVAILABLE` |
| 计划与 SQL | `PLAN_EXPIRED`、`PLAN_INVALID`、`SQL_UNSUPPORTED` |
| 兜底 | `INTERNAL_ERROR` |

计划结果四类：`ALLOW_ALL`（无需过滤）、`DENY_ALL`（拒绝所有行）、`FILTER`（追加条件树）、`INDETERMINATE`（无法安全生成确定计划，PEP fail-close）。

**TRUE 谓词与 ALLOW_ALL 链路**：当谓词为空或 `operator` 为 `TRUE` 时，计划结果为 `ALLOW_ALL`，这是合法放行，不是失败。PDP 返回 `ALLOW_ALL` 时不带 predicate；PEP 收到 `ALLOW_ALL` 后不改写 SQL。`TRUE` 操作符在 PEP SQL 编译中产出 `1 = 1`（无害恒真条件）。`PredicateOperator` 白名单已包含 `TRUE`（位于 `EXISTS_PATH` 与 `CUSTOM_AST` 之间）。

### 业务侧数据权限配置页要求

当本 skill 与业务应用（如 Demo OA）联动生成数据权限配置页时，必须生成与中心端模型一致的轻封装，而不是在业务侧另建一套数据范围模型。

- 页面必须支持授权目标视角和数据表视角两种列表：授权目标视角按主体查看已绑定规则，数据表视角按 relation resource 查看已绑定规则；两者复用同一套 `access-grants/search` + `access-policies/search` + 主体查询数据。
- 新建/编辑抽屉必须支持授权目标类型 `ROLE/GROUP/USER/DEPARTMENT/POSITION`、多授权目标、多数据表、多 `operationCode`（READ/UPDATE/DELETE）和五项模板。
- 数据表候选来自 `relation-resources/search`；数据归属字段候选来自 `relation-resources/{id}/fields/search`，且只允许 `filterableFlag != false` 的 ACTIVE 字段。
- 关联表模式必须从 `access-paths/search` 选择访问路径，再加载目标 relation resource 的可过滤字段；前端展示应以“关联表 + 数据归属字段”为主，不把 path key 作为主要业务选项。
- 保存入口可由业务侧暴露轻封装端点，例如 `POST /{app}/api/v1/rbac/data-permissions/templates/save`；后端负责校验主体、资源、字段、访问路径，复用/创建蓝图、策略、修订、授权和授权参数，并调用 simulation 返回摘要。
- `ALL_DATA` 与其它过滤模板对同一主体、资源、operation 同时存在时，前端必须提示冲突；保存编辑结果若生成了新 grant，应作废旧 grant，避免旧 ACTIVE 授权继续生效。
- 身份类参数不得硬编码默认值：`PERSON_SHARE.fromUserKey`、simulation 测试人员等必须由用户选择或由当前登录态显式传入。

## 配置流程

1. **需求澄清**：确认 `tenantKey`/`appKey`、需要行过滤的业务表、数据归属字段、数据范围规则
2. **主体查询**：查授权主体的 `principalId`（角色/用户组/用户/组织/岗位）
3. **目录治理**：登记物理表资源 + 导入字段 + 登记访问路径（如需要）
4. **策略治理**：创建规则蓝图 → 创建访问策略 → 创建策略修订 → 激活
5. **授权治理**：创建授权（绑定主体 `principalId` + 策略）→ 配置授权参数
6. **模拟验证**：调用 `simulations` 断言预期决策，验证通过后才能切换 ENFORCE
7. **生产观察**：调用业务接口触发 `data-authz/plans`，用 decisionKey 查询审计记录，确认生产计划与预期一致

## 禁止事项

- 禁止创建/修改用户、组织、岗位、角色、用户组主体（只读查询，主体由权威源/回路 1 维护）
- 禁止把行数据范围规则写入回路 1 RBAC 表或前端逻辑
- 禁止在策略中保存 SpEL、SQL 片段或任意脚本字符串，只保存受限 Predicate AST
- 禁止一个访问策略混合多个资源、动作或策略效果
- 禁止跳过模拟验证直接上线 ENFORCE 模式
- 禁止把敏感授权参数写入日志、Explain 或差异报告明文
