<template>
  <a-row v-if="mode === 'roles'" :gutter="14" class="rbac-grid">
    <a-col :xs="24">
      <a-card :bordered="false" class="rbac-panel">
        <template #title><span class="title-with-icon"><TeamOutlined />角色管理</span></template>
        <template #extra>
          <a-space>
            <a-input-search v-model:value="roleKeyword" size="small" allow-clear placeholder="搜索角色" class="rbac-table-search" />
            <a-popconfirm v-if="selectedRoleKeys.length" :title="`确认批量删除 ${selectedRoleKeys.length} 个角色？`" ok-text="确认" cancel-text="取消" @confirm="emit('batchDeleteRoles', filteredRoles.filter(r => selectedRoleKeys.includes(r.id)))">
              <a-button type="primary" size="small" danger :disabled="!canManageRbac">批量删除 ({{ selectedRoleKeys.length }})</a-button>
            </a-popconfirm>
            <a-button type="primary" size="small" :disabled="!canManageRbac" @click="roleCreateOpen = true">
              <template #icon><PlusOutlined /></template>
              新建角色
            </a-button>
          </a-space>
        </template>
        <a-table size="small" :pagination="false" :columns="roleColumns" :data-source="filteredRoles" row-key="id" :row-selection="{ selectedRowKeys: selectedRoleKeys, onChange: onRoleSelectionChange }" :row-class-name="roleRowClassName" class="role-table">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'description'">{{ record.description || '-' }}</template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="emit('inspectRole', record)">详情</a-button>
                <a-button type="link" size="small" @click="emit('selectRolePermissions', record)">授权</a-button>
                <a-button type="link" size="small" @click="emit('selectRoleMemberships', record)">成员管理</a-button>
                <a-button type="link" size="small" :disabled="!canManageRbac" @click="emit('copyRole', record)">复制</a-button>
                <a-button type="link" size="small" :disabled="!canManageRbac" @click="openRoleEdit(record)">编辑</a-button>
                <a-button type="link" size="small" danger :disabled="!canManageRbac" @click="emit('deleteRole', record)">删除</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-col>
  </a-row>

  <a-row v-if="mode === 'groups'" :gutter="14" class="rbac-grid">
    <a-col :xs="24">
      <a-card :bordered="false" class="rbac-panel">
        <template #title><span class="title-with-icon"><ApartmentOutlined />用户组管理</span></template>
        <template #extra>
          <a-space>
            <a-input-search v-model:value="groupKeyword" size="small" allow-clear placeholder="搜索用户组" class="rbac-table-search" />
            <a-popconfirm v-if="selectedGroupKeys.length" :title="`确认批量停用 ${selectedGroupKeys.length} 个用户组？`" ok-text="确认" cancel-text="取消" @confirm="emit('batchDisableGroups', filteredGroups.filter(g => selectedGroupKeys.includes(g.id)))">
              <a-button type="primary" size="small" danger :disabled="!canManageRbac">批量停用 ({{ selectedGroupKeys.length }})</a-button>
            </a-popconfirm>
            <a-button type="primary" size="small" :disabled="!canManageRbac" @click="groupCreateOpen = true">
              <template #icon><PlusOutlined /></template>
              新建用户组
            </a-button>
          </a-space>
        </template>
        <a-table size="small" :pagination="false" :columns="groupColumns" :data-source="filteredGroups" row-key="id" :row-selection="{ selectedRowKeys: selectedGroupKeys, onChange: onGroupSelectionChange }" :row-class-name="groupRowClassName" class="role-table">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'description'">{{ record.description || '-' }}</template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="emit('inspectGroup', record)">详情</a-button>
                <a-button type="link" size="small" @click="emit('selectGroupDataPermissions', record)">授权</a-button>
                <a-button type="link" size="small" @click="emit('selectGroupMemberships', record)">成员管理</a-button>
                <a-button type="link" size="small" :disabled="!canManageRbac" @click="emit('copyGroup', record)">复制</a-button>
                <a-button type="link" size="small" :disabled="!canManageRbac" @click="openGroupEdit(record)">编辑</a-button>
                <a-button type="link" size="small" danger :disabled="!canManageRbac" @click="emit('deleteGroup', record)">停用</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-col>
  </a-row>

  <a-modal v-model:open="roleCreateOpen" title="新建角色" ok-text="创建" cancel-text="取消" :confirm-loading="loading" :ok-button-props="roleCreateOkButtonProps" @ok="submitRoleCreate">
    <a-form layout="vertical" class="compact-form">
      <a-form-item label="角色编码">
        <a-input v-model:value="roleCode" :disabled="!canManageRbac" placeholder="demo_oa_custom" />
      </a-form-item>
      <a-form-item label="角色名称">
        <a-input v-model:value="roleName" :disabled="!canManageRbac" />
      </a-form-item>
      <a-form-item label="说明">
        <a-input v-model:value="roleDescription" :disabled="!canManageRbac" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal v-model:open="roleEditOpen" title="编辑角色" ok-text="保存" cancel-text="取消" :confirm-loading="loading" :ok-button-props="roleEditOkButtonProps" @ok="submitRoleUpdate">
    <a-form layout="vertical" class="compact-form">
      <a-form-item label="角色编码">
        <a-input :value="editingRole?.code" disabled />
      </a-form-item>
      <a-form-item label="角色名称">
        <a-input v-model:value="roleEditDraft.name" :disabled="!canManageRbac" />
      </a-form-item>
      <a-form-item label="说明">
        <a-input v-model:value="roleEditDraft.description" :disabled="!canManageRbac" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal v-model:open="groupCreateOpen" title="新建用户组" ok-text="创建" cancel-text="取消" :confirm-loading="loading" :ok-button-props="groupCreateOkButtonProps" @ok="submitGroupCreate">
    <a-form layout="vertical" class="compact-form">
      <a-form-item label="用户组编码">
        <a-input v-model:value="groupCode" :disabled="!canManageRbac" placeholder="demo_oa_project_group" />
      </a-form-item>
      <a-form-item label="用户组名称">
        <a-input v-model:value="groupName" :disabled="!canManageRbac" />
      </a-form-item>
      <a-form-item label="说明">
        <a-input v-model:value="groupDescription" :disabled="!canManageRbac" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal v-model:open="groupEditOpen" title="编辑用户组" ok-text="保存" cancel-text="取消" :confirm-loading="loading" :ok-button-props="groupEditOkButtonProps" @ok="submitGroupUpdate">
    <a-form layout="vertical" class="compact-form">
      <a-form-item label="用户组编码">
        <a-input :value="editingGroup?.code" disabled />
      </a-form-item>
      <a-form-item label="用户组名称">
        <a-input v-model:value="groupEditDraft.name" :disabled="!canManageRbac" />
      </a-form-item>
      <a-form-item label="说明">
        <a-input v-model:value="groupEditDraft.description" :disabled="!canManageRbac" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ApartmentOutlined, PlusOutlined, TeamOutlined } from '@ant-design/icons-vue'
import type { RbacGroup, RbacRole } from '../../../types/domain'

type Draft = { code: string; name: string; description: string }

const props = defineProps<{
  mode: 'roles' | 'groups'
  roles: RbacRole[]
  groups: RbacGroup[]
  roleDraft: Draft
  groupDraft: Draft
  selectedRoleId: string | null
  selectedGroupId: string | null
  canManageRbac: boolean
  loading: boolean
}>()

const emit = defineEmits<{
  'update:roleDraft': [value: Draft]
  'update:groupDraft': [value: Draft]
  createRole: []
  createGroup: []
  updateRole: [role: RbacRole, draft: { name: string; description: string }]
  updateGroup: [group: RbacGroup, draft: { name: string; description: string }]
  deleteRole: [role: RbacRole]
  deleteGroup: [group: RbacGroup]
  batchDeleteRoles: [roles: RbacRole[]]
  batchDisableGroups: [groups: RbacGroup[]]
  inspectRole: [role: RbacRole]
  inspectGroup: [group: RbacGroup]
  copyRole: [role: RbacRole]
  copyGroup: [group: RbacGroup]
  selectRolePermissions: [role: RbacRole]
  selectRoleDataPermissions: [role: RbacRole]
  selectRoleMemberships: [role: RbacRole]
  selectGroupMemberships: [group: RbacGroup]
  selectGroupDataPermissions: [group: RbacGroup]
}>()

const roleCreateOpen = ref(false)
const groupCreateOpen = ref(false)
const roleEditOpen = ref(false)
const groupEditOpen = ref(false)
const roleKeyword = ref('')
const groupKeyword = ref('')
const selectedRoleKeys = ref<string[]>([])
const selectedGroupKeys = ref<string[]>([])

function onRoleSelectionChange(keys: (string | number)[]): void {
  selectedRoleKeys.value = keys.map(String)
}

function onGroupSelectionChange(keys: (string | number)[]): void {
  selectedGroupKeys.value = keys.map(String)
}

function roleRowClassName(record: RbacRole): string {
  return record.id === props.selectedRoleId ? 'rbac-row-highlighted' : ''
}

function groupRowClassName(record: RbacGroup): string {
  return record.id === props.selectedGroupId ? 'rbac-row-highlighted' : ''
}
const editingRole = ref<RbacRole | null>(null)
const editingGroup = ref<RbacGroup | null>(null)
const roleEditDraft = ref({ name: '', description: '' })
const groupEditDraft = ref({ name: '', description: '' })

const roleColumns = [
  { title: '角色名称', dataIndex: 'name', key: 'name' },
  { title: '角色编码', dataIndex: 'code', key: 'code' },
  { title: '说明', dataIndex: 'description', key: 'description' },
  { title: '操作', key: 'action', width: 380 },
]
const groupColumns = [
  { title: '用户组名称', dataIndex: 'name', key: 'name' },
  { title: '用户组编码', dataIndex: 'code', key: 'code' },
  { title: '说明', dataIndex: 'description', key: 'description' },
  { title: '操作', key: 'action', width: 360 },
]

const roleCreateOkButtonProps = computed(() => ({ disabled: !props.canManageRbac || !roleCode.value.trim() || !roleName.value.trim() }))
const roleEditOkButtonProps = computed(() => ({ disabled: !props.canManageRbac || !editingRole.value || !roleEditDraft.value.name.trim() }))
const groupCreateOkButtonProps = computed(() => ({ disabled: !props.canManageRbac || !groupCode.value.trim() || !groupName.value.trim() }))
const groupEditOkButtonProps = computed(() => ({ disabled: !props.canManageRbac || !editingGroup.value || !groupEditDraft.value.name.trim() }))
const filteredRoles = computed(() => filterPrincipals(props.roles, roleKeyword.value))
const filteredGroups = computed(() => filterPrincipals(props.groups, groupKeyword.value))

function filterPrincipals<T extends { code: string; name: string; description?: string }>(items: T[], keyword: string): T[] {
  const text = keyword.trim().toLowerCase()
  if (!text) {
    return items
  }
  return items.filter(item => `${item.name} ${item.code} ${item.description || ''}`.toLowerCase().includes(text))
}

function submitRoleCreate(): void {
  if (roleCreateOkButtonProps.value.disabled) { return }
  roleCreateOpen.value = false
  emit('createRole')
}

function submitGroupCreate(): void {
  if (groupCreateOkButtonProps.value.disabled) { return }
  groupCreateOpen.value = false
  emit('createGroup')
}

function openRoleEdit(role: RbacRole): void {
  editingRole.value = role
  roleEditDraft.value = { name: role.name, description: role.description || '' }
  roleEditOpen.value = true
}

function openGroupEdit(group: RbacGroup): void {
  editingGroup.value = group
  groupEditDraft.value = { name: group.name, description: group.description || '' }
  groupEditOpen.value = true
}

function submitRoleUpdate(): void {
  if (!editingRole.value || roleEditOkButtonProps.value.disabled) { return }
  roleEditOpen.value = false
  emit('updateRole', editingRole.value, roleEditDraft.value)
}

function submitGroupUpdate(): void {
  if (!editingGroup.value || groupEditOkButtonProps.value.disabled) { return }
  groupEditOpen.value = false
  emit('updateGroup', editingGroup.value, groupEditDraft.value)
}

const roleCode = computed({ get: () => props.roleDraft.code, set: value => emit('update:roleDraft', { ...props.roleDraft, code: value }) })
const roleName = computed({ get: () => props.roleDraft.name, set: value => emit('update:roleDraft', { ...props.roleDraft, name: value }) })
const roleDescription = computed({ get: () => props.roleDraft.description, set: value => emit('update:roleDraft', { ...props.roleDraft, description: value }) })
const groupCode = computed({ get: () => props.groupDraft.code, set: value => emit('update:groupDraft', { ...props.groupDraft, code: value }) })
const groupName = computed({ get: () => props.groupDraft.name, set: value => emit('update:groupDraft', { ...props.groupDraft, name: value }) })
const groupDescription = computed({ get: () => props.groupDraft.description, set: value => emit('update:groupDraft', { ...props.groupDraft, description: value }) })
</script>
