# AuthzCraft RBAC API 契约

> 本契约用于约束按 `rbac-system-builder` 生成的前端管理页与后端/AuthzCraft 的接口字段。路径可以由业务系统代理层适配，但语义、字段和长 ID 处理规则必须保持稳定。若目标系统已有等价接口，优先复用并编写字段映射说明；若接口缺失，先输出缺口清单和接口草案，不得用静态 mock 冒充完成。

## 一、通用约定

### 1. 响应封装

业务系统代理接口 MUST 使用统一响应封装：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {},
  "requestKey": "req_202608260001"
}
```

AuthzCraft 原生接口若返回格式不同，前端 API adapter 必须归一为当前项目通用格式，页面组件不得直接依赖多套响应结构。

### 2. 分页结构

```json
{
  "list": [],
  "total": 0,
  "pageNo": 1,
  "pageSize": 20
}
```

若 AuthzCraft 原生接口使用 `limit/offset`，前端或业务侧代理负责映射为项目统一分页结构。

### 3. ID 与主体键

- 所有用户、组织、岗位、角色、用户组、资源、策略、授权 ID 在前端类型中 MUST 使用 `string`。
- 禁止 `Number(id)`、`parseInt(id)`、一元 `+id`。
- 用户主体键优先使用统一身份 `userKey = user_id`。
- 角色和用户组绑定优先使用稳定编码：`roleCode`、`groupCode`。

### 4. 租户与应用

所有管理接口请求都必须携带或由后端上下文补齐：

```json
{
  "tenantKey": "platform",
  "appKey": "crm"
}
```

前端不得在页面中硬编码生产环境 `tenantKey` / `appKey`；应从运行时配置、当前应用上下文或后端初始化接口获取。

## 二、主体管理契约

### 1. 查询角色

**语义路径**：`POST /authzcraft/api/v1/principals/roles/search`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "keyword": "销售",
  "status": "ACTIVE",
  "pageNo": 1,
  "pageSize": 20
}
```

**响应 data**：

```json
{
  "list": [
    {
      "roleCode": "sales_manager",
      "roleName": "销售经理",
      "description": "管理销售团队",
      "status": "ACTIVE",
      "memberCount": 12,
      "permissionCount": 36,
      "dataPermissionCount": 3,
      "createdAt": "2026-08-26T10:00:00+08:00",
      "updatedAt": "2026-08-26T10:00:00+08:00"
    }
  ],
  "total": 1,
  "pageNo": 1,
  "pageSize": 20
}
```

### 2. 创建/更新角色

**语义路径**：`POST /authzcraft/api/v1/principals/roles`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "roleCode": "sales_manager",
  "roleName": "销售经理",
  "description": "管理销售团队",
  "status": "ACTIVE"
}
```

**响应 data**：角色详情。`roleCode` 冲突时必须返回可识别错误码和错误信息。

### 3. 退休角色

**语义路径**：`POST /authzcraft/api/v1/principals/roles/{roleCode}/retire`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "reason": "角色停用",
  "confirmed": true
}
```

**前置要求**：前端必须先展示影响范围，包括成员数、功能权限数、字段权限数、数据权限数。

### 4. 查询用户组

**语义路径**：`POST /authzcraft/api/v1/principals/groups/search`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "keyword": "华东",
  "status": "ACTIVE",
  "pageNo": 1,
  "pageSize": 20
}
```

**响应 data.list[]**：

```json
{
  "groupCode": "east_sales_group",
  "groupName": "华东销售组",
  "description": "华东区域销售成员",
  "status": "ACTIVE",
  "memberCount": 30,
  "roleCount": 2,
  "dataPermissionCount": 1,
  "createdAt": "2026-08-26T10:00:00+08:00",
  "updatedAt": "2026-08-26T10:00:00+08:00"
}
```

### 5. 创建/更新用户组

**语义路径**：`POST /authzcraft/api/v1/principals/groups`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "groupCode": "east_sales_group",
  "groupName": "华东销售组",
  "description": "华东区域销售成员",
  "status": "ACTIVE"
}
```

### 6. 退休用户组

**语义路径**：`POST /authzcraft/api/v1/principals/groups/{groupCode}/retire`

请求结构同退休角色，前端必须展示成员数、关联角色数和数据权限影响范围。

## 三、成员关系契约

### 1. 查询用户、组织、岗位

**语义路径**：

- `POST /authzcraft/api/v1/principals/users/search`
- `POST /authzcraft/api/v1/principals/organizations/search`
- `POST /authzcraft/api/v1/principals/positions/search`

**用户查询请求**：

```json
{
  "tenantKey": "platform",
  "keyword": "张三",
  "departmentCode": "dept_sales",
  "positionCode": "sales",
  "status": "ACTIVE",
  "pageNo": 1,
  "pageSize": 20
}
```

**用户响应 data.list[]**：

```json
{
  "userKey": "10001234",
  "userName": "张三",
  "staffNo": "S001234",
  "loginUserId": "zhangsan",
  "departmentCode": "dept_sales",
  "departmentName": "销售部",
  "positionCode": "sales_manager",
  "positionName": "销售经理",
  "status": "ACTIVE"
}
```

### 2. 查询成员关系

**语义路径**：`POST /authzcraft/api/v1/principals/memberships/search`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "principalType": "USER",
  "principalKey": "10001234",
  "targetType": "ROLE",
  "targetCode": "sales_manager",
  "status": "ACTIVE",
  "pageNo": 1,
  "pageSize": 20
}
```

`principalType` 可为 `USER`、`ORG`、`POSITION`；`targetType` 可为 `ROLE`、`GROUP`。

**响应 data.list[]**：

```json
{
  "membershipId": "2068959871393136600",
  "principalType": "USER",
  "principalKey": "10001234",
  "principalName": "张三",
  "targetType": "ROLE",
  "targetCode": "sales_manager",
  "targetName": "销售经理",
  "status": "ACTIVE",
  "effectiveAt": "2026-08-26T00:00:00+08:00",
  "expireAt": "2026-12-31T23:59:59+08:00"
}
```

### 3. 创建/更新成员关系

**语义路径**：`POST /authzcraft/api/v1/principals/memberships`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "items": [
    {
      "principalType": "USER",
      "principalKey": "10001234",
      "targetType": "ROLE",
      "targetCode": "sales_manager",
      "effectiveAt": "2026-08-26T00:00:00+08:00",
      "expireAt": "2026-12-31T23:59:59+08:00"
    }
  ],
  "confirmDiff": true
}
```

**响应 data**：

```json
{
  "createdCount": 1,
  "updatedCount": 0,
  "skippedCount": 0,
  "failedCount": 0,
  "failures": []
}
```

### 4. 关闭成员关系

**语义路径**：`POST /authzcraft/api/v1/principals/memberships/close`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "membershipIds": ["2068959871393136600"],
  "reason": "人员调岗",
  "confirmed": true
}
```

前端必须在调用前展示将关闭的成员关系清单。

## 四、功能权限契约

### 1. 查询权限点

**语义路径**：`POST /api/rbac/permissions/search`

**响应 data.list[]**：

```json
{
  "code": "app:customer:read",
  "name": "查看客户",
  "description": "查看客户列表和详情"
}
```

权限点仅含 `code/name/description` 三字段。分组逻辑由前端"菜单模块映射表"承载，不在权限点行内冗余 moduleCode。权限点编码为三段式 `appKey:resourceKey:operation`，operation 是面向业务的动作（read/create/update/delete/manage/submit/audit 等），由探查确定。

### 2. 查询角色功能授权

**语义路径**：`POST /api/rbac/roles/{roleCode}/permissions/search`

**响应 data**：

```json
{
  "roleCode": "sales_manager",
  "permissionCodes": ["app:customer:read", "app:customer:update"],
  "updatedAt": "2026-08-26T10:00:00+08:00"
}
```

### 3. 保存角色功能授权

**语义路径**：`POST /api/rbac/roles/{roleCode}/permissions/replace`

**请求**：

```json
{
  "permissionCodes": ["app:customer:read", "app:customer:update"],
  "confirmDiff": true,
  "expectedVersion": "v12"
}
```

**响应 data**：

```json
{
  "roleCode": "sales_manager",
  "added": ["app:customer:update"],
  "removed": [],
  "unchanged": ["app:customer:read"],
  "version": "v13"
}
```

### 4. 查询用户组功能授权

**语义路径**：`POST /api/rbac/groups/{groupCode}/permissions/search`

**响应 data**：

```json
{
  "groupCode": "finance_reviewers",
  "permissionCodes": ["app:expense:read", "app:expense:audit"],
  "updatedAt": "2026-08-26T10:00:00+08:00"
}
```

### 5. 保存用户组功能授权

**语义路径**：`POST /api/rbac/groups/{groupCode}/permissions/replace`

请求和响应结构同角色功能授权。默认样例运行时不自动把用户组功能/字段授权合并进当前用户权限；如目标系统需要用户组授权运行时生效，必须在当前用户解析阶段显式合并角色和用户组授权，并提供冲突优先级验证。

## 五、字段权限契约（两维模型）

### 1. 查询字段权限 option 定义

**语义路径**：`POST /api/rbac/field-permissions/search`

**响应 data.list[]**：

```json
{
  "code": "CUSTOMER.phone:READ",
  "resourceKey": "CUSTOMER",
  "resourceName": "客户",
  "fieldKey": "phone",
  "fieldName": "手机号",
  "securityRequirement": "MASKED",
  "accessLevel": "READ",
  "name": "手机号"
}
```

`security_requirement` 是字段固有属性（PLAIN/MASKED/HIDDEN），权威来源是 AuthzCraft 回路 2 字段目录 `authzcraft_resource_field`；`access_level` 是授权可变属性（NONE/READ/PLAIN_READ/WRITE）。`code` 格式为 `resourceKey.fieldKey:accessLevel`。`name` 只存字段业务名，不拼接访问级别。

### 2. 幂等创建/确保字段权限定义

**语义路径**：`POST /api/rbac/field-permissions/create`

**请求**：

```json
{
  "resourceKey": "CUSTOMER",
  "fieldKey": "phone"
}
```

语义是 ensure，不是盲目 insert：后端必须先按 `resourceKey + fieldKey` 查找是否已有字段权限定义；已存在时直接返回字段信息，不重复插入 `NONE/READ/PLAIN_READ/WRITE` options；不存在时读取 AuthzCraft 字段目录中的 `securityRequirement`，生成该字段允许的完整 option 集并返回。运行期管理页不得提交或修改 `securityRequirement`。

### 3. 查询授权目标已绑定字段权限

**角色语义路径**：`POST /api/rbac/roles/{roleCode}/field-permissions/search`

**用户组语义路径**：`POST /api/rbac/groups/{groupCode}/field-permissions/search`

**响应 data**：

```json
["CUSTOMER.phone:READ", "CUSTOMER.idCardNo:NONE"]
```

只返回当前授权目标已经绑定的字段权限 code，不返回全局字段定义。前端字段权限 tab 必须用该 code 集合与字段权限 option 定义取交集后渲染。未选择授权目标时前端不得调用保存，也不得显示全局字段定义。

### 4. 保存字段权限

**角色语义路径**：`POST /api/rbac/roles/{roleCode}/field-permissions/replace`

**用户组语义路径**：`POST /api/rbac/groups/{groupCode}/field-permissions/replace`

**请求**：

```json
{
  "fieldPermissionCodes": ["CUSTOMER.phone:READ", "CUSTOMER.idCardNo:NONE"],
  "confirmDiff": true,
  "expectedVersion": "v7"
}
```

后端必须校验所有 code 均来自字段权限 option 定义；同一授权目标的同一 `resourceKey + fieldKey` 只能保存一个 code。`NONE` 是显式绑定值，用于覆盖默认访问级别；缺少绑定记录表示使用 `securityRequirement` 默认访问级别，不表示无权限。

字段访问级别切换和保存只通过 replace 更新当前角色/用户组绑定；`fieldPermissionCodes` 中不包含某字段时表示该主体回退默认访问级别。

### 5. 修改字段权限定义展示信息

**语义路径**：`POST /api/rbac/field-permissions/update`

**请求**：

```json
{
  "resourceKey": "CUSTOMER",
  "fieldKey": "phone",
  "name": "手机号"
}
```

只允许修改字段业务名、描述等本地展示信息；`securityRequirement` 权威来源是 AuthzCraft 字段目录，运行期管理页只能只读展示，不能提交修改。

### 6. 删除字段权限定义

**语义路径**：`POST /api/rbac/field-permissions/delete`

**请求**：

```json
{
  "resourceKey": "CUSTOMER",
  "fieldKey": "phone",
  "confirmed": true
}
```

这是全局高风险治理动作：后端必须删除该字段全部 access_level option，并清理所有角色/用户组字段绑定。前端确认文案必须明确“删除该字段及其所有权限配置”。这不是删除当前授权目标绑定；若只想让当前主体不显示字段，应保存该字段的 `NONE` option。

## 六、当前用户权限契约

**语义路径**：`POST /api/rbac/authz/current-permissions`

**响应 data**：

```json
{
  "userKey": "10001234",
  "roles": ["sales_manager"],
  "groups": ["east_sales_group"],
  "permissions": ["app:customer:read", "app:customer:update"],
  "fieldPermissions": [
    {
      "code": "CUSTOMER.phone:READ",
      "resourceKey": "CUSTOMER",
      "fieldKey": "phone",
      "accessLevel": "READ",
      "securityRequirement": "MASKED",
      "name": "手机号"
    }
  ],
  "requestKey": "req_202608260001"
}
```

`fieldPermissions` 返回数组，每项含 `securityRequirement`（字段固有属性）和 `accessLevel`（授权可变属性）。前端菜单、按钮、字段交互只能基于该接口结果，不得从角色名、用户编码或组织编码自行推导。

## 七、数据权限配置契约

数据权限接口可以由业务侧代理到 AuthzCraft 回路 2。前端契约固定为模板化请求，后端负责映射为 AuthzCraft 策略、策略实例和授权绑定。模板参数到 Predicate AST、主体属性引用、授权参数和 simulation 的编译规则见 [`data-permission-template-ast-spec.md`](./data-permission-template-ast-spec.md)。

### 1. 查询可授权业务资源

**语义路径**：`POST /api/data-authz/resources/search`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "keyword": "客户",
  "pageNo": 1,
  "pageSize": 20
}
```

**响应 data.list[]**：

```json
{
  "resourceKey": "customer",
  "resourceName": "客户",
  "resourceType": "TABLE",
  "supportedOperations": ["READ"],
  "ownerAnchorFields": [
    { "fieldKey": "owner_user_id", "fieldName": "负责人" },
    { "fieldKey": "created_by", "fieldName": "创建人" }
  ],
  "departmentAnchorFields": [
    { "fieldKey": "department_code", "fieldName": "所属部门" }
  ]
}
```

### 2. 保存“我的数据”规则

**语义路径**：`POST /api/data-authz/rules/my-data/save`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "principalType": "ROLE",
  "principalCode": "sales_manager",
  "resourceKey": "customer",
  "operationCodes": ["READ"],
  "accessPathKey": "OPTIONAL_ROOT_TO_DETAIL_PATH",
  "ownerAnchorField": "owner_user_id",
  "effectiveAt": "2026-08-26T00:00:00+08:00",
  "expireAt": "2026-12-31T23:59:59+08:00",
  "remark": "销售经理查看本人负责客户"
}
```

`accessPathKey` 可选；为空时数据归属字段属于当前资源，不为空时数据归属字段属于该访问路径的目标资源，后端需按 `EXISTS_PATH` 编译。

### 2-A. 保存“全部数据”规则

**语义路径**：`POST /api/data-authz/rules/all-data/save`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "principalType": "ROLE",
  "principalCode": "sales_manager",
  "resourceKey": "customer",
  "operationCodes": ["READ"],
  "effectiveAt": "2026-08-26T00:00:00+08:00",
  "expireAt": "2026-12-31T23:59:59+08:00",
  "remark": "销售经理查看全部客户数据"
}
```

无需数据归属字段、部门范围、归属人等参数；后端编译为 `{"operator":"TRUE"}` 谓词，simulation 期望 `ALLOW_ALL`。会覆盖同主体同资源的其它数据范围规则，保存前必须提示。

### 3. 保存“我管理部门的数据”规则

**语义路径**：`POST /api/data-authz/rules/managed-departments/save`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "principalType": "ROLE",
  "principalCode": "sales_manager",
  "resourceKey": "customer",
  "operationCodes": ["READ"],
  "departmentAnchorField": "department_code",
  "departmentScopes": ["DIRECT_MANAGED", "SUB_MANAGED"],
  "subDepartmentDepth": 2,
  "effectiveAt": "2026-08-26T00:00:00+08:00",
  "expireAt": "2026-12-31T23:59:59+08:00",
  "remark": "销售经理查看管理部门客户"
}
```

`departmentScopes` 可选值：`DIRECT_MANAGED`（直接管理部门）、`SUB_MANAGED`（管理的下级部门）、`PORTION_MANAGED`（分管的部门）。当包含 `SUB_MANAGED` 时，`subDepartmentDepth` 必填，可为 `1`、`2`、`3` 或 `ALL`。

### 4. 保存“指定部门数据”规则

**语义路径**：`POST /api/data-authz/rules/specified-departments/save`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "principalType": "ROLE",
  "principalCode": "sales_manager",
  "resourceKey": "customer",
  "operationCodes": ["READ"],
  "departmentAnchorField": "department_code",
  "departmentCodes": ["D001", "D002"],
  "effectiveAt": "2026-08-26T00:00:00+08:00",
  "expireAt": "2026-12-31T23:59:59+08:00",
  "remark": "销售经理查看指定部门客户"
}
```

`departmentCodes` 至少指定一个，允许指定多个，直接作为授权参数落地，不经过管理层级解析。

### 5. 保存“分享某人的数据权限给某人”规则

**语义路径**：`POST /api/data-authz/rules/person-share/save`

**请求**：

```json
{
  "tenantKey": "platform",
  "appKey": "crm",
  "fromUserKey": "10001234",
  "toPrincipalType": "USER",
  "toPrincipalCode": "10005678",
  "resourceKey": "customer",
  "operationCodes": ["READ"],
  "ownerAnchorField": "owner_user_id",
  "effectiveAt": "2026-08-26T00:00:00+08:00",
  "expireAt": "2026-12-31T23:59:59+08:00",
  "allowReshare": false,
  "reason": "临时协同跟进客户"
}
```

### 6. 数据权限保存响应

五类规则保存后均返回：

```json
{
  "ruleId": "2068959871393136601",
  "assignmentId": "2068959871393136602",
  "ruleTemplate": "MY_DATA",
  "status": "ACTIVE",
  "summary": "允许销售经理查看负责人是本人的客户数据",
  "auditUrl": "/rbac/audit?assignmentId=2068959871393136602"
}
```

`ruleTemplate` 可选值：`ALL_DATA`、`MY_DATA`、`MANAGED_DEPARTMENTS`、`SPECIFIED_DEPARTMENTS`、`PERSON_SHARE`。

当请求携带 `accessPathKey` 时，响应建议回显 `accessPathKey`，用于前端展示和审计入口跳转。

## 八、前端缺口清单格式

当任一接口不存在或字段无法确认时，生成结果必须包含：

```json
{
  "missingApi": "POST /api/data-authz/rules/my-data/save",
  "usedByPage": "数据权限配置页",
  "requiredFor": "保存我的数据规则",
  "blocking": true,
  "suggestedRequestFields": ["tenantKey", "appKey", "principalType", "principalCode", "resourceKey", "operationCodes", "ownerAnchorField"],
  "suggestedResponseFields": ["ruleId", "assignmentId", "ruleTemplate", "status", "summary"]
}
```

缺口存在时，前端可以实现只读页面或 disabled 状态，但不得宣称数据权限配置已完成。