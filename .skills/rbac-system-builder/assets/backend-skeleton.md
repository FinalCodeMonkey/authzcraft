# RBAC 后端服务骨架

> 技术栈无关：按目标后端语言/框架（Spring Boot / Express / FastAPI / Go 等）落地。
> 核心原则：**角色管理（数据 CRUD）≠ 运行时鉴权（逻辑）**，两者必须分离实现。
> 改造既有系统时，优先复用现有认证、CurrentUser、PEP SDK 和工程分层，禁止生成第二套身份源或第二套数据 PEP。
>
> **默认运行模式：`AUTHZCRAFT`，运行时单权威路径，DDL 双模式基线。**
> 默认运行时用户/组织/岗位/角色/用户组/成员关系的读写全部通过 AuthzCraft principals API 完成；
> DDL 仍必须保留 `users`、`roles`、`user_roles`、`groups`、`user_groups`，但它们只服务显式 `LOCAL` profile/配置下的隔离适配器。
> 功能/字段授权两种模式统一使用 `role_permissions.role_code` / `role_field_permissions.role_code`，不生成旧 `role_id` 绑定表。

---

## 一、目录结构（建议）

```
backend/
├── controller/          # 管理接口
│   ├── RoleController          # 角色 CRUD（转发 AuthzCraft /principals/roles）
│   ├── GroupController         # 用户组 CRUD（转发 AuthzCraft /principals/groups）
│   ├── MembershipController    # 成员分配（转发 AuthzCraft /principals/memberships）
│   └── PermissionController    # 权限点 / 字段权限管理（本地）
├── service/             # 业务逻辑
│   ├── RoleService            # 调 PrincipalAdminGateway 管理角色
│   ├── GroupService           # 调 PrincipalAdminGateway 管理用户组
│   ├── MembershipService      # 调 PrincipalAdminGateway 管理成员关系
│   ├── PermissionService      # 本地权限点管理
│   └── FieldPermissionService # 本地字段权限管理
├── port/                # 应用端口（业务层只依赖这里）
│   ├── PrincipalDirectory     # 用户/组织/岗位/角色/用户组/成员查询端口
│   ├── PrincipalAdminGateway  # 角色/用户组/成员写入端口
│   └── RoleAuthorizationStore # role_code → 功能/字段权限查询端口
├── security/            # 运行时鉴权（逻辑，独立于管理接口）
│   ├── AuthFilter          # 认证：解析 token → 当前用户
│   ├── AuthzInterceptor    # 授权：调 PrincipalDirectory 获取角色 → 查权限 → 判断
│   └── CurrentUser         # 当前用户上下文
├── client/              # 外部服务客户端
│   └── AuthzCraftPrincipalClient # 调 AuthzCraft principals API（角色/用户组/成员关系/用户/组织/岗位）
├── repository/          # 数据访问（功能权限 + LOCAL 本地主体/成员表）
│   ├── PermissionRepository
│   └── LocalPrincipalRepository # 仅 LOCAL 适配器使用
├── adapter/
│   ├── AuthzCraftPrincipalDirectory # 默认条件 Bean，唯一主路径
│   └── LocalPrincipalDirectory      # 仅 LOCAL 条件 Bean
├── config/
│   └── RbacModeConfiguration        # 唯一允许按 storage-mode 选择实现的位置
└── entity/              # 实体（功能权限 + LOCAL 本地投影）
    ├── Permission
    ├── RolePermission          # role_code 引用 AuthzCraft 角色编码
    ├── FieldPermission
    └── RoleFieldPermission     # role_code 引用 AuthzCraft 角色编码
```

`LOCAL` 不是另一套 DDL，也不是与 `AUTHZCRAFT` 并列的主路径。它只能是同一份 DDL 下的隔离适配器：显式启用 `LOCAL` 后，`LocalPrincipalDirectory` 才能读取本地 `users/roles/user_roles/groups/user_groups` 解析角色/用户组/成员关系；默认 `AuthzCraftPrincipalDirectory` 不得混读本地表来补全用户、角色或成员。默认不要生成 `rbac_sync_outbox`；需要异步同步时必须另行确认。

---

## 二、角色管理接口（数据 CRUD）

### 默认 `AUTHZCRAFT` 模式端点（角色/用户组/成员关系管理）

默认 `AUTHZCRAFT` 模式下，用户、组织、岗位、角色、用户组、成员关系的读写全部通过 AuthzCraft principals API 完成，业务系统本地表只作为 `LOCAL` 适配器可运行的投影/备选，不作为默认权威源。
字段级请求/响应契约见 [`authzcraft-rbac-api-contract.md`](./authzcraft-rbac-api-contract.md)。若目标系统通过业务侧代理转发 AuthzCraft API，代理层必须保持契约语义稳定，并统一处理响应封装、长 ID 字符串和分页结构。

| 方法 | 路径 | 说明 | 风险 |
|---|---|---|---|
| POST | `/authzcraft/api/v1/principals/roles` | 创建/更新角色（upsert by roleCode） | write |
| POST | `/authzcraft/api/v1/principals/roles/search` | 查询角色列表 | read |
| POST | `/authzcraft/api/v1/principals/roles/{roleCode}/retire` | 退休角色 | **high-risk-write** |
| POST | `/authzcraft/api/v1/principals/groups` | 创建/更新用户组（upsert by groupCode） | write |
| POST | `/authzcraft/api/v1/principals/groups/search` | 查询用户组列表 | read |
| POST | `/authzcraft/api/v1/principals/groups/{groupCode}/retire` | 退休用户组 | **high-risk-write** |
| POST | `/authzcraft/api/v1/principals/memberships` | 创建/更新成员关系（upsert） | write |
| POST | `/authzcraft/api/v1/principals/memberships/search` | 查询成员关系 | read |
| POST | `/authzcraft/api/v1/principals/memberships/close` | 关闭成员关系 | **high-risk-write** |
| POST | `/authzcraft/api/v1/principals/users/search` | 查询用户投影（中文名、员工号、部门、岗位等） | read |
| POST | `/authzcraft/api/v1/principals/organizations/search` | 查询组织投影 | read |
| POST | `/authzcraft/api/v1/principals/positions/search` | 查询岗位投影 | read |

### 本地接口（功能权限 / 字段权限管理）

功能权限点和字段权限定义存储在本地，按 `role_code` / `group_code` 绑定到 AuthzCraft 角色或用户组。字段定义和主体绑定必须分离：字段定义表按 `resource_key + field_key + access_level` 提供可绑定 option；绑定表只保存当前主体选择的 option。`NONE` 必须是显式 option，不能用缺少绑定记录表达无权限。

| 方法 | 路径 | 说明 | 风险 |
|---|---|---|---|
| POST | `/api/rbac/permissions/search` | 权限点列表 | read |
| POST | `/api/rbac/field-permissions/search` | 查询字段权限 option 定义 | read |
| POST | `/api/rbac/field-permissions/create` | 幂等确保某资源字段的字段权限 option 定义存在 | write |
| POST | `/api/rbac/field-permissions/update` | 修改字段业务名等本地展示信息，`security_requirement` 只读 | write |
| POST | `/api/rbac/field-permissions/delete` | 全局删除字段定义并清理角色/用户组字段绑定 | **high-risk-write** |
| POST | `/api/rbac/roles/{roleCode}/permissions/search` | 查询角色功能权限 | read |
| POST | `/api/rbac/roles/{roleCode}/permissions/replace` | 绑定角色功能权限（role_code → 本地 permissions） | write |
| POST | `/api/rbac/roles/{roleCode}/field-permissions/search` | 查询角色字段权限 | read |
| POST | `/api/rbac/roles/{roleCode}/field-permissions/replace` | 绑定角色字段权限 | write |
| POST | `/api/rbac/groups/{groupCode}/permissions/search` | 查询用户组功能权限 | read |
| POST | `/api/rbac/groups/{groupCode}/permissions/replace` | 绑定用户组功能权限 | write |
| POST | `/api/rbac/groups/{groupCode}/field-permissions/search` | 查询用户组字段权限 | read |
| POST | `/api/rbac/groups/{groupCode}/field-permissions/replace` | 绑定用户组字段权限 | write |
| POST | `/api/rbac/authz/current-permissions` | 当前用户权限点与字段权限 | read |

### 硬约束

1. **删除角色、清空成员是高风险操作**：必须先确认影响范围（该角色关联了多少用户、多少权限），再执行。
2. **角色编码 `code` 应用内唯一**：创建时校验唯一性，冲突返回明确错误。
3. **权限点编码 `code` 与 DDL 权限表逐字一致**：绑定权限时校验权限点存在。
4. **当前用户权限接口必须由后端计算**：返回 `permissions`、`fieldPermissions`、`roles`（可选）和 `requestKey`，前端不得自行按角色推断。
5. **字段权限绑定必须校验字段定义存在**：`resourceKey + fieldKey + accessLevel` 必须来自字段权限表或等价配置，禁止前端提交任意字段键后直接落库；保存绑定时同一 `resourceKey + fieldKey` 对同一主体只能保留一个 `accessLevel`。
6. **字段定义创建必须幂等**：绑定前按 `resourceKey + fieldKey` 查找是否已有定义；已存在时直接复用，不得重复插入 `NONE/READ/WRITE/...` options；不存在时从 AuthzCraft 字段目录读取 `security_requirement`，生成该字段允许的完整 option 集。
7. **业务层只依赖端口**：Controller、Service、拦截器不得直接依赖 `AuthzCraftPrincipalClient` 或 `LocalPrincipalRepository`；只依赖 `PrincipalDirectory` / `PrincipalAdminGateway` / `RoleAuthorizationStore` 等端口。
8. **模式选择只在装配层**：`storage-mode` 判断只能在配置类、条件 Bean 或模块 wiring 中出现；禁止在应用服务、Controller、拦截器里写 `if (AUTHZCRAFT) ... else LOCAL ...`。
9. **AuthzCraft 用户中文名必须来自投影接口**：默认模式下用 `/principals/users/search` 获取用户 `displayName/staffName/departmentName/postName`；不得用通用 `/principals/search` 替代，也不得回退本地 `users.display_name` 当权威数据。

---

## 三、运行时鉴权（逻辑，独立于管理接口）

### 认证（AuthFilter）

```
请求 → 解析 token → 查用户 → 写入 CurrentUser 上下文
失败 → 返回 401 Unauthorized
```

### 授权（AuthzInterceptor）

```
请求
→ 从 CurrentUser 取 userKey
→ 调 PrincipalDirectory 获取角色集合（默认实现内部调用 AuthzCraft /principals/memberships/search，可短 TTL 缓存）
→ 查 role_permissions（按 role_code） → 得权限点集合
→ 根据 endpoint 权限映射取 requiredPermission
→ 判断请求所需权限点是否在集合中
→ 是：放行；否：返回 403 Forbidden
```

### 硬约束

1. **401 和 403 必须区分**：未认证返回 401，已认证但无权限返回 403。
2. **401/403 必须有可解析响应体**：统一返回项目的 `ApiResponse` 或等价错误结构，包含稳定 `code/message`；拦截器直接写响应时必须确保 body 被提交，不能只设置 HTTP 状态。
3. **403 不能触发回路 2**：无功能权限时必须在业务查询和数据 PEP 前返回，响应不得带 `decisionKey`，日志/审计不得生成数据权限决策记录。
4. **鉴权拦截器必须真实接入请求链**：禁止只写拦截器不注册到路由/中间件链。
5. **禁止把角色/权限硬编码在业务代码里**：必须查库或调 AuthzCraft API，禁止 `if (user.role == "admin")` 这种写死判断。
6. **权限点编码逐字一致**：拦截器判断的权限点编码必须与 DDL 权限表、前端配置完全一致。
7. **API 权限映射显式化**：每个受保护 Controller/API 必须声明 `permissionCode`，并沉淀为清单；禁止运行时从 URL 模糊猜权限点。
8. **字段权限后端真实执行**：读接口必须按 `fieldPermissions` 做响应投影、隐藏或脱敏；写接口收到只读/无权字段时必须拒绝或剔除，并留下审计；不能只依赖前端隐藏。
9. **AuthzCraft 角色查询可短 TTL 缓存**：`/principals/memberships/search` 结果建议缓存 30s–60s，缓存失效后重新拉取；AuthzCraft 不可用时按配置的 `fail-open`/`fail-close` 策略处理。
10. **双模式不得分叉授权表**：默认 AuthzCraft 模式下角色、用户组、成员关系 CRUD 使用 `roleCode/groupCode/userKey`；`LOCAL` 适配器可以使用本地 `role_id/group_id/user_id` 维护成员关系，但功能/字段授权两种模式都必须使用 `role_code` 绑定表，禁止生成或读取旧 `role_id` 权限绑定表。
11. **禁止运行时混合回退**：AuthzCraft 缺用户、角色、用户组、成员关系或中文名投影时，必须暴露同步/投影缺口并通过同步或数据修复解决；默认模式不得临时查本地表补齐后继续放行。
12. **运行时授权合并边界**：默认样例运行时只根据当前用户所属角色展开功能权限和字段权限；用户组授权用于管理页配置、数据权限主体绑定和扩展场景。若目标系统要求用户组功能/字段权限在运行时生效，必须显式实现 `role + group` 合并规则、冲突优先级和验证用例。

### 字段权限执行（两维模型）

字段权限必须在后端统一执行，前端只负责用户体验。

**两维模型**：`security_requirement`（字段固有属性，PLAIN/MASKED/HIDDEN）× `access_level`（授权可变属性，NONE/READ/PLAIN_READ/WRITE）。同一字段只有一种 `security_requirement`，定义在回路2 `authzcraft_resource_field` 表上，字段导入时通过 MCP/脚本设置，**运行期不允许通过管理界面修改**（修改会直接影响存量授权的字段可见性行为，属于开发期目录治理行为）；不同角色可获授不同 `access_level`。

```
readResource(resourceKey, rows):
    fieldPermissions = fieldPermissionService.resolve(currentUser, resourceKey)
    return rows.map(row => projectReadableFields(row, fieldPermissions))

writeResource(resourceKey, requestBody):
    fieldPermissions = fieldPermissionService.resolve(currentUser, resourceKey)
    deniedFields = findFieldsWithoutWritePermission(requestBody, fieldPermissions)
    if deniedFields not empty: return 403
    continue business write
```

字段权限效果（两维组合）：

`security_requirement` 是字段固有属性，定义在回路2 `authzcraft_resource_field` 表上（与 `sensitivity_level` 并存）。每个 `security_requirement` 有对应的默认 `access_level`——用户无字段权限绑定时使用默认值。

| `security_requirement` | 默认 `access_level` | 无绑定时读行为 | 说明 |
|---|---|---|---|
| PLAIN | READ | 返回原值 | 明文字段，默认可读 |
| MASKED | READ | 返回脱敏值（`***`） | 脱敏字段，默认脱敏可见 |
| HIDDEN | NONE | 不返回 | 隐藏字段，默认不可见 |

| `security_requirement` | `access_level` | 读接口 | 写接口 |
|---|---|---|---|
| HIDDEN | NONE（默认） | 字段不返回 | 拒绝提交 |
| HIDDEN | READ | 返回原值 | 拒绝提交 |
| HIDDEN | WRITE | 返回原值 | 允许提交 |
| MASKED | NONE | 字段不返回 | 拒绝提交 |
| MASKED | READ（默认） | 返回脱敏值（`***`） | 拒绝提交 |
| MASKED | PLAIN_READ | 返回原值 | 拒绝提交 |
| MASKED | WRITE | 返回原值 | 允许提交 |
| PLAIN | NONE | 字段不返回 | 拒绝提交 |
| PLAIN | READ（默认） | 返回原值 | 拒绝提交 |
| PLAIN | WRITE | 返回原值 | 允许提交 |

同一用户通过多个角色获得不同字段权限时，必须有确定性合并规则：只比较 `access_level` rank，取最高的那条：`WRITE(4) > PLAIN_READ(3) > READ(2) > NONE(1)`。`security_requirement` 是字段固有属性不需要比较。

**字段权限 MUST 面向配置实现（NON-NEGOTIABLE）**：

1. **字段安全要求（`security_requirement`）MUST 定义在回路2 `authzcraft_resource_field` 表上**，与 `sensitivity_level` 并存。它是字段固有属性（PLAIN/MASKED/HIDDEN），不是授权可变属性。回路1的 `field_permissions` 表默认不生成 `security_requirement` 列，只以 `access_level` 作为授权可变项；如确需本地快照，必须单独确认并同步 Mapper/Repository，且只能作为展示/校验快照，不能作为权威源。
2. **业务查询 SQL MUST 查出全部字段**（`SELECT *` 或等价方式）。不得只查部分字段——未查出的受控字段无法被投影/脱敏/移除，字段权限形同虚设。新增受控字段时不需要改 SQL。
3. **字段投影逻辑 MUST 从回路2 `authzcraft_resource_field` 表配置驱动**。`projectRows`（或等价方法）从回路2查字段 `security_requirement`，不得在代码里 `if/else` 硬编码"哪个字段是 MASKED/HIDDEN"。
4. **用户无字段权限绑定时 MUST 使用 `security_requirement` 对应的默认 `access_level`**：PLAIN→READ（原值）、MASKED→READ（脱敏 `***`）、HIDDEN→NONE（不返回）。不得 `continue` 跳过。
5. **`NONE` MUST 显式绑定**：缺少绑定记录表示使用默认 `access_level`，不是无权限。若要让默认 `MASKED` 字段对某个角色/用户组不返回，必须绑定 `resource.field:NONE`。
6. **所有业务查询接口 MUST 统一经过 `projectRows`**。不得只给个别表加字段权限而遗漏其他表。
7. **字段权限的 `name` MUST 只存字段业务名**（如"报销金额"），不得拼接访问级别后缀（如"报销金额可读"）。
8. **删除语义 MUST 明确**：字段权限单选切换和保存只更新当前主体绑定；“删除字段”是全局删除定义的高风险治理动作，必须二次确认，并同时清理角色/用户组两类字段绑定。

---

## 四、与 AuthzCraft 回路 2 数据权限协同（接入时必做）

回路 1 只做功能/API/字段权限；行数据权限由 AuthzCraft 回路 2 执行。若目标业务系统存在“只能看本人/本部门/管理部门及下级/跨表关联数据”等需求，后端必须接入数据 PEP SDK/Starter，而不是在 RBAC 拦截器中拼 SQL。
若前端提供“全部数据”“我的数据”“我管理部门的数据”“指定部门数据”“分享某人的数据权限给某人”五项模板配置，后端代理必须按 [`data-permission-template-ast-spec.md`](./data-permission-template-ast-spec.md) 将模板参数编译为 AuthzCraft Predicate AST、授权参数和主体绑定，并通过 simulation 后再返回可启用状态。

若模板参数携带 `accessPathKey`，后端代理必须先校验该访问路径属于当前根资源，再把模板条件编译进 `EXISTS_PATH.pathPredicate`。字段校验必须针对访问路径目标资源执行，而不是错误地只查根资源字段。典型例子：`WB_MEETING` 通过 `WB_MEETING_TO_PARTICIPANT` 到 `WB_MEETING_PARTICIPANT.participant_user_id`，表达“我参与的会议”。

### Java Spring Boot 接入项

Maven 依赖（按目标工程版本管理）：

```xml
<dependency>
        <groupId>com.fcm</groupId>
        <artifactId>authzcraft-pep-spring-boot-starter</artifactId>
</dependency>
```

最小配置项：

```properties
authzcraft.pep.enabled=true
authzcraft.pep.center-base-url=http://localhost:8080
authzcraft.pep.tenant-key=platform
authzcraft.pep.app-key=<business-app-key>
authzcraft.pep.requester-source=AUTH_CONTEXT
authzcraft.pep.failure-mode=FAIL_CLOSE
```

Mapper/API 资源映射必须逐项声明，覆盖 READ/UPDATE/DELETE 三类操作：

| `endpoint` | `permissionCode` | Mapper / 查询入口 | `resourceKey` | `operationCode` | 数据 PEP 行为 |
|---|---|---|---|---|---|
| 列表/详情查询 API | 功能权限点编码 | MyBatis `mappedStatementId` | 物理关系资源编码 | `READ` | 调用 `/data-authz/plans` 并执行 `RowFilterPlan` |
| 更新 API | 功能权限点编码 | MyBatis `mappedStatementId` | 物理关系资源编码 | `UPDATE` | PEP 拦截 `Executor.update`，按行过滤改写 UPDATE SQL |
| 删除 API | 功能权限点编码 | MyBatis `mappedStatementId` | 物理关系资源编码 | `DELETE` | PEP 拦截 `Executor.update`，按行过滤改写 DELETE SQL |

该清单是回路 1 与回路 2 的连接件：`permissionCode` 判断"能否访问功能/API"，`resourceKey + operationCode` 判断"能看到/改/删哪些行"。两者必须都存在，但不能混用。

**功能权限操作段说明**：`permissionCode` 的第三段（`app:resource:operation` 的 operation）是面向业务的动作（如 read/create/update/delete/manage/submit/audit），由探查业务 API 时确定，不是固定 CRUD 枚举。与回路 2 数据权限的 `operationCode`（固定 READ/UPDATE/DELETE）是两个概念，功能权限操作段不与之绑定。

### 请求链顺序

```
请求
    → 认证：解析当前用户，生成 requestKey
    → 回路 1：功能/API/字段权限校验，无权限直接 403
    → 业务查询
    → 回路 2 数据 PEP：调用 AuthzCraft，按 RowFilterPlan 改写 SQL 或返回空集
    → 审计：回路 1 记录与回路 2 decisionKey 使用同一 requestKey 串联
```

### 主体权威与 LOCAL 适配器

**默认运行模式为 `AUTHZCRAFT`，DDL 保留 `LOCAL` 可运行表。**

默认 `AUTHZCRAFT` 模式下，人员、组织、岗位、角色、用户组、成员关系的权威数据在 AuthzCraft 主体域，业务系统运行时通过 AuthzCraft API 实时查询或写入：

- **用户/组织/岗位**：通过 `POST /authzcraft/api/v1/principals/users/search`、`/organizations/search`、`/positions/search` 实时查询，可短 TTL 缓存。
- **角色/用户组**：通过 `POST /authzcraft/api/v1/principals/roles/search`、`/groups/search` 实时查询。
- **成员关系**：通过 `POST /authzcraft/api/v1/principals/memberships/search` 实时查询当前用户的角色集合。
- **角色/用户组/成员关系 CRUD**：全部通过 AuthzCraft API 完成；本地 `users/roles/user_roles/groups/user_groups` 只供 `LOCAL` 适配器使用。

两种模式都允许业务系统本地保存功能权限点、字段权限点，以及 `role_code -> permission/fieldPermission` 授权绑定；这些表属于回路 1 授权配置，不是角色主数据。实现时必须使用编码绑定表或等价结构，不能为了复用旧 SQL 生成 `role_id` 权限绑定表。

**`LOCAL` 模式**：业务系统显式启用 `Local*` 适配器后，才使用同一份 DDL 中的本地 `users/roles/user_roles/groups/user_groups` 表解析角色、用户组和成员关系。默认 `AUTHZCRAFT` 适配器不得读取这些表。若这些本地主体参与回路 2 数据策略，必须确认投影/同步到 AuthzCraft PIP 的方式。同步内容至少包含：

- 用户、组织、岗位若需要本地投影，只能从 AuthzCraft 主体域拉取；本业务系统不得创建第二套用户、组织或岗位权威主数据。拉取接口使用 `POST /authzcraft/api/v1/principals/users/search`、`POST /authzcraft/api/v1/principals/organizations/search`、`POST /authzcraft/api/v1/principals/positions/search`，并通过 `limit/offset` 分页初始化或对账
- 角色/用户组编码、名称、所属 `appKey`、生命周期状态
- 用户与角色/用户组的成员关系、生效时间、失效时间
- 用户键使用统一身份 `user_id`，不要使用业务库自增 ID 作为跨回路主体键
- 默认 `AUTHZCRAFT` 模式下，角色/用户组/成员关系变更直接写 AuthzCraft API，并从 AuthzCraft 回读 `principalId`、生命周期状态、同步版本和失败原因用于对账。
- `LOCAL` 模式下，如果本地角色/用户组/成员关系参与回路 2 数据策略，必须设计投影/同步到 AuthzCraft PIP 的机制；同步事件必须幂等，撤销、移除成员、禁用角色必须同步为失效，而不是只物理删除本地记录。
- 同步成功后必须能触发或等待 AuthzCraft PIP/PDP 相关缓存失效，避免功能权限已变更但数据范围仍使用旧主体快照。

### 既有系统改造检查

改造已经接入 AuthzCraft PEP 的系统时，先检查而不是重建：

- `pom.xml` 是否已有 `authzcraft-pep-spring-boot-starter`；已有则不重复添加。
- `application.properties` 是否已有 `authzcraft.pep.*`，只补缺失项和错误项。
- 当前用户来源是否已经通过 Header、Session、JWT 或 `CurrentUser` 传给 PEP；回路 1 复用同一主体键。
- **默认 `AUTHZCRAFT` 模式下**：用户、组织、岗位、角色、用户组是否已通过 AuthzCraft API 实时获取；本地 `users/roles/user_roles/groups/user_groups` 只作为 `LOCAL` 可运行表保留，默认运行时不得读取它们。
- **`LOCAL` 模式下**：用户、组织、岗位是否已经来自 AuthzCraft 主体域投影或统一身份投影；不得新增第二套身份/组织/岗位主数据。
- 运行时鉴权是否按 storage-mode 分支：`AUTHZCRAFT` 通过 AuthzCraft `/principals/memberships/search` 获取角色集合，`LOCAL` 才读取本地 `user_roles/user_groups`。
- `role_permissions` / `role_field_permissions` 表是否已使用 `role_code`（VARCHAR）作为绑定键；若仍存在旧 `role_id` 版权限绑定表，必须迁移到 `role_code` 绑定表，不得在新系统中继续生成或读取。
- MyBatis `mappedStatementId -> resourceKey/operationCode/accessPath` 映射是否已存在；新增回路 1 时不得破坏已有映射。
- 回路 1 拦截器顺序必须早于业务查询和数据 PEP 触发点；功能权限失败时不能产生回路 2 决策记录。

### 禁止事项

- 禁止在 RBAC 拦截器中实现“本人/本部门/下级部门”SQL 条件。
- 禁止让前端按角色过滤业务行数据。
- 禁止把 AuthzCraft 中心返回的计划当作可拼接 SQL；业务侧只能通过数据 PEP SDK 消费结构化 `RowFilterPlan`。
- 禁止功能权限失败后继续触发业务查询或数据 PEP。
- 禁止把本地 `users/roles/user_roles/groups/user_groups` 作为默认 `AUTHZCRAFT` 模式的运行时权威源；它们必须作为双模式 DDL 保留，只有 `LOCAL` 分支读取。
- 禁止生成或读取旧 `role_id` 版权限绑定表；功能/字段授权必须统一通过 `role_code` 绑定。

---

## 五、伪代码示例（技术栈无关）

### 角色管理（数据 CRUD）

```
// 删除角色（高风险，需确认）
function deleteRole(roleId):
    role = roleRepository.findById(roleId)
    if role == null: return 404
    userCount = userRoleRepository.countByRoleId(roleId)
    permCount = rolePermissionRepository.countByRoleId(roleId)
    // 返回影响范围，等待确认
    return { role, userCount, permCount, requiresConfirmation: true }
```

### 运行时鉴权（逻辑）

```
// 授权拦截器
function authorize(request, requiredPermission):
    user = currentUser(request)          // 认证层已解析
    if user == null: return 401
    roles = userRoleRepository.findRolesByUserId(user.id)
    permissions = rolePermissionRepository.findPermissionsByRoleIds(roles)
    if requiredPermission not in permissions: return 403
    return pass
```

---

## 六、验证标准

| 验证项 | 标准 |
|---|---|
| 未认证请求 | 返回 401 |
| 已认证但无权限 | 返回 403 |
| 有权限请求 | 正常放行 |
| 鉴权拦截器 | 真实接入请求链（非只写不接） |
| 权限点编码 | 与 DDL、前端逐字一致 |
| 删除角色 | 先确认影响范围再执行 |
| 字段权限 | 后端读响应隐藏/脱敏无权字段；写接口拒绝或剔除无权字段；绕过前端直接提交不能生效 |
| 主体同步 | 用户/组织/岗位来自 AuthzCraft 投影；角色/用户组和成员关系可推送到 AuthzCraft，也可查询 AuthzCraft 同步状态和主体 ID |
| 回路 2 SDK | 已配置 SDK/Starter、资源映射和 fail-close |
| 回路协同 | 无功能权限直接 403；有功能权限但无数据授权时由数据 PEP 返回空集/拒绝；`requestKey` 可串联回路 2 `decisionKey` |
| 既有系统改造 | 不新增第二身份源、不重复 PEP、不破坏已有 Mapper 映射和回路 2 验收脚本 |
