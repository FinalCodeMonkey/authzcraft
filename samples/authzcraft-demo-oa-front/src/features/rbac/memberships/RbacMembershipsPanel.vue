<template>
  <a-row :gutter="14" class="rbac-grid">
    <a-col :xs="24">
      <div class="member-workbench-toolbar">
        <a-segmented v-model:value="activeMembershipMode" :options="filteredMembershipModeOptions" />
        <a-button v-if="backLabel" size="small" @click="emit('backToList')">{{ backLabel }}</a-button>
      </div>
    </a-col>

    <a-col v-if="activeMembershipMode === 'ROLE'" :xs="24">
      <a-card :bordered="false" class="rbac-panel">
        <template #title><span class="title-with-icon"><UserAddOutlined />角色成员</span></template>
        <template #extra>
          <a-select v-model:value="selectedRoleModel" size="small" show-search :options="roleOptions" :filter-option="filterRoleOption" placeholder="选择角色" class="rbac-role-selector" />
        </template>
        <a-empty v-if="!selectedRoleId" :description="null" />
        <template v-else>
          <a-row :gutter="12" class="member-add-row">
            <a-col :xs="24" :md="16">
              <a-select v-model:value="memberUserKeyModel" mode="multiple" show-search :options="userOptions" :filter-option="filterUserOption" placeholder="搜索并选择用户（可多选）" :maxTagCount="3" />
            </a-col>
            <a-col :xs="24" :md="8">
              <a-button type="primary" block :disabled="!canManageRbac || !memberUserKeys.length" :loading="loading" @click="emit('addRoleUser')">添加成员</a-button>
            </a-col>
          </a-row>
          <div v-if="selectedRoleUserKeys.length" class="batch-toolbar">
            <a-popconfirm :title="`确认批量移除 ${selectedRoleUserKeys.length} 名角色成员？`" ok-text="确认" cancel-text="取消" @confirm="emit('batchRemoveRoleUsers', roleUsers.filter(u => selectedRoleUserKeys.includes(u.userKey)))">
              <a-button type="primary" size="small" danger :disabled="!canManageRbac">批量移除 ({{ selectedRoleUserKeys.length }})</a-button>
            </a-popconfirm>
          </div>
          <a-table size="small" :pagination="false" :columns="memberColumns" :data-source="roleUsers" row-key="userKey" :row-selection="{ selectedRowKeys: selectedRoleUserKeys, onChange: onRoleUserSelectionChange }" class="member-list">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'displayName'">{{ record.displayName || record.userKey }}</template>
              <template v-if="column.key === 'action'">
                <a-popconfirm title="确认移除该角色成员？" ok-text="确认" cancel-text="取消" @confirm="emit('removeRoleUser', record)">
                  <a-button type="link" size="small" danger :disabled="!canManageRbac">移除</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </template>
      </a-card>
    </a-col>

    <a-col v-if="activeMembershipMode === 'GROUP'" :xs="24">
      <a-card :bordered="false" class="rbac-panel">
        <template #title><span class="title-with-icon"><UserAddOutlined />用户组成员</span></template>
        <template #extra>
          <a-select v-model:value="selectedGroupModel" size="small" show-search :options="groupOptions" :filter-option="filterGroupOption" placeholder="选择用户组" class="rbac-role-selector" />
        </template>
        <a-empty v-if="!selectedGroupId" :description="null" />
        <template v-else>
          <a-row :gutter="12" class="member-add-row">
            <a-col :xs="24" :md="16">
              <a-select v-model:value="groupMemberUserKeyModel" mode="multiple" show-search :options="userOptions" :filter-option="filterUserOption" placeholder="搜索并选择用户（可多选）" :maxTagCount="3" />
            </a-col>
            <a-col :xs="24" :md="8">
              <a-button type="primary" block :disabled="!canManageRbac || !groupMemberUserKeys.length" :loading="loading" @click="emit('addGroupUser')">添加成员</a-button>
            </a-col>
          </a-row>
          <div v-if="selectedGroupUserKeys.length" class="batch-toolbar">
            <a-popconfirm :title="`确认批量移除 ${selectedGroupUserKeys.length} 名用户组成员？`" ok-text="确认" cancel-text="取消" @confirm="emit('batchRemoveGroupUsers', groupUsers.filter(u => selectedGroupUserKeys.includes(u.userKey)))">
              <a-button type="primary" size="small" danger :disabled="!canManageRbac">批量移除 ({{ selectedGroupUserKeys.length }})</a-button>
            </a-popconfirm>
          </div>
          <a-table size="small" :pagination="false" :columns="memberColumns" :data-source="groupUsers" row-key="userKey" :row-selection="{ selectedRowKeys: selectedGroupUserKeys, onChange: onGroupUserSelectionChange }" class="member-list">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'displayName'">{{ record.displayName || record.userKey }}</template>
              <template v-if="column.key === 'action'">
                <a-popconfirm title="确认移除该用户组成员？" ok-text="确认" cancel-text="取消" @confirm="emit('removeGroupUser', record)">
                  <a-button type="link" size="small" danger :disabled="!canManageRbac">移除</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </template>
      </a-card>
    </a-col>

    <a-col v-if="activeMembershipMode === 'PEOPLE'" :xs="24">
      <a-card :bordered="false" class="rbac-panel">
        <template #title><span class="title-with-icon"><SwapOutlined />人员维度分配</span></template>
        <a-form layout="vertical" class="compact-form">
          <a-row :gutter="16">
            <a-col :xs="24">
              <a-form-item label="选择人员">
                <a-select v-model:value="assignmentUserKeys" mode="multiple" show-search :options="userOptions" :filter-option="filterUserOption" :disabled="!canManageRbac" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="8">
              <a-form-item label="分配对象类型">
                <a-segmented v-model:value="assignmentTargetType" :options="assignmentTargetTypeOptions" :disabled="!canManageRbac" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="16">
              <a-form-item label="选择角色或用户组">
                <a-select v-model:value="assignmentTargetCodes" mode="multiple" show-search :options="assignmentTargetOptions" :disabled="!canManageRbac" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-button class="section-action" type="primary" block :disabled="!canManageRbac || !assignmentCanSave" :loading="loading" @click="emit('saveBatchAssignments')">保存批量分配</a-button>
        </a-form>
        <a-alert v-if="batchAssignmentSummary" :type="batchAssignmentFailures.length ? 'warning' : 'success'" show-icon :message="batchAssignmentSummary" />
        <a-table
          v-if="batchAssignmentFailures.length"
          size="small"
          :pagination="false"
          :columns="batchFailureColumns"
          :data-source="batchAssignmentFailures"
          row-key="itemKey"
        />
        <a-divider orientation="left">人员权限画像</a-divider>
        <a-row :gutter="12" class="member-add-row">
          <a-col :xs="24" :md="8">
            <a-select v-model:value="selectedMembershipUserKeyModel" show-search :options="userOptions" :filter-option="filterUserOption" placeholder="选择人员" />
          </a-col>
        </a-row>
        <a-table
          size="small"
          :pagination="false"
          :loading="userMembershipLoading"
          :columns="membershipColumns"
          :data-source="membershipRows"
          row-key="rowKey"
        />
      </a-card>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { SwapOutlined, UserAddOutlined } from '@ant-design/icons-vue'
import type { AssignmentTargetType, BatchFailure, RbacGroup, RbacRole, RbacUser, RbacUserMembership, SelectOption } from '../../../types/domain'

type AssignmentDraft = { userKeys: string[]; targetType: AssignmentTargetType; targetCodes: string[] }
type SelectValue = string | number
type MembershipMode = 'ROLE' | 'GROUP' | 'PEOPLE'

type TargetOption = { label: string; value: string }
type MembershipRow = RbacUserMembership & { rowKey: string; targetTypeName: string; targetName: string }

const props = defineProps<{
  selectedRoleId: string | null
  selectedRoleName: string
  selectedGroupId: string | null
  selectedGroupName: string
  roles: RbacRole[]
  groups: RbacGroup[]
  roleUsers: RbacUser[]
  groupUsers: RbacUser[]
  userMemberships: RbacUserMembership[]
  userMembershipLoading: boolean
  memberUserKeys: string[]
  groupMemberUserKeys: string[]
  selectedMembershipUserKey: string
  assignmentDraft: AssignmentDraft
  membershipMode: MembershipMode
  assignmentTargetTypeOptions: Array<{ label: string; value: AssignmentTargetType }>
  assignmentTargetOptions: TargetOption[]
  assignmentCanSave: boolean
  batchAssignmentSummary: string
  batchAssignmentFailures: BatchFailure[]
  backLabel?: string
  entryMode: 'menu' | 'role' | 'group'
  userOptions: SelectOption[]
  canManageRbac: boolean
  loading: boolean
  filterUserOption: (input: string, option: SelectOption) => boolean
}>()

const emit = defineEmits<{
  'update:memberUserKeys': [value: string[]]
  'update:groupMemberUserKeys': [value: string[]]
  'update:assignmentDraft': [value: AssignmentDraft]
  'update:selectedMembershipUserKey': [value: string]
  'update:membershipMode': [value: MembershipMode]
  selectRole: [role: RbacRole]
  selectGroup: [group: RbacGroup]
  addRoleUser: []
  removeRoleUser: [user: RbacUser]
  batchRemoveRoleUsers: [users: RbacUser[]]
  addGroupUser: []
  removeGroupUser: [user: RbacUser]
  batchRemoveGroupUsers: [users: RbacUser[]]
  loadUserMemberships: [userKey: string]
  backToList: []
  saveBatchAssignments: []
}>()

const selectedRoleUserKeys = ref<string[]>([])
const selectedGroupUserKeys = ref<string[]>([])

function onRoleUserSelectionChange(keys: (string | number)[]): void {
  selectedRoleUserKeys.value = keys.map(String)
}

function onGroupUserSelectionChange(keys: (string | number)[]): void {
  selectedGroupUserKeys.value = keys.map(String)
}

const membershipModeOptions = [
  { label: '按角色维护', value: 'ROLE' },
  { label: '按用户组维护', value: 'GROUP' },
  { label: '按人员分配', value: 'PEOPLE' },
]
const filteredMembershipModeOptions = computed(() => {
  if (props.entryMode === 'role') {
    return membershipModeOptions.filter(item => item.value !== 'GROUP')
  }
  if (props.entryMode === 'group') {
    return membershipModeOptions.filter(item => item.value !== 'ROLE')
  }
  return membershipModeOptions
})
const activeMembershipMode = computed({
  get: () => props.membershipMode,
  set: value => emit('update:membershipMode', value as MembershipMode),
})

const batchFailureColumns = [
  { title: '对象', dataIndex: 'itemKey', key: 'itemKey' },
  { title: '失败原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
]
const memberColumns = [
  { title: '姓名', dataIndex: 'displayName', key: 'displayName' },
  { title: '用户账号', dataIndex: 'userKey', key: 'userKey' },
  { title: '操作', key: 'action', width: 90 },
]
const membershipColumns = [
  { title: '类型', dataIndex: 'targetTypeName', key: 'targetTypeName', width: 90 },
  { title: '名称', dataIndex: 'targetName', key: 'targetName' },
  { title: '编码', dataIndex: 'containerCode', key: 'containerCode', ellipsis: true },
  { title: '状态', dataIndex: 'lifecycleState', key: 'lifecycleState', width: 90 },
]

const roleOptions = computed(() => props.roles.map(role => ({ label: `${role.name} · ${role.code}`, value: role.id })))
const groupOptions = computed(() => props.groups.map(group => ({ label: `${group.name} · ${group.code}`, value: group.id })))
const selectedRoleModel = computed({
  get: () => props.selectedRoleId ?? undefined,
  set: value => {
    const role = props.roles.find(item => item.id === String(value))
    if (role) {
      emit('selectRole', role)
    }
  },
})
function filterRoleOption(input: string, option: { label?: string }): boolean {
  return String(option.label ?? '').toLowerCase().includes(input.toLowerCase())
}
const selectedGroupModel = computed({
  get: () => props.selectedGroupId ?? undefined,
  set: value => {
    const group = props.groups.find(item => item.id === String(value))
    if (group) {
      emit('selectGroup', group)
    }
  },
})
function filterGroupOption(input: string, option: { label?: string }): boolean {
  return String(option.label ?? '').toLowerCase().includes(input.toLowerCase())
}

const memberUserKeyModel = computed({ get: () => props.memberUserKeys, set: value => emit('update:memberUserKeys', value as string[]) })
const groupMemberUserKeyModel = computed({ get: () => props.groupMemberUserKeys, set: value => emit('update:groupMemberUserKeys', value as string[]) })
const selectedMembershipUserKeyModel = computed({
  get: () => props.selectedMembershipUserKey || undefined,
  set: value => {
    const userKey = String(value ?? '')
    emit('update:selectedMembershipUserKey', userKey)
    emit('loadUserMemberships', userKey)
  },
})
const assignmentUserKeys = computed({
  get: () => props.assignmentDraft.userKeys,
  set: value => emit('update:assignmentDraft', { ...props.assignmentDraft, userKeys: value as string[] }),
})
const assignmentTargetType = computed({
  get: () => props.assignmentDraft.targetType,
  set: value => emit('update:assignmentDraft', { ...props.assignmentDraft, targetType: value as AssignmentTargetType, targetCodes: [] }),
})
const assignmentTargetCodes = computed({
  get: () => props.assignmentDraft.targetCodes,
  set: value => emit('update:assignmentDraft', { ...props.assignmentDraft, targetCodes: (value as SelectValue[]).map(String) }),
})
const roleNameByCode = computed(() => new Map(props.roles.map(role => [role.code, role.name])))
const groupNameByCode = computed(() => new Map(props.groups.map(group => [group.code, group.name])))
const membershipRows = computed<MembershipRow[]>(() => props.userMemberships.map((membership, index) => {
  const kind = membership.containerKind === 'GROUP' ? '用户组' : '角色'
  const nameMap = membership.containerKind === 'GROUP' ? groupNameByCode.value : roleNameByCode.value
  return {
    ...membership,
    rowKey: `${membership.containerKind}-${membership.containerCode}-${membership.membershipId || index}`,
    targetTypeName: kind,
    targetName: nameMap.get(membership.containerCode) || membership.containerCode,
    lifecycleState: membership.lifecycleState || '-',
  }
}))
</script>
