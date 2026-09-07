# Demo OA 回路 1 RBAC 改造前盘点清单

> 适用范围：`authzcraft-demo-oa` 后端与 `authzcraft-demo-oa-front` 前端。  
> 状态：盘点已确认，并已进入实现。本文继续作为改造边界和验收入口清单维护。

## 1. 系统基线

| 项目 | 现状 | 结论 |
|---|---|---|
| 后端技术栈 | Java 8、Spring Boot 2.3.12、MyBatis Plus、MySQL；模块为 Spring Boot jar。 | 后续 RBAC 后端改造应继续放在 `authzcraft-demo-oa`，遵循 interfaces/application/domain/infrastructure 分层。 |
| 前端技术栈 | Vue 3.5、Vite 6、TypeScript 5.8、Ant Design Vue 4；独立演示台端口 18180。 | 后续 RBAC 配置界面放在 `authzcraft-demo-oa-front`，复用现有 Vite proxy。 |
| 数据库 | 后端连接 MySQL `authzcraft-demo-oa` 业务库；业务表为 5 张 OA 演示表。 | RBAC DDL 应独立新增回路 1 表，不改写现有业务表和回路 2 中心表。 |
| 现有认证/当前用户来源 | 无业务登录、无 CurrentUser 服务、无 RBAC 拦截器；回路 2 PEP 从 `X-AuthzCraft-Requester-Kind` 与 `X-AuthzCraft-Requester-Key` Header 读取访问人。 | 复用 Header/current requester 作为 demo 当前用户来源；禁止新增密码登录或第二身份源。后续需补齐 401 认证失败与 403 功能权限失败。 |
| 用户主体键 | 前端与 PEP 使用 `panhuidong`（管理者）、`fanlaihua`（管理员）、`zhangchunhui`/`guyanzhao`/`wangdaoxin`（普通用户）等 `requesterKey`；AuthzCraft 主体域约定用户键优先为统一身份 `user_id`。 | RBAC 用户表只保存用户投影和主体键，主体键必须与 AuthzCraft PIP 同域；不得改成本地自增 ID 作为鉴权键。 |
| 用户/组织/岗位投影来源 | AuthzCraft 中心已有 `authzcraft_principal`、USER/ORGANIZATION/POSITION 子表和 `/principals/search` 查询接口。 | 默认 `AUTHZCRAFT` 模式下 Demo OA 运行时认证只查询 AuthzCraft ACTIVE USER，不再写本地用户投影；不维护第二套用户、组织、岗位主数据。 |
| 角色/用户组同步 | 中心已具备 `roles/groups/memberships` 实时 CRUD/search，以及 `sync/application-rbac` 批量同步和状态回读。 | 默认 `AUTHZCRAFT` 模式下 Demo OA 角色、用户组和成员关系实时读写中心主体域；本地仅保存 `role_code` 功能/字段授权绑定，旧本地主体表 + outbox 仅作为 `LOCAL_LEGACY` 分支保留。 |
| 安全风险 | `application.properties` 当前包含数据库连接配置与密码字段。 | 后续脚本、日志、验收报告不得输出密钥明文；建议后续把敏感配置改为环境变量或本地覆盖文件。 |

## 2. 回路 1 盘点

| `endpoint` / 前端入口 | `permissionCode` 草案 | 菜单/按钮 | 字段权限候选 | 是否需要新增 |
|---|---|---|---|---|
| `POST /authzcraft-demo-oa/api/v1/meetings/search` | `demo-oa:meetings:read` | 会议记录场景、运行当前场景、批量验证 | `topic`、`host_user_id`、`host_dept_code`、`meeting_source` | 是：Controller 显式声明权限点，拦截器在 Mapper 查询前校验。 |
| `POST /authzcraft-demo-oa/api/v1/meeting-participants/search` | `demo-oa:meeting-participants:read` | 会议参会人场景、运行当前场景、批量验证 | `participant_user_id`、`participant_dept_code`、`participant_dept_path`、`attendance_minutes` | 是：Controller 显式声明权限点，响应字段按后端字段权限处理。 |
| `POST /authzcraft-demo-oa/api/v1/leave-requests/search` | `demo-oa:leave-requests:read` | 请假单场景、运行当前场景、批量验证 | `applicant_user_id`、`applicant_name`、`applicant_dept_code`、`leave_days`、`status` | 是：Controller 显式声明权限点，字段权限候选需业务确认。 |
| `POST /authzcraft-demo-oa/api/v1/expense-requests/search` | `demo-oa:expense-requests:read` | 报销单场景、运行当前场景、批量验证 | `applicant_user_id`、`dept_code`、`expense_type`、`amount`、`status` | 是：`amount` 建议作为脱敏/隐藏验证字段候选。 |
| `POST /authzcraft-demo-oa/api/v1/leave-approvals/search` | `demo-oa:leave-approvals:read` | 请假审批场景、运行当前场景、批量验证 | `approver_user_id`、`approver_name`、`action`、`approved_at` | 是：Controller 显式声明权限点，字段权限候选需业务确认。 |
| 当前前端演示台 | 待从后端 `/auth/me/permissions` 类接口获取 | 当前无菜单/按钮权限控制，仅按 persona 预期展示回路 2 决策 | 当前无 `fieldPermissions` 消费 | 是：新增当前用户权限加载、菜单/按钮隐藏、字段交互表现。 |
| RBAC 管理入口 | 待确认角色清单后生成 | 角色管理、权限配置、成员分配 | 权限项字段说明、字段权限配置 | 是：当前不存在角色 CRUD、权限绑定、成员分配页面。 |

已确认角色清单：使用 `demo_oa_admin`、`demo_oa_manager`、`demo_oa_user`。`admin/manager` 默认拥有 5 个查询功能权限，`user` 默认无查询功能权限。

## 3. 回路 2 现状

| 查询入口 / Mapper | 现有 PEP | `resourceKey` | `operationCode` | 访问路径 | 处理结论 |
|---|---|---|---|---|---|
| `DemoOaMapper.searchMeetings` | 有 | `WB_MEETING` | `READ` | `WB_MEETING_TO_PARTICIPANT`，通过 `wb_meeting.meeting_id -> wb_meeting_participant.meeting_id` 展开 | 复用；禁止在 RBAC 中重新实现会议行过滤。 |
| `DemoOaMapper.searchMeetingParticipants` | 有 | `WB_MEETING_PARTICIPANT` | `READ` | 无显式访问路径 | 复用；行级过滤继续由 PEP 改写 SQL。 |
| `DemoOaMapper.searchLeaveRequests` | 有 | `WB_LEAVE_REQUEST` | `READ` | 无显式访问路径 | 复用；回路 1 只做 API 功能权限。 |
| `DemoOaMapper.searchExpenseRequests` | 有 | `WB_EXPENSE_REQUEST` | `READ` | 无显式访问路径 | 复用；回路 1 不保存本人/部门/管理部门范围。 |
| `DemoOaMapper.searchLeaveApprovals` | 有 | `WB_LEAVE_REQUEST_APPROVAL` | `READ` | 无显式访问路径 | 复用；审批数据范围继续由 AuthzCraft 中心策略控制。 |

回路 2 已具备：`authzcraft-pep-spring-boot-starter` 依赖、`authzcraft.pep.*` 配置、Header requester、中心服务地址、5 个 Mapper 显式映射、PEP 响应 Header、decisionKey 审计反查和回归脚本。后续不得重复生成第二套数据 PEP。

## 4. 双回路映射清单

| `endpoint` | `permissionCode` | Mapper / 查询入口 | `resourceKey` | `operationCode` | 备注 |
|---|---|---|---|---|---|
| `POST /authzcraft-demo-oa/api/v1/meetings/search` | `demo-oa:meetings:read` | `DemoOaMapper.searchMeetings` | `WB_MEETING` | `READ` | 回路 1 通过后才进入回路 2；需共享 `requestKey` 并保留 `decisionKey`。 |
| `POST /authzcraft-demo-oa/api/v1/meeting-participants/search` | `demo-oa:meeting-participants:read` | `DemoOaMapper.searchMeetingParticipants` | `WB_MEETING_PARTICIPANT` | `READ` | 无功能权限时应 403，且不触发 Mapper/PEP。 |
| `POST /authzcraft-demo-oa/api/v1/leave-requests/search` | `demo-oa:leave-requests:read` | `DemoOaMapper.searchLeaveRequests` | `WB_LEAVE_REQUEST` | `READ` | 无 requester 时应先返回 401；有 requester 但无功能权限返回 403。 |
| `POST /authzcraft-demo-oa/api/v1/expense-requests/search` | `demo-oa:expense-requests:read` | `DemoOaMapper.searchExpenseRequests` | `WB_EXPENSE_REQUEST` | `READ` | 字段权限建议覆盖 `amount`。 |
| `POST /authzcraft-demo-oa/api/v1/leave-approvals/search` | `demo-oa:leave-approvals:read` | `DemoOaMapper.searchLeaveApprovals` | `WB_LEAVE_REQUEST_APPROVAL` | `READ` | 字段权限建议覆盖审批人信息。 |

## 5. 字段权限真实校验

| 资源 | 字段 | 权限效果 | 后端读校验 | 后端写校验 | 前端表现 |
|---|---|---|---|---|---|
| `WB_EXPENSE_REQUEST` | `amount` | `MASKED` / `EDIT` | 查询响应由后端按字段权限脱敏或完整返回；前端绕过无效。 | 当前无写接口；若后续新增报销写接口，必须拒绝无编辑权限字段。 | 表格列隐藏或脱敏展示，来源于后端 `fieldPermissions`。 |
| `WB_MEETING_PARTICIPANT` | `participant_dept_path` | `HIDDEN` 候选 | 后端响应投影移除字段。 | 当前无写接口。 | 表格不展示该列。 |
| `WB_LEAVE_REQUEST` | `applicant_name`、`applicant_dept_code` | `MASKED` / `HIDDEN` 候选 | 后端响应投影/脱敏。 | 当前无写接口。 | 表格隐藏或脱敏，不作为安全边界。 |
| `WB_LEAVE_REQUEST_APPROVAL` | `approver_name`、`approver_user_id` | `MASKED` / `HIDDEN` 候选 | 后端响应投影/脱敏。 | 当前无写接口。 | 表格隐藏或脱敏。 |
| 全部资源 | 业务返回字段集合 | `READONLY` / `EDIT` 暂不适用 | 当前 5 个端点均为查询，先落读字段权限。 | 写字段权限待出现创建/更新 API 后再补，不应空造业务写接口。 | 前端只做表现控制。 |

字段权限缺口：当前后端 Mapper 返回 `Map<String,Object>`，没有 DTO 投影层；后续若落字段权限，应在 Application/Interfaces 边界补响应投影或字段过滤服务，不能只让前端隐藏列。

## 6. 改造边界

| 类型 | 文件/模块 | 处理方式 | 原因 |
|---|---|---|---|
| 复用 | `authzcraft-demo-oa` 的 5 个业务查询 Controller/Service/Mapper | 保留业务查询语义，在 Controller 或拦截器层补回路 1 权限声明与校验。 | 现有链路已接入回路 2 PEP，回路 1 应在查询前完成。 |
| 复用 | `authzcraft-demo-oa/src/main/resources/application.properties` 中 `authzcraft.pep.*` 映射 | 只读确认或最小补齐，不重写 PEP 配置。 | 已验证 5 个资源映射和访问路径。 |
| 复用 | `authzcraft/scripts/run-demo-oa-acceptance.ps1` 与 `run-loop2-regression.ps1` | 保留为回路 2 回归入口，新增回路 1 验证时不破坏原断言。 | 脚本已调整为普通用户回路 1 403、缺 requester 401。 |
| 复用 | `authzcraft-demo-oa-front` 当前演示台 | 保留“数据权限演示台”作为回路 2 验收页面。 | 改造目标是叠加 RBAC 配置与功能权限，不替换数据权限演示。 |
| 保留 | `authzcraft-demo-oa` 回路 1 DDL / migration 或独立 SQL | 作为 `LOCAL_LEGACY` 分支保留用户投影、角色、权限、字段权限、用户角色、角色权限、角色字段权限和 outbox。 | 默认 `AUTHZCRAFT` 模式不再使用这些表；保留用于回退和对比验收。 |
| 新增 | 后端 RBAC domain/application/interfaces/infrastructure 包 | 新增角色管理接口、当前用户权限接口、运行时鉴权拦截器、字段权限执行点。 | 当前无角色管理和运行时功能权限。 |
| 新增 | 前端 RBAC API 客户端与配置页 | 新增角色管理、权限配置、成员分配、当前用户权限消费。 | 当前前端无回路 1 管理界面和菜单/按钮权限控制。 |
| 禁止改动 | AuthzCraft 中心 PAP/PDP/PIP 回路 2 策略模型 | 不把本人/部门/管理部门及下级范围搬进 RBAC 表。 | 行数据权限属于回路 2。 |
| 禁止改动 | `authzcraft-pep-spring-boot-starter` 数据 PEP 实现 | 本次不生成第二套 PEP，不在回路 1 拦截器拼 SQL。 | 现有 PEP 已承担 SQL 行过滤。 |
| 禁止改动 | 现有统一主体域语义 | RBAC 角色/成员若参与数据策略，需同步或投影给 AuthzCraft PIP。 | 保持主体键同域，避免第二身份源。 |

## 7. 验证入口

| 验证项 | 命令或步骤 | 通过标准 |
|---|---|---|
| DDL | 后续新增 RBAC SQL 后，在测试库执行建表、唯一约束和回滚脚本。 | 角色名、权限编码、角色-权限、用户-角色、角色-字段权限唯一约束生效；种子数据可回滚。 |
| 后端编译 | `mvn -f authzcraft/pom.xml -pl authzcraft-demo-oa -am test-compile -q` | demo OA 主代码和 smoke helper 编译通过。 |
| PEP SQL 编译回归 | `mvn -f authzcraft/pom.xml -pl authzcraft-pep-spring-boot-starter -am test -q` | 现有 SQL 改写与 DENY_ALL 断言不退化。 |
| 回路 2 端到端 | `authzcraft/scripts/run-loop2-regression.ps1` 或 `authzcraft/scripts/run-demo-oa-acceptance.ps1` | 原有 5 个 OA 查询、PEP Header、decisionKey 审计反查、缺 requester-key 401 保持通过。 |
| 功能权限 | `run-demo-oa-acceptance.ps1` 已验证：中心存在 Demo OA ROLE/membership；未带 requester 返回 401；`zhangchunhui`（普通用户）有身份但无查询 `permissionCode` 返回 403；`panhuidong`（管理者）有功能权限才触发业务查询。 | 403 时不执行 Mapper/PEP；有功能权限但无数据授权时由回路 2 返回空集或拒绝。 |
| 字段权限 | 后续新增测试：无查看权限字段在响应中缺失或脱敏；直接提交无权字段不能生效。 | 字段权限由后端执行，前端隐藏不作为安全边界。 |
| 主体同步/回读 | 默认模式下 Demo OA 通过中心 `roles/groups/memberships` 接口实时读写；`POST /authzcraft-demo-oa/api/v1/rbac/sync/application-rbac/status/search` 回读中心投影状态。 | 2026-08-31 回归已断言中心存在 `demo_oa_admin/demo_oa_manager/demo_oa_user`，`panhuidong -> demo_oa_manager`（管理者）、`fanlaihua -> demo_oa_admin`（管理员）、`zhangchunhui -> demo_oa_user`（普通用户）；本地 outbox 仅 `LOCAL_LEGACY` 使用。 |
| 数据权限模板 | `POST /authzcraft-demo-oa/api/v1/rbac/data-permissions/templates/save` 保存三种模板到 AuthzCraft 回路 2：校验主体/资源/字段，复用或创建标准蓝图、策略、修订、授权和授权参数，并调用 simulation。 | `MY_DATA`、`MANAGED_DEPARTMENTS`、`PERSON_SHARE` 三条实际 HTTP 调用均返回 `code=0` 且 `simulation.assertionPassed=true`；当前管理部门模板按 PIP 现有能力落为“全部管理部门”。 |
| 前端 | `npm --prefix authzcraft/authzcraft-demo-oa-front run build`；人工验证 RBAC 页面真实调用后端接口。 | 菜单/按钮/字段状态来自后端 `permissions` 与 `fieldPermissions`，不得从角色名推导。 |

## 8. 已确认决策

1. 角色编码：`demo_oa_admin`、`demo_oa_manager`、`demo_oa_user`。
2. 默认授权：`admin/manager` 拥有 5 个查询权限，`user` 无查询权限；普通用户请求业务 API 时回路 1 返回 403，不进入回路 2。
3. 字段权限：首批只落 `WB_EXPENSE_REQUEST.amount`，管理员 `EDIT` 完整返回，管理者 `MASKED` 脱敏返回。
4. 当前用户来源：继续使用 Header requester，不新增密码登录或第二身份源。
5. 角色/成员参与回路 2 数据策略：默认 `AUTHZCRAFT` 模式下 Demo OA 不再以本地 outbox 为权威，而是实时调用 AuthzCraft `roles/groups/memberships` 接口维护 ROLE/GROUP 主体和成员关系；功能/字段授权通过本地 `role_code` 绑定表生效，不再按角色编码硬编码推导。前端中心回读表直接展示主体 ID、成员关系 ID、生命周期、有效期和失败原因。旧本地 outbox 分支通过 `LOCAL_LEGACY` 保留。
6. 数据权限模板：前端只收集业务化模板参数；Demo OA 后端代理负责编译为 AuthzCraft 标准策略修订并绑定授权，保存后通过中心 simulation 验证；行过滤仍由回路 2 PEP 执行。