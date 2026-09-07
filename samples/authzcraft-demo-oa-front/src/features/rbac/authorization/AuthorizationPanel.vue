<template>
  <a-card :bordered="false" class="rbac-panel authz-page-panel">
    <template #title><span class="title-with-icon"><LockOutlined />授权管理</span></template>
    <template #extra>
      <a-space>
        <a-button v-if="backLabel" size="small" @click="emit('backToList')">{{ backLabel }}</a-button>
        <a-select v-if="entryMode === 'menu'" v-model:value="principalTypeModel" size="small" :options="principalTypeOptions" style="width: 100px" />
        <a-select
          v-if="entryMode === 'menu'"
          v-model:value="selectedPrincipalCodes"
          mode="multiple"
          show-search
          :options="principalSelectOptions"
          :filter-option="filterPrincipalOption"
          :placeholder="principalPlaceholder"
          style="min-width: 280px"
          :maxTagCount="2"
        />
        <a-select v-else v-model:value="selectedPrincipalModel" size="small" show-search :options="principalSelectOptions" :filter-option="filterPrincipalOption" :placeholder="principalPlaceholder" class="rbac-role-selector" disabled />
      </a-space>
    </template>
    <div class="authz-page-toolbar">
      <a-space>
        <span v-if="selectedPrincipalLabels" class="authz-page-role-name">{{ selectedPrincipalLabels }}</span>
        <a-tag v-if="hasActivePrincipal && totalSelectedCount > 0" color="blue">已选 {{ selectedPermissionCodes.length }} 个功能权限{{ fieldPermissionCount ? `，${fieldPermissionCount} 个字段权限` : '' }}{{ dataPermissionCount ? `，${dataPermissionCount} 个数据权限` : '' }}</a-tag>
      </a-space>
    </div>

    <a-tabs v-model:active-key="activeTab" class="authz-page-tabs">
        <a-tab-pane key="functional" tab="功能权限">
          <div class="functional-perm-toolbar">
            <a-space>
              <a-tag v-if="permissionAddedCount" color="success">新增 {{ permissionAddedCount }}</a-tag>
              <a-tag v-if="permissionRemovedCount" color="warning">移除 {{ permissionRemovedCount }}</a-tag>
            </a-space>
            <a-space>
              <a-checkbox
                :checked="isAllPermissionChecked"
                :indeterminate="isAllPermissionIndeterminate"
                :disabled="!canManageRbac"
                @change="toggleAllPermissions"
              >全选</a-checkbox>
              <a-button size="small" type="link" :disabled="!canManageRbac" @click="invertPermissions">反选</a-button>
              <a-input-search v-model:value="permissionKeyword" allow-clear size="small" placeholder="搜索功能权限" class="authz-search" />
            </a-space>
          </div>
          <div class="authz-tree-table">
            <a-table
              size="small"
              :pagination="false"
              :columns="permissionColumns"
              :data-source="filteredPermissionTreeData"
              row-key="key"
              :expand-column-width="200"
              :default-expand-all-rows="true"
              :scroll="{ y: 'calc(100vh - 280px)' }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'name'">
                  <span :style="{ fontWeight: record.isGroup ? 600 : 400 }">{{ record.name }}</span>
                </template>
                <template v-if="column.key === 'permissions'">
                  <template v-if="record.isGroup">
                    <a-space>
                      <a-checkbox
                        :checked="isGroupAllChecked(record)"
                        :indeterminate="isGroupIndeterminate(record)"
                        :disabled="!canManageRbac"
                        @change="(e: CheckboxChangeEvent) => toggleGroup(record, e.target.checked)"
                      >全选</a-checkbox>
                      <a-button size="small" type="link" :disabled="!canManageRbac" @click="invertGroup(record)">反选</a-button>
                    </a-space>
                  </template>
                  <template v-else>
                    <a-checkbox
                      v-for="option in record.options"
                      :key="option.value"
                      :checked="props.selectedPermissionCodes.includes(option.value)"
                      :disabled="!canManageRbac"
                      :value="option.value"
                      class="perm-checkbox-item"
                      @change="(e: CheckboxChangeEvent) => toggleSinglePermission(option.value, e.target.checked)"
                    >{{ option.label }}</a-checkbox>
                  </template>
                </template>
              </template>
            </a-table>
          </div>
          <a-button type="primary" :disabled="!canManageRbac || !canSaveFunctionalPermissions" :loading="loading" @click="emit('savePermissions')">保存功能权限</a-button>
        </a-tab-pane>

        <a-tab-pane key="field" tab="字段权限">
          <div class="field-perm-toolbar">
            <a-input-search v-model:value="fieldPermissionKeyword" allow-clear size="small" placeholder="搜索字段权限" class="authz-search" />
            <a-button type="primary" size="small" :disabled="!canManageRbac" @click="fieldCreateOpen = true">添加字段</a-button>
          </div>
          <a-collapse :bordered="false" class="authz-field-collapse" :default-active-key="filteredFieldPermissionGroups.map(g => g.key)">
            <a-collapse-panel v-for="group in filteredFieldPermissionGroups" :key="group.key" :header="group.title">
              <a-table size="small" :pagination="false" :columns="fieldPermissionColumns" :data-source="group.rows" row-key="value">
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'security'">
                    <a-tag :color="record.securityRequirement === 'HIDDEN' ? 'red' : record.securityRequirement === 'MASKED' ? 'orange' : 'green'">{{ securityRequirementLabel(record.securityRequirement) }}</a-tag>
                  </template>
                  <template v-if="column.key === 'access'">
                    <a-radio-group :value="selectedFieldPermissionForRow(record)" :disabled="!canManageRbac" @change="(e: RadioChangeEvent) => selectFieldPermissionFromEvent(record, e)">
                      <a-radio v-for="option in record.options" :key="option.value" :value="option.value">{{ accessLevelLabel(option.accessLevel) }}</a-radio>
                    </a-radio-group>
                  </template>
                  <template v-if="column.key === 'fieldAction'">
                    <a-space>
                      <a-button type="link" size="small" :disabled="!canManageRbac" @click="openFieldEdit(record)">编辑</a-button>
                      <a-popconfirm title="确认删除该字段及其所有权限配置？" ok-text="确认" cancel-text="取消" @confirm="deleteFieldPermission(record)">
                        <a-button type="link" size="small" danger :disabled="!canManageRbac">删除</a-button>
                      </a-popconfirm>
                    </a-space>
                  </template>
                </template>
              </a-table>
            </a-collapse-panel>
          </a-collapse>
          <a-button type="primary" :disabled="!canManageRbac || !canSaveFunctionalPermissions" :loading="loading" @click="emit('saveFieldPermissions')">保存字段权限</a-button>
        </a-tab-pane>

        <a-tab-pane key="data" tab="数据权限" force-render>
          <div class="authz-data-tab">
            <DataPermissionPanel
              ref="dataPermissionPanelRef"
              :can-manage-rbac="canManageRbac"
              :roles="roles"
              :groups="groups"
              :user-options="userOptions"
              :department-options="departmentOptions"
              :position-options="positionOptions"
              :persona-key="personaKey"
              :filter-user-option="filterUserOption"
              :embedded="true"
              @fields-loaded="(_key: string, _names: Record<string, string>) => {}"
              @grant-count-changed="(count: number) => dataPermissionCount = count"
            />
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="fieldCreateOpen" title="添加字段" ok-text="创建" cancel-text="取消" :confirm-loading="loading" @ok="submitFieldCreate">
      <a-form layout="vertical" class="compact-form">
        <a-form-item label="数据表">
          <a-select v-model:value="fieldCreateDraft.resourceId" :options="catalogResourceOptions" :filter-option="filterCatalogOption" show-search placeholder="选择数据表" @change="onCatalogResourceChange" />
        </a-form-item>
        <a-form-item label="字段编码">
          <a-select v-model:value="fieldCreateDraft.fieldKey" :options="catalogFieldOptions" :filter-option="filterCatalogOption" show-search placeholder="选择字段" :disabled="!fieldCreateDraft.resourceId" @change="onCatalogFieldChange" />
        </a-form-item>
        <a-form-item label="字段名称">
          <a-input v-model:value="fieldCreateDraft.name" placeholder="字段名称" />
        </a-form-item>
        <a-alert type="info" show-icon message="字段安全要求（明文/脱敏/隐藏）由开发期通过 MCP 在回路2资源字段登记时指定，运行期不可修改" />
      </a-form>
    </a-modal>

    <a-modal v-model:open="fieldEditOpen" title="编辑字段" ok-text="保存" cancel-text="取消" :confirm-loading="loading" @ok="submitFieldEdit">
      <a-form layout="vertical" class="compact-form">
        <a-form-item label="字段编码">
          <a-input :value="fieldEditDraft.fieldKey" disabled />
        </a-form-item>
        <a-form-item label="字段名称">
          <a-input v-model:value="fieldEditDraft.name" />
        </a-form-item>
        <a-form-item label="安全要求">
          <a-tag :color="fieldEditDraft.securityRequirement === 'HIDDEN' ? 'red' : fieldEditDraft.securityRequirement === 'MASKED' ? 'orange' : 'green'">{{ securityRequirementLabel(fieldEditDraft.securityRequirement) }}</a-tag>
          <span style="margin-left: 8px; font-size: 12px; color: #8c8c8c">由开发期 MCP 设置，运行期不可修改</span>
        </a-form-item>
      </a-form>
    </a-modal>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { LockOutlined } from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'
import type { RbacGroup, RbacRole, SelectOption } from '../../../types/domain'
import { postJsonWithAuth, postJson, type ApiResponse } from '../../../services/http'
import DataPermissionPanel from './DataPermissionPanel.vue'

type CheckboxOption = { label: string; value: string }
type CheckboxChangeEvent = { target: { checked: boolean } }
type RadioChangeEvent = { target: { value: string } }
type FieldPermissionRow = CheckboxOption & { resourceKey: string; fieldKey: string; accessLevel: string; securityRequirement: string; name: string }
type FieldPermissionMatrixRow = { value: string; resourceKey: string; fieldKey: string; name: string; securityRequirement: string; options: FieldPermissionRow[] }
type FieldPermissionGroup = { key: string; title: string; rows: FieldPermissionMatrixRow[] }
type PermissionTreeNode = {
  key: string
  name: string
  isGroup: boolean
  options: CheckboxOption[]
  children?: PermissionTreeNode[]
}

const props = defineProps<{
  role: RbacRole | null
  roles: RbacRole[]
  groups: RbacGroup[]
  selectedRoleId: string | null
  selectedRoleName: string
  selectedPermissionCodes: string[]
  selectedFieldPermissionCodes: string[]
  originalPermissionCodes: string[]
  originalFieldPermissionCodes: string[]
  permissionOptions: CheckboxOption[]
  fieldPermissionOptions: CheckboxOption[]
  backLabel?: string
  entryMode: 'menu' | 'role' | 'group'
  lockedPrincipalCode?: string
  canManageRbac: boolean
  loading: boolean
  userOptions: SelectOption[]
  departmentOptions: SelectOption[]
  positionOptions: SelectOption[]
  personaKey: string
  filterUserOption: (input: string, option: SelectOption) => boolean
}>()

const emit = defineEmits<{
  'update:selectedPermissionCodes': [value: string[]]
  'update:selectedFieldPermissionCodes': [value: string[]]
  selectRole: [role: RbacRole]
  selectGroup: [group: RbacGroup]
  backToList: []
  savePermissions: []
  saveFieldPermissions: []
  fieldPermissionsChanged: []
}>()

const activeTab = ref<'functional' | 'field' | 'data'>('functional')
const permissionKeyword = ref('')
const fieldPermissionKeyword = ref('')
const dataPermissionPanelRef = ref<InstanceType<typeof DataPermissionPanel> | null>(null)
const dataPermissionCount = ref(0)
const principalType = ref<'ROLE' | 'GROUP' | 'USER' | 'DEPARTMENT' | 'POSITION'>('ROLE')

const principalTypeOptions = [
  { label: '角色', value: 'ROLE' },
  { label: '用户组', value: 'GROUP' },
  { label: '用户', value: 'USER' },
  { label: '部门', value: 'DEPARTMENT' },
  { label: '岗位', value: 'POSITION' },
]

const principalTypeModel = computed({
  get: () => principalType.value,
  set: (value: 'ROLE' | 'GROUP' | 'USER' | 'DEPARTMENT' | 'POSITION') => {
    principalType.value = value
    selectedPrincipalCodes.value = []
  },
})

const selectedPrincipalCodes = ref<string[]>([])

watch(selectedPrincipalCodes, (codes) => {
  if (!codes.length) {
    emit('update:selectedPermissionCodes', [])
    emit('update:selectedFieldPermissionCodes', [])
    dataPermissionCount.value = 0
    // 未选授权目标时清空数据权限列表（不再按任何主体过滤）
    dataPermissionPanelRef.value?.clearPrincipal()
    return
  }
  const last = codes[codes.length - 1]
  if (principalType.value === 'ROLE') {
    const role = props.roles.find(item => item.code === last)
    if (role) {
      emit('selectRole', role)
    }
  } else if (principalType.value === 'GROUP') {
    const group = props.groups.find(item => item.code === last)
    if (group) {
      emit('selectGroup', group)
    }
  }
  const typeMap: Record<string, 'ROLE' | 'GROUP' | 'USER'> = { ROLE: 'ROLE', GROUP: 'GROUP', USER: 'USER', DEPARTMENT: 'GROUP', POSITION: 'USER' }
  const mappedType = typeMap[principalType.value] ?? 'GROUP'
  nextTick(() => {
    dataPermissionPanelRef.value?.selectPrincipal(mappedType, last)
  })
})

const principalSelectOptions = computed(() => {
  if (principalType.value === 'USER') {
    return props.userOptions
  }
  if (principalType.value === 'ROLE') {
    return props.roles.map(item => ({ label: `${item.name} · ${item.code}`, value: item.code }))
  }
  if (principalType.value === 'GROUP') {
    return props.groups.map(item => ({ label: `${item.name} · ${item.code}`, value: item.code }))
  }
  if (principalType.value === 'DEPARTMENT') {
    return props.departmentOptions
  }
  if (principalType.value === 'POSITION') {
    return props.positionOptions
  }
  return []
})

const principalPlaceholder = computed(() => {
  if (principalType.value === 'USER') { return '选择用户' }
  if (principalType.value === 'GROUP') { return '选择用户组' }
  if (principalType.value === 'DEPARTMENT') { return '选择部门' }
  if (principalType.value === 'POSITION') { return '选择岗位' }
  return '选择角色'
})

const selectedPrincipalModel = computed({
  get: () => {
    if (props.entryMode === 'role' || principalType.value === 'ROLE') {
      return props.roles.find(r => r.id === props.selectedRoleId)?.code ?? props.lockedPrincipalCode
    }
    if (props.entryMode === 'group') {
      return props.lockedPrincipalCode
    }
    return undefined
  },
  set: (value: string | undefined) => {
    if (principalType.value === 'ROLE') {
      const role = props.roles.find(item => item.code === value)
      if (role) {
        emit('selectRole', role)
      }
    } else if (value) {
      activeTab.value = 'data'
      const typeMap: Record<string, 'ROLE' | 'GROUP' | 'USER'> = { ROLE: 'ROLE', GROUP: 'GROUP', USER: 'USER', DEPARTMENT: 'GROUP', POSITION: 'USER' }
      const mappedType = typeMap[principalType.value] ?? 'GROUP'
      nextTick(() => {
        dataPermissionPanelRef.value?.selectPrincipal(mappedType, value)
      })
    }
  },
})

function filterPrincipalOption(input: string, option: { label?: string }): boolean {
  return String(option.label ?? '').toLowerCase().includes(input.toLowerCase())
}

const selectedPrincipalLabels = computed(() => {
  if (!selectedPrincipalCodes.value.length) {
    // 锁定模式（从角色/用户组列表进入）显示锁定的主体名
    if (props.entryMode === 'role') { return props.selectedRoleName || props.role?.name || '' }
    if (props.entryMode === 'group') { return props.lockedPrincipalCode || '' }
    // 菜单进入时未选择则不显示
    return ''
  }
  const labels: string[] = []
  for (const code of selectedPrincipalCodes.value) {
    const role = props.roles.find(r => r.code === code)
    if (role) { labels.push(role.name); continue }
    const group = props.groups.find(g => g.code === code)
    if (group) { labels.push(group.name); continue }
    const user = props.userOptions.find(u => u.value === code)
    if (user) { labels.push(String(user.label ?? user.value)); continue }
    labels.push(code)
  }
  return labels.join('、')
})
// 菜单模块枚举：value 用稳定英文标识（代码面向它处理），label 用中文（仅展示）
type MenuModuleKey = 'rbac' | 'meetings' | 'leave' | 'expense'

const MENU_MODULES: Record<MenuModuleKey, { label: string }> = {
  rbac: { label: '权限管理' },
  meetings: { label: '会议信息' },
  leave: { label: '请假申请' },
  expense: { label: '费用申请' },
}

// 功能权限 code 的 resource 段 -> 菜单模块枚举
const permissionMenuGroupMap: Record<string, MenuModuleKey> = {
  rbac: 'rbac',
  meetings: 'meetings',
  'meeting-participants': 'meetings',
  'leave-requests': 'leave',
  'leave-approvals': 'leave',
  'expense-requests': 'expense',
}

// 字段权限资源（AuthzCraft resourceKey，如 WB_EXPENSE_REQUEST）-> 菜单模块枚举
const permissionGroupLabels: Record<string, MenuModuleKey> = {
  WB_MEETING: 'meetings',
  WB_MEETING_PARTICIPANT: 'meetings',
  WB_LEAVE_REQUEST: 'leave',
  WB_LEAVE_REQUEST_APPROVAL: 'leave',
  WB_EXPENSE_REQUEST: 'expense',
}

function permissionMenuGroup(code: string): MenuModuleKey {
  const resource = code.split(':')[1] || ''
  return permissionMenuGroupMap[resource] ?? 'expense'
}

function menuModuleLabel(key: MenuModuleKey | string): string {
  return MENU_MODULES[key as MenuModuleKey]?.label ?? key
}

const permissionTreeData = computed<PermissionTreeNode[]>(() => {
  const groups = new Map<MenuModuleKey, CheckboxOption[]>()
  for (const option of props.permissionOptions) {
    const key = permissionMenuGroup(option.value)
    groups.set(key, [...(groups.get(key) ?? []), option])
  }
  return [...groups.entries()].map(([key, options]) => ({
    key,
    name: menuModuleLabel(key),
    isGroup: true,
    options: [],
    children: [{ key: `${key}__items`, name: '', isGroup: false, options }],
  }))
})

const filteredPermissionTreeData = computed<PermissionTreeNode[]>(() => {
  const keyword = permissionKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return permissionTreeData.value
  }
  return permissionTreeData.value
    .map(group => ({
      ...group,
      children: (group.children ?? []).map(child => ({
        ...child,
        options: child.options.filter(option => `${option.label} ${option.value}`.toLowerCase().includes(keyword)),
      })),
    }))
    .filter(group => (group.children ?? []).some(child => child.options.length > 0))
})

const permissionColumns = [
  { title: '菜单模块', dataIndex: 'name', key: 'name', width: 240 },
  { title: '操作权限', key: 'permissions' },
]

const permissionAddedCount = computed(() => props.selectedPermissionCodes.filter(code => !props.originalPermissionCodes.includes(code)).length)
const permissionRemovedCount = computed(() => props.originalPermissionCodes.filter(code => !props.selectedPermissionCodes.includes(code)).length)
const fieldPermissionCount = computed(() => {
  // 已选字段权限数：基于当前授权目标已选的字段权限（selectedFieldPermissionCodes）去重字段，
  // 而不是全量 fieldPermissionOptions —— 未选授权目标时应为 0
  const fieldKeys = new Set<string>()
  for (const code of props.selectedFieldPermissionCodes) {
    const parts = code.split(':')
    const resourceField = parts[0] ?? ''
    const dotIdx = resourceField.lastIndexOf('.')
    const fieldKey = dotIdx > 0 ? resourceField.substring(dotIdx + 1) : resourceField
    fieldKeys.add(fieldKey)
  }
  return fieldKeys.size
})
// 是否有明确的授权目标：锁定模式（从角色/用户组列表进入）恒为 true；菜单模式需已选授权目标
const hasActivePrincipal = computed(() => {
  if (props.entryMode === 'role' || props.entryMode === 'group') {
    return Boolean(props.lockedPrincipalCode || props.selectedRoleId)
  }
  return selectedPrincipalCodes.value.length > 0
})
// 功能权限/字段权限支持角色和用户组类型保存
const canSaveFunctionalPermissions = computed(() => {
  if (props.entryMode === 'role') { return true }
  if (props.entryMode === 'group') { return true }
  if (principalType.value === 'ROLE' && selectedPrincipalCodes.value.length > 0) { return true }
  if (principalType.value === 'GROUP' && selectedPrincipalCodes.value.length > 0) { return true }
  return false
})
const totalSelectedCount = computed(() => props.selectedPermissionCodes.length + fieldPermissionCount.value + dataPermissionCount.value)

const isAllPermissionChecked = computed(() => {
  const allCodes = props.permissionOptions.map(option => option.value)
  return allCodes.length > 0 && allCodes.every(code => props.selectedPermissionCodes.includes(code))
})
const isAllPermissionIndeterminate = computed(() => {
  const allCodes = props.permissionOptions.map(option => option.value)
  const selectedCount = allCodes.filter(code => props.selectedPermissionCodes.includes(code)).length
  return selectedCount > 0 && selectedCount < allCodes.length
})

function toggleAllPermissions(event: CheckboxChangeEvent): void {
  const allCodes = props.permissionOptions.map(option => option.value)
  if (event.target.checked) {
    emit('update:selectedPermissionCodes', Array.from(new Set([...props.selectedPermissionCodes, ...allCodes])))
  } else {
    emit('update:selectedPermissionCodes', props.selectedPermissionCodes.filter(code => !allCodes.includes(code)))
  }
}

function invertPermissions(): void {
  const allCodes = props.permissionOptions.map(option => option.value)
  const inverted = allCodes.filter(code => !props.selectedPermissionCodes.includes(code))
  emit('update:selectedPermissionCodes', inverted)
}

function isGroupAllChecked(group: PermissionTreeNode): boolean {
  const codes = (group.children ?? []).flatMap(child => child.options.map(option => option.value))
  return codes.length > 0 && codes.every(code => props.selectedPermissionCodes.includes(code))
}

function isGroupIndeterminate(group: PermissionTreeNode): boolean {
  const codes = (group.children ?? []).flatMap(child => child.options.map(option => option.value))
  const selectedCount = codes.filter(code => props.selectedPermissionCodes.includes(code)).length
  return selectedCount > 0 && selectedCount < codes.length
}

function toggleGroup(group: PermissionTreeNode, checked: boolean): void {
  const codes = (group.children ?? []).flatMap(child => child.options.map(option => option.value))
  const next = checked
    ? Array.from(new Set([...props.selectedPermissionCodes, ...codes]))
    : props.selectedPermissionCodes.filter(code => !codes.includes(code))
  emit('update:selectedPermissionCodes', next)
}

function invertGroup(group: PermissionTreeNode): void {
  const codes = (group.children ?? []).flatMap(child => child.options.map(option => option.value))
  const inverted = codes.filter(code => !props.selectedPermissionCodes.includes(code))
  const others = props.selectedPermissionCodes.filter(code => !codes.includes(code))
  emit('update:selectedPermissionCodes', [...others, ...inverted])
}

function toggleSinglePermission(code: string, checked: boolean): void {
  if (checked) {
    emit('update:selectedPermissionCodes', Array.from(new Set([...props.selectedPermissionCodes, code])))
  } else {
    emit('update:selectedPermissionCodes', props.selectedPermissionCodes.filter(c => c !== code))
  }
}

// Field permissions
const fieldPermissionColumns = [
  { title: '字段编码', dataIndex: 'fieldKey', key: 'fieldKey', width: 160 },
  { title: '字段名称', dataIndex: 'name', key: 'name' },
  { title: '安全要求', key: 'security', width: 100 },
  { title: '访问级别', key: 'access', width: 360 },
  { title: '操作', key: 'fieldAction', width: 120 },
]

function fieldPermissionRow(option: CheckboxOption): FieldPermissionRow {
  // 后端已返回 resourceKey/fieldKey/accessLevel/securityRequirement/name，直接使用
  const resourceKey = (option as any).resourceKey as string || '其他'
  const fieldKey = (option as any).fieldKey as string || option.value
  const accessLevel = (option as any).accessLevel as string || '-'
  const securityRequirement = (option as any).securityRequirement as string || 'PLAIN'
  const name = (option as any).name as string || option.label
  return {
    ...option,
    resourceKey,
    fieldKey,
    accessLevel,
    securityRequirement,
    name,
  }
}

const fieldPermissionGroups = computed<FieldPermissionGroup[]>(() => {
  // 未选授权目标时不显示字段权限列表
  if (!hasActivePrincipal.value) { return [] }
  // 只显示与当前授权目标已绑定的字段
  const boundFields = new Set<string>()
  for (const code of props.selectedFieldPermissionCodes) {
    const colonIdx = code.lastIndexOf(':')
    if (colonIdx > 0) { boundFields.add(code.substring(0, colonIdx)) }
  }
  const groups = new Map<string, Map<string, FieldPermissionRow[]>>()
  for (const option of props.fieldPermissionOptions) {
    const row = fieldPermissionRow(option)
    const fieldKey = `${row.resourceKey}.${row.fieldKey}`
    if (!boundFields.has(fieldKey)) { continue }
    const fields = groups.get(row.resourceKey) ?? new Map<string, FieldPermissionRow[]>()
    fields.set(row.fieldKey, [...(fields.get(row.fieldKey) ?? []), row])
    groups.set(row.resourceKey, fields)
  }
  return [...groups.entries()].map(([key, fields]) => ({
    key,
    title: menuModuleLabel(permissionGroupLabels[key] ?? key),
    rows: [...fields.entries()].map(([fieldKey, options]) => ({
      value: `${key}.${fieldKey}`,
      resourceKey: key,
      fieldKey,
      name: options[0]?.name || fieldKey,
      securityRequirement: options[0]?.securityRequirement || 'PLAIN',
      options: [...options].sort((a, b) => accessRank(a.accessLevel) - accessRank(b.accessLevel)),
    })),
  }))
})

const filteredFieldPermissionGroups = computed<FieldPermissionGroup[]>(() => {
  const keyword = fieldPermissionKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return fieldPermissionGroups.value
  }
  return fieldPermissionGroups.value
    .map(group => ({ ...group, rows: group.rows.filter(row => `${row.resourceKey} ${row.fieldKey} ${row.name}`.toLowerCase().includes(keyword)) }))
    .filter(group => group.rows.length > 0)
})

function selectedFieldPermissionForRow(row: FieldPermissionMatrixRow): string {
  // 已配置绑定的字段：返回已绑定的 access_level
  const bound = row.options.find(option => props.selectedFieldPermissionCodes.includes(option.value))
  if (bound) { return bound.value }
  // 未配置绑定的字段：返回 security_requirement 对应的默认 access_level
  const defaultLevel = defaultAccessLevelForSecurity(row.securityRequirement)
  return row.options.find(option => option.accessLevel === defaultLevel)?.value ?? row.options[0]?.value ?? ''
}

function defaultAccessLevelForSecurity(securityRequirement: string): string {
  if (securityRequirement === 'HIDDEN') { return 'NONE' }
  if (securityRequirement === 'MASKED') { return 'READ' }
  return 'READ' // PLAIN
}

function selectFieldPermission(row: FieldPermissionMatrixRow, code: string): void {
  const rowCodes = row.options.map(option => option.value)
  const withoutCurrentField = props.selectedFieldPermissionCodes.filter(item => !rowCodes.includes(item))
  emit('update:selectedFieldPermissionCodes', [...withoutCurrentField, code])
}

function selectFieldPermissionFromEvent(row: FieldPermissionMatrixRow, event: RadioChangeEvent): void {
  selectFieldPermission(row, event.target.value)
}

function accessLevelLabel(level: string): string {
  if (level === 'WRITE') { return '可写' }
  if (level === 'PLAIN_READ') { return '可明文读' }
  if (level === 'READ') { return '可读' }
  if (level === 'NONE') { return '无权限' }
  return level
}

function accessRank(level: string): number {
  if (level === 'NONE') { return 0 }
  if (level === 'READ') { return 1 }
  if (level === 'PLAIN_READ') { return 2 }
  if (level === 'WRITE') { return 3 }
  return 0
}

function securityRequirementLabel(req: string): string {
  if (req === 'PLAIN') { return '明文' }
  if (req === 'MASKED') { return '脱敏' }
  if (req === 'HIDDEN') { return '隐藏' }
  return req
}

// Field permission CRUD
type CatalogResource = { id: string; resourceKey: string; displayName: string; physicalName?: string }
type CatalogField = { id: string; fieldKey: string; displayName: string; filterableFlag?: boolean }

const fieldCreateOpen = ref(false)
const fieldCreateDraft = ref({ resourceId: '', resourceKey: '', fieldKey: '', name: '', securityRequirement: 'PLAIN' })
const catalogResources = ref<CatalogResource[]>([])
const catalogFields = ref<CatalogField[]>([])

const catalogResourceOptions = computed(() => catalogResources.value.map(r => ({ label: `${r.displayName || r.resourceKey} · ${r.resourceKey}`, value: r.id })))
const catalogFieldOptions = computed(() => catalogFields.value.map(f => ({ label: `${f.displayName || f.fieldKey} · ${f.fieldKey}`, value: f.fieldKey })))

function filterCatalogOption(input: string, option: { label?: string }): boolean {
  return String(option.label ?? '').toLowerCase().includes(input.toLowerCase())
}

async function loadCatalogResources(): Promise<void> {
  try {
    const response = await postJson<ApiResponse<CatalogResource[]>>('/center-api/authzcraft/api/v1/catalog/relation-resources/search', { tenantKey: 'platform', appKey: 'authzcraft-demo-oa' })
    catalogResources.value = response.data ?? []
  } catch { catalogResources.value = [] }
}

async function onCatalogResourceChange(resourceId: string): Promise<void> {
  fieldCreateDraft.value.resourceId = resourceId
  fieldCreateDraft.value.fieldKey = ''
  fieldCreateDraft.value.name = ''
  const resource = catalogResources.value.find(r => r.id === resourceId)
  fieldCreateDraft.value.resourceKey = resource?.resourceKey ?? ''
  try {
    const response = await postJson<ApiResponse<CatalogField[]>>(`/center-api/authzcraft/api/v1/catalog/relation-resources/${resourceId}/fields/search`, {})
    catalogFields.value = response.data ?? []
  } catch { catalogFields.value = [] }
}

function onCatalogFieldChange(fieldKey: string): void {
  const field = catalogFields.value.find(f => f.fieldKey === fieldKey)
  fieldCreateDraft.value.name = field?.displayName || fieldKey
}

async function submitFieldCreate(): Promise<void> {
  if (!fieldCreateDraft.value.resourceKey || !fieldCreateDraft.value.fieldKey) { return }
  try {
    const response = await postJsonWithAuth<ApiResponse<Record<string, unknown>>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/field-permissions/create', {
      resourceKey: fieldCreateDraft.value.resourceKey,
      fieldKey: fieldCreateDraft.value.fieldKey,
      name: fieldCreateDraft.value.name || fieldCreateDraft.value.fieldKey,
    }, props.personaKey)
    fieldCreateOpen.value = false
    fieldCreateDraft.value = { resourceId: '', resourceKey: '', fieldKey: '', name: '', securityRequirement: 'PLAIN' }
    // 创建字段后自动绑定到当前授权目标（使用 security_requirement 对应的默认 access_level）
    autoBindNewField(response.data)
    emit('fieldPermissionsChanged')
  } catch (e) {
    Modal.error({ title: '创建失败', content: e instanceof Error ? e.message : String(e) })
  }
}

function autoBindNewField(fieldInfo: Record<string, unknown> | undefined): void {
  if (!fieldInfo) { return }
  const resourceKey = String(fieldInfo.resourceKey ?? '')
  const fieldKey = String(fieldInfo.fieldKey ?? '')
  const securityRequirement = String(fieldInfo.securityRequirement ?? 'PLAIN')
  const defaultLevel = defaultAccessLevelForSecurity(securityRequirement)
  const defaultCode = `${resourceKey}.${fieldKey}:${defaultLevel}`
  if (!props.selectedFieldPermissionCodes.includes(defaultCode)) {
    emit('update:selectedFieldPermissionCodes', [...props.selectedFieldPermissionCodes, defaultCode])
  }
}

async function deleteFieldPermission(row: FieldPermissionMatrixRow): Promise<void> {
  await postJsonWithAuth<ApiResponse<void>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/field-permissions/delete', {
    resourceKey: row.resourceKey,
    fieldKey: row.fieldKey,
  }, props.personaKey)
  // 从已选列表中移除该字段的所有 code
  const remaining = props.selectedFieldPermissionCodes.filter(code => {
    const colonIdx = code.lastIndexOf(':')
    const fieldKey = colonIdx > 0 ? code.substring(0, colonIdx) : code
    return fieldKey !== `${row.resourceKey}.${row.fieldKey}`
  })
  emit('update:selectedFieldPermissionCodes', remaining)
  emit('fieldPermissionsChanged')
}

// Field edit
const fieldEditOpen = ref(false)
const fieldEditDraft = ref({ resourceKey: '', fieldKey: '', name: '', securityRequirement: 'PLAIN' })

function openFieldEdit(row: FieldPermissionMatrixRow): void {
  fieldEditDraft.value = {
    resourceKey: row.resourceKey,
    fieldKey: row.fieldKey,
    name: row.name,
    securityRequirement: row.securityRequirement,
  }
  fieldEditOpen.value = true
}

async function submitFieldEdit(): Promise<void> {
  if (!fieldEditDraft.value.resourceKey || !fieldEditDraft.value.fieldKey) { return }
  await postJsonWithAuth<ApiResponse<Record<string, unknown>>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/field-permissions/update', {
    resourceKey: fieldEditDraft.value.resourceKey,
    fieldKey: fieldEditDraft.value.fieldKey,
    name: fieldEditDraft.value.name,
  }, props.personaKey)
  fieldEditOpen.value = false
  emit('fieldPermissionsChanged')
}

watch(() => fieldCreateOpen.value, (open) => {
  if (open && !catalogResources.value.length) {
    void loadCatalogResources()
  }
})

watch(() => props.selectedRoleId, async (id) => {
  if (props.entryMode === 'role' && id && props.role) {
    principalType.value = 'ROLE'
    activeTab.value = 'functional'
    await nextTick()
    dataPermissionPanelRef.value?.selectPrincipal('ROLE', props.role.code)
    await loadDataPermissionCount(props.role.code, 'ROLE')
  }
})

watch(() => props.entryMode, (mode) => {
  if (mode === 'role') {
    principalType.value = 'ROLE'
    activeTab.value = 'functional'
    // Load data permission count for the role after props update
    nextTick().then(() => {
      const code = props.role?.code ?? props.lockedPrincipalCode ?? ''
      if (code) {
        void loadDataPermissionCount(code, 'ROLE')
      }
    })
  } else if (mode === 'group') {
    principalType.value = 'GROUP'
    if (props.lockedPrincipalCode) {
      nextTick(() => {
        dataPermissionPanelRef.value?.selectPrincipal('GROUP', props.lockedPrincipalCode!)
      })
      loadDataPermissionCount(props.lockedPrincipalCode, 'GROUP')
    }
  } else if (mode === 'menu') {
    principalType.value = 'ROLE'
    activeTab.value = 'functional'
  }
}, { immediate: true })

async function loadDataPermissionCount(principalCode: string, principalKind: string): Promise<void> {
  try {
    const principalRes = await postJson<ApiResponse<{ id: string; principalKey: string }[]>>('/center-api/authzcraft/api/v1/principals/search', {
      tenantKey: 'platform', principalKind,
    })
    const grantRes = await postJson<ApiResponse<{ principalId: string; lifecycleState: string }[]>>('/center-api/authzcraft/api/v1/grants/access-grants/search', {
      tenantKey: 'platform', appKey: 'authzcraft-demo-oa',
    })
    const principal = (principalRes.data ?? []).find(p => p.principalKey === principalCode)
    if (principal) {
      dataPermissionCount.value = (grantRes.data ?? []).filter(g => g.principalId === principal.id && g.lifecycleState !== 'REVOKED').length
    }
  } catch {
    dataPermissionCount.value = 0
  }
}
</script>

<style scoped>
.authz-page-panel {
  min-height: auto;
  max-height: none;
  overflow: visible;
  border: 1px solid var(--line);
  box-shadow: none;
}

.authz-page-toolbar {
  display: flex;
  align-items: center;
  padding: 0 0 12px;
  border-bottom: 1px solid #f0f0f0;
}

.functional-perm-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.authz-page-role-name {
  font-size: 16px;
  font-weight: 600;
}

.authz-page-tabs {
  padding-top: 4px;
}

.authz-search {
  width: 300px;
}

.authz-tree-table :deep(.ant-table-body) {
  overflow: auto !important;
}

.perm-checkbox-item {
  margin-right: 12px;
  margin-bottom: 4px;
}

.authz-field-collapse {
  margin-top: 10px;
}

.field-perm-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.authz-data-tab {
  padding-top: 10px;
}

.authz-data-hint {
  margin-bottom: 12px;
}
</style>
