# 数据权限模板到 Predicate AST 编译规格

> 本规格约束 RBAC 管理页中五项通用数据权限模板如何落到 AuthzCraft 回路 2。前端只负责收集业务化模板参数；后端或业务侧代理负责校验参数、编译受限 Predicate AST、创建/更新策略修订、绑定主体授权、写入授权参数并调用 simulation 验证。前端不得直接拼 SQL、不得直接编辑 AST、不得把行过滤规则写入回路 1 RBAC 表。

## 一、编译边界

### 前端职责

- 展示业务资源、主体、字段候选和五项通用模板。
- 从 PAP 查询蓝图 `input_schema`，根据 `fieldInputs`/`argumentInputs` 动态渲染配置表单字段（`CUSTOM_BLUEPRINT` 模板有 `input_schema`，`STANDARD_BLUEPRINT` 的 `input_schema` 为 `{}`）。
- 收集模板参数并调用模板化保存 API。
- 展示策略摘要、影响范围、保存结果和审计入口。
- 不暴露底层策略 DSL、SQL、PIP 参数名或内部主键作为主要输入。

### 后端职责

- 校验 `tenantKey`、`appKey`、主体、资源、操作、字段和模板参数合法性。
- 将模板参数编译为 AuthzCraft 回路 2 的 `predicate_ast`、`attribute_references`、`argument_schema` 和授权参数。
- 创建或复用规则蓝图、访问策略、策略修订和授权绑定。
- 调用 AuthzCraft simulation，验证可解释结果后再激活或返回待启用状态。
- 返回业务化摘要和审计入口。

**模板 → 规则蓝图映射（必须）**：每个数据权限模板（`ALL_DATA`/`MY_DATA`/`MANAGED_DEPARTMENTS`/`SPECIFIED_DEPARTMENTS`/`PERSON_SHARE`）MUST 对应一条独立的 `authzcraft_rule_blueprint` 记录——`blueprint_key` 用裸模板名（如 `MY_DATA`、`ALL_DATA`），`display_name` 用模板业务名（如"我的数据模板"）。`blueprint_kind` 按是否使用占位符选择：`ALL_DATA` 用 `STANDARD_BLUEPRINT`（无条件放行，`predicate_template`/`input_schema` 为 `{}`）；其余 4 个模板用 `CUSTOM_BLUEPRINT`（`predicate_template` 含 `FIELD_PLACEHOLDER`/`ATTRIBUTE_PLACEHOLDER`/`ARGUMENT_PLACEHOLDER` 占位符骨架，`input_schema` 声明占位符如何填写供前端动态渲染）。策略修订关联对应模板蓝图，不得让多个模板共用单一蓝图，否则中心 `access-policies/search` 返回的 `ruleBlueprintDisplayName` 无法区分模板，前端"数据规则模板"列会全部显示同一值。保存模板时若该模板蓝图不存在则创建并发布，已存在则复用（按 `blueprint_key` 幂等）。

**CUSTOM_BLUEPRINT 蓝图骨架示例**（MY_DATA 模板）：

```json
// predicate_template
{"operator":"EQ","left":{"kind":"FIELD_PLACEHOLDER","name":"anchorField"},"right":{"kind":"ATTRIBUTE_PLACEHOLDER","name":"currentUserId"}}

// input_schema
{"fieldInputs":[{"key":"anchorField","label":"数据归属字段","candidate":"FILTERABLE_FIELD","required":true}],"attributeInputs":[{"key":"currentUserId","attributeKey":"currentUserId","valueKind":"STRING","resolveKind":"REQUESTER_USER_ID"}],"argumentInputs":[]}
```

**templateBinding 格式**（CUSTOM_BLUEPRINT 路径）：

```json
{"arguments":{"anchorField":"applicant_user_id","currentUserId":"currentUserId"},"accessPathKey":"WB_MEETING_TO_PARTICIPANT"}
```

PAP `compileCustomBlueprint` 用 `arguments` 替换骨架中的占位符生成 `predicate_ast`；若携带 `accessPathKey`，替换后用 `EXISTS_PATH` 包裹。`input_schema` 供前端动态渲染配置表单（`fieldInputs` 控制字段选择器显示，`argumentInputs` 控制参数输入控件显示）。

## 二、通用输入模型

五项模板的保存请求至少包含：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "principalType": "ROLE",
  "principalCode": "sales_manager",
  "resourceKey": "customer",
  "operationCodes": ["READ"],
  "accessPathKey": "OPTIONAL_ROOT_TO_DETAIL_PATH",
  "effectiveAt": "2026-08-26T00:00:00+08:00",
  "expireAt": "2026-12-31T23:59:59+08:00",
  "remark": "业务说明"
}
```

约束：

- `principalType` 可为 `ROLE`、`GROUP`、`USER`，必须能解析到 AuthzCraft 主体域中的 `principalId`。
- `resourceKey` 必须已登记为回路 2 关系资源或可授权业务资源。
- `accessPathKey` 为可选字段；不为空时表示数据归属字段来自该访问路径的目标资源，而不是根资源本身。
- `operationCodes` 必须非空；标准读数据场景默认只允许 `READ`。
- 时间字段使用 ISO 8601 字符串；所有 ID/编码在前端按字符串处理。
- 同一次保存可以为多个 `operationCode` 创建同构策略，但每个访问策略最终仍绑定单一 `resourceKey` 和单一 `operationCode`。
- 每次保存请求只携带一个 `ruleTemplate`，后端只生成/复用该模板对应的一条策略修订和一条授权绑定；前端草稿里属于其他未选中模板的字段（如切换过的部门范围、指定部门、数据归属人）即使仍留在页面状态中，也不得被读取或落地为规则，后端编译逻辑必须严格按当前 `ruleTemplate` 分支取值。
- 前端草稿中用于标识"数据归属人/被分享人"等身份类参数（如 `fromUserKey`）MUST NOT 预置任何真实用户或角色编码作为默认值；必须以空值起始，并在保存前做必填校验（未选择时阻止保存并提示），避免管理员未主动选择就被静默提交成他人数据的分享方。测试/调试用的请求人参数（如 `simulationRequesterKey`）同理，不得硬编码固定身份，应默认为空并回退到当前登录人。

## 三、编译流程

1. **解析主体**：按 `principalType + principalCode` 查询 AuthzCraft 主体，得到 `principalId`。
2. **校验资源**：查询 `resourceKey`，确认资源存在、状态可用、支持目标 `operationCode`。
3. **校验字段**：确认数据归属字段存在、可过滤、类型与模板匹配。
4. **生成 AST**：按模板生成受限 `predicate_ast`，只使用白名单操作符和引用类型。
5. **生成策略修订**：写入 `predicate_ast`、`attribute_references`、`argument_schema`。
6. **绑定授权**：创建主体授权，写入 `effectiveAt`、`expireAt`、授权参数。
7. **模拟验证**：调用 simulation 验证策略能产生预期 `FILTER` 或明确 `DENY_ALL`，失败时不得激活。
8. **返回摘要**：返回 `ruleId`、`assignmentId`、`ruleTemplate`、`status`、业务化 `summary`、`auditUrl`。

工程约束：模板代理必须使用 AuthzCraft 中心和业务侧 PEP 都支持的 Predicate 形态。若当前 PEP 不支持 `CUSTOM_AST`，不得通过自由 `customAst` 字段提交模板结果；应使用通用蓝图（STANDARD_BLUEPRINT 或 CUSTOM_BLUEPRINT）或受控编译器生成普通 `EQ/IN/AND/OR/EXISTS_PATH` AST。策略、修订、授权和授权参数必须通过稳定键或内容摘要幂等复用，重复保存同一模板不得创建重复授权或触发唯一约束异常。

## 三-A、关联资源字段锚点（访问路径）

数据范围锚点可以来自当前资源，也可以来自通过 `authzcraft_access_path` 白名单到达的关联资源。关联字段锚点是横切能力，不是单独模板；`MY_DATA`、`MANAGED_DEPARTMENTS`、`SPECIFIED_DEPARTMENTS` 等模板都可以把自身条件放入访问路径的 `pathPredicate` 中。

### 前端参数

```json
{
  "resourceKey": "WB_MEETING",
  "ruleTemplate": "MY_DATA",
  "accessPathKey": "WB_MEETING_TO_PARTICIPANT",
  "ownerAnchorField": "participant_user_id"
}
```

### 校验规则

- `accessPathKey` 必须存在于 `authzcraft_access_path`，且 `root_relation_id` 对应当前 `resourceKey`。
- 数据归属字段必须属于访问路径 `destination_relation_id` 对应资源，并且 `filterableFlag=true`。
- 业务侧 PEP 必须声明同名 access path 映射，包含 `targetTable`、根表到目标表的关联字段、以及目标资源字段到 SQL 列的映射；缺失时必须输出缺口清单。
- 不得让前端直接填写 SQL 子查询、表连接条件或 `accessPathKey`；前端只展示业务化关联表与目标字段，`accessPathKey` 由所选关联表在内部映射得到。

### Predicate AST 包装规则

若模板原本会生成如下条件：

```json
{
  "operator": "EQ",
  "left": { "fieldKey": "participant_user_id" },
  "right": { "attributeKey": "currentUserId" }
}
```

当传入 `accessPathKey` 时，必须包装为：

```json
{
  "operator": "EXISTS_PATH",
  "accessPathKey": "WB_MEETING_TO_PARTICIPANT",
  "pathPredicate": {
    "operator": "EQ",
    "left": { "fieldKey": "participant_user_id" },
    "right": { "attributeKey": "currentUserId" }
  }
}
```

业务语义示例：`允许{主体名称}访问会议中通过参会人明细到达的参会人账号是本人的数据`，即“我参与的会议”。

## 四、模板 0：全部数据（ALL_DATA）

### 前端参数

```json
{
  "ruleTemplate": "ALL_DATA"
}
```

ALL_DATA 模板不需要数据归属字段、部门范围、归属人等参数。前端选择此模板时隐藏数据归属字段来源、关联表、数据归属字段等配置项。

### 字段校验

无需数据归属字段校验。`anchorField` 固定为 `id`。

### Predicate AST

```json
{
  "operator": "TRUE"
}
```

### templateBinding

```json
{"joiner": "AND", "conditions": []}
```

空 conditions 数组编译为 `TRUE` 节点。

### attribute_references

```json
[]
```

### argument_schema 与授权参数

该模板不需要授权参数。`argumentSchema`/`attributeReferences`/授权参数均为空。

### 业务摘要

`允许{主体名称}访问{资源名称}的全部数据`。

### 冲突语义

ALL_DATA 会覆盖同主体同资源的其它数据范围规则。保存前必须检测冲突并提示（见第九节）。

## 五、模板 1：我的数据

### 前端参数

```json
{
  "ruleTemplate": "MY_DATA",
  "ownerAnchorField": "owner_user_id"
}
```

### 字段校验

- `ownerAnchorField` 必须来自资源的 `ownerAnchorFields` 候选。
- 字段类型必须能与当前请求用户标识比较，通常为字符串或可归一化为字符串。

### Predicate AST

```json
{
  "operator": "EQ",
  "left": { "fieldKey": "owner_user_id" },
  "right": { "attributeKey": "currentUserId" }
}
```

### attribute_references

```json
[
  {
    "attributeKey": "currentUserId",
    "valueKind": "STRING",
    "resolveKind": "REQUESTER_USER_ID",
    "required": true
  }
]
```

### argument_schema 与授权参数

该模板默认不需要授权参数。`effectiveAt`、`expireAt` 属于授权绑定生命周期，不写入 `predicate_ast`。

### 业务摘要

`允许{主体名称}访问{资源名称}中{数据归属字段名称}是本人的数据`。

## 六、模板 2：我管理部门的数据

### 前端参数

```json
{
  "ruleTemplate": "MANAGED_DEPARTMENTS",
  "departmentAnchorField": "department_code",
  "departmentScopes": ["DIRECT_MANAGED", "SUB_MANAGED", "PORTION_MANAGED"],
  "subDepartmentDepth": "2"
}
```

### 字段校验

- `departmentAnchorField` 必须来自资源的 `departmentAnchorFields` 候选。
- 字段值必须能与组织编码比较，通常为部门编码字符串。
- `departmentScopes` 至少选择一项。
- 当 `departmentScopes` 包含 `SUB_MANAGED` 时，`subDepartmentDepth` 必填，可选 `1`、`2`、`3`、`ALL`。

### Predicate AST

```json
{
  "operator": "IN",
  "left": { "fieldKey": "department_code" },
  "right": { "attributeKey": "managedDepartmentCodes" }
}
```

### attribute_references

```json
[
  {
    "attributeKey": "managedDepartmentCodes",
    "valueKind": "STRING_SET",
    "resolveKind": "MANAGED_DEPARTMENT_CODES",
    "required": true
  }
]
```

### argument_schema

```json
{
  "departmentScopes": {
    "valueKind": "STRING_SET",
    "required": true,
    "label": "部门范围"
  },
  "subDepartmentDepth": {
    "valueKind": "STRING",
    "required": false,
    "label": "下级部门层级"
  }
}
```

### 授权参数

```json
{
  "departmentScopes": ["DIRECT_MANAGED", "SUB_MANAGED"],
  "subDepartmentDepth": "2"
}
```

### PIP 要求

`MANAGED_DEPARTMENT_CODES` 解析器必须能消费授权参数中的 `departmentScopes` 与 `subDepartmentDepth`，输出最终部门编码集合。权威解析规则归属 `authzcraft-data-authz-builder`：`DIRECT_MANAGED` 使用 `authzcraft_organization_closure.depth = 0`，`SUB_MANAGED` 使用 `depth > 0` 并按 `subDepartmentDepth` 限制层级，`PORTION_MANAGED` 使用 `authzcraft_principal_organization.portion_manage_user_id = requesterKey` 定位分管部门。若目标环境的 PIP 尚未实现该参数化解析，业务系统只能降级为已支持范围或输出缺口清单，不得在前端或回路 1 表中自行计算部门集合。

### 业务摘要

`允许{主体名称}访问{资源名称}中所属部门属于本人{部门范围描述}的数据`。

## 七、模板 3：指定部门数据

### 前端参数

```json
{
  "ruleTemplate": "SPECIFIED_DEPARTMENTS",
  "departmentAnchorField": "department_code",
  "departmentCodes": ["D001", "D002"]
}
```

### 字段校验

- `departmentAnchorField` 必须来自资源的 `departmentAnchorFields` 候选。
- `departmentCodes` 至少指定一个，允许指定多个，不做管理层级或组织归属校验；由业务人员直接选定literal部门编码。
- 与模板 2「我管理部门的数据」的区别：本模板不经过 `MANAGED_DEPARTMENT_CODES` 解析器推导管理范围，部门编码在保存时作为授权参数直接落地，请求时无需解析请求人的组织关系。

### Predicate AST

```json
{
  "operator": "IN",
  "left": { "fieldKey": "department_code" },
  "right": { "argumentKey": "specifiedDepartmentCodes" }
}
```

### attribute_references

该模板不依赖请求人属性解析，部门编码集合以授权参数（`argument`）形式直接提供，不生成 `attribute_references` 条目。

### argument_schema

```json
{
  "specifiedDepartmentCodes": {
    "valueKind": "STRING_SET",
    "required": true,
    "label": "指定部门"
  }
}
```

### 授权参数

```json
{
  "specifiedDepartmentCodes": ["D001", "D002"]
}
```

### 业务摘要

`允许{主体名称}访问{资源名称}中{数据归属字段名称}属于指定部门（{部门编码列表}）的数据`。

## 八、模板 4：分享某人的数据权限给某人

### 前端参数

```json
{
  "ruleTemplate": "PERSON_SHARE",
  "fromUserKey": "10001234",
  "toPrincipalType": "USER",
  "toPrincipalCode": "10005678",
  "ownerAnchorField": "owner_user_id",
  "allowReshare": false,
  "reason": "临时协同跟进客户"
}
```

### 主体语义

- `fromUserKey` 是被分享数据的归属人。
- `toPrincipalType + toPrincipalCode` 是被授予访问权的主体。
- 授权绑定的主体必须是被分享人或被分享角色/用户组，而不是 `fromUserKey`。

### 字段校验

- `ownerAnchorField` 必须来自资源的 `ownerAnchorFields` 候选。
- `fromUserKey` 必须能在 AuthzCraft 用户主体域中查询到。
- 被分享主体必须能解析到 `principalId`。

### Predicate AST

```json
{
  "operator": "EQ",
  "left": { "fieldKey": "owner_user_id" },
  "right": { "argumentKey": "sharedOwnerUserKey" }
}
```

### attribute_references

该模板默认不依赖请求人属性解析；如果需要审计请求人，可额外记录 `currentUserId`，但不得改变过滤语义。

### argument_schema

```json
{
  "sharedOwnerUserKey": {
    "valueKind": "STRING",
    "required": true,
    "label": "数据归属人"
  },
  "allowReshare": {
    "valueKind": "BOOLEAN",
    "required": false,
    "label": "是否允许再次分享"
  }
}
```

### 授权参数

```json
{
  "sharedOwnerUserKey": "10001234",
  "allowReshare": false
}
```

### 业务摘要

`将{fromUserName}负责的{资源名称}访问权限分享给{toPrincipalName}`。

## 九、组合与冲突规则

- 五项模板可以同时授予同一主体同一资源同一操作；运行时由回路 2 对多个授权策略做并集，除非存在显式 DENY 策略。
- 通用模板默认生成 ALLOW 策略，不生成 DENY 策略。
- 相同主体、资源、操作、模板、数据归属字段和参数完全一致时，应幂等复用或更新，不重复创建等价授权。
- 授权过期或撤销后必须不再参与运行时决策。
- 模板编译结果必须可审计：保存原始模板参数、编译后的 AST、策略修订 ID、授权 ID、创建人、更新时间。
- **ALL_DATA 冲突检测**：保存 ALL_DATA 模板前，必须检测同主体同资源是否已存在其它数据范围规则。如果存在，必须弹出冲突警告（Modal/Popconfirm），用业务语言说明"全部数据规则会使以下规则失效"，用户确认后才保存。反之，保存其它模板时如果已存在 ALL_DATA 规则，也必须警告"已有全部数据规则，新规则会被其覆盖而失效"。

## 十、模拟验证要求

每次保存或启用模板规则前，必须执行至少一个 simulation：

| 模板 | 正向断言 | 反向断言 |
|---|---|---|
| ALL_DATA | 当前用户查询该资源应产生 `ALLOW_ALL` | 无反向断言（全部放行） |
| MY_DATA | 当前用户查询自己数据归属字段匹配的数据应产生 `FILTER` | 不匹配本人数据归属字段的数据不应被允许 |
| MANAGED_DEPARTMENTS | 部门字段在解析后的管理部门集合中应产生 `FILTER` | 不在部门集合中的数据不应被允许 |
| SPECIFIED_DEPARTMENTS | 部门字段命中指定部门编码集合中的任一项应产生 `FILTER` | 不在指定部门编码集合中的数据不应被允许 |
| PERSON_SHARE | 被分享主体查询归属人为 `fromUserKey` 的数据应产生 `FILTER` | 非被分享主体或其他归属人的数据不应被允许 |

simulation 失败时，保存接口必须返回失败原因和 explain 摘要，不得激活策略。

## 十一、缺口判断

如果出现以下任一情况，必须停止在“可启用数据权限配置”之前，输出缺口清单：


- 资源未登记或数据归属字段未标记为可过滤。
- 主体无法解析到 AuthzCraft 主体域。
- `MANAGED_DEPARTMENT_CODES` 无法按部门范围和下级层级参数解析。
- AuthzCraft 缺少策略、策略修订、授权绑定或 simulation API。
- 生成的 AST 无法通过字段、类型、操作符校验。
- 关联字段锚点使用的 access path 未登记、未激活、root 不匹配、destination 字段不可过滤，或业务侧 PEP 未配置同名 access path 映射。

缺口清单必须包含缺失能力、影响模板、阻塞程度、建议补齐接口或数据项。