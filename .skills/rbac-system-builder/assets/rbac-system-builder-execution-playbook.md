# RBAC System Builder 执行编排

> 本 playbook 用于约束 `rbac-system-builder` 的实际执行顺序。目标是避免只生成局部代码或孤立页面，确保回路 1 RBAC、AuthzCraft 主体管理、回路 2 数据权限配置入口、AST 编译、验收检查形成闭环。

## 一、总原则

1. 先盘点，再改造；既有系统未完成盘点前不得直接写 DDL、后端或前端代码。
2. 先契约，再页面；前端实现前必须确认 API 字段契约或输出缺口清单。
3. 先功能权限，再数据权限；回路 1 判断能不能访问功能/API，回路 2 判断能看到哪些行。
4. 前端只做配置入口和交互控制；行数据过滤、字段权限执行、功能鉴权必须由后端完成。
5. mock 只能用于临时开发，不得作为完成依据。
6. 每个阶段都有可执行验证；无法验证时必须说明阻塞点和替代验收方式。

## 二、资产加载顺序

执行 `rbac-system-builder` 时按需读取以下资产，不得跳过与当前任务相关的资产：

| 阶段 | 必读资产 | 目的 |
|---|---|---|
| 既有系统盘点 | `preflight-inventory-template.md` | 确认认证、主体、API、Mapper、PEP、前端入口和验证边界 |
| 后端实现 | `backend-skeleton.md`、`authzcraft-rbac-api-contract.md` | 确认角色/用户组/成员、功能权限、字段权限、当前用户权限接口契约 |
| 前端实现 | `frontend-skeleton.md`、`frontend-rbac-page-spec.md`、`authzcraft-rbac-api-contract.md` | 确认页面信息架构、组件交互、API client 字段和状态处理 |
| 数据权限模板 | `data-permission-template-ast-spec.md`、`authzcraft-rbac-api-contract.md` | 确认五项模板如何生成 Predicate AST、授权参数和 simulation |
| 验收 | `rbac-frontend-acceptance-checklist.md` | 逐项确认前端、后端、回路协同是否完成 |

## 三、执行流程

### 0. 识别任务类型

先判断用户需求属于哪类：

- **新建 RBAC**：需要 DDL、后端、前端、验收全链路。
- **改造既有系统**：必须先输出改造前盘点清单。
- **只做前端管理页**：仍需确认 API 契约；接口缺失时输出缺口清单。
- **只做数据权限模板**：必须读取 AST 编译规格，确认回路 2 API 与 PIP 能力。
- **只做验收/评审**：直接按验收清单检查，输出通过项、失败项、阻塞项。

### 1. 盘点与决策

必须确认：

- 当前用户来源、统一身份键、是否存在第二身份源风险。
- 用户、组织、岗位、角色、用户组是否来自 AuthzCraft 主体域。
- 运行时权威源是否唯一：默认 `AUTHZCRAFT`，`LOCAL` 是否仅作为条件装配的隔离适配器。
- `storage-mode` 判断是否只在配置类/条件 Bean/模块 wiring 中出现，业务服务、Controller、拦截器、前端是否无双模式分支。
- 目标系统是否已有 AuthzCraft PEP SDK/Starter。
- 前端是否已有路由、API client、权限 store、组件库。
- 功能权限点、字段权限点、数据资源和操作是否已有基线。
- 五项数据权限模板所需数据归属字段是否存在。
- 需要按明细/关联表判断数据范围时，必须盘点 `authzcraft_access_path` 和业务侧 PEP access-path 映射是否存在。

决策规则：

- 默认运行模式为 `AUTHZCRAFT`，通过 AuthzCraft API 读写人员/组织/岗位/角色/用户组/成员关系；但生成 DDL 时必须保留 `LOCAL` 可运行所需的本地 `users/roles/user_roles/groups/user_groups` 表。
- 运行时代码必须采用“单端口 + 条件适配器”：业务层只依赖 `PrincipalDirectory` / `PrincipalAdminGateway` / `RoleAuthorizationStore` 等端口；`AuthzCraft*` 与 `Local*` 实现只能由配置类/条件 Bean 选择。
- `LOCAL` 是显式启用的隔离适配器，使用同一份 DDL 的本地主体/成员表；不要默认生成同步 outbox，除非用户明确要求异步同步。
- `AUTHZCRAFT` 模式下必须做“权威键检查”：角色、用户组和成员关系以 AuthzCraft `roleCode/groupCode/userKey` 为业务键；本地功能/字段授权绑定只能使用这些编码键，不得依赖本地 `role_id/group_id/user_id` 外键。
- `AUTHZCRAFT` 和 `LOCAL` 必须隔离：`AUTHZCRAFT` 运行时不得写本地主体表、不得读本地 `users/roles/groups/user_roles/user_groups` 修补主体、角色、用户组、成员关系或中文名；`LOCAL` 才能读取本地主体/成员表。两种模式下功能/字段授权都必须走 `role_code` 绑定表，禁止生成或读取旧 `role_id` 绑定表。
- `AUTHZCRAFT` 用户中文名、组织、岗位必须来自 `/principals/users/search`、`/organizations/search`、`/positions/search` 投影接口；不得用通用 `/principals/search` 替代，不得在投影缺失时静默回退本地 users 表。
- 数据权限规则不得进入回路 1 RBAC 表，只能保存到 AuthzCraft 回路 2。
- 已有 PEP 不重建，只补配置、映射和验证。

### 2. 后端契约与实现

先对照 `authzcraft-rbac-api-contract.md`：

- 角色、用户组、成员关系接口字段是否齐全。
- 功能授权、字段授权、当前用户权限接口是否可用。
- 数据权限资源、模板保存、审计查询接口是否可用。
- 长 ID 是否以字符串传输。
- 批量接口是否能返回成功数、失败数和失败明细。

若接口缺失，必须输出缺口清单，包含：缺失 API、使用页面、阻塞程度、请求字段草案、响应字段草案、临时不可验收项。

### 3. 数据权限模板后端代理

当实现“我的数据”“我管理部门的数据”“分享某人的数据权限给某人”时，必须按 `data-permission-template-ast-spec.md`：

1. 校验主体、资源、操作和数据归属字段。
2. 编译 Predicate AST。
3. 生成 `attribute_references` 和 `argument_schema`。
4. 创建或复用策略、策略修订和授权绑定。
5. 写入授权参数。
6. 调用 simulation。
7. simulation 通过后再激活或返回可启用状态。

“我管理部门的数据”依赖 AuthzCraft 回路 2 的 `MANAGED_DEPARTMENT_CODES` 参数化解析：直接管理部门基于 `authzcraft_organization_closure.depth = 0`，下级层级基于 `depth > 0` 和 `subDepartmentDepth`，分管部门基于 `authzcraft_principal_organization.portion_manage_user_id`。若目标环境 PIP 尚未实现该解析，必须输出缺口，不能宣称“我管理部门的数据”完整可用，也不能在业务系统或前端自行计算部门集合。

实现前必须确认中心运行时实际支持的 Predicate 形态：如果业务侧 PEP 只支持普通 `EQ/IN/AND/OR/EXISTS_PATH` 等操作符，不得提交会被中心包装成 `CUSTOM_AST` 的自由 AST；应使用中心可编译为普通 Predicate AST 的通用蓝图（STANDARD_BLUEPRINT 或 CUSTOM_BLUEPRINT）或等价受控编译路径。策略、修订和授权必须按稳定 `policyKey/grantKey/contentDigest` 幂等复用，重复保存不得因唯一约束失败。

### 4. 前端实现

按 `frontend-rbac-page-spec.md` 实现：

- 角色管理、用户组管理、人员分配、成员管理、功能授权、字段授权、数据权限配置。
- 成员分配必须有人员维度和角色/用户组维度双向入口。
- 功能授权优先使用资源树 + 权限矩阵或树表。
- 五项数据权限必须使用模板向导，不暴露 AST、SQL、PIP 参数名。
- 关联字段数据范围必须使用“关联表 + 目标资源字段”选择，前端内部映射已登记 access path，后端编译 `EXISTS_PATH`；不得让前端或业务人员填写 access path key、SQL join/子查询。
- 批量操作必须有差异预览、二次确认、结果反馈和失败明细。

前端 API client 必须通过 adapter 对齐 `authzcraft-rbac-api-contract.md`，页面组件不得直接依赖多个后端响应形态。

### 5. 验收与收口

按 `rbac-frontend-acceptance-checklist.md` 逐项验收：

- 通过项：列出证据，如测试、截图、接口响应、日志或人工验收步骤。
- 失败项：说明失败行为、影响范围和建议修复点。
- 阻塞项：说明缺失接口、缺失数据、缺失 PIP 能力或环境问题。
- 不适用项：说明为什么不适用，不能直接删除。

运行 jar 的项目必须先重新打包再跑端到端验收；`test-compile` 只能证明源码编译，不能证明启动脚本使用了新代码。若回归同时启动 AuthzCraft 中心和业务应用，二者必须一起打包，避免源码和 jar 的接口契约不一致。

## 四、阶段门禁

| 阶段 | 进入条件 | 退出条件 |
|---|---|---|
| 盘点 | 用户要求新建或改造 RBAC | 输出复用点、改造点、禁止改动点和验证入口 |
| 后端契约 | 需要生成前端或后端接口 | API 契约齐全，或缺口清单明确 |
| 后端实现 | 契约确认或用户接受缺口处理方案 | 401/403、功能授权、字段权限、主体管理可验证 |
| 数据权限模板 | 用户需要五项通用数据权限配置 | AST 编译、授权绑定、simulation 可验证，或缺口明确 |
| 前端实现 | API client 和路由入口确认 | 页面真实调用接口，状态处理完整，无 mock-only 完成 |
| 验收 | 实现完成或阶段性完成 | 验收清单有通过/失败/阻塞结论 |

## 五、输出格式

执行完成后输出：

1. 改动摘要：按后端、前端、数据权限、文档分类。
2. 验证结果：列出实际运行的测试、构建、接口检查或人工验收步骤。
3. 缺口清单：如果有，明确阻塞程度和下一步。
4. 风险说明：是否存在 mock-only、接口未就绪、PIP 能力缺失、前端行过滤风险。

## 六、禁止事项

- 禁止跳过盘点直接改既有系统。
- 禁止没有 API 契约就生成依赖猜测字段的前端页面。
- 禁止把五项数据权限模板保存到回路 1 RBAC 表。
- 禁止前端生成或编辑底层 Predicate AST。
- 禁止 simulation 未通过就宣称数据权限配置完成。
- 禁止验收时只说“页面已生成”，必须按行为证明完成。