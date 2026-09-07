<template>
  <a-config-provider :theme="theme">
    <main class="shell">
      <header class="topbar">
        <div class="brand">
          <h1>业务数据权限演示台</h1>
          <span class="brand-sub">AuthzCraft Demo OA</span>
        </div>
        <div class="service-status">
          <a-tag :color="centerOnline ? 'success' : 'error'" class="status-tag">
            <a-badge :status="centerOnline ? 'success' : 'error'" />
            数据权限中心服务 8088 {{ centerOnline ? '在线' : '离线' }}
          </a-tag>
          <a-tag :color="demoOnline ? 'success' : 'error'" class="status-tag">
            <a-badge :status="demoOnline ? 'success' : 'error'" />
            Demo OA 18080 {{ demoOnline ? '在线' : '离线' }}
          </a-tag>
        </div>
      </header>

      <section class="app-layout">
        <aside class="side-nav">
          <div class="current-user-card">
            <span>当前访问人</span>
            <a-select v-model:value="personaKey" :options="personaOptions" size="small" />
            <a-tooltip :title="currentUserMeta" placement="bottomLeft">
              <small class="current-user-meta">{{ currentUserMeta }}</small>
            </a-tooltip>
          </div>
          <nav class="side-menu" aria-label="主菜单">
            <section v-if="canReadBusinessMenu" class="menu-group">
              <div class="menu-group-title">
                <AppstoreOutlined />
                <span>业务处理</span>
              </div>
              <button v-if="canReadMeetingMenu" class="menu-entry child" :class="{ active: selectedLeftMenuKey === 'meeting-analytics' }" type="button" @click="selectMenu('meeting-analytics')">会议信息</button>
              <button v-if="canReadExpenseMenu" class="menu-entry child" :class="{ active: selectedLeftMenuKey === 'expense-applications' }" type="button" @click="selectMenu('expense-applications')">费用申请</button>
              <button v-if="canReadLeaveMenu" class="menu-entry child" :class="{ active: selectedLeftMenuKey === 'leave-applications' }" type="button" @click="selectMenu('leave-applications')">请假申请</button>
            </section>
            <section v-if="canReadRbac" class="menu-group">
              <div class="menu-group-title">
                <SettingOutlined />
                <span>权限管理</span>
              </div>
              <button class="menu-entry child" :class="{ active: selectedLeftMenuKey === 'rbac:roles' }" type="button" @click="selectRbacSubMenu('roles')">角色管理</button>
              <button class="menu-entry child" :class="{ active: selectedLeftMenuKey === 'rbac:groups' }" type="button" @click="selectRbacSubMenu('groups')">用户组管理</button>
              <button class="menu-entry child" :class="{ active: selectedLeftMenuKey === 'rbac:memberships' }" type="button" @click="selectRbacSubMenu('memberships')">成员管理</button>
              <button class="menu-entry child" :class="{ active: selectedLeftMenuKey === 'rbac:authorization' }" type="button" @click="selectRbacSubMenu('authorization')">授权管理</button>
            </section>
          </nav>
        </aside>

        <section class="page-stage">
      <section v-if="activeMenu === 'rbac'" class="rbac-band">
        <template v-if="canReadRbac">
          <RbacPrincipalsPanel
            v-if="rbacSubMenu === 'roles' || rbacSubMenu === 'groups'"
            v-model:role-draft="roleDraft"
            v-model:group-draft="groupDraft"
            :mode="rbacSubMenu"
            :roles="roles"
            :groups="groups"
            :selected-role-id="selectedRoleId"
            :selected-group-id="selectedGroupId"
            :can-manage-rbac="canManageRbac"
            :loading="rbacLoading"
            @create-role="createRole"
            @create-group="createGroup"
            @update-role="updateRole"
            @update-group="updateGroup"
            @delete-role="deleteRole"
            @delete-group="deleteGroup"
            @batch-delete-roles="batchDeleteRoles"
            @batch-disable-groups="batchDisableGroups"
            @inspect-role="openRoleDetail"
            @inspect-group="openGroupDetail"
            @copy-role="openRoleCopy"
            @copy-group="openGroupCopy"
            @select-role-permissions="selectRoleForPermissions"
            @select-role-data-permissions="selectRoleForDataPermissions"
            @select-role-memberships="selectRoleForMemberships"
            @select-group-memberships="selectGroupForMemberships"
            @select-group-data-permissions="selectGroupForDataPermissions"
          />

          <RbacMembershipsPanel
            v-if="rbacSubMenu === 'memberships'"
            v-model:member-user-keys="memberDraft.userKeys"
            v-model:group-member-user-keys="groupMemberDraft.userKeys"
            v-model:assignment-draft="assignmentDraft"
            v-model:selected-membership-user-key="selectedMembershipUserKey"
            v-model:membership-mode="rbacMembershipMode"
            :selected-role-id="selectedRoleId"
            :selected-role-name="selectedRoleName"
            :selected-group-id="selectedGroupId"
            :selected-group-name="selectedGroupName"
            :roles="roles"
            :groups="groups"
            :role-users="roleUsers"
            :group-users="groupUsers"
            :user-memberships="userMemberships"
            :user-membership-loading="userMembershipLoading"
            :assignment-target-type-options="assignmentTargetTypeOptions"
            :assignment-target-options="assignmentTargetOptions"
            :assignment-can-save="assignmentCanSave"
            :batch-assignment-summary="batchAssignmentSummary"
            :batch-assignment-failures="batchAssignmentFailures"
            :back-label="rbacReturnLabel"
            :entry-mode="membershipsEntryMode"
            :user-options="allUserOptions"
            :can-manage-rbac="canManageRbac"
            :loading="rbacLoading"
            :filter-user-option="filterUserOption"
            @select-role="selectRole"
            @select-group="selectGroup"
            @add-role-user="addRoleUser"
            @remove-role-user="removeRoleUser"
            @batch-remove-role-users="batchRemoveRoleUsers"
            @add-group-user="addGroupUser"
            @remove-group-user="removeGroupUser"
            @batch-remove-group-users="batchRemoveGroupUsers"
            @load-user-memberships="loadUserMemberships"
            @back-to-list="returnToRbacList"
            @save-batch-assignments="saveBatchAssignments"
          />

          <a-row v-if="rbacSubMenu === 'authorization'" :gutter="14" class="rbac-grid">
            <a-col :xs="24">
              <AuthorizationPanel
                :role="selectedRole"
                :roles="roles"
                :groups="groups"
                :selected-role-id="selectedRoleId"
                :selected-role-name="selectedRoleName"
                :selected-permission-codes="selectedPermissionCodes"
                :selected-field-permission-codes="selectedFieldPermissionCodes"
                :original-permission-codes="originalPermissionCodes"
                :original-field-permission-codes="originalFieldPermissionCodes"
                :permission-options="permissionOptions"
                :field-permission-options="fieldPermissionOptions"
                :back-label="rbacReturnLabel"
                :entry-mode="authzEntryMode"
                :locked-principal-code="authzLockedPrincipalCode"
                :can-manage-rbac="canManageRbac"
                :loading="rbacLoading"
                :user-options="allUserOptions"
                :department-options="departmentOptions"
                :position-options="positionOptions"
                :persona-key="personaKey"
                :filter-user-option="filterUserOption"
                @update:selected-permission-codes="selectedPermissionCodes = $event"
                @update:selected-field-permission-codes="selectedFieldPermissionCodes = $event"
                @select-role="selectRole"
                @select-group="selectGroupForAuthz"
                @back-to-list="returnToRbacList"
                @save-permissions="savePermissions"
                @save-field-permissions="saveFieldPermissions"
                @field-permissions-changed="reloadFieldPermissions"
              />
            </a-col>
          </a-row>
        </template>
      </section>

      <section v-if="activeBusinessConfig" class="business-page">
        <div v-if="showBusinessSegmented" class="business-hero">
          <a-segmented v-model:value="activeBusinessScenarioKey" :options="activeBusinessDatasetOptions" />
        </div>

        <a-alert
          v-if="!activeBusinessReadable"
          type="warning"
          show-icon
          class="business-alert"
          message="当前访问人没有该菜单权限"
          description="菜单入口来自后端当前用户权限接口；直接调用业务 API 仍会由后端返回 403。"
        />
        <a-alert
          v-else-if="restrictedBusinessDatasets.length"
          type="warning"
          show-icon
          class="business-alert"
          :message="`以下数据集因缺少对应功能权限暂不可用：${restrictedBusinessDatasets.join('、')}`"
        />

        <template v-else>
          <a-row v-if="activeBusinessConfig.masterDetail" :gutter="14" class="business-grid master-detail-grid">
            <a-col :xs="24" :xl="masterDetailCollapsed ? undefined : 15" :style="masterDetailCollapsed ? { flex: '1 1 0', maxWidth: 'none' } : undefined">
              <a-card :bordered="false" class="business-panel">
                <template #title>
                  <span class="title-with-icon"><TableOutlined />{{ activeBusinessDatasetTitle }}</span>
                </template>
                <template #extra>
                  <a-tag :color="businessActiveResult && resultPassed(businessActiveResult) ? 'success' : 'default'">
                    {{ businessActiveResult ? businessResultSummary(businessActiveResult) : '待加载' }}
                  </a-tag>
                </template>
                <a-empty v-if="!businessActiveResult" description="点击刷新业务数据加载当前菜单" />
                <template v-else>
                  <a-alert
                    v-if="businessActiveResult.errorMessage"
                    type="error"
                    show-icon
                    class="business-alert"
                    :message="businessActiveResult.errorMessage"
                  />
                  <a-table
                    size="small"
                    :pagination="false"
                    :scroll="{ x: 780 }"
                    :columns="businessTableColumns"
                    :data-source="businessTableRows"
                    row-key="__rowKey"
                    :custom-row="masterDetailRowProps"
                    :row-class-name="masterDetailRowClassName"
                  />
                </template>
              </a-card>
            </a-col>
            <a-col :xs="24" :xl="masterDetailCollapsed ? undefined : 9" :style="masterDetailCollapsed ? { flex: '0 0 36px', maxWidth: '36px', minWidth: '36px' } : undefined">
              <a-card v-if="masterDetailCollapsed" :bordered="false" class="business-panel detail-collapsed">
                <a-button type="text" block class="detail-expand-btn" @click="masterDetailCollapsed = false">
                  <DoubleLeftOutlined />
                  <span class="vertical-text">审批信息</span>
                </a-button>
              </a-card>
              <a-card v-else :bordered="false" class="business-panel detail-panel">
                <template #title>
                  <span class="title-with-icon">
                    <TableOutlined />{{ masterDetailChildScenario?.title ?? '明细' }}
                    <span v-if="selectedMasterRow" class="title-sub">· {{ masterDetailTitle(selectedMasterRow) }}</span>
                  </span>
                </template>
                <template #extra>
                  <a-space :size="8">
                    <a-button v-if="activeMenu === 'leave-applications'" size="small" type="text" @click="masterDetailCollapsed = true">
                      <DoubleRightOutlined />收起明细
                    </a-button>
                    <a-tag v-if="selectedMasterRow" color="success">{{ masterDetailChildSummarySub }}</a-tag>
                  </a-space>
                </template>
                <a-empty v-if="!selectedMasterRow" :description="`暂无${activeBusinessConfig.title}记录`" />
                <template v-else>
                  <div class="participant-list">
                    <a-empty v-if="!selectedMasterChildRows.length" :description="`该${activeBusinessConfig.title}暂无可见${masterDetailChildScenario?.title ?? '明细'}`" />
                    <div v-for="row in selectedMasterChildRows" v-else :key="row.__rowKey" class="participant-item">
                      <div class="participant-main">
                        <strong>{{ participantDetailName(row) }}</strong>
                        <span>{{ participantDetailSub(row) }}</span>
                      </div>
                      <a-tag color="processing">{{ participantDetailTag(row) }}</a-tag>
                    </div>
                  </div>
                </template>
              </a-card>
            </a-col>
          </a-row>

          <a-row v-else :gutter="14" class="business-grid">
            <a-col :span="24">
              <a-card :bordered="false" class="business-panel">
                <template #title>
                  <span class="title-with-icon"><TableOutlined />{{ activeBusinessDatasetTitle }}</span>
                </template>
                <template #extra>
                  <a-space :size="8">
                    <a-button v-if="hasBusinessFormFields" size="small" type="primary" @click="openBusinessCreateDrawer">
                      <PlusOutlined />新建
                    </a-button>
                    <a-tag :color="businessActiveResult && resultPassed(businessActiveResult) ? 'success' : 'default'">
                      {{ businessActiveResult ? businessResultSummary(businessActiveResult) : '待加载' }}
                    </a-tag>
                  </a-space>
                </template>
                <a-empty v-if="!businessActiveResult" description="点击刷新业务数据加载当前菜单" />
                <template v-else>
                  <a-alert
                    v-if="businessActiveResult.errorMessage"
                    type="error"
                    show-icon
                    class="business-alert"
                    :message="businessActiveResult.errorMessage"
                  />
                  <a-table
                    size="small"
                    :pagination="false"
                    :scroll="{ x: 780 }"
                    :columns="businessTableColumns"
                    :data-source="businessTableRows"
                    row-key="__rowKey"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'actions'">
                        <a-space :size="4">
                          <a-button size="small" type="link" @click="openBusinessEditDrawer(record)">修改</a-button>
                          <a-popconfirm title="确认删除该记录？" @confirm="deleteBusinessRecord(record)">
                            <a-button size="small" type="link" danger>删除</a-button>
                          </a-popconfirm>
                        </a-space>
                      </template>
                    </template>
                  </a-table>
                </template>
              </a-card>
            </a-col>
          </a-row>

          <a-collapse v-model:active-key="evidencePanelKeys" class="evidence-collapse" :bordered="false">
            <a-collapse-panel key="dual-loop">
              <template #header>
                <span class="title-with-icon"><AuditOutlined />双回路检查</span>
                <a-tooltip title="逐项核对回路 1（功能权限）与回路 2（数据权限）在各环节的表现，“差异”代表与预期不符">
                  <QuestionCircleOutlined class="hint-icon" @click.stop />
                </a-tooltip>
              </template>
              <a-row :gutter="12">
                <a-col v-for="check in businessChecks" :key="check.name" :xs="24" :md="12" :xl="8">
                  <a-card class="check-card" :class="check.passed ? 'passed' : 'failed'" size="small" :bordered="false">
                    <div class="check-card-head">
                      <h3>{{ check.name }}</h3>
                      <a-tag :color="check.passed ? 'success' : 'error'">{{ check.passed ? '通过' : '差异' }}</a-tag>
                    </div>
                    <p>预期：{{ check.expected }}</p>
                    <p>实际：{{ check.actual }}</p>
                    <p class="check-desc">{{ checkDescription(check.name) }}</p>
                  </a-card>
                </a-col>
              </a-row>
            </a-collapse-panel>
          </a-collapse>
        </template>
      </section>
        </section>
      </section>

      <a-modal v-model:open="deleteImpactOpen" :title="deleteImpactTitle" ok-text="确认执行" cancel-text="取消" :confirm-loading="rbacLoading" :ok-button-props="deleteImpactOkButtonProps" @ok="confirmDeletePrincipal">
        <a-descriptions v-if="deleteImpact" size="small" :column="1" bordered>
          <a-descriptions-item label="对象">{{ deleteImpactPrincipalName }}</a-descriptions-item>
          <a-descriptions-item label="成员数量">{{ deleteImpact.userCount ?? 0 }}</a-descriptions-item>
          <a-descriptions-item v-if="pendingDeleteRole" label="功能权限">{{ deleteImpact.permissionCount ?? 0 }}</a-descriptions-item>
          <a-descriptions-item v-if="pendingDeleteRole" label="字段权限">{{ deleteImpact.fieldPermissionCount ?? 0 }}</a-descriptions-item>
          <a-descriptions-item v-if="pendingDeleteGroup" label="关联角色">{{ deleteImpact.roleCount ?? 0 }}</a-descriptions-item>
          <a-descriptions-item v-if="pendingDeleteGroup" label="数据权限">{{ deleteImpact.dataPermissionCount ?? 0 }}</a-descriptions-item>
        </a-descriptions>
      </a-modal>

      <a-drawer v-model:open="principalDetailOpen" :title="principalDetailTitle" width="520">
        <a-skeleton v-if="principalDetailLoading" active />
        <a-descriptions v-else-if="principalDetail" size="small" :column="1" bordered>
          <a-descriptions-item label="名称">{{ principalDetail.name }}</a-descriptions-item>
          <a-descriptions-item label="编码">{{ principalDetail.code }}</a-descriptions-item>
          <a-descriptions-item label="说明">{{ principalDetail.description || '-' }}</a-descriptions-item>
          <a-descriptions-item label="成员数量">{{ principalDetail.userCount }}</a-descriptions-item>
          <a-descriptions-item v-if="principalDetail.kind === 'role'" label="功能权限">{{ principalDetail.permissionCount }}</a-descriptions-item>
          <a-descriptions-item v-if="principalDetail.kind === 'role'" label="字段权限">{{ principalDetail.fieldPermissionCount }}</a-descriptions-item>
        </a-descriptions>
      </a-drawer>

      <a-modal v-model:open="roleCopyOpen" title="复制角色" ok-text="创建并复制权限" cancel-text="取消" :confirm-loading="rbacLoading" :ok-button-props="roleCopyOkButtonProps" @ok="confirmRoleCopy">
        <a-form layout="vertical" class="compact-form">
          <a-form-item label="来源角色">
            <a-input :value="roleCopySourceLabel" disabled />
          </a-form-item>
          <a-form-item label="新角色编码">
            <a-input v-model:value="roleCopyDraft.code" :disabled="!canManageRbac" />
          </a-form-item>
          <a-form-item label="新角色名称">
            <a-input v-model:value="roleCopyDraft.name" :disabled="!canManageRbac" />
          </a-form-item>
          <a-form-item label="说明">
            <a-input v-model:value="roleCopyDraft.description" :disabled="!canManageRbac" />
          </a-form-item>
        </a-form>
      </a-modal>

      <a-modal v-model:open="groupCopyOpen" title="复制用户组" ok-text="创建并复制成员" cancel-text="取消" :confirm-loading="rbacLoading" :ok-button-props="groupCopyOkButtonProps" @ok="confirmGroupCopy">
        <a-form layout="vertical" class="compact-form">
          <a-form-item label="来源用户组">
            <a-input :value="groupCopySourceLabel" disabled />
          </a-form-item>
          <a-form-item label="新用户组编码">
            <a-input v-model:value="groupCopyDraft.code" :disabled="!canManageRbac" />
          </a-form-item>
          <a-form-item label="新用户组名称">
            <a-input v-model:value="groupCopyDraft.name" :disabled="!canManageRbac" />
          </a-form-item>
          <a-form-item label="说明">
            <a-input v-model:value="groupCopyDraft.description" :disabled="!canManageRbac" />
          </a-form-item>
        </a-form>
      </a-modal>

      <a-drawer
        v-model:open="businessDrawerOpen"
        :title="businessDrawerMode === 'create' ? `新建${activeBusinessDatasetTitle}` : `修改${activeBusinessDatasetTitle}`"
        width="480"
      >
        <a-form layout="vertical" class="compact-form">
          <a-form-item v-for="field in activeBusinessFormFields" :key="field.field" :label="field.label">
            <a-select
              v-if="field.type === 'select'"
              v-model:value="businessDraft[field.field]"
              :options="field.options"
              allow-clear
            />
            <a-input-number
              v-else-if="field.type === 'number'"
              v-model:value="businessDraft[field.field]"
              style="width: 100%"
            />
            <a-input v-else v-model:value="businessDraft[field.field]" />
          </a-form-item>
        </a-form>
        <template #footer>
          <a-space :size="8">
            <a-button @click="businessDrawerOpen = false">取消</a-button>
            <a-button type="primary" @click="saveBusinessRecord">保存</a-button>
          </a-space>
        </template>
      </a-drawer>
    </main>
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { TableColumnsType } from 'ant-design-vue'
import {
  AppstoreOutlined,
  AuditOutlined,
  DoubleLeftOutlined,
  DoubleRightOutlined,
  PlusOutlined,
  QuestionCircleOutlined,
  SettingOutlined,
  TableOutlined,
} from '@ant-design/icons-vue'
import AuthorizationPanel from './features/rbac/authorization/AuthorizationPanel.vue'
import RbacMembershipsPanel from './features/rbac/memberships/RbacMembershipsPanel.vue'
import RbacPrincipalsPanel from './features/rbac/principals/RbacPrincipalsPanel.vue'
import { authHeaders as buildAuthHeaders, postJson, postJsonWithAuth as requestJsonWithAuth, type ApiResponse } from './services/http'
import { businessPageConfigs, scenarios, theme } from './config/demoCatalog'
import type {
  AssignmentTargetType,
  AuthzState,
  BatchFailure,
  BusinessPageConfig,
  BusinessRow,
  ComparisonCheck,
  MenuKey,
  RbacFieldPermission,
  RbacGroup,
  RbacPermission,
  RbacRole,
  RbacSubMenuKey,
  RbacUser,
  RbacUserMembership,
  RunResult,
  Scenario,
  SelectOption,
} from './types/domain'

type RbacDeleteImpact = {
  userCount?: number
  permissionCount?: number
  fieldPermissionCount?: number
  roleCount?: number
  dataPermissionCount?: number
  requiresConfirmation?: boolean
}

type PrincipalDetail = {
  kind: 'role' | 'group'
  name: string
  code: string
  description?: string
  userCount: number
  permissionCount?: number
  fieldPermissionCount?: number
}

const activeMenu = ref<MenuKey>('meeting-analytics')
const rbacSubMenu = ref<RbacSubMenuKey>('roles')
const rbacListReturnTarget = ref<'roles' | 'groups' | null>(null)
const rbacMembershipMode = ref<'ROLE' | 'GROUP' | 'PEOPLE'>('ROLE')
const membershipsEntryMode = ref<'menu' | 'role' | 'group'>('menu')
const authzEntryMode = ref<'menu' | 'role' | 'group'>('menu')
const authzLockedPrincipalCode = ref<string | undefined>(undefined)
const authzPrincipalType = ref<'ROLE' | 'GROUP'>('ROLE')
const personaKey = ref<string>('')
const businessLoading = ref(false)
const businessResults = ref<Record<string, RunResult | null>>({})
const activeBusinessScenarioKey = ref('')
const businessDrawerOpen = ref(false)
const businessDrawerMode = ref<'create' | 'edit'>('create')
const businessDraft = ref<Record<string, any>>({})
const businessEditingRow = ref<BusinessRow | null>(null)
const selectedMasterRowKey = ref('')
const masterDetailCollapsed = ref(false)
const masterDetailChildTotal = ref<number>(0)
const evidencePanelKeys = ref<string[]>([])
const centerOnline = ref(false)
const demoOnline = ref(false)
const authzState = ref<AuthzState | null>(null)
const roles = ref<RbacRole[]>([])
const groups = ref<RbacGroup[]>([])
const permissions = ref<RbacPermission[]>([])
const fieldPermissions = ref<RbacFieldPermission[]>([])
const users = ref<RbacUser[]>([])
const roleUsers = ref<RbacUser[]>([])
const groupUsers = ref<RbacUser[]>([])
const selectedRoleId = ref<string | null>(null)
const selectedGroupId = ref<string | null>(null)
const selectedPermissionCodes = ref<string[]>([])
const selectedFieldPermissionCodes = ref<string[]>([])
const originalPermissionCodes = ref<string[]>([])
const originalFieldPermissionCodes = ref<string[]>([])

// 未选授权目标（切换类型/清空选择）时，同步清空原始权限码，避免残留"移除 N"差异标签
watch([selectedPermissionCodes, selectedFieldPermissionCodes], ([codes, fieldCodes]) => {
  if (codes.length === 0) {
    originalPermissionCodes.value = []
  }
  if (fieldCodes.length === 0) {
    originalFieldPermissionCodes.value = []
  }
})
const rbacLoading = ref(false)
const roleDraft = ref({ code: '', name: '', description: '' })
const groupDraft = ref({ code: '', name: '', description: '' })
const memberDraft = ref({ userKeys: [] as string[] })
const groupMemberDraft = ref({ userKeys: [] as string[] })
const assignmentDraft = ref<{ userKeys: string[]; targetType: AssignmentTargetType; targetCodes: string[] }>({ userKeys: [], targetType: 'ROLE', targetCodes: [] })
const batchAssignmentSummary = ref('')
const batchAssignmentFailures = ref<BatchFailure[]>([])
const selectedMembershipUserKey = ref('')
const userMemberships = ref<RbacUserMembership[]>([])
const userMembershipLoading = ref(false)
const deleteImpactOpen = ref(false)
const pendingDeleteRole = ref<RbacRole | null>(null)
const pendingDeleteGroup = ref<RbacGroup | null>(null)
const deleteImpact = ref<RbacDeleteImpact | null>(null)
const principalDetailOpen = ref(false)
const principalDetailLoading = ref(false)
const principalDetail = ref<PrincipalDetail | null>(null)
const roleCopyOpen = ref(false)
const roleCopySource = ref<RbacRole | null>(null)
const roleCopyDraft = ref({ code: '', name: '', description: '' })
const groupCopyOpen = ref(false)
const groupCopySource = ref<RbacGroup | null>(null)
const groupCopyDraft = ref({ code: '', name: '', description: '' })
const resourceFieldNames = ref<Record<string, Record<string, string>>>({})
const organizationNames = ref<Record<string, string>>({})
const departmentOptions = computed(() => Object.entries(organizationNames.value).map(([code, name]) => ({ label: `${name} · ${code}`, value: code })))
const positionOptions = ref<SelectOption[]>([])
// 全量用户名称映射（用于业务表格人员字段展示），与“当前访问人”下拉使用的受限 users 列表解耦：
// users 只含加入了角色/用户组的用户，不能用于解析业务数据里的申请人/主持人/参会人等任意 user_id。
const userDisplayNames = ref<Record<string, string>>({})
// 全量人员选项（来自中心服务 /principals/users/search），用于成员管理添加成员、授权管理等需要选择全部人员的场景
const allUserOptions = computed(() => {
  return Object.entries(userDisplayNames.value).map(([key, name]) => ({ label: `${name} · ${key}`, value: key }))
})

async function checkServiceStatus(): Promise<void> {
  centerOnline.value = await probeService('/center-api/authzcraft/api/v1/data-authz/simulations', simulationProbeBody())
  demoOnline.value = await probeService('/demo-api/authzcraft-demo-oa/api/v1/meetings/search', { limit: 1 }, {
    'X-AuthzCraft-Requester-Kind': 'USER',
    'X-AuthzCraft-Requester-Key': 'panhuidong',
  })
}

async function probeService(url: string, body: unknown, headers: Record<string, string> = {}): Promise<boolean> {
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...headers },
      body: JSON.stringify(body),
    })
    return response.ok
  } catch {
    return false
  }
}

function simulationProbeBody() {
  return {
    tenantKey: 'platform',
    appKey: 'authzcraft-demo-oa',
    requesterKind: 'USER',
    requesterKey: 'panhuidong',
    operationCode: 'READ',
    resources: [{ resourceKey: 'WB_MEETING' }],
    expectations: [{
      targetResourceKey: 'WB_MEETING',
      expectedPlanDecision: 'FILTER',
      expectedFailureCode: '',
      expectedAccessPathKeys: ['WB_MEETING_TO_PARTICIPANT'],
    }],
  }
}

checkServiceStatus()
setInterval(checkServiceStatus, 10000)

// 中心服务冷启动时名称映射接口可能尚未就绪，导致表头/人员/部门名称回退为英文或编码。
// 当中心服务从离线变为在线时，若映射表仍为空则补拉一次，避免首屏显示英文表头或原始编码。
watch(centerOnline, async online => {
  if (!online) {
    return
  }
  const nameMapsEmpty = Object.keys(resourceFieldNames.value).length === 0
    && Object.keys(organizationNames.value).length === 0
    && Object.keys(userDisplayNames.value).length === 0
  if (!nameMapsEmpty) {
    return
  }
  await Promise.all([
    loadResourceFieldDisplayNames(),
    loadOrganizationDisplayNames(),
    loadUserDisplayNames(),
    loadPositionOptions(),
  ])
  // 名称映射补齐后，重新渲染当前业务表格。
  if (activeBusinessConfig.value) {
    await loadBusinessPage()
  }
})

const roleNameByCode = computed(() => new Map(roles.value.map(item => [item.code, item.name])))
function roleDisplayName(roleCode: string): string {
  return roleNameByCode.value.get(roleCode) || roleCode
}
const personaOptions = computed(() => users.value.map(item => ({ label: `${item.displayName || item.userKey} · ${(item.roles ?? []).map(roleDisplayName).join('、') || '无角色'}`, value: item.userKey })))

const currentUserMeta = computed(() => {
  const userKey = authzState.value?.userKey ?? (personaKey.value || '未选择')
  const roleCodes = authzState.value?.roles.join(', ') || '-'
  return `${userKey} · ${roleCodes}`
})
const canReadRbac = computed(() => hasPermission('demo-oa:rbac:read'))
const canManageRbac = computed(() => hasPermission('demo-oa:rbac:manage'))
const canReadMeetingMenu = computed(() => hasAnyPermission(['demo-oa:meetings:read', 'demo-oa:meeting-participants:read']))
const canReadExpenseMenu = computed(() => hasPermission('demo-oa:expense-requests:read'))
const canReadLeaveMenu = computed(() => hasAnyPermission(['demo-oa:leave-requests:read', 'demo-oa:leave-approvals:read']))
const canReadBusinessMenu = computed(() => canReadMeetingMenu.value || canReadExpenseMenu.value || canReadLeaveMenu.value)
const selectedLeftMenuKey = computed(() => activeMenu.value === 'rbac' ? `rbac:${rbacSubMenu.value}` : activeMenu.value)
const activeBusinessConfig = computed(() => businessPageConfigs.find(item => item.key === activeMenu.value) ?? null)
const activeBusinessReadable = computed(() => Boolean(activeBusinessConfig.value && hasAnyPermission(activeBusinessConfig.value.requiredPermissions)))
const activeBusinessScenarios = computed(() => activeBusinessConfig.value?.scenarioKeys.map(findScenario).filter(Boolean) as Scenario[] ?? [])
const activeBusinessDatasetOptions = computed(() => activeBusinessScenarios.value.map(item => ({
  label: item.title,
  value: item.key,
  disabled: Boolean(item.requiredPermission) && !hasPermission(item.requiredPermission as string),
})))
const showBusinessSegmented = computed(() => !activeBusinessConfig.value?.masterDetail && activeBusinessDatasetOptions.value.length > 1)
const restrictedBusinessDatasets = computed(() => activeBusinessScenarios.value.filter(item => item.requiredPermission && !hasPermission(item.requiredPermission)).map(item => item.title))
const activeBusinessDatasetTitle = computed(() => findScenario(activeBusinessScenarioKey.value)?.title ?? activeBusinessConfig.value?.title ?? '业务数据')
const businessActiveResult = computed(() => businessResults.value[activeBusinessScenarioKey.value] ?? null)
const businessTableColumns = computed<TableColumnsType>(() => {
  const scenario = findScenario(activeBusinessScenarioKey.value)
  if (!scenario) {
    return []
  }
  // 根据实际返回数据动态过滤列：后端 projectRows 移除的字段（如 HIDDEN 无权限）不在数据中，前端列头也不显示
  const actualRows = businessActiveResult.value?.businessRows ?? []
  const availableFields = actualRows.length > 0 ? new Set(Object.keys(actualRows[0])) : null
  const visibleColumns = availableFields
    ? scenario.columns.filter(column => availableFields.has(column))
    : scenario.columns
  const cols = visibleColumns.map(column => {
    const base = { title: columnTitle(scenario.resourceKey, column), dataIndex: column, key: column, ellipsis: true, width: columnWidth(column) }
    if (column === 'host_user_id' || column === 'applicant_user_id' || column === 'approver_user_id') {
      return { ...base, customRender: (opt: { record?: BusinessRow }) => userName(opt?.record?.[column]) }
    }
    if (column === 'host_dept_code' || column === 'applicant_dept_code' || column === 'participant_dept_code' || column === 'dept_code') {
      return { ...base, customRender: (opt: { record?: BusinessRow }) => departmentName(opt?.record?.[column]) }
    }
    if (column === 'start_time' || column === 'approved_at') {
      return { ...base, customRender: (opt: { record?: BusinessRow }) => formatDateTime(opt?.record?.[column]) }
    }
    if (column === 'start_date' || column === 'end_date') {
      return { ...base, customRender: (opt: { record?: BusinessRow }) => formatDate(opt?.record?.[column]) }
    }
    if (column === 'duration_minutes' || column === 'attendance_minutes') {
      return { ...base, customRender: (opt: { record?: BusinessRow }) => formatMinutes(opt?.record?.[column]) }
    }
    if (column === 'leave_type') {
      return { ...base, customRender: (opt: { record?: BusinessRow }) => leaveTypeLabel(opt?.record?.[column]) }
    }
    if (column === 'status') {
      return { ...base, customRender: (opt: { record?: BusinessRow }) => leaveStatusLabel(opt?.record?.[column]) }
    }
    return base
  })
  if (scenario.formFields?.length && !activeBusinessConfig.value?.masterDetail) {
    return [...cols, { title: '操作', key: 'actions', dataIndex: 'actions', width: 120 }]
  }
  return cols
})
const businessTableRows = computed<Array<BusinessRow & { __rowKey: string }>>(() => (businessActiveResult.value?.businessRows ?? []).map((row, index) => ({ ...row, __rowKey: `${activeBusinessScenarioKey.value || 'business'}-${index}` })))
const businessChecks = computed(() => businessActiveResult.value?.checks ?? [])
const activeBusinessFormFields = computed(() => findScenario(activeBusinessScenarioKey.value)?.formFields ?? [])
const hasBusinessFormFields = computed(() => activeBusinessFormFields.value.length > 0)
const selectedMasterRow = computed(() => {
  if (!activeBusinessConfig.value?.masterDetail) {
    return null
  }
  return businessTableRows.value.find(row => row.__rowKey === selectedMasterRowKey.value) ?? businessTableRows.value[0] ?? null
})
const masterDetailChildScenario = computed(() => {
  const childKey = activeBusinessConfig.value?.masterDetail?.childScenarioKey
  return childKey ? findScenario(childKey) : null
})
function masterDetailChildRows(record: BusinessRow): Array<BusinessRow & { __rowKey: string }> {
  const masterDetail = activeBusinessConfig.value?.masterDetail
  const scenario = masterDetailChildScenario.value
  if (!masterDetail || !scenario) {
    return []
  }
  const childRows = businessResults.value[scenario.key]?.businessRows ?? []
  const childJoinField = masterDetail.childJoinField ?? masterDetail.joinField
  return childRows
    .filter(row => row[childJoinField] === record[masterDetail.joinField])
    .map((row, index) => ({ ...row, __rowKey: `${scenario.key}-${record.__rowKey}-${index}` }))
}
const selectedMasterChildRows = computed(() => selectedMasterRow.value ? masterDetailChildRows(selectedMasterRow.value) : [])
const masterDetailChildSummarySub = computed(() => {
  const unit = masterDetailChildScenario.value?.key === 'meetingParticipants' ? '人' : '条'
  const total = masterDetailChildTotal.value
  const visible = selectedMasterChildRows.value.length
  return `共 ${total} ${unit}，有权限查看 ${visible} ${unit}`
})

async function refreshMasterDetailChildTotal(): Promise<void> {
  const masterDetail = activeBusinessConfig.value?.masterDetail
  const scenario = masterDetailChildScenario.value
  const record = selectedMasterRow.value
  if (!masterDetail || !scenario || !record) {
    masterDetailChildTotal.value = 0
    return
  }
  const joinValue = record[masterDetail.joinField]
  const requestKey = scenario.key === 'meetingParticipants' ? 'meetingId' : 'leaveRequestId'
  try {
    const response = await postJsonWithAuth<ApiResponse<number>>(`/demo-api${scenario.countEndpoint}`, { [requestKey]: joinValue }, effectiveRequesterKey())
    masterDetailChildTotal.value = typeof response.data === 'number' ? response.data : Number(response.data ?? 0)
  } catch {
    masterDetailChildTotal.value = 0
  }
}

watch(selectedMasterRow, () => {
  void refreshMasterDetailChildTotal()
})
function selectMasterDetailRow(record: BusinessRow & { __rowKey?: string }): void {
  selectedMasterRowKey.value = record.__rowKey ?? ''
}
function masterDetailRowProps(record: BusinessRow): { onClick: () => void } {
  return { onClick: () => selectMasterDetailRow(record) }
}
function masterDetailRowClassName(record: BusinessRow & { __rowKey?: string }): string {
  return record.__rowKey === selectedMasterRowKey.value ? 'master-row selected' : 'master-row'
}
const selectedRole = computed(() => roles.value.find(item => item.id === selectedRoleId.value) ?? null)
const selectedRoleName = computed(() => selectedRole.value ? `${selectedRole.value.name} · ${selectedRole.value.code}` : '')
const selectedGroup = computed(() => groups.value.find(item => item.id === selectedGroupId.value) ?? null)
const selectedGroupName = computed(() => selectedGroup.value ? `${selectedGroup.value.name} · ${selectedGroup.value.code}` : '')
const permissionOptions = computed(() => permissions.value.map(item => ({ label: item.name, value: item.code })))
const fieldPermissionOptions = computed(() => fieldPermissions.value.map(item => ({ label: `${item.name} · ${item.securityRequirement}:${item.accessLevel}`, value: item.code, resourceKey: item.resourceKey, fieldKey: item.fieldKey, accessLevel: item.accessLevel, securityRequirement: item.securityRequirement, name: item.name })))
function filterUserOption(input: string, option: { label?: string }): boolean {
  if (!input) {
    return true
  }
  return String(option.label ?? '').toLowerCase().includes(input.toLowerCase())
}
const assignmentTargetTypeOptions: Array<{ label: string; value: AssignmentTargetType }> = [{ label: '角色', value: 'ROLE' }, { label: '用户组', value: 'GROUP' }]
const assignmentTargetOptions = computed(() => (assignmentDraft.value.targetType === 'ROLE' ? roles.value : groups.value).map(item => ({ label: `${item.name} · ${item.code}`, value: item.code })))
const assignmentCanSave = computed(() => assignmentDraft.value.userKeys.length > 0 && assignmentDraft.value.targetCodes.length > 0)
const rbacReturnLabel = computed(() => rbacListReturnTarget.value ? `返回${rbacListReturnTarget.value === 'groups' ? '用户组管理' : '角色管理'}` : '')
const deleteImpactTitle = computed(() => pendingDeleteRole.value ? '删除角色影响分析' : '停用用户组影响分析')
const deleteImpactPrincipalName = computed(() => {
  const item = pendingDeleteRole.value ?? pendingDeleteGroup.value
  return item ? `${item.name} · ${item.code}` : '-'
})
const deleteImpactOkButtonProps = computed(() => ({ disabled: !canManageRbac.value || (!pendingDeleteRole.value && !pendingDeleteGroup.value) }))
const principalDetailTitle = computed(() => principalDetail.value?.kind === 'group' ? '用户组详情' : '角色详情')
const roleCopySourceLabel = computed(() => roleCopySource.value ? `${roleCopySource.value.name} · ${roleCopySource.value.code}` : '-')
const roleCopyOkButtonProps = computed(() => ({ disabled: !canManageRbac.value || !roleCopySource.value || !roleCopyDraft.value.code.trim() || !roleCopyDraft.value.name.trim() }))
const groupCopySourceLabel = computed(() => groupCopySource.value ? `${groupCopySource.value.name} · ${groupCopySource.value.code}` : '-')
const groupCopyOkButtonProps = computed(() => ({ disabled: !canManageRbac.value || !groupCopySource.value || !groupCopyDraft.value.code.trim() || !groupCopyDraft.value.name.trim() }))

watch(personaKey, async () => {
  await loadCurrentAuthz()
  await loadUsers()
  await loadRbacData()
  ensureActiveMenuAllowed()
  if (activeBusinessConfig.value) {
    await loadBusinessPage()
  }
})

watch(businessTableRows, rows => {
  if (!activeBusinessConfig.value?.masterDetail) {
    return
  }
  const currentStillVisible = rows.some(row => row.__rowKey === selectedMasterRowKey.value)
  if (!currentStillVisible) {
    selectedMasterRowKey.value = rows[0]?.__rowKey ?? ''
  }
  void refreshMasterDetailChildTotal()
})

watch(activeMenu, () => {
  masterDetailCollapsed.value = activeMenu.value === 'leave-applications'
})

async function loadCurrentAuthz(): Promise<void> {
  try {
    const response = await postJsonWithAuth<ApiResponse<AuthzState>>('/demo-api/authzcraft-demo-oa/api/v1/auth/me/permissions', {})
    authzState.value = response.data
  } catch {
    authzState.value = null
  }
}

async function loadUsers(): Promise<void> {
  try {
    const response = await postJsonWithAuth<ApiResponse<RbacUser[]>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/users/search', {})
    users.value = sortedUsers(response.data ?? [])
  } catch {
    // 用户目录加载失败时保留已有列表，访问人下拉仍可切换
  }
}

async function loadRbacData(): Promise<void> {
  if (!canReadRbac.value) {
    // roles/groups 作为角色名/组名查询表在所有访问人下均需可用（如访问人下拉展示角色名），不随权限清空
    permissions.value = []
    fieldPermissions.value = []
    roleUsers.value = []
    groupUsers.value = []
    selectedRoleId.value = null
    selectedGroupId.value = null
    return
  }
  rbacLoading.value = true
  try {
    const [roleResponse, groupResponse, permissionResponse, fieldResponse, userResponse] = await Promise.all([
      postJsonWithAuth<ApiResponse<RbacRole[]>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/search', {}),
      postJsonWithAuth<ApiResponse<RbacGroup[]>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/search', {}),
      postJsonWithAuth<ApiResponse<RbacPermission[]>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/permissions/search', {}),
      postJsonWithAuth<ApiResponse<RbacFieldPermission[]>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/field-permissions/search', {}),
      postJsonWithAuth<ApiResponse<RbacUser[]>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/users/search', {}),
    ])
    roles.value = roleResponse.data ?? []
    groups.value = groupResponse.data ?? []
    permissions.value = permissionResponse.data ?? []
    fieldPermissions.value = fieldResponse.data ?? []
    users.value = sortedUsers(userResponse.data ?? [])
    // 菜单模式授权管理页不自动选角色，等用户手动选择授权目标
    const skipAutoSelect = rbacSubMenu.value === 'authorization' && authzEntryMode.value === 'menu'
    if (!skipAutoSelect && !selectedRoleId.value && roles.value.length) {
      await selectRole(roles.value[0])
    }
    if (!skipAutoSelect && !selectedGroupId.value && groups.value.length) {
      await selectGroup(groups.value[0])
    }
  } finally {
    rbacLoading.value = false
  }
}

async function selectRole(role: RbacRole): Promise<void> {
  authzPrincipalType.value = 'ROLE'
  selectedRoleId.value = role.id
  selectedGroupId.value = null
  const [rolePermissions, roleFieldPermissions, roleUserResponse] = await Promise.all([
    postJsonWithAuth<ApiResponse<string[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${role.id}/permissions/search`, {}),
    postJsonWithAuth<ApiResponse<string[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${role.id}/field-permissions/search`, {}),
    postJsonWithAuth<ApiResponse<RbacUser[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${role.id}/users/search`, {}),
  ])
  selectedPermissionCodes.value = rolePermissions.data ?? []
  selectedFieldPermissionCodes.value = roleFieldPermissions.data ?? []
  originalPermissionCodes.value = [...selectedPermissionCodes.value]
  originalFieldPermissionCodes.value = [...selectedFieldPermissionCodes.value]
  roleUsers.value = sortedUsers(roleUserResponse.data ?? [])
}

async function selectRoleForPermissions(role: RbacRole): Promise<void> {
  await selectRole(role)
  rbacListReturnTarget.value = 'roles'
  authzEntryMode.value = 'role'
  authzLockedPrincipalCode.value = role.code
  rbacSubMenu.value = 'authorization'
}

async function selectRoleForDataPermissions(role: RbacRole): Promise<void> {
  await selectRole(role)
  rbacListReturnTarget.value = 'roles'
  authzEntryMode.value = 'role'
  authzLockedPrincipalCode.value = role.code
  rbacSubMenu.value = 'authorization'
}

async function selectRoleForMemberships(role: RbacRole): Promise<void> {
  await selectRole(role)
  rbacListReturnTarget.value = 'roles'
  rbacMembershipMode.value = 'ROLE'
  membershipsEntryMode.value = 'role'
  rbacSubMenu.value = 'memberships'
}

async function selectGroupForMemberships(group: RbacGroup): Promise<void> {
  await selectGroup(group)
  rbacListReturnTarget.value = 'groups'
  rbacMembershipMode.value = 'GROUP'
  membershipsEntryMode.value = 'group'
  rbacSubMenu.value = 'memberships'
}

async function selectGroupForDataPermissions(group: RbacGroup): Promise<void> {
  rbacListReturnTarget.value = 'groups'
  authzEntryMode.value = 'group'
  authzLockedPrincipalCode.value = group.code
  rbacSubMenu.value = 'authorization'
}

function returnToRbacList(): void {
  rbacSubMenu.value = rbacListReturnTarget.value ?? 'roles'
  rbacListReturnTarget.value = null
  void nextTick(() => {
    const el = document.querySelector('.rbac-row-highlighted')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  })
}

async function createRole(): Promise<void> {
  await withRbacLoading(async () => {
    await postJsonWithAuth<ApiResponse<{ id: string }>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/roles', roleDraft.value)
    roleDraft.value = { code: '', name: '', description: '' }
    await loadRbacData()
  })
}

async function createGroup(): Promise<void> {
  await withRbacLoading(async () => {
    await postJsonWithAuth<ApiResponse<{ id: string }>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/groups', groupDraft.value)
    groupDraft.value = { code: '', name: '', description: '' }
    await loadRbacData()
  })
}

async function updateRole(role: RbacRole, draft: { name: string; description: string }): Promise<void> {
  await withRbacLoading(async () => {
    await postJsonWithAuth<ApiResponse<{ id: string }>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${role.id}/update`, draft)
    await loadRbacData()
  })
}

async function updateGroup(group: RbacGroup, draft: { name: string; description: string }): Promise<void> {
  await withRbacLoading(async () => {
    await postJsonWithAuth<ApiResponse<{ id: string }>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${group.id}/update`, draft)
    await loadRbacData()
  })
}

async function deleteRole(role: RbacRole): Promise<void> {
  await withRbacLoading(async () => {
    const response = await postJsonWithAuth<ApiResponse<RbacDeleteImpact>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${role.id}/delete-impact`, {})
    pendingDeleteRole.value = role
    pendingDeleteGroup.value = null
    deleteImpact.value = response.data ?? {}
    deleteImpactOpen.value = true
  })
}

async function deleteGroup(group: RbacGroup): Promise<void> {
  await withRbacLoading(async () => {
    const response = await postJsonWithAuth<ApiResponse<RbacDeleteImpact>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${group.id}/delete-impact`, {})
    pendingDeleteRole.value = null
    pendingDeleteGroup.value = group
    deleteImpact.value = response.data ?? {}
    deleteImpactOpen.value = true
  })
}

async function confirmDeletePrincipal(): Promise<void> {
  if (!pendingDeleteRole.value && !pendingDeleteGroup.value) { return }
  await withRbacLoading(async () => {
    if (pendingDeleteRole.value) {
      await postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${pendingDeleteRole.value.id}/delete`, { confirmed: true })
      selectedRoleId.value = null
    }
    if (pendingDeleteGroup.value) {
      await postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${pendingDeleteGroup.value.id}/delete`, { confirmed: true })
      selectedGroupId.value = null
    }
    deleteImpactOpen.value = false
    pendingDeleteRole.value = null
    pendingDeleteGroup.value = null
    deleteImpact.value = null
    await loadRbacData()
  })
}

async function batchDeleteRoles(selectedRoles: RbacRole[]): Promise<void> {
  if (!selectedRoles.length) { return }
  await withRbacLoading(async () => {
    for (const role of selectedRoles) {
      try {
        await postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${role.id}/delete`, { confirmed: true })
      } catch { /* continue on individual failure */ }
    }
    selectedRoleId.value = null
    await loadRbacData()
  })
}

async function batchDisableGroups(selectedGroups: RbacGroup[]): Promise<void> {
  if (!selectedGroups.length) { return }
  await withRbacLoading(async () => {
    for (const group of selectedGroups) {
      try {
        await postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${group.id}/delete`, { confirmed: true })
      } catch { /* continue on individual failure */ }
    }
    selectedGroupId.value = null
    await loadRbacData()
  })
}

async function openRoleDetail(role: RbacRole): Promise<void> {
  principalDetailOpen.value = true
  principalDetailLoading.value = true
  try {
    const [rolePermissions, roleFieldPermissions, roleUserResponse] = await Promise.all([
      postJsonWithAuth<ApiResponse<string[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${role.id}/permissions/search`, {}),
      postJsonWithAuth<ApiResponse<string[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${role.id}/field-permissions/search`, {}),
      postJsonWithAuth<ApiResponse<RbacUser[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${role.id}/users/search`, {}),
    ])
    principalDetail.value = {
      kind: 'role',
      name: role.name,
      code: role.code,
      description: role.description,
      userCount: roleUserResponse.data?.length ?? 0,
      permissionCount: rolePermissions.data?.length ?? 0,
      fieldPermissionCount: roleFieldPermissions.data?.length ?? 0,
    }
  } finally {
    principalDetailLoading.value = false
  }
}

async function openGroupDetail(group: RbacGroup): Promise<void> {
  principalDetailOpen.value = true
  principalDetailLoading.value = true
  try {
    const response = await postJsonWithAuth<ApiResponse<RbacUser[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${group.id}/users/search`, {})
    principalDetail.value = {
      kind: 'group',
      name: group.name,
      code: group.code,
      description: group.description,
      userCount: response.data?.length ?? 0,
    }
  } finally {
    principalDetailLoading.value = false
  }
}

function openRoleCopy(role: RbacRole): void {
  roleCopySource.value = role
  roleCopyDraft.value = { code: `${role.code}_copy`, name: `${role.name}副本`, description: role.description || '' }
  roleCopyOpen.value = true
}

function openGroupCopy(group: RbacGroup): void {
  groupCopySource.value = group
  groupCopyDraft.value = { code: `${group.code}_copy`, name: `${group.name}副本`, description: group.description || '' }
  groupCopyOpen.value = true
}

async function confirmRoleCopy(): Promise<void> {
  if (!roleCopySource.value || roleCopyOkButtonProps.value.disabled) { return }
  const sourceRole = roleCopySource.value
  await withRbacLoading(async () => {
    const [rolePermissions, roleFieldPermissions] = await Promise.all([
      postJsonWithAuth<ApiResponse<string[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${sourceRole.id}/permissions/search`, {}),
      postJsonWithAuth<ApiResponse<string[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${sourceRole.id}/field-permissions/search`, {}),
    ])
    const createResponse = await postJsonWithAuth<ApiResponse<{ id: string }>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/roles', roleCopyDraft.value)
    const newRoleId = createResponse.data?.id || roleCopyDraft.value.code
    await Promise.all([
      postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${newRoleId}/permissions/replace`, { permissionCodes: rolePermissions.data ?? [] }),
      postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${newRoleId}/field-permissions/replace`, { fieldPermissionCodes: roleFieldPermissions.data ?? [] }),
    ])
    roleCopyOpen.value = false
    roleCopySource.value = null
    roleCopyDraft.value = { code: '', name: '', description: '' }
    await loadRbacData()
    const copied = roles.value.find(item => item.id === newRoleId || item.code === newRoleId)
    if (copied) {
      await selectRole(copied)
    }
  })
}

async function confirmGroupCopy(): Promise<void> {
  if (!groupCopySource.value || groupCopyOkButtonProps.value.disabled) { return }
  const sourceGroup = groupCopySource.value
  await withRbacLoading(async () => {
    const memberResponse = await postJsonWithAuth<ApiResponse<RbacUser[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${sourceGroup.id}/users/search`, {})
    const createResponse = await postJsonWithAuth<ApiResponse<{ id: string }>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/groups', groupCopyDraft.value)
    const newGroupId = createResponse.data?.id || groupCopyDraft.value.code
    for (const user of memberResponse.data ?? []) {
      await postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${newGroupId}/users/add`, { userKey: user.userKey, displayName: user.displayName })
    }
    groupCopyOpen.value = false
    groupCopySource.value = null
    groupCopyDraft.value = { code: '', name: '', description: '' }
    await loadRbacData()
    const copied = groups.value.find(item => item.id === newGroupId || item.code === newGroupId)
    if (copied) {
      await selectGroup(copied)
    }
  })
}

async function savePermissions(): Promise<void> {
  const isGroup = authzEntryMode.value === 'group' || (authzEntryMode.value === 'menu' && authzPrincipalType.value === 'GROUP')
  const principalId = isGroup ? selectedGroupId.value : selectedRoleId.value
  if (!principalId) { return }
  await withRbacLoading(async () => {
    const base = isGroup
      ? `/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${principalId}/permissions`
      : `/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${principalId}/permissions`
    await postJsonWithAuth<ApiResponse<void>>(`${base}/replace`, { permissionCodes: selectedPermissionCodes.value })
    originalPermissionCodes.value = [...selectedPermissionCodes.value]
    await loadCurrentAuthz()
  })
}

async function saveFieldPermissions(): Promise<void> {
  const isGroup = authzEntryMode.value === 'group' || (authzEntryMode.value === 'menu' && authzPrincipalType.value === 'GROUP')
  const principalId = isGroup ? selectedGroupId.value : selectedRoleId.value
  if (!principalId) { return }
  await withRbacLoading(async () => {
    const base = isGroup
      ? `/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${principalId}/field-permissions`
      : `/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${principalId}/field-permissions`
    await postJsonWithAuth<ApiResponse<void>>(`${base}/replace`, { fieldPermissionCodes: selectedFieldPermissionCodes.value })
    originalFieldPermissionCodes.value = [...selectedFieldPermissionCodes.value]
    await loadCurrentAuthz()
  })
}

async function selectGroupForAuthz(group: RbacGroup): Promise<void> {
  authzPrincipalType.value = 'GROUP'
  selectedGroupId.value = group.id
  selectedRoleId.value = null
  const [groupPermissions, groupFieldPermissions] = await Promise.all([
    postJsonWithAuth<ApiResponse<string[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${group.id}/permissions/search`, {}),
    postJsonWithAuth<ApiResponse<string[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${group.id}/field-permissions/search`, {}),
  ])
  selectedPermissionCodes.value = groupPermissions.data ?? []
  selectedFieldPermissionCodes.value = groupFieldPermissions.data ?? []
  originalPermissionCodes.value = [...selectedPermissionCodes.value]
  originalFieldPermissionCodes.value = [...selectedFieldPermissionCodes.value]
}

async function reloadFieldPermissions(): Promise<void> {
  await withRbacLoading(async () => {
    const response = await postJsonWithAuth<ApiResponse<RbacFieldPermission[]>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/field-permissions/search', {})
    fieldPermissions.value = response.data ?? []
  })
}

async function addRoleUser(): Promise<void> {
  if (!selectedRoleId.value || !memberDraft.value.userKeys.length) { return }
  await withRbacLoading(async () => {
    for (const userKey of memberDraft.value.userKeys) {
      const user = users.value.find(item => item.userKey === userKey)
      try {
        await postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${selectedRoleId.value}/users/add`, { userKey, displayName: user?.displayName })
      } catch { /* continue on individual failure */ }
    }
    memberDraft.value = { userKeys: [] }
    await selectRole(selectedRole.value as RbacRole)
  })
}

async function selectGroup(group: RbacGroup): Promise<void> {
  selectedGroupId.value = group.id
  const response = await postJsonWithAuth<ApiResponse<RbacUser[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${group.id}/users/search`, {})
  groupUsers.value = sortedUsers(response.data ?? [])
}

async function removeRoleUser(user: RbacUser): Promise<void> {
  if (!selectedRoleId.value) { return }
  await withRbacLoading(async () => {
    await postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${selectedRoleId.value}/users/remove`, { userKey: user.userKey, confirmed: true })
    await selectRole(selectedRole.value as RbacRole)
  })
}

async function addGroupUser(): Promise<void> {
  if (!selectedGroupId.value || !groupMemberDraft.value.userKeys.length) { return }
  await withRbacLoading(async () => {
    for (const userKey of groupMemberDraft.value.userKeys) {
      const user = users.value.find(item => item.userKey === userKey)
      try {
        await postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${selectedGroupId.value}/users/add`, { userKey, displayName: user?.displayName })
      } catch { /* continue on individual failure */ }
    }
    groupMemberDraft.value = { userKeys: [] }
    if (selectedGroup.value) {
      await selectGroup(selectedGroup.value)
    }
  })
}

async function removeGroupUser(user: RbacUser): Promise<void> {
  if (!selectedGroupId.value) { return }
  await withRbacLoading(async () => {
    await postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${selectedGroupId.value}/users/remove`, { userKey: user.userKey, confirmed: true })
    if (selectedGroup.value) {
      await selectGroup(selectedGroup.value)
    }
  })
}

async function batchRemoveRoleUsers(users: RbacUser[]): Promise<void> {
  if (!selectedRoleId.value || !users.length) { return }
  await withRbacLoading(async () => {
    for (const user of users) {
      try {
        await postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${selectedRoleId.value}/users/remove`, { userKey: user.userKey, confirmed: true })
      } catch { /* continue on individual failure */ }
    }
    await selectRole(selectedRole.value as RbacRole)
  })
}

async function batchRemoveGroupUsers(users: RbacUser[]): Promise<void> {
  if (!selectedGroupId.value || !users.length) { return }
  await withRbacLoading(async () => {
    for (const user of users) {
      try {
        await postJsonWithAuth<ApiResponse<void>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${selectedGroupId.value}/users/remove`, { userKey: user.userKey, confirmed: true })
      } catch { /* continue on individual failure */ }
    }
    if (selectedGroup.value) {
      await selectGroup(selectedGroup.value)
    }
  })
}

async function saveBatchAssignments(): Promise<void> {
  if (!assignmentCanSave.value) { return }
  const failures: BatchFailure[] = []
  let successCount = 0
  await withRbacLoading(async () => {
    for (const userKey of assignmentDraft.value.userKeys) {
      for (const targetCode of assignmentDraft.value.targetCodes) {
        try {
          const path = assignmentDraft.value.targetType === 'ROLE'
            ? `/demo-api/authzcraft-demo-oa/api/v1/rbac/roles/${targetCode}/users/add`
            : `/demo-api/authzcraft-demo-oa/api/v1/rbac/groups/${targetCode}/users/add`
          await postJsonWithAuth<ApiResponse<void>>(path, { userKey })
          successCount += 1
        } catch (error) {
          failures.push({ itemKey: `${userKey} -> ${targetCode}`, reason: error instanceof Error ? error.message : String(error) })
        }
      }
    }
    batchAssignmentFailures.value = failures
    batchAssignmentSummary.value = `保存完成：成功 ${successCount} 条，失败 ${failures.length} 条`
    await Promise.all([selectedRole.value ? selectRole(selectedRole.value) : Promise.resolve(), selectedGroup.value ? selectGroup(selectedGroup.value) : Promise.resolve()])
    if (selectedMembershipUserKey.value) {
      await loadUserMemberships(selectedMembershipUserKey.value)
    }
  })
}

async function loadUserMemberships(userKey: string): Promise<void> {
  selectedMembershipUserKey.value = userKey
  if (!userKey) {
    userMemberships.value = []
    return
  }
  userMembershipLoading.value = true
  try {
    const response = await postJsonWithAuth<ApiResponse<RbacUserMembership[]>>(`/demo-api/authzcraft-demo-oa/api/v1/rbac/users/${userKey}/memberships/search`, {})
    userMemberships.value = response.data ?? []
  } finally {
    userMembershipLoading.value = false
  }
}

function columnTitle(resourceKey: string, fieldKey: string): string {
  return resourceFieldNames.value[resourceKey]?.[fieldKey]
    || fieldKey
}

function columnWidth(fieldKey: string): number | undefined {
  if (fieldKey === 'host_user_id' || fieldKey === 'applicant_user_id' || fieldKey === 'participant_user_id') {
    return 90
  }
  if (fieldKey === 'host_dept_code' || fieldKey === 'applicant_dept_code' || fieldKey === 'participant_dept_code') {
    return 170
  }
  if (fieldKey === 'start_time' || fieldKey === 'end_date' || fieldKey === 'start_date' || fieldKey === 'approved_at') {
    return 150
  }
  if (fieldKey === 'duration_minutes' || fieldKey === 'attendance_minutes' || fieldKey === 'leave_days') {
    return 110
  }
  if (fieldKey === 'topic' || fieldKey === 'title' || fieldKey === 'reason') {
    return 150
  }
  if (fieldKey === 'status' || fieldKey === 'action' || fieldKey === 'leave_type') {
    return 110
  }
  return undefined
}

function userName(userKey: unknown): string {
  const key = String(userKey ?? '').trim()
  if (!key) {
    return '-'
  }
  return userDisplayNames.value[key] || users.value.find(item => item.userKey === key)?.displayName || key
}

function departmentName(deptCode: unknown): string {
  const code = String(deptCode ?? '').trim()
  if (!code) {
    return '-'
  }
  return organizationNames.value[code] || code
}

function formatDateTime(value: unknown): string {
  if (value == null || value === '') {
    return '-'
  }
  const date = new Date(String(value))
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  const pad = (part: number): string => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatDate(value: unknown): string {
  if (value == null || value === '') {
    return '-'
  }
  const text = String(value)
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(text)
  return match ? match[0] : text
}

function formatMinutes(value: unknown): string {
  const minutes = Number(value)
  if (!Number.isFinite(minutes)) {
    return String(value ?? '-')
  }
  return `${minutes} 分钟`
}

const leaveTypeLabels: Record<string, string> = {
  PERSONAL: '事假',
  SICK: '病假',
  ANNUAL: '年假',
  COMPENSATORY: '调休',
  MARRIAGE: '婚假',
}

function leaveTypeLabel(value: unknown): string {
  const key = String(value ?? '').trim()
  return leaveTypeLabels[key] || key || '-'
}

const leaveStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  APPROVED: '已通过',
}

function leaveStatusLabel(value: unknown): string {
  const key = String(value ?? '').trim()
  return leaveStatusLabels[key] || key || '-'
}

const approvalActionLabels: Record<string, string> = {
  APPROVE: '通过',
  REJECT: '驳回',
}

function approvalActionLabel(value: unknown): string {
  const key = String(value ?? '').trim()
  return approvalActionLabels[key] || key || '-'
}

function participantDetailName(row: BusinessRow): string {
  const scenario = masterDetailChildScenario.value
  if (!scenario) {
    return '-'
  }
  if (scenario.key === 'meetingParticipants') {
    return userName(row['participant_user_id'])
  }
  if (scenario.key === 'leaveApprovals') {
    return userName(row['approver_user_id'])
  }
  const first = scenario.columns?.[0]
  return String(row[first ?? 'id'] ?? '-')
}

function masterDetailTitle(row: BusinessRow | null): string {
  if (!row) {
    return '-'
  }
  const config = activeBusinessConfig.value
  const scenarioKey = config?.scenarioKeys?.[0]
  if (scenarioKey === 'meetings') {
    return String(row['topic'] ?? row['meeting_id'] ?? '-')
  }
  if (scenarioKey === 'leaveRequests') {
    return String(row['applicant_name'] ?? row['id'] ?? '-')
  }
  return String(row[scenarioKey ?? 'id'] ?? '-')
}

function participantDetailSub(row: BusinessRow): string {
  const scenario = masterDetailChildScenario.value
  if (!scenario) {
    return '-'
  }
  if (scenario.key === 'meetingParticipants') {
    return departmentName(row['participant_dept_code'])
  }
  if (scenario.key === 'leaveApprovals') {
    const action = approvalActionLabel(row['action'])
    const comment = String(row['comment'] ?? '').trim()
    return comment ? `${action} · ${comment}` : action
  }
  const second = scenario.columns?.[1]
  return String(row[second ?? 'id'] ?? '-')
}

function participantDetailTag(row: BusinessRow): string {
  const scenario = masterDetailChildScenario.value
  if (!scenario) {
    return '-'
  }
  if (scenario.key === 'meetingParticipants') {
    return formatMinutes(row['attendance_minutes'])
  }
  if (scenario.key === 'leaveApprovals') {
    return formatDateTime(row['approved_at'])
  }
  const third = scenario.columns?.[2]
  return String(row[third ?? 'id'] ?? '-')
}

function recordResourceFieldNames(resourceKey: string, fieldNames: Record<string, string>): void {
  resourceFieldNames.value[resourceKey] = fieldNames
}

// 独立于数据权限管理面板（该面板仅在用户手动进入对应 Tab 时才挂载）直接拉取业务表头展示名，
// 保证表头无论当前访问人权限、当前所在菜单如何都能展示 authzcraft_resource_field.display_name。
async function loadResourceFieldDisplayNames(): Promise<void> {
  try {
    const resourceResponse = await postJson<ApiResponse<Array<{ id: string; resourceKey: string }>>>('/center-api/authzcraft/api/v1/catalog/relation-resources/search', {
      tenantKey: 'platform',
      appKey: 'authzcraft-demo-oa',
    })
    const resources = resourceResponse.data ?? []
    await Promise.all(resources.map(async resource => {
      const fieldResponse = await postJson<ApiResponse<Array<{ fieldKey: string; displayName: string }>>>(`/center-api/authzcraft/api/v1/catalog/relation-resources/${resource.id}/fields/search`, {})
      const fields = fieldResponse.data ?? []
      recordResourceFieldNames(resource.resourceKey, Object.fromEntries(fields.map(field => [field.fieldKey, field.displayName])))
    }))
  } catch (error) {
    console.error('加载资源字段展示名称失败', error)
  }
}

async function loadOrganizationDisplayNames(): Promise<void> {
  try {
    const response = await postJson<ApiResponse<Array<{ principalKey: string; displayName?: string; departmentName?: string }>>>('/center-api/authzcraft/api/v1/principals/organizations/search', {
      tenantKey: 'platform',
    })
    const organizations = response.data ?? []
    const names: Record<string, string> = {}
    for (const org of organizations) {
      const code = org.principalKey
      if (!code) {
        continue
      }
      names[code] = org.displayName || org.departmentName || code
    }
    organizationNames.value = names
  } catch (error) {
    console.error('加载组织展示名称失败', error)
  }
}

async function loadPositionOptions(): Promise<void> {
  try {
    const response = await postJson<ApiResponse<Array<{ principalKey?: string; postName?: string; postCode?: string; displayName?: string; departmentName?: string }>>>('/center-api/authzcraft/api/v1/principals/positions/search', {
      tenantKey: 'platform',
    })
    const positions = response.data ?? []
    positionOptions.value = positions
      .filter(item => item.principalKey)
      .map(item => {
        const postName = item.postName || item.displayName || item.principalKey
        const deptPart = item.departmentName ? `（${item.departmentName}）` : ''
        return { label: `${postName}${deptPart} · ${item.principalKey}`, value: item.principalKey! }
      })
  } catch (error) {
    console.error('加载岗位列表失败', error)
  }
}

async function loadUserDisplayNames(): Promise<void> {
  try {
    const names: Record<string, string> = {}
    const pageSize = 1000
    let offset = 0
    while (true) {
      const response = await postJson<ApiResponse<Array<{ userId?: string; principalKey?: string; displayName?: string; staffName?: string }>>>('/center-api/authzcraft/api/v1/principals/users/search', {
        tenantKey: 'platform',
        lifecycleState: 'ACTIVE',
        limit: pageSize,
        offset,
      })
      const list = response.data ?? []
      for (const user of list) {
        const key = user.userId || user.principalKey
        if (!key) {
          continue
        }
        names[key] = user.displayName || user.staffName || key
      }
      if (list.length < pageSize) {
        break
      }
      offset += pageSize
    }
    userDisplayNames.value = names
  } catch (error) {
    console.error('加载用户展示名称失败', error)
  }
}

async function withRbacLoading(action: () => Promise<void>): Promise<void> {
  rbacLoading.value = true
  try {
    await action()
  } finally {
    rbacLoading.value = false
  }
}

function hasPermission(permissionCode: string): boolean {
  return Boolean(authzState.value?.permissions.includes(permissionCode))
}

function userHasPermission(userKey: string, permissionCode: string): boolean {
  if (userKey === authzState.value?.userKey) {
    return hasPermission(permissionCode)
  }
  const user = users.value.find(item => item.userKey === userKey)
  return Boolean(user?.permissions?.includes(permissionCode))
}

function effectiveRequesterKey(): string {
  // 默认探针身份用管理员 fanlaihua（demo_oa_admin 有 demo-oa:rbac:read），保证 initialLoad 能加载 RBAC 数据
  return personaKey.value || 'fanlaihua'
}

function authHeaders(selectedPersonaKey: string = effectiveRequesterKey()): Record<string, string> {
  return buildAuthHeaders(selectedPersonaKey)
}

async function postJsonWithAuth<T>(url: string, body: unknown, selectedPersonaKey: string = effectiveRequesterKey()): Promise<T> {
  return requestJsonWithAuth<T>(url, body, selectedPersonaKey)
}

async function initialLoad(): Promise<void> {
  // 先以默认探针身份（personaKey 为空时回退 fanlaihua 管理员）完成一次有权限的引导加载，
  // 确保角色名/表头 display_name 等查询表被填充后，再切换为实际默认访问人。
  await loadUsers()
  await loadCurrentAuthz()
  await loadRbacData()
  await loadResourceFieldDisplayNames()
  await loadOrganizationDisplayNames()
  await loadUserDisplayNames()
  await loadUsers()
  if (!personaKey.value && users.value.length) {
    personaKey.value = users.value[0].userKey
  }
}

initialLoad()

function selectMenu(key: MenuKey): void {
  activeMenu.value = key
  const config = businessPageConfigs.find(item => item.key === key)
  if (config) {
    ensureActiveBusinessScenario(config)
    void loadBusinessPage()
  }
}

function selectRbacSubMenu(key: RbacSubMenuKey): void {
  activeMenu.value = 'rbac'
  rbacListReturnTarget.value = null
  if (key === 'authorization') {
    authzEntryMode.value = 'menu'
    authzLockedPrincipalCode.value = undefined
    // 菜单模式授权管理：清空之前角色管理页面自动选中的角色权限数据
    selectedRoleId.value = null
    selectedGroupId.value = null
    selectedPermissionCodes.value = []
    selectedFieldPermissionCodes.value = []
  }
  if (key === 'memberships') {
    membershipsEntryMode.value = 'menu'
  }
  rbacSubMenu.value = key
}

async function loadBusinessPage(): Promise<void> {
  const config = activeBusinessConfig.value
  if (!config || !activeBusinessReadable.value) {
    return
  }
  ensureActiveBusinessScenario(config)
  await withBusinessLoading(async () => {
    const nextResults = { ...businessResults.value }
    for (const scenarioKey of config.scenarioKeys) {
      const scenario = findScenario(scenarioKey)
      if (!scenario) {
        continue
      }
      if (scenario.requiredPermission && !hasPermission(scenario.requiredPermission)) {
        continue
      }
      const result = await runScenario(scenario, personaKey.value)
      nextResults[scenarioKey] = result
    }
    businessResults.value = nextResults
  })
}

function openBusinessCreateDrawer(): void {
  businessDrawerMode.value = 'create'
  businessDraft.value = {}
  businessEditingRow.value = null
  businessDrawerOpen.value = true
}

function openBusinessEditDrawer(record: BusinessRow): void {
  businessDrawerMode.value = 'edit'
  businessEditingRow.value = record
  businessDraft.value = { ...record }
  delete businessDraft.value.__rowKey
  businessDrawerOpen.value = true
}

async function saveBusinessRecord(): Promise<void> {
  const scenario = findScenario(activeBusinessScenarioKey.value)
  if (!scenario) { return }
  const base = scenario.endpoint.replace('/search', '')
  try {
    if (businessDrawerMode.value === 'create') {
      await postJsonWithAuth<ApiResponse<unknown>>(`/demo-api${base}/create`, businessDraft.value)
    } else {
      const id = String(businessEditingRow.value?.id ?? '')
      await postJsonWithAuth<ApiResponse<unknown>>(`/demo-api${base}/${id}/update`, businessDraft.value)
    }
    businessDrawerOpen.value = false
    await loadBusinessPage()
  } catch (error) {
    console.error('保存业务记录失败', error)
  }
}

async function deleteBusinessRecord(record: BusinessRow): Promise<void> {
  const scenario = findScenario(activeBusinessScenarioKey.value)
  if (!scenario) { return }
  const base = scenario.endpoint.replace('/search', '')
  try {
    await postJsonWithAuth<ApiResponse<void>>(`/demo-api${base}/${String(record.id ?? '')}/delete`, {})
    await loadBusinessPage()
  } catch (error) {
    console.error('删除业务记录失败', error)
  }
}

async function withBusinessLoading(action: () => Promise<void>): Promise<void> {
  businessLoading.value = true
  try {
    await action()
  } finally {
    businessLoading.value = false
  }
}

function ensureActiveMenuAllowed(): void {
  if (activeMenu.value === 'rbac' && canReadRbac.value) {
    return
  }
  if (activeMenu.value === 'meeting-analytics' && canReadMeetingMenu.value) {
    return
  }
  if (activeMenu.value === 'expense-applications' && canReadExpenseMenu.value) {
    return
  }
  if (activeMenu.value === 'leave-applications' && canReadLeaveMenu.value) {
    return
  }
  const fallback = businessPageConfigs.find(item => item.requiredPermissions.some(hasPermission))
  activeMenu.value = fallback?.key ?? 'rbac'
  if (activeMenu.value === 'rbac') {
    rbacSubMenu.value = 'roles'
  }
}

function hasAnyPermission(permissionCodes: string[]): boolean {
  return permissionCodes.some(hasPermission)
}

function findScenario(key: string): Scenario | null {
  return scenarios.find(item => item.key === key) ?? null
}

function ensureActiveBusinessScenario(config: BusinessPageConfig): void {
  const currentAvailable = config.scenarioKeys.includes(activeBusinessScenarioKey.value)
  if (currentAvailable && activeBusinessScenarioKey.value) {
    return
  }
  const allowedScenario = config.scenarioKeys
    .map(findScenario)
    .filter((scenario): scenario is Scenario => scenario !== null)
    .find(scenario => !scenario.requiredPermission || hasPermission(scenario.requiredPermission))
  activeBusinessScenarioKey.value = config.masterDetail ? config.scenarioKeys[0] : allowedScenario?.key ?? config.scenarioKeys[0]
}

function sortedUsers(items: RbacUser[]): RbacUser[] {
  return [...items].sort((left, right) => staffSortKey(left).localeCompare(staffSortKey(right), 'zh-Hans-CN', { numeric: true }))
}

function staffSortKey(user: RbacUser): string {
  return user.staffNo || user.userKey || user.id || ''
}

async function runScenario(scenario: Scenario, selectedPersonaKey: string): Promise<RunResult> {
  const expectedPlanDecision = scenario.requiredPermission && !userHasPermission(selectedPersonaKey, scenario.requiredPermission)
    ? 'RBAC_FORBIDDEN'
    : 'FILTER'
  const expectedAccessPathKeys = expectedPlanDecision === 'FILTER' ? scenario.expectedAccessPathKeys : []

  try {
    if (expectedPlanDecision === 'RBAC_FORBIDDEN') {
      const businessResponse = await fetch(`/demo-api${scenario.endpoint}`, {
        method: 'POST',
        headers: authHeaders(selectedPersonaKey),
        body: JSON.stringify({ limit: 100 }),
      })
      const businessJson = await businessResponse.json() as ApiResponse<BusinessRow[]>
      const checks = buildRbacForbiddenChecks(businessResponse.status, businessJson.code, businessResponse.headers.get('X-AuthzCraft-Decision-Key'))
      return {
        scenarioKey: scenario.key,
        scenarioTitle: scenario.title,
        personaKey: selectedPersonaKey,
        expectedDecision: expectedPlanDecision,
        expectedAccessPathKeys,
        businessStatus: businessResponse.status,
        businessCode: businessJson.code,
        businessRows: [],
        totalCount: 0,
        pepDecision: null,
        decisionKey: null,
        requiredAccessPathKeys: [],
        simulationDecision: null,
        simulationPassed: true,
        simulationRecordedProduction: false,
        auditDecision: null,
        auditFound: false,
        checks,
        errorMessage: null,
      }
    }

    const simulation = await postJson<ApiResponse<{
      plans: Array<{ decisionKey: string; planDecision: string; requiredAccessPathKeys?: string[] }>
      productionDecisionRecorded: boolean
      assertionPassed: boolean
      explains: unknown[]
    }>>('/center-api/authzcraft/api/v1/data-authz/simulations', {
      tenantKey: 'platform',
      appKey: 'authzcraft-demo-oa',
      requesterKind: 'USER',
      requesterKey: selectedPersonaKey,
      operationCode: 'READ',
      resources: [{ resourceKey: scenario.resourceKey }],
      expectations: [{
        targetResourceKey: scenario.resourceKey,
        expectedPlanDecision,
        expectedFailureCode: '',
        expectedAccessPathKeys,
      }],
    })
    const simulationPlan = simulation.data?.plans?.[0]

    const businessResponse = await fetch(`/demo-api${scenario.endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-AuthzCraft-Requester-Kind': 'USER',
        'X-AuthzCraft-Requester-Key': selectedPersonaKey,
      },
      body: JSON.stringify({ limit: 100 }),
    })
    const businessJson = await businessResponse.json() as ApiResponse<BusinessRow[]>
    const businessRows = Array.isArray(businessJson.data) ? businessJson.data : []
    const totalCount = await fetchTotalCount(scenario.countEndpoint, selectedPersonaKey)
    const decisionKey = businessResponse.headers.get('X-AuthzCraft-Decision-Key')
    const pepDecision = businessResponse.headers.get('X-AuthzCraft-Plan-Decision')
    const requiredAccessPathKeys = splitHeader(businessResponse.headers.get('X-AuthzCraft-Required-Access-Paths'))
    const audit = decisionKey
      ? await postJson<ApiResponse<{ planDecision: string }>>(`/center-api/authzcraft/api/v1/audit/decision-records/${decisionKey}`, {})
      : null
    const auditDecision = audit?.code === '0' ? audit.data?.planDecision ?? null : null
    const auditFound = Boolean(auditDecision)
    const checks = buildChecks({
      expectedDecision: expectedPlanDecision,
      expectedAccessPathKeys,
      businessStatus: businessResponse.status,
      businessCode: businessJson.code,
      businessRows,
      pepDecision,
      requiredAccessPathKeys,
      simulationDecision: simulationPlan?.planDecision ?? null,
      simulationPassed: Boolean(simulation.data?.assertionPassed),
      productionDecisionRecorded: simulation.data?.productionDecisionRecorded ?? null,
      auditDecision,
      auditFound,
    })

    return {
      scenarioKey: scenario.key,
      scenarioTitle: scenario.title,
      personaKey: selectedPersonaKey,
      expectedDecision: expectedPlanDecision,
      expectedAccessPathKeys,
      businessStatus: businessResponse.status,
      businessCode: businessJson.code,
      businessRows,
      totalCount,
      pepDecision,
      decisionKey,
      requiredAccessPathKeys,
      simulationDecision: simulationPlan?.planDecision ?? null,
      simulationPassed: Boolean(simulation.data?.assertionPassed),
      simulationRecordedProduction: simulation.data?.productionDecisionRecorded ?? null,
      auditDecision,
      auditFound,
      checks,
      errorMessage: null,
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    return failedResult(scenario, selectedPersonaKey, expectedPlanDecision, expectedAccessPathKeys, message)
  }
}

function failedResult(scenario: Scenario, selectedPersonaKey: string, expectedPlanDecision: string, expectedAccessPathKeys: string[], message: string): RunResult {
  return {
    scenarioKey: scenario.key,
    scenarioTitle: scenario.title,
    personaKey: selectedPersonaKey,
    expectedDecision: expectedPlanDecision,
    expectedAccessPathKeys,
    businessStatus: 0,
    businessCode: 'ERROR',
    businessRows: [],
    totalCount: 0,
    pepDecision: null,
    decisionKey: null,
    requiredAccessPathKeys: [],
    simulationDecision: null,
    simulationPassed: false,
    simulationRecordedProduction: null,
    auditDecision: null,
    auditFound: false,
    checks: [{ name: '接口调用', expected: '全部接口成功返回', actual: message, passed: false }],
    errorMessage: message,
  }
}

function buildChecks(input: {
  expectedDecision: string
  expectedAccessPathKeys: string[]
  businessStatus: number
  businessCode: string
  businessRows: BusinessRow[]
  pepDecision: string | null
  requiredAccessPathKeys: string[]
  simulationDecision: string | null
  simulationPassed: boolean
  productionDecisionRecorded: boolean | null
  auditDecision: string | null
  auditFound: boolean
}): ComparisonCheck[] {
  const expectsEmptyRows = input.expectedDecision === 'DENY_ALL'
  return [
    {
      name: '模拟验证结果',
      expected: `${decisionLabel(input.expectedDecision)} 且不写生产审计`,
      actual: `${decisionLabel(input.simulationDecision)} / ${input.simulationPassed ? '断言通过' : '断言失败'} / production=${input.productionDecisionRecorded}`,
      passed: input.simulationDecision === input.expectedDecision && input.simulationPassed && input.productionDecisionRecorded === false,
    },
    {
      name: '实际执行结果',
      expected: decisionLabel(input.expectedDecision),
      actual: decisionLabel(input.pepDecision),
      passed: input.pepDecision === input.expectedDecision,
    },
    {
      name: '访问路径',
      expected: input.expectedAccessPathKeys.length ? input.expectedAccessPathKeys.join(', ') : '无',
      actual: input.requiredAccessPathKeys.length ? input.requiredAccessPathKeys.join(', ') : '无',
      passed: sameSet(input.expectedAccessPathKeys, input.requiredAccessPathKeys),
    },
    {
      name: '业务结果',
      expected: expectsEmptyRows ? '返回空集' : '返回成功且可展示数据',
      actual: `HTTP ${input.businessStatus}, code=${input.businessCode}, rows=${input.businessRows.length}`,
      passed: input.businessStatus === 200 && input.businessCode === '0' && (expectsEmptyRows ? input.businessRows.length === 0 : true),
    },
    {
      name: '生产审计',
      expected: `审计记录 ${decisionLabel(input.expectedDecision)}`,
      actual: input.auditFound ? decisionLabel(input.auditDecision) : '未命中',
      passed: input.auditDecision === input.expectedDecision,
    },
  ]
}

function buildRbacForbiddenChecks(status: number, code: string, decisionKey: string | null): ComparisonCheck[] {
  return [
    {
      name: '回路 1 功能权限',
      expected: 'HTTP 403 / DEMO_OA_FORBIDDEN',
      actual: `HTTP ${status} / ${code}`,
      passed: status === 403 && code === 'DEMO_OA_FORBIDDEN',
    },
    {
      name: '回路 2 未触发',
      expected: '无 decisionKey',
      actual: decisionKey ?? '-',
      passed: !decisionKey,
    },
  ]
}

function resultPassed(result: RunResult): boolean {
  return result.checks.every(check => check.passed) && !result.errorMessage
}

function businessResultSummary(result: RunResult): string {
  const total = result.totalCount
  const visible = result.businessRows.length
  return `共 ${total} 条，有权限查看 ${visible} 条`
}

async function fetchTotalCount(countEndpoint: string, selectedPersonaKey: string): Promise<number> {
  if (!countEndpoint) {
    return 0
  }
  try {
    const response = await postJsonWithAuth<ApiResponse<number>>(`/demo-api${countEndpoint}`, {}, selectedPersonaKey)
    return typeof response.data === 'number' ? response.data : Number(response.data ?? 0)
  } catch {
    return 0
  }
}

const checkDescriptions: Record<string, string> = {
  '模拟验证结果': '调用回路 2 中心的模拟决策接口（不写入生产审计，即 PDP 模拟），验证策略配置本身是否会产出预期决策',
  '实际执行结果': '业务网关（PEP）拦截该次请求后，实际执行并写入响应头的授权决策',
  '访问路径': '本次决策实际触发的跨表访问路径（Access Path），用于确认数据范围裁剪走的是哪条关联规则',
  '业务结果': '业务接口真实返回的 HTTP 状态、业务码与数据行数，用于确认前端拿到的数据是否符合预期',
  '生产审计': '从生产审计库反查到的历史决策记录，确认本次访问是否被正确留痕且决策一致',
  '回路 1 功能权限': '验证 RBAC 功能权限是否按预期拦截：无权限时业务接口应直接返回 403',
  '回路 2 未触发': '功能权限被拒绝时，数据权限（回路 2）不应被调用，因此不应产生 decisionKey',
  '接口调用': '演示流程中的某个 HTTP 请求未能正常完成，需检查服务是否在线',
}

function checkDescription(name: string): string {
  return checkDescriptions[name] ?? ''
}

const decisionLabels: Record<string, string> = {
  ALLOW_ALL: '全部可见',
  DENY_ALL: '全部不可见',
  FILTER: '部分可见（按规则过滤）',
  INDETERMINATE: '无法判定',
  RBAC_FORBIDDEN: '无功能权限（直接禁止）',
}

function decisionLabel(code: string | null | undefined): string {
  if (!code) {
    return '-'
  }
  const friendly = decisionLabels[code]
  return friendly ?? '未知决策'
}

function splitHeader(value: string | null): string[] {
  if (!value) {
    return []
  }
  return value.split(',').map(item => item.trim()).filter(Boolean)
}

function sameSet(left: string[], right: string[]): boolean {
  return left.length === right.length && left.every(item => right.includes(item))
}
</script>