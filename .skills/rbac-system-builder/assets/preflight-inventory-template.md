# 改造前盘点清单模板

> 适用于既有系统接入或补齐回路 1 RBAC。清单确认前，不得进入 DDL、后端或前端改造。

## 1. 系统基线

| 项目 | 现状 | 结论 |
|---|---|---|
| 后端技术栈 |  |  |
| 前端技术栈 |  |  |
| 数据库 |  |  |
| 现有认证/当前用户来源 |  | 复用 / 补齐 / 禁止新增第二身份源 |
| 用户主体键 |  | 是否与 AuthzCraft 主体域同域 |
| 用户/组织/岗位来源 |  | 默认从 AuthzCraft API 实时获取（`/principals/users/search`、`/organizations/search`、`/positions/search`）/ 需补齐 / 存在第二主数据风险 |
| 角色/用户组同步 |  | 默认通过 AuthzCraft API CRUD（`/principals/roles`、`/principals/groups`）/ 需补齐 |
| 成员关系同步 |  | 默认通过 AuthzCraft API CRUD（`/principals/memberships`）/ 需补齐 |
| 运行时权威源 |  | 默认 `AUTHZCRAFT` 单权威 / `LOCAL` 隔离适配器 / 存在混读风险 |
| 模式装配位置 |  | 仅配置类/条件 Bean / 业务代码中存在 `storage-mode` 分支需整改 |

## 2. 回路 1 盘点

| `endpoint` / 前端入口 | `permissionCode` 草案 | 菜单/按钮 | 字段权限候选 | 是否需要新增 |
|---|---|---|---|---|
|  |  |  |  |  |

## 3. 回路 2 现状

| 查询入口 / Mapper | 现有 PEP | `resourceKey` | `operationCode` | 访问路径 | 处理结论 |
|---|---|---|---|---|---|
|  | 有 / 无 |  |  |  | 复用 / 补齐 / 禁止改动 |

## 4. 双回路映射清单

| `endpoint` | `permissionCode` | Mapper / 查询入口 | `resourceKey` | `operationCode` | 备注 |
|---|---|---|---|---|---|
|  |  |  |  |  |  |

## 5. 字段权限真实校验

| 资源 | 字段 | 权限效果 | 后端读校验 | 后端写校验 | 前端表现 |
|---|---|---|---|---|---|
|  |  | security_requirement: PLAIN/MASKED/HIDDEN（字段固有） × access_level: NONE/READ/PLAIN_READ/WRITE（授权可变） | 隐藏 / 脱敏 / 原样返回 | 拒绝 / 剔除 / 允许 | 隐藏 / 只读 / 脱敏展示 |

## 6. 改造边界

| 类型 | 文件/模块 | 处理方式 | 原因 |
|---|---|---|---|
| 复用 |  |  |  |
| 新增 |  |  |  |
| 禁止改动 |  |  |  |

## 7. 主体同步契约

> **默认运行模式：`AUTHZCRAFT`；运行时单权威路径；DDL 必须支持双模式。**
> 默认运行时“权威/读取方向”和“写入方向”均指向 AuthzCraft API；DDL 仍必须保留 `LOCAL` 可运行所需的本地 `users/roles/user_roles/groups/user_groups` 表，但这些表只能由显式 `LOCAL` 适配器读取。功能/字段授权两种模式统一使用 `role_code` 绑定表。

| 主体类型 | 权威/读取方向 | 写入方向 | 回读/对账 | 结论 |
|---|---|---|---|---|
| 用户 | AuthzCraft 主体域（`/principals/users/search` 实时获取） | 禁止回路 1 创建第二主数据 | 校验 `user_id`、生命周期状态 |  |
| 组织 | AuthzCraft 主体域（`/principals/organizations/search` 实时获取） | 禁止回路 1 创建第二主数据 | 校验组织编码、生命周期状态 |  |
| 岗位 | AuthzCraft 主体域（`/principals/positions/search` 实时获取） | 禁止回路 1 创建第二主数据 | 校验岗位编码、生命周期状态 |  |
| 角色 | AuthzCraft 主体域（`/principals/roles` + `/principals/roles/search`） | 通过 AuthzCraft API CRUD | 回读 `principalId`、状态、失败原因 |  |
| 用户组 | AuthzCraft 主体域（`/principals/groups` + `/principals/groups/search`） | 通过 AuthzCraft API CRUD | 回读 `principalId`、状态、失败原因 |  |
| 成员关系 | AuthzCraft 主体域（`/principals/memberships` + `/principals/memberships/search`） | 通过 AuthzCraft API CRUD | 回读版本、状态、生效失效区间、失败原因 |  |

> **`LOCAL` 运行模式**：只能通过独立 `Local*` 适配器读取本地 `users/roles/groups/user_roles/user_groups`；业务服务不得同时调用 AuthzCraft 与本地表。若这些主体参与回路 2 数据策略，必须明确如何投影到 AuthzCraft PIP。默认不要增加 outbox，同步/对账优先使用实时 API 或快照接口；只有用户明确要求异步同步时才设计 outbox。

## 8. 验证入口

| 验证项 | 命令或步骤 | 通过标准 |
|---|---|---|
| DDL |  | 双模式基线表齐全：`users/roles/user_roles/groups/user_groups/permissions/field_permissions/role_permissions/role_field_permissions`；建表、唯一约束、回滚通过 |
| 功能权限 |  | 无权限 403，且不触发业务查询/数据 PEP |
| 字段权限 |  | 后端响应隐藏/脱敏；直接提交无权字段不能写入 |
| 角色查询 |  | 通过 AuthzCraft `/principals/memberships/search` 获取角色集合，可短 TTL 缓存 |
| 主体数据 |  | `AUTHZCRAFT` 模式通过 AuthzCraft API 实时获取/写入；`LOCAL` 模式只由独立适配器读取本地主体/成员表；业务层无双模式分支、无混读回退 |
| 回路 2 |  | PEP 行过滤、decisionKey、审计反查保持通过 |
| 前端 |  | 权限来自后端，按钮/字段状态正确 |