---
name: rbac-system-builder
description: '在目标业务系统中完整创建或改造回路 1 RBAC 权限系统：DDL 脚本、后端服务实现、前端 RBAC 管理页，并与 AuthzCraft 回路 2 数据权限协同。Use when 用户需要搭建角色权限系统、用户-角色-权限模型、功能/字段权限校验、角色/用户组/成员分配页面、功能授权管理、数据权限配置页、既有系统回路 1 改造、AI Coding 回路 1、或接入回路 2 数据权限 SDK。'
argument-hint: '描述业务系统的权限需求，例如：管理员可管理全部数据，普通用户只能查看和编辑自己的数据。'
---

# RBAC 权限系统创建

在目标业务系统中完整创建或改造一个回路 1 RBAC（Role-Based Access Control）权限系统，交付三件套：**DDL 脚本**、**后端服务实现**、**前端 RBAC 管理页**。若目标系统同时使用 AuthzCraft 回路 2 数据权限，本 skill 还必须生成回路 1 与回路 2 的协同接入清单，并实现匹配 AuthzCraft 后端服务的前端配置页面。

本 skill 的设计原则提炼自 `authz-guide-skill-source/` 的方法论（两层边界分离、角色管理≠运行时鉴权、安全约束、可验证目标）。创建前 MUST 先读这些原则，禁止凭经验直接写代码。

## 何时用

- 用户要在业务系统中搭建角色权限、用户-角色-权限模型
- 用户要改造既有业务系统，补齐回路 1 功能/API/字段权限
- 需要生成权限相关的 DDL 脚本（用户表、角色表、权限表、关联表）
- 需要实现后端鉴权服务（认证、授权、角色管理、权限校验）
- 需要实现前端 RBAC 配置界面（角色管理、权限配置、成员分配）
- 需要实现匹配 AuthzCraft 后端服务的前端管理页（角色、用户组、成员、功能授权、数据权限配置）
- 需要权限控制（谁能做什么）

## 核心方法论（创建前必读）

> 这些原则来自 `authz-guide-skill-source/`，是 RBAC 系统设计的**硬约束**，不是可选建议。

### 原则 1：两层边界分离

RBAC 系统必须清晰分离两层，禁止混为一谈：

| 层 | 概念 | 回答的问题 | 对应交付物 |
|---|---|---|---|
| 认证层 Authentication | 你是谁 | "谁能登录？" | 用户表、登录、token |
| 授权层 Authorization | 你能做什么 | "谁能做什么？" | 角色表、权限表、关联表、鉴权逻辑 |

**禁止**：把"谁能登录"（认证）和"谁能做什么"（授权）混在一个表或一个接口里。

### 原则 2：角色管理 ≠ 运行时鉴权

- **角色管理**（DDL + 管理接口）：角色的 CRUD、成员分配、权限绑定——这是**数据**。
- **运行时鉴权**（后端拦截器/中间件）：判断当前用户是否有某权限——这是**逻辑**。

**禁止**：用角色管理接口的返回结果推断运行时鉴权行为；运行时鉴权必须独立实现。

### 原则 3：回路 1 ≠ 回路 2

RBAC skill 只负责回路 1：**功能权限、字段权限、菜单/按钮/API 权限**。行数据权限属于 AuthzCraft 回路 2，由中心 PAP/PDP/PIP 与业务侧数据 PEP SDK 完成。

| 回路 | 负责内容 | 执行位置 | 本 skill 的职责 |
|---|---|---|---|
| 回路 1 | 功能、菜单、按钮、API、字段显示/编辑 | 业务系统内，由 AI Coding 生成 | 必须实现 |
| 回路 2 | 行数据范围、跨表访问路径、SQL 行过滤 | AuthzCraft 中心 + 业务侧 PEP SDK | 只做协同接入，不重新实现 |

回路 1 和回路 2 的协同要求：
- 回路 1 先判断用户是否能访问某功能/API；通过后，业务查询进入回路 2 数据 PEP，由 AuthzCraft 返回 `RowFilterPlan` 并改写 SQL。
- 回路 1 不得在 RBAC 表中保存“本人/本部门/管理部门及下级”等行数据范围规则；这些规则必须登记为回路 2 数据权限策略。
- 两条回路必须共享同一 `requestKey`，便于功能鉴权记录与数据权限 `decisionKey` 串联审计。
- 用户、组织、岗位、角色、用户组的主体键必须与 AuthzCraft 主体域同域；用户键优先使用统一身份 `user_id`，不要随意改成数据库自增 ID。

**默认运行模式：`AUTHZCRAFT`。回路 1 运行时不以本地人员、组织、岗位、角色、用户组表作为权威源，而是通过 AuthzCraft 接口实时获取和保存。**

**单权威运行路径（NON-NEGOTIABLE）**：DDL 可以支持双模式，但运行时代码不得形成 `AUTHZCRAFT` 与 `LOCAL` 两条同等主路径。默认主路径只能是 `AUTHZCRAFT`，`LOCAL` 只能作为显式 profile/配置下启用的隔离适配器。业务用例、Controller、鉴权拦截器、前端 API 调用都必须面向同一个应用端口（如 `PrincipalDirectory` / `RoleAuthorizationStore`），不得在业务代码里散落 `if (storageMode == LOCAL) ... else ...` 判断。模式选择只能发生在装配层（Spring `@ConditionalOnProperty`、配置类、模块 wiring），且同一请求链内只能使用一个权威源。

**DDL 生成必须直接支持双模式（NON-NEGOTIABLE）**：即使默认运行在 `AUTHZCRAFT` 模式，生成的回路 1 DDL 也 MUST 保留本地 `xx_users`、`xx_roles`、`xx_user_roles`、`xx_groups`、`xx_user_groups` 五类表，用于显式切换到 `LOCAL` 时可运行。但这只是 DDL 能力，不代表运行时存在两条主路径。默认 `AUTHZCRAFT` 模式下这些本地主体/成员表不得作为权威源、不得参与运行时角色解析、不得作为 AuthzCraft 缺失数据的静默回退；`LOCAL` 模式下才读取它们，且必须由独立 `Local*` 适配器承载。

默认 `AUTHZCRAFT` 模式下，角色、用户组、成员关系的 CRUD 全部通过 AuthzCraft API 完成：

| 操作 | AuthzCraft API |
|---|---|
| 创建/更新角色 | `POST /authzcraft/api/v1/principals/roles` |
| 查询角色 | `POST /authzcraft/api/v1/principals/roles/search` |
| 退休角色 | `POST /authzcraft/api/v1/principals/roles/{roleCode}/retire` |
| 创建/更新用户组 | `POST /authzcraft/api/v1/principals/groups` |
| 查询用户组 | `POST /authzcraft/api/v1/principals/groups/search` |
| 退休用户组 | `POST /authzcraft/api/v1/principals/groups/{groupCode}/retire` |
| 创建/更新成员关系 | `POST /authzcraft/api/v1/principals/memberships` |
| 查询成员关系 | `POST /authzcraft/api/v1/principals/memberships/search` |
| 关闭成员关系 | `POST /authzcraft/api/v1/principals/memberships/close` |
| 查询用户/组织/岗位 | `POST /authzcraft/api/v1/principals/users/search`、`/organizations/search`、`/positions/search` |

运行时功能权限检查时，回路 1 通过 AuthzCraft `/principals/memberships/search` 获取当前用户的角色集合（可短 TTL 缓存），再查本地 `role_permissions` 表获取功能权限点。AuthzCraft 不可用时按 `fail-open` 或 `fail-close` 策略配置处理。

默认样例运行时只根据当前用户所属角色展开功能权限和字段权限；用户组授权可在管理页配置，并可作为回路 2 数据权限的主体，但不会自动合并进 `CurrentUser.permissions/fieldPermissions`。如果目标系统明确要求用户组功能/字段授权运行时生效，必须同步实现用户组 membership 解析、`role + group` 权限合并、字段权限冲突优先级（WRITE > PLAIN_READ > READ > NONE）和对应验证用例。

功能/字段授权绑定表两种模式 MUST 统一使用角色编码：`role_permissions.role_code` / `role_field_permissions.role_code` 引用 AuthzCraft 角色编码或本地 `roles.code`，不生成、不中途回退到 `role_id` 版授权表。禁止生成 `xx_role_permissions(role_id, permission_id)`、`xx_role_field_permissions(role_id, field_permission_id)` 这类旧绑定表，避免 `AUTHZCRAFT` 与 `LOCAL` 两套授权模型分叉。

`LOCAL` 模式是显式启用的备用适配器，不是默认主路径，也不是另一套 DDL：它复用同一份 DDL 中的 `xx_users`、`xx_roles`、`xx_user_roles`、`xx_groups`、`xx_user_groups`，但功能/字段权限仍查 `role_code` 绑定表。`LOCAL` 代码必须隔离在 `Local*` Repository/Gateway/Directory 实现中，默认 `AUTHZCRAFT` 代码不得调用它来补全用户、角色、用户组或成员关系。不要默认生成同步 outbox；如果目标系统确实需要异步同步，再单独确认并设计。默认同步/对账优先采用实时 API 或快照接口。

- API 功能权限点与回路 2 数据资源不是同一个概念；必须建立 `endpoint -> permissionCode -> resourceKey + operationCode` 的显式映射清单。
- **功能权限操作段是业务动作，非固定 CRUD**：`permissionCode` 的第三段（`app:resource:operation` 的 operation）是面向业务的动作（如 `read` / `create` / `update` / `delete` / `manage` / `submit` / `audit` 等），由探查业务 API 时确定，不是固定枚举。与回路 2 数据权限的 `operationCode`（固定 `READ / UPDATE / DELETE`）是两个概念，功能权限操作段不与之绑定。可见性（read）与写操作必须拆成独立权限点分别授权；写操作权限点按业务动作命名，不强制每个资源都拆出 create/update/delete 四件套。
- **权限点编码命名约定**：权限点编码为三段式 `appKey:resourceKey:operation`，避免改造时生成两段式导致与 AuthzCraft 资源映射（resourceKey + operationCode）无法对齐。
- **数据 PEP 覆盖 READ/UPDATE/DELETE**：PEP 映射不止覆盖查询，还必须覆盖 UPDATE/DELETE 的 `mappedStatementId`；拦截器 `@Intercepts` 必须同时声明 `Executor.query`（4 参 + 6 参）与 `Executor.update`（2 参）。
- 已经接入回路 2 PEP 的业务系统，不得重复生成第二套数据 PEP；只能复用既有 `authzcraft-pep-spring-boot-starter`、补齐配置、资源映射和验证。

**禁止**：在回路 1 RBAC 里重新实现行过滤、拼接 SQL、维护管理部门数据范围，或让前端按角色自行过滤业务数据。

### 回路 1 完成后的数据权限配置

回路 1 RBAC 构建完成后，如果用户提出"谁能看哪些行"类数据权限需求，MUST 切换到 `authzcraft-data-authz-builder` skill 完成回路 2 配置。

仅当用户提出数据权限需求时才切换，不主动触发。

回路 1 需要传递给回路 2 的信息：

- `tenantKey` / `appKey`（与回路 1 一致）
- 需要行过滤的业务表清单（`resourceKey` 候选）
- 每个表的过滤数据归属字段（如 `owner_id`、`dept_code`）
- 已同步到 AuthzCraft 的角色/用户组编码（作为授权主体）
- 数据范围规则描述（如"本人负责或本人管理部门及下级"）

### 接口归属边界

按"权威源"划分接口归属：

| 数据 | 权威源 | 写接口归属 | 只读接口 |
|---|---|---|---|
| 角色、用户组、成员关系 | 回路 1 | rbac-system-builder（9 个全掌握） | 两边都可读 |
| 物理表、策略、授权 | 回路 2 | authzcraft-data-authz-builder | — |

rbac-system-builder 保留 9 个主体 CRUD 接口（roles/groups/memberships 的 upsert/search/retire/close），因为"增加角色"是回路 1 的核心交付物。authzcraft-data-authz-builder 只读查询主体（用于授权时查 `principalId`），不创建/修改主体。

### 原则 4：安全约束

1. **禁止输出密钥**（密码、token、secret）到终端明文或日志。
2. **写入/删除操作前必须确认用户意图**，支持 `--dry-run` 预览。
3. **高风险操作门禁**：删除角色、清空成员、删除权限等不可逆操作，必须先确认影响范围再执行。
4. **最小权限原则**：默认拒绝，显式授权。

### 原则 5：可验证目标

每个交付物 MUST 有可验证的验收目标（可被测试/运行验证的行为），禁止"生成了文件即完成"。

| 交付物 | 可验证目标 |
|---|---|
| DDL | 建表脚本可执行，外键/唯一约束正确，种子数据可回滚 |
| 后端 | 鉴权拦截器真实接入请求链；无权限返回 403；未认证返回 401 |
| 前端 | 角色/用户组/成员分配、功能授权、数据权限配置页面真实调用后端或 AuthzCraft 接口；无权限的菜单/按钮真实隐藏 |
| 改造前盘点 | 输出既有系统清单，明确复用点、改造点、禁止改动点和验证入口 |
| 字段权限 | 后端真实过滤/脱敏/拒绝无权字段，前端只做交互表现 |
| 回路协同 | 功能/API 鉴权 403 与数据 PEP 行过滤分别可验证；同一请求可串联 `requestKey` 与 `decisionKey` |

### 原则 6：方法论必须驱动交付物

本 skill 不是只给原则说明；每条方法论都必须驱动生成明确产物。若任务是既有系统改造，第一份产物必须是**改造前盘点清单**，并且在清单确认前不得进入代码改造。

盘点清单必须至少覆盖：
- 已有认证/当前用户来源、用户主体键和是否存在第二身份源风险
- 用户/组织/岗位是否从 AuthzCraft 主体域获取投影，是否存在本地第二主数据
- 默认运行权威源是否唯一；`storage-mode` 判断是否只出现在装配层，业务服务是否只依赖单一端口
- 角色/用户组是否具备与 AuthzCraft 的双向同步机制、同步回执、状态查询和失败重试
- 现有 Controller/API、前端入口、按钮/菜单、字段权限候选点
- 已有 Mapper/Repository、回路 2 PEP SDK、`resourceKey + operationCode` 映射
- `endpoint -> permissionCode -> resourceKey + operationCode` 显式映射草案
- 需要复用、需要新增、禁止改动的边界
- 后端字段权限真实校验点：读响应投影/脱敏、写请求字段拒绝或剔除
- 可执行验证入口：单元测试、集成测试、构建、回路 2 回归脚本或人工验收步骤

## 创建流程

执行编排见 `assets/rbac-system-builder-execution-playbook.md`。每次使用本 skill 进行新建、改造、前端实现、数据权限模板或验收时，MUST 先按该 playbook 判断任务类型、加载对应资产、执行阶段门禁并输出验证结果。

### 第 0 步：改造前盘点清单（既有系统必做）

如果目标是改造既有系统，MUST 先输出改造前盘点清单，禁止直接写 DDL、后端代码或前端代码。清单模板见 [`assets/preflight-inventory-template.md`](./assets/preflight-inventory-template.md)。

清单必须给出明确结论：
- 是否存在现成认证、CurrentUser、PEP SDK、Mapper 映射、前端 API 客户端和演示/验收链路
- 回路 1 应该新增在哪里，回路 2 应该原样复用在哪里
- 哪些字段权限需要后端真实执行，读接口和写接口分别如何验证
- 哪些文件可以改、哪些文件只能读取、哪些配置不得破坏

### 第 1 步：需求澄清（先读规范，缺失即报告）

创建前 MUST 先确认以下信息，缺失时**报告缺失、不瞎猜**：

1. **技术栈**：后端语言/框架、前端框架、数据库类型（本 skill 技术栈无关，每次使用 MUST 询问，禁止默认假设）
2. **新建还是改造**：若是既有系统，先盘点已有认证、用户来源、Controller/API、Mapper、前端路由、现有 PEP 配置，禁止直接套新项目模板
3. **角色清单**：系统有哪些角色（如 admin / manager / user）
4. **权限点清单**：系统有哪些可授权的操作（权限点编码为三段式 `app:resource:operation`，operation 是面向业务的动作，如 read/create/update/delete/manage/submit/audit，由探查确定）
5. **用户来源**：用户是系统自建，还是对接外部身份（如 SSO、LDAP、SourceID、请求头模拟用户）；外部身份场景不得生成密码登录作为第二身份源
6. **字段权限范围**：哪些字段需要隐藏、脱敏、明文只读、可写；字段权限采用两维模型（`security_requirement` 为字段固有属性，`access_level` 为授权可变属性）；读接口和写接口都必须在后端真实执行字段权限，不能只靠前端隐藏
7. **前端管理页范围**：必须确认角色管理、用户组管理、成员批量分配、功能授权管理、数据权限配置页的路由入口、组件位置、API 客户端和当前设计系统
8. **是否接入 AuthzCraft 回路 2**：若接入，必须确认 `tenantKey`、`appKey`、中心服务地址、业务资源 `resourceKey`、操作 `operationCode`、需要保护的 Mapper/API、以及当前用户键是否与 AuthzCraft 主体域一致
9. **用户/组织/岗位/角色/用户组来源**：默认运行模式为通过 AuthzCraft API 实时获取；DDL 仍必须保留 `LOCAL` 所需本地主体/成员表；若用户启用本地模式，则确认初始化、切换和同步/对账方式。必须同时确认运行时代码是否采用“单端口 + 条件适配器”结构，禁止在应用服务、Controller、拦截器或前端中混写 `AUTHZCRAFT`/`LOCAL` 分支
10. **角色/用户组是否参与数据策略**：默认模式下角色/用户组直接存储在 AuthzCraft；`LOCAL` 模式下必须确认如何把本地角色/用户组/成员关系投影到 AuthzCraft 主体域，供回路 2 授权只读查询
11. **数据权限配置入口**：若需要业务人员在前端配置数据权限，必须确认 AuthzCraft 回路 2 已具备资源、策略模板、授权绑定 API；缺失时先输出缺口清单并切换/协同 `authzcraft-data-authz-builder` 补齐后端配置

### 第 2 步：数据模型设计（DDL 脚本）

按两层边界分离设计表结构。**默认生成双模式基线 DDL**：运行默认 `AUTHZCRAFT`，但 DDL 同时保留 `LOCAL` 可运行所需的本地主体/成员表。

```
权限表 permissions               —— 授权层
字段权限表 field_permissions     —— 授权层
角色-权限关联 role_permissions   —— 授权层（role_code 引用 AuthzCraft 角色编码）
角色-字段权限关联 role_field_permissions —— 授权层
用户表 users           —— 认证层
角色表 roles           —— 授权层
用户-角色关联 user_roles      —— 授权层
用户组表 groups               —— 授权层
用户-用户组关联 user_groups    —— 授权层
```

DDL 硬约束：
- 关联表用联合主键或唯一约束，禁止重复授权
- 角色名、权限点编码必须**应用内唯一**（对应 authz-guide 的"定位键逐字一致"原则）
- 外键约束明确级联策略（删除角色时关联如何处理）
- 提供种子数据（默认角色 + 默认权限）和回滚脚本
- 字段权限必须有独立定义表或等价结构，按 `resource_key + field_key + access_level` 生成可绑定 option；`NONE` 必须是可落库、可绑定的显式 option，不能用“缺少绑定记录”表达无权限
- 字段定义和主体绑定必须分离：定义表描述“这个字段有哪些可选访问级别”，角色/用户组绑定表描述“当前授权目标选择了哪个访问级别”；同一字段同一主体最终只能绑定一个 `access_level`
- 字段安全要求 `security_requirement`（PLAIN/MASKED/HIDDEN）权威来源是回路 2 `authzcraft_resource_field` 字段目录；回路 1 字段定义表默认不生成 `security_requirement` 列，只保存 `access_level` option。若目标系统确需本地快照，必须单独确认并同步 Mapper/Repository，且该快照不能作为可编辑权威源
- `role_permissions.role_code` / `role_field_permissions.role_code` 两种模式都使用角色编码，不引用本地角色表 ID；默认 `AUTHZCRAFT` 模式引用 AuthzCraft 角色编码，`LOCAL` 模式引用本地 `roles.code`
- DDL 必须保留 `users`、`roles`、`user_roles`、`groups`、`user_groups`，保证配置切到 `LOCAL` 后系统可运行；这些表必须在注释中标明“LOCAL/profile 备用或投影缓存”，不得描述为默认权威主数据
- 禁止默认生成同步 outbox 和旧 `role_id` 版权限绑定表；异步 outbox 只有在用户明确要求时另行设计
- 不要把行数据范围规则塞进 RBAC 表

DDL 模板见 [`assets/ddl-template.sql`](./assets/ddl-template.sql)。

### 第 3 步：后端服务实现

按"角色管理 ≠ 运行时鉴权"分离实现：

**角色管理接口**（数据 CRUD）：
- 角色 CRUD、成员分配、权限绑定
- 高风险操作（删除角色、清空成员）必须确认

**运行时鉴权**（逻辑，独立于管理接口）：
- 认证：解析 token → 当前用户
- 授权：查用户角色 → 查角色权限 → 判断是否有权限点
- 鉴权拦截器/中间件真实接入请求链，禁止只写不接

后端硬约束：
- 未认证返回 401，无权限返回 403（区分清楚）
- 权限点编码与 DDL 中的权限表**逐字一致**
- 禁止把角色/权限硬编码在业务代码里，必须查库
- 默认 `AUTHZCRAFT` 模式必须作为唯一运行主路径实现：应用层只依赖 `PrincipalDirectory`、`RoleAuthorizationStore`、`CurrentUserResolver` 等业务端口；`AuthzCraft*` 与 `Local*` 只能作为不同基础设施适配器，由配置类/条件 Bean 选择
- 应用服务、Controller、鉴权拦截器中禁止出现 `if (authzCraftStorage())`、`if (storageMode == LOCAL)` 等双模式分支；若已有此类代码，改造任务必须优先收敛为“单端口 + 条件适配器”
- `AUTHZCRAFT` 适配器查询用户中文名、组织、岗位时必须调用 `/authzcraft/api/v1/principals/users/search`、`/organizations/search`、`/positions/search` 等投影接口；不得用通用 `/principals/search` 替代用户投影接口，也不得在投影缺失时静默回退本地 `users` 表作为默认权威源
- 本地 `users/roles/user_roles/groups/user_groups` 只能被 `Local*` 适配器读取；默认 `AUTHZCRAFT` 适配器不得混读本地成员关系、本地用户显示名或本地角色表来修补运行结果。若 AuthzCraft 主体域数据缺失，必须暴露为同步/投影缺口并走主体同步或数据修复，而不是运行时混合回退
- 每个受保护 API 必须显式声明所需 `permissionCode`；不要靠 URL 字符串临时推断权限
- 字段权限必须由后端真实执行：读接口按字段权限做响应投影/脱敏/隐藏；写接口校验字段编辑权限，无权字段必须拒绝或剔除并可审计
- **字段权限 MUST 面向配置实现，禁止硬编码**：
  - 字段安全要求（`security_requirement`：PLAIN/MASKED/HIDDEN）MUST 定义在回路2 `authzcraft_resource_field` 表上，是字段固有属性；字段导入时通过 MCP/脚本设置，**运行期管理界面 MUST NOT 暴露 `security_requirement` 修改入口**（修改会直接影响存量授权的字段可见性行为，属于开发期目录治理行为）；回路1的 `field_permissions` 表默认不生成 `security_requirement` 列，只以 `access_level` 作为授权可变项；如确需本地快照，必须单独确认并同步 Mapper/Repository，且不能作为权威源
  - 业务查询 SQL MUST 查出表的全部字段（`SELECT *` 或等价方式），不得只查部分字段——否则未查出的受控字段无法被 `projectRows` 投影/脱敏/移除，字段权限形同虚设
  - 字段投影逻辑（`projectRows`）MUST 从回路2 `authzcraft_resource_field` 表查 `security_requirement` 配置驱动，不得在代码里 `if/else` 硬编码"哪个字段是 MASKED/HIDDEN"
  - 用户无字段权限绑定时，MUST 使用 `security_requirement` 对应的默认 `access_level`：PLAIN→READ（原值）、MASKED→READ（脱敏 `***`）、HIDDEN→NONE（不返回）——不得 `continue` 跳过（否则受控字段不受控制）
  - `NONE` 是显式授权级别，不是空值。若要让某个默认 `MASKED` 字段对某个角色/用户组完全不返回，必须绑定该字段的 `NONE` option；不能删除绑定来表达，因为无绑定会回退到安全要求默认值
  - 所有业务查询接口 MUST 统一经过 `projectRows` 投影，不得只给个别表加字段权限而遗漏其他表
  - 字段权限的 `name` 字段 MUST 只存字段业务名（如"报销金额"），不得拼接访问级别后缀（如"报销金额可读"）——访问级别是授权可变属性，不是字段固有属性
  - 字段权限定义创建/导入 MUST 幂等：绑定前按 `resourceKey + fieldKey` 查找是否已存在字段定义；已存在时直接复用并返回字段信息，不得重复插入 `NONE/READ/WRITE/...` options；不存在时先根据回路 2 字段目录的 `security_requirement` 生成该字段允许的完整 option 集，再进入绑定流程
  - 授权页中的“删除字段权限”语义 MUST 明确区分：修改当前授权目标访问级别时走 replace 保存绑定集合；删除字段定义是全局治理动作，会删除该字段全部 access_level option，并清理所有角色/用户组绑定，必须二次确认并提示“删除该字段及其所有权限配置”
- 若接入 AuthzCraft 回路 2，业务应用必须引入数据 PEP SDK/Starter，只消费 `RowFilterPlan`；不得在回路 1 拦截器中自行拼接数据权限 SQL
- 回路 1 拦截器必须在业务查询前完成功能/API 权限校验；数据 PEP 负责查询执行时的行过滤，两者共享 `requestKey`
- 若目标系统已存在 PEP Starter，只检查和复用现有依赖、配置、Mapper 映射、Header/CurrentUser 适配，不重复生成 PEP 实现
- 默认 `AUTHZCRAFT` 模式下，用户、组织、岗位、角色、用户组和成员关系必须实时读写 AuthzCraft 主体域：通过 `/principals/users/search`、`/principals/organizations/search`、`/principals/positions/search` 查询用户/组织/岗位，通过 `/principals/roles`、`/principals/groups`、`/principals/memberships` 维护角色、用户组和成员关系；业务系统只可短 TTL 缓存或保存授权投影，不得把本地 `users/roles/user_roles/groups/user_groups` 表作为默认权威源。DDL 仍必须保留这些表，且只有配置切换到 `LOCAL` 后才读取；若 `LOCAL` 的本地主体参与回路 2 数据策略，必须确认投影/同步到 AuthzCraft 的方式，并从 AuthzCraft 查询主体 ID/状态用于对账

后端骨架见 [`assets/backend-skeleton.md`](./assets/backend-skeleton.md)。

### 第 4 步：前端 RBAC 管理页实现

实现匹配 AuthzCraft 后端服务的 RBAC 管理页。前端必须面向业务管理员可操作，不只交付 API 调试表单。

前端交互必须符合 RBAC 权限管理最佳实践：以“角色/用户组/人员/权限/数据范围”的清晰对象模型组织页面；常用管理动作使用列表、详情抽屉、树表、权限矩阵、穿梭选择器或批量选择面板；所有批量变更必须提供已选对象数量、变更差异预览、冲突/重复提示、保存结果反馈和失败项明细。默认遵循最小授权原则，新增角色/用户组默认不授予高风险权限。

#### 4.1 角色、用户组与成员管理

- **角色管理页**：角色列表、搜索、创建、编辑、退休/删除（高风险操作需确认），真实调用 AuthzCraft 角色接口或业务侧代理接口
- **用户组管理页**：用户组列表、搜索、创建、编辑、退休/删除（高风险操作需确认），真实调用 AuthzCraft 用户组接口或业务侧代理接口
- **按人员分配**：选择一个或多个人员后，批量分配角色或用户组；支持搜索人员、查看已有关联、增量添加和批量移除
- **按角色/用户组分配**：进入某个角色或用户组详情后，批量添加人员、移除人员、查看成员清单；支持分页、搜索、已选去重和结果回显
- **便捷性要求**：成员分配必须支持双向入口（人 -> 角色/用户组、角色/用户组 -> 人），避免只能逐人或逐角色重复操作
- **防误操作要求**：批量授权/移除前必须展示本次新增、保留、移除的差异；清空成员、删除角色、删除用户组必须二次确认并展示影响范围

#### 4.2 功能授权管理

- **权限点管理/展示**：按菜单、页面、按钮、API、字段分组展示权限点，权限点编码必须与后端 `permissionCode` 逐字一致
- **角色/用户组授权页**：给角色或用户组批量勾选功能/API/菜单/按钮权限，支持全选/半选、搜索过滤、变更预览和保存结果反馈
- **字段授权页**：配置字段两维模型（`security_requirement` 为字段固有属性，`access_level` 为授权可变），并提示字段权限最终由后端执行，前端只负责展示和交互控制
- **授权目标切换语义（NON-NEGOTIABLE）**：功能权限、字段权限、数据权限三个 tab 都必须以当前选中的角色/用户组/人员为视角；切换授权目标时重新查询该目标已绑定集合；未选择授权目标时保存按钮置灰，功能权限可保留全量权限项但不勾选任何项，字段权限和数据权限不得显示全局全部配置
- **字段权限列表语义（NON-NEGOTIABLE）**：字段权限 tab 只显示当前授权目标已绑定或本次待保存绑定的字段，不显示全局所有字段定义；否则管理员会误以为这些字段都已绑定到当前目标
- **添加字段语义**：在字段权限 tab 点击“添加字段”时，先从字段目录/字段定义中选择数据表和字段；后端幂等 ensure 字段权限定义，前端把该字段的默认 access level code（PLAIN/MASKED→READ，HIDDEN→NONE，或后端返回默认值）加入当前目标的待保存 `selectedFieldPermissionCodes`，列表立即出现该字段；只有点击“保存字段权限”后才持久化绑定
- **刷新时序约束**：创建/ensure 字段定义后可以刷新字段定义 options，但不得自动重新查询当前角色/用户组绑定并覆盖未保存的本地选择；保存成功后再把当前选择作为原始值或重新加载当前目标
- **字段定义编辑/删除语义**：字段编辑只允许修改字段业务名等本地展示信息，`security_requirement` 只读展示；字段删除是全局删除定义，不是删除当前授权目标绑定，必须清理角色/用户组字段绑定并刷新当前目标列表
- **权限控制**：根据当前用户权限接口返回的 `permissions` 和 `fieldPermissions` 控制菜单、按钮、字段显示/只读/脱敏表现
- **授权矩阵要求**：功能授权优先采用“权限资源树 + 角色列/操作列”的矩阵或树表形态，支持按模块折叠、搜索定位、半选态和只看已授权，避免让管理员在平铺长列表中逐项寻找

#### 4.3 数据权限配置页（AuthzCraft 回路 2 协同）

数据权限页面是 AuthzCraft 回路 2 的业务化配置入口，前端可以在 RBAC 管理区承载入口，但规则必须保存到 AuthzCraft 数据权限后端，不得写入回路 1 RBAC 表，也不得让前端过滤业务数据。

必须支持以下业务人员可理解的规则模板：

| 规则模板 | 前端配置能力 | 落地要求 |
|---|---|---|
| 全部数据 | 选择数据表和操作范围，无需指定数据归属字段、部门范围或归属人 | 编译为 `{"operator":"TRUE"}` 谓词，simulation 期望 `ALLOW_ALL`；会覆盖同主体同资源的其它规则，保存前必须提示 |
| 我的数据 | 选择数据表和数据归属字段，如负责人、创建人、销售等 | 映射为 AuthzCraft 行过滤策略模板，不在前端拼接过滤条件 |
| 我管理部门的数据 | 部门范围支持复选：直接管理部门、管理的下级部门、分管的部门；“管理的下级部门”必须可指定下钻级数 | 生成可审计的策略参数，部门关系由 AuthzCraft/PIP 解析 |
| 指定部门数据 | 直接多选一个或多个部门编码，不依赖管理层级推导 | 编译为按参数化部门编码集合的 `IN` 过滤，部门编码作为授权参数随策略保存，不经 PIP 解析管理范围 |
| 分享某人的数据权限给某人 | 支持选择授权人、被分享人、数据表、失效时间/状态和可访问操作 | 保存为 AuthzCraft 授权绑定或等价分享策略，支持撤销和审计 |

五项通用数据权限场景必须简单易用，优先采用模板向导而不是暴露底层策略表达式：第一步选择授权目标（角色/用户组/人员），第二步选择数据表，第三步选择“全部数据 / 我的数据 / 我管理部门的数据 / 指定部门数据 / 分享某人的数据权限给某人”可见数据范围，第四步只填写该模板必要参数并预览影响范围。默认值必须贴近日常业务：全部数据无需数据归属字段；我的数据默认使用本表的负责人/创建人候选数据归属字段；我管理部门的数据默认勾选直接管理部门并允许追加下级层级；指定部门数据默认要求至少选择一个部门编码，允许多选；分享规则默认需要失效时间或撤销入口。

数据权限配置页还必须提供：数据表选择、模板说明、策略预览、影响范围提示、保存前确认、保存后回显、错误信息展示、禁用状态处理和只读审计信息入口。页面文案必须使用业务语言（如“负责人是本人”“查看我直接管理部门的数据”），不得把 `resourceKey`、`operationCode`、PIP 参数等底层字段直接暴露给业务人员作为主要交互。

页面主文案 MUST 使用以下定稿术语：`授权主体类型` → `授权目标类型`，`授权对象` → `授权目标`，`业务资源` → `数据表`，`数据权限模板` → `可见数据范围`，`规则模板` 保留，`锚点位置` → `数据归属字段来源`，来源选项为 `本表` / `关联表`，`当前资源本人/归属人锚点字段` 与 `关联资源本人/归属人锚点字段` → `数据归属字段`，`关联资源` → `关联表`，`失效时间` 保留。`保存后验证访问人` 不得作为主配置表单字段展示；若需要测试入口，必须独立放在测试/调试区域，并命名为 `测试人员`。

页面交互 MUST 禁止无效解释标签：当控件本身、选项文本或所在分组已经清楚表达含义时，不得再添加重复 label、说明标题或提示文案。典型反例：在 `角色 / 用户组 / 人员` 分段控件上方再写 `授权目标类型`；在 `本表 / 关联表` 分段控件上方再写一段重复解释；在卡片标题已经是 `可见数据范围` 时，紧邻控件再重复同义标签。字段标签只有在能说明“要填写/选择什么值”、约束、单位或消除歧义时才保留；否则应移除，或改为更高层级的分组标题（如 `授权给谁`）并让控件选项直接承担选择语义。辅助说明只用于解释业务后果、风险或不可见规则，不用于复述控件文字。

当数据归属字段不在本表，而在通过业务关系可到达的明细/关联表上时，前端 MUST 支持选择 `关联表` 来源：先选择根数据表，再选择业务人员能理解的关联表（如“会议参会人”），最后选择该关联表上的数据归属字段（如“参会人账号”或“参会人部门编码”）。前端不得把 `WB_MEETING_TO_PARTICIPANT` 这类 `authzcraft_access_path.path_key` 作为主要选项展示；它只能作为关联表选择后的内部映射值提交给后端。后端 MUST 校验 access path 的 rootRelation 与当前资源一致、数据归属字段属于 destinationRelation 且可过滤，并将模板 Predicate 包装为 `EXISTS_PATH(accessPathKey, pathPredicate)`。典型场景：对会议 `WB_MEETING` 授权“我的数据”时，按参会人明细 `WB_MEETING_PARTICIPANT.participant_user_id = currentUserId` 判断“我参与的会议”，不得退化为只能按会议主持人字段配置。

前端硬约束：
- 菜单/按钮的隐藏必须基于后端返回的权限点，禁止前端写死
- 删除角色、清空成员等不可逆操作必须有确认弹窗
- 前端只做菜单、按钮和字段级交互控制；不得根据角色在前端过滤业务行数据，行数据过滤必须由回路 2 数据 PEP 在后端执行
- 前端必须从后端当前用户权限接口获取 `permissions` 和 `fieldPermissions`，不得从角色名、用户编码或组织编码推导权限
- 前端字段权限 tab 的数据源必须是“当前授权目标绑定 codes ∩ 字段权限 option 定义”；搜索只在这个交集中搜索，不得因为搜索或刷新而退回展示全部字段定义
- 角色、用户组、成员关系管理必须真实调用 AuthzCraft 主体接口或业务侧代理接口，不得使用静态 mock 作为最终实现
- 数据权限配置页必须调用 AuthzCraft 回路 2 的资源、策略、授权接口或业务侧代理接口；若后端接口未就绪，必须明确输出接口缺口清单和临时不可完成项
- 用户、组织、岗位、角色、用户组 ID 在前端必须保持字符串，不得使用 `Number()`、`parseInt()` 或一元 `+` 转换
- 前端不得要求业务人员理解底层策略 DSL、SQL、PIP 参数名或内部主键；必须通过模板、选择器、预览和业务化文案完成配置
- 通过脚本、API 或种子数据写入字段中文名、表中文名、数据规则中文名时必须使用 UTF-8 without BOM；Windows PowerShell 内联 `python -c` / here-string 写中文到 MySQL 容易把中文写成 `?`，必须改用 UTF-8 `.py`/JSON 文件或后端 API 正常提交

前端骨架见 [`assets/frontend-skeleton.md`](./assets/frontend-skeleton.md)，详细页面规格见 `assets/frontend-rbac-page-spec.md`，API 字段契约见 `assets/authzcraft-rbac-api-contract.md`，数据权限模板到回路 2 AST 的编译规格见 `assets/data-permission-template-ast-spec.md`，前端验收清单见 `assets/rbac-frontend-acceptance-checklist.md`。生成或改造前端页面前 MUST 先读取页面规格和 API 契约；生成或改造数据权限后端代理前 MUST 先读取 AST 编译规格；验收前 MUST 对照前端验收清单逐项检查，不满足项必须明确标注为未完成或阻塞。

### 第 5 步：验证

按"可验证目标"逐项验证：

1. **DDL 验证**：建表脚本可执行，约束正确，种子数据可回滚
2. **后端验证**：鉴权拦截器真实接入；无权限 403、未认证 401；权限点逐字一致
3. **字段权限验证**：无查看权限字段不出现在响应中或被脱敏；只读/无编辑权限字段在写接口被拒绝或剔除；前端绕过控件直接提交也不能生效
  - 必须额外验证：对一个字段权限空列表的角色/用户组，通过“添加字段”选择已存在字段定义时不重复插入定义，列表立即只显示该字段，保存后切换/刷新仍只显示该目标绑定字段；绑定 `NONE` 后默认 `MASKED` 字段在运行时完全不返回
4. **主体来源验证**：`AUTHZCRAFT` 模式下角色/用户组/成员关系通过 AuthzCraft API CRUD 管理，运行时通过 API 实时获取用户角色集合；`LOCAL` 模式下读取本地 `users/roles/user_roles/groups/user_groups`，且功能/字段授权仍通过 `role_code` 绑定表生效；两种模式互不串用默认权威源
5. **前端验证**：必须对照 `assets/rbac-frontend-acceptance-checklist.md` 验收；角色/用户组管理界面真实调用后端或 AuthzCraft；人员维度和角色/用户组维度均可批量分配；批量变更有差异预览和失败项反馈；功能授权保存后运行时权限生效；无权限菜单/按钮真实隐藏；字段控制来源于后端 `fieldPermissions`；数据权限配置能通过模板向导保存“全部数据”“我的数据”“我管理部门的数据”“指定部门数据”“分享某人的数据权限给某人”，业务人员不接触底层策略表达式，后端能按模板生成 Predicate AST、绑定授权并通过 simulation，保存后可在回路 2 查询/审计
6. **验收测试**：用测试把可验证行为钉死，防止回退——测试对象是**业务行为**（401/403 正确、鉴权拦截器真实接入、字段权限真实生效、主体同步真实生效、权限点编码逐字一致、删除角色需确认），不是设计原则本身。测试写法按目标技术栈落地（JUnit / pytest / Jest / Go test 等），本 skill 不提供技术栈无关的测试模板。
7. **回路协同验证**（接入 AuthzCraft 时必做）：有功能权限但无数据授权时，API 通过回路 1 后由回路 2 返回空集或拒绝；无功能权限时直接 403 且不执行数据查询；审计能用同一 `requestKey` 串联回路 1 记录与回路 2 `decisionKey`。
8. **既有系统改造验证**：确认没有新增第二身份源、没有重复数据 PEP、没有前端行过滤；原有回路 2 Demo 场景在新增回路 1 后仍可通过验收。

## 交付物清单

| 交付物 | 内容 | 验证标准 | 模板资产 |
|---|---|---|---|
| 改造前盘点清单 | 既有认证、API、Mapper、PEP、前端、字段权限、验证入口盘点 | 清单明确复用点、改造点、禁止改动点和映射草案 | [`assets/preflight-inventory-template.md`](./assets/preflight-inventory-template.md) |
| DDL 脚本 | 建表 + 种子数据 + 回滚 | 可执行、约束正确 | [`assets/ddl-template.sql`](./assets/ddl-template.sql) |
| 后端服务 | 角色管理接口 + 鉴权拦截器 + 字段权限执行 | 401/403 正确、鉴权真实接入、字段权限真实生效 | [`assets/backend-skeleton.md`](./assets/backend-skeleton.md) |
| 前端配置 | 角色/用户组管理 + 双向批量成员分配 + 功能授权 + 字段授权 + 数据权限配置 + 权限控制 | 真实调用后端/AuthzCraft 接口、权限真实隐藏、数据权限规则保存到回路 2 | [`assets/frontend-skeleton.md`](./assets/frontend-skeleton.md)、`assets/frontend-rbac-page-spec.md`、`assets/authzcraft-rbac-api-contract.md`、`assets/data-permission-template-ast-spec.md`、`assets/rbac-frontend-acceptance-checklist.md` |
| 执行编排 | 任务类型识别、资产加载顺序、阶段门禁、输出格式 | 不跳过盘点/契约/AST/验收，缺口明确标注 | `assets/rbac-system-builder-execution-playbook.md` |
| 回路 2 协同清单 | SDK 依赖、配置、资源映射、主体同步、审计串联 | 功能权限与数据权限各自生效且可审计 | [`assets/backend-skeleton.md`](./assets/backend-skeleton.md) |

## 禁止事项

- 禁止把"谁能登录"和"谁能做什么"混在一个表/接口
- 禁止用角色管理接口推断运行时鉴权行为
- 禁止把角色/权限硬编码在业务代码里
- 禁止输出密钥到明文/日志
- 禁止"生成了文件即完成"，每个交付物必须有可验证目标
- 禁止在需求缺失时瞎猜技术栈、角色清单、权限点清单
- 禁止把回路 2 的行数据权限策略写入回路 1 RBAC 表或前端逻辑
- 禁止业务侧绕过 AuthzCraft 数据 PEP 自行解释 `RowFilterPlan` 之外的策略模型
- 禁止把角色/用户组/成员分配、功能授权、数据权限配置页面做成静态展示或 mock-only 实现
