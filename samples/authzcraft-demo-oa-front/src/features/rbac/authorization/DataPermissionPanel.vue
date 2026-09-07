<template>
  <a-card :bordered="false" :class="['rbac-panel', { 'rbac-panel-embedded': embedded }]">
    <template v-if="!embedded" #title><span class="title-with-icon"><TableOutlined />数据权限配置</span></template>
    <template v-if="!embedded" #extra>
      <a-space>
        <a-button v-if="backLabel" size="small" @click="emit('backToList')">{{ backLabel }}</a-button>
      </a-space>
    </template>

    <a-segmented v-model:value="listViewMode" :options="listViewModeOptions" block class="list-view-switch" />

    <div v-if="listViewMode === 'PRINCIPAL'" class="grant-workbench">
      <div class="grant-toolbar">
        <a-input-search v-model:value="grantKeyword" allow-clear size="small" placeholder="搜索授权目标、数据规则、状态" class="grant-search" />
        <a-button type="primary" size="small" :disabled="!canManageRbac" @click="openCreateDrawer">新建规则</a-button>
      </div>
      <a-table size="small" :pagination="{ pageSize: 10, showSizeChanger: true, showQuickJumper: true, pageSizeOptions: ['10', '20', '50'] }" :loading="grantLoading" :columns="grantColumns" :data-source="filteredAccessGrantRows" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'lifecycleState'">
            <a-tag :color="grantStateColor(record.lifecycleState)">{{ grantStateLabel(record.lifecycleState) }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" :disabled="!canManageRbac" @click="openEditDrawer(record)">修改</a-button>
              <a-button v-if="record.lifecycleState === 'ACTIVE'" type="link" size="small" :disabled="!canManageRbac" @click="updateGrantLifecycle(record, 'suspend')">禁用</a-button>
              <a-button v-if="record.lifecycleState === 'SUSPENDED'" type="link" size="small" :disabled="!canManageRbac" @click="updateGrantLifecycle(record, 'resume')">启用</a-button>
              <a-popconfirm title="确认作废该数据授权？" ok-text="确认" cancel-text="取消" @confirm="updateGrantLifecycle(record, 'revoke')">
                <a-button type="link" size="small" danger :disabled="!canManageRbac || record.lifecycleState === 'REVOKED'">作废</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <div v-if="listViewMode === 'RESOURCE'" class="resource-view-layout">
      <div class="resource-view-sidebar">
        <a-list size="small" bordered :data-source="resourceCardsWithGrantCount" class="data-resource-list">
          <template #renderItem="{ item }">
            <a-list-item :class="['data-resource-list-item', selectedTableId === item.id ? 'active' : '']" @click="selectTableForView(item.id)">
              <div class="data-resource-item-content">
                <div class="data-resource-item-header">
                  <span class="data-resource-item-name">{{ item.displayName }}</span>
                  <a-tag :color="item.grantCount > 0 ? 'blue' : 'default'">{{ item.grantCount }} 条规则</a-tag>
                </div>
                <div class="data-resource-item-key">{{ item.resourceKey }}</div>
              </div>
            </a-list-item>
          </template>
        </a-list>
      </div>
      <div class="resource-view-main">
        <div class="grant-toolbar">
          <a-input-search v-model:value="resourceViewKeyword" allow-clear size="small" placeholder="搜索授权目标、数据规则、状态" class="grant-search" />
          <a-button type="primary" size="small" :disabled="!canManageRbac" @click="openCreateDrawerForResourceView">新建规则</a-button>
        </div>
        <a-table size="small" :pagination="{ pageSize: 10, showSizeChanger: true, showQuickJumper: true, pageSizeOptions: ['10', '20', '50'] }" :loading="grantLoading" :columns="resourceViewColumns" :data-source="filteredResourceViewRows" row-key="id" :scroll="{ x: 'max-content' }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'lifecycleState'">
              <a-tag :color="grantStateColor(record.lifecycleState)">{{ grantStateLabel(record.lifecycleState) }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" :disabled="!canManageRbac" @click="openEditDrawer(record)">修改</a-button>
                <a-button v-if="record.lifecycleState === 'ACTIVE'" type="link" size="small" :disabled="!canManageRbac" @click="updateGrantLifecycle(record, 'suspend')">禁用</a-button>
                <a-button v-if="record.lifecycleState === 'SUSPENDED'" type="link" size="small" :disabled="!canManageRbac" @click="updateGrantLifecycle(record, 'resume')">启用</a-button>
                <a-popconfirm title="确认作废该数据授权？" ok-text="确认" cancel-text="取消" @confirm="updateGrantLifecycle(record, 'revoke')">
                  <a-button type="link" size="small" danger :disabled="!canManageRbac || record.lifecycleState === 'REVOKED'">作废</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <a-alert v-if="resultMessage" class="sync-status-alert" type="success" show-icon :message="resultMessage" />

    <a-drawer :open="drawerOpen" :title="drawerMode === 'edit' ? '编辑数据权限规则' : '新建数据权限规则'" :width="720" @close="drawerOpen = false" :body-style="{ paddingBottom: '80px' }">
      <a-segmented v-model:value="drawerViewMode" :options="drawerViewModeOptions" block class="drawer-view-switch" />
      <a-form layout="vertical" :class="['compact-form', 'data-permission-form', drawerViewMode === 'RESOURCE' ? 'resource-first' : 'principal-first']">
        <a-row :gutter="16">
          <a-col :xs="24" :md="24" class="dp-field-principal-type">
            <a-form-item label="授权目标类型">
              <a-segmented v-model:value="draft.principalType" :options="principalTypeOptions" :disabled="!canManageRbac" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="24" class="dp-field-principal">
            <a-form-item label="授权目标">
              <a-select v-model:value="draft.principalCodes" mode="multiple" show-search :options="principalOptions" :filter-option="draft.principalType === 'USER' ? filterUserOption : undefined" :disabled="!canManageRbac" :placeholder="principalPlaceholder" :maxTagCount="2" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="24" class="dp-field-resource">
            <a-form-item label="数据表">
              <a-select v-model:value="draft.resourceIds" mode="multiple" show-search :options="resourceOptions" :disabled="!canManageRbac" :maxTagCount="2" @change="onResourceChange" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="24" class="dp-field-template">
            <a-form-item label="可见数据范围">
              <a-segmented v-model:value="draft.ruleTemplate" :options="templateOptions" :disabled="!canManageRbac" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12" class="dp-field-operation">
            <a-form-item label="数据操作">
              <a-checkbox-group v-model:value="draft.operationCodes" :disabled="!canManageRbac">
                <a-checkbox v-for="opt in operationCodeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-checkbox>
              </a-checkbox-group>
            </a-form-item>
          </a-col>
          <a-col v-if="hasFieldInput" :xs="24" :md="12" class="dp-field-anchor-mode">
            <a-form-item label="数据归属字段来源">
              <a-segmented v-model:value="draft.anchorMode" :options="anchorModeOptions" :disabled="!canManageRbac" @change="onAnchorModeChange" />
            </a-form-item>
          </a-col>
          <a-col v-if="hasFieldInput && draft.anchorMode === 'ACCESS_PATH'" :xs="24" :md="12" class="dp-field-related-resource">
            <a-form-item label="关联表">
              <a-select v-model:value="draft.relatedResourceId" show-search :options="relatedResourceOptions" :disabled="!canManageRbac" placeholder="选择关联表" @change="loadRelatedResourceFields" />
            </a-form-item>
          </a-col>
          <a-col v-if="hasFieldInput" :xs="24" :md="12" class="dp-field-anchor-field">
            <a-form-item label="数据归属字段">
              <a-select v-model:value="draft.anchorField" show-search :options="anchorFieldOptions" :disabled="!canManageRbac" />
            </a-form-item>
          </a-col>
          <a-col v-if="hasArgument('departmentScopes')" :xs="24" :md="12" class="dp-field-rule-params">
            <a-form-item label="部门范围">
              <a-select v-model:value="draft.departmentScopes" mode="multiple" :options="departmentScopeOptions" :disabled="!canManageRbac" />
            </a-form-item>
          </a-col>
          <a-col v-if="hasArgument('departmentScopes') && draft.departmentScopes.includes('SUB_MANAGED')" :xs="24" :md="12" class="dp-field-rule-params">
            <a-form-item label="下级层级">
              <a-segmented v-model:value="draft.subDepartmentDepth" :options="depthOptions" :disabled="!canManageRbac" />
            </a-form-item>
          </a-col>
          <a-col v-if="hasArgument('specifiedDepartmentCodes')" :xs="24" :md="24" class="dp-field-rule-params">
            <a-form-item label="指定部门">
              <a-select v-model:value="draft.departmentCodes" mode="multiple" show-search :options="departmentOptions" :filter-option="filterUserOption" :disabled="!canManageRbac" :maxTagCount="3" placeholder="选择部门" />
            </a-form-item>
          </a-col>
          <a-col v-if="hasArgument('sharedOwnerUserKey')" :xs="24" :md="24" class="dp-field-rule-params">
            <a-form-item label="数据归属人">
              <a-select v-model:value="draft.fromUserKeys" mode="multiple" show-search :options="userOptions" :filter-option="filterUserOption" :disabled="!canManageRbac" :maxTagCount="3" placeholder="选择数据归属人" />
            </a-form-item>
          </a-col>
          <a-col v-if="hasArgument('allowReshare')" :xs="24" :md="12" class="dp-field-rule-params">
            <a-form-item label="允许再次分享">
              <a-switch v-model:checked="draft.allowReshare" :disabled="!canManageRbac" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12" class="dp-field-valid-until">
            <a-form-item label="失效时间">
              <a-input v-model:value="draft.validUntil" :disabled="!canManageRbac" placeholder="2026-12-31T23:59:59" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="drawer-footer">
        <a-space>
          <a-button @click="drawerOpen = false">取消</a-button>
          <a-button type="primary" :loading="loading" :disabled="!canManageRbac" @click="saveTemplate">保存</a-button>
        </a-space>
      </div>
    </a-drawer>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { TableOutlined } from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'
import { postJson, postJsonWithAuth, type ApiResponse } from '../../../services/http'
import type { RbacGroup, RbacRole, SelectOption } from '../../../types/domain'

type PrincipalType = 'ROLE' | 'GROUP' | 'USER' | 'DEPARTMENT' | 'POSITION'
type AnchorMode = 'DIRECT' | 'ACCESS_PATH'
type PermissionTemplate = 'ALL_DATA' | 'MY_DATA' | 'MANAGED_DEPARTMENTS' | 'SPECIFIED_DEPARTMENTS' | 'PERSON_SHARE'
type DataResource = { id: string; resourceKey: string; displayName: string; physicalName?: string; lifecycleState?: string }
type DataResourceField = { id: string; fieldKey: string; displayName: string; filterableFlag?: boolean }
type DataAccessPath = { id: string; pathKey: string; displayName: string; rootRelationId: string; destinationRelationId: string; executionMode: string; lifecycleState?: string }
type SaveResult = { summary: string; policyKey: string; grantId: string; simulation?: { assertionPassed?: boolean } }
type BlueprintInputField = { key: string; label: string; candidate: string; required: boolean }
type BlueprintInputAttribute = { key: string; attributeKey: string; valueKind: string; resolveKind: string }
type BlueprintInputArgument = { key: string; argumentKey: string; valueKind: string; required: boolean; label: string }
type BlueprintInputSchema = { fieldInputs: BlueprintInputField[]; attributeInputs: BlueprintInputAttribute[]; argumentInputs: BlueprintInputArgument[] }
type RuleBlueprintInfo = { id: string; blueprintKey: string; blueprintKind: string; displayName: string; inputSchema: string }
type AccessGrant = {
  id: string
  grantKey: string
  principalId: string
  policyId: string
  validUntil?: string
  lifecycleState?: string
  grantSource?: string
  reason?: string
}
type PrincipalInfo = { id: string; principalKind: string; principalKey: string; displayName?: string }
type AccessPolicyInfo = { id: string; policyKey: string; displayName?: string; operationCode?: string; ruleTemplate?: string; anchorField?: string; resourceKey?: string; targetRelationId?: string; departmentScopes?: string[]; departmentCodes?: string[]; fromUserKey?: string; allowReshare?: boolean; accessPathKey?: string; subDepartmentDepth?: string; ruleBlueprintDisplayName?: string }
type AccessGrantRow = AccessGrant & { principalTypeLabel: string; principalLabel: string; resourceLabel: string; policyLabel: string; ruleTemplateLabel: string }
type GrantLifecycleAction = 'suspend' | 'resume' | 'revoke'

const props = defineProps<{
  canManageRbac: boolean
  roles: RbacRole[]
  groups: RbacGroup[]
  userOptions: SelectOption[]
  departmentOptions: SelectOption[]
  positionOptions: SelectOption[]
  personaKey: string
  backLabel?: string
  embedded?: boolean
  filterUserOption: (input: string, option: SelectOption) => boolean
}>()

const emit = defineEmits<{
  fieldsLoaded: [resourceKey: string, fieldNames: Record<string, string>]
  backToList: []
  grantCountChanged: [count: number]
}>()

const loading = ref(false)
const grantLoading = ref(false)
const resources = ref<DataResource[]>([])
const blueprintSchemas = ref<Record<string, BlueprintInputSchema>>({})
const currentSchema = computed<BlueprintInputSchema | null>(() => {
  const schema = blueprintSchemas.value[draft.value.ruleTemplate]
  if (!schema) return null
  return schema
})
const hasFieldInput = computed(() => (currentSchema.value?.fieldInputs?.length ?? 0) > 0)
const hasArgument = (argumentKey: string) => currentSchema.value?.argumentInputs?.some(a => a.argumentKey === argumentKey) ?? false
const resourceFields = ref<DataResourceField[]>([])
const accessPaths = ref<DataAccessPath[]>([])
const accessGrants = ref<AccessGrant[]>([])
const grantKeyword = ref('')
// 当前查看的授权目标 ID：右上角选中某主体后，列表只显示该主体的数据权限规则
const viewPrincipalId = ref<string>('')
const principalsById = ref<Record<string, PrincipalInfo>>({})
const policiesById = ref<Record<string, AccessPolicyInfo>>({})
const resourceFieldCache = ref<Record<string, DataResourceField[]>>({})
const drawerViewMode = ref<'PRINCIPAL' | 'RESOURCE'>('PRINCIPAL')
const listViewMode = ref<'PRINCIPAL' | 'RESOURCE'>('PRINCIPAL')
const selectedTableId = ref<string>('')
const resourceViewKeyword = ref('')
const draft = ref({
  principalType: 'ROLE' as PrincipalType,
  principalCodes: [] as string[],
  resourceIds: [] as string[],
  operationCodes: ['READ'] as string[],
  ruleTemplate: 'MY_DATA' as PermissionTemplate,
  anchorMode: 'DIRECT' as AnchorMode,
  relatedResourceId: '',
  accessPathKey: '',
  anchorField: '',
  departmentScopes: ['DIRECT_MANAGED'],
  subDepartmentDepth: 'ALL',
  departmentCodes: [] as string[],
  fromUserKeys: [] as string[],
  allowReshare: false,
  validUntil: '',
  simulationRequesterKey: '',
})
const resultMessage = ref('')
const drawerOpen = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')

function openCreateDrawer(): void {
  drawerMode.value = 'create'
  drawerViewMode.value = listViewMode.value
  drawerOpen.value = true
}

async function selectTableForView(resourceId: string): Promise<void> {
  selectedTableId.value = resourceId
}

async function openCreateDrawerForResourceView(): Promise<void> {
  if (selectedTableId.value) {
    draft.value.resourceIds = [selectedTableId.value]
    await loadResourceFields(selectedTableId.value)
  }
  drawerMode.value = 'create'
  drawerViewMode.value = 'RESOURCE'
  drawerOpen.value = true
}

async function openEditDrawer(grant: AccessGrantRow): Promise<void> {
  const principal = principalsById.value[grant.principalId]
  const policy = policiesById.value[grant.policyId]
  if (!principal || !policy) {
    return
  }
  drawerMode.value = 'edit'
  // Resolve principal type
  const principalType = (principal.principalKind === 'ROLE' ? 'ROLE' : principal.principalKind === 'GROUP' ? 'GROUP' : principal.principalKind === 'DEPARTMENT' ? 'DEPARTMENT' : principal.principalKind === 'POSITION' ? 'POSITION' : 'USER') as PrincipalType
  draft.value.principalType = principalType
  draft.value.principalCodes = [principal.principalKey]
  // Resolve resource from policy.targetRelationId
  const resourceId = policy.targetRelationId ?? ''
  if (resourceId) {
    draft.value.resourceIds = [resourceId]
    await loadResourceFields(resourceId)
  }
  // Parse ruleTemplate from policyKey or displayName
  const displayParts = policy.displayName?.split(' - ') ?? []
  const templateLabel = displayParts[0] ?? ''
  const templateMap: Record<string, PermissionTemplate> = {
    '全部数据': 'ALL_DATA',
    '我的数据': 'MY_DATA',
    '我管理部门的数据': 'MANAGED_DEPARTMENTS',
    '管理部门数据': 'MANAGED_DEPARTMENTS',
    '指定部门数据': 'SPECIFIED_DEPARTMENTS',
    '分享数据': 'PERSON_SHARE',
  }
  draft.value.ruleTemplate = policy.ruleTemplate as PermissionTemplate ?? templateMap[templateLabel] ?? 'MY_DATA'
  // Parse anchorField from displayName (third part)
  const anchorField = displayParts.length >= 3 ? displayParts[2] : ''
  draft.value.anchorField = policy.anchorField ?? anchorField
  // Determine anchor mode from accessPathKey
  if (policy.accessPathKey) {
    draft.value.anchorMode = 'ACCESS_PATH'
    draft.value.accessPathKey = policy.accessPathKey
  } else {
    draft.value.anchorMode = 'DIRECT'
    draft.value.accessPathKey = ''
  }
  draft.value.relatedResourceId = ''
  // Restore rule parameters from policy
  draft.value.departmentScopes = policy.departmentScopes ?? ['DIRECT_MANAGED']
  draft.value.subDepartmentDepth = policy.subDepartmentDepth ?? 'ALL'
  draft.value.departmentCodes = policy.departmentCodes ?? []
  draft.value.fromUserKeys = policy.fromUserKey ? policy.fromUserKey.split(',') : []
  draft.value.allowReshare = policy.allowReshare ?? false
  draft.value.validUntil = grant.validUntil ?? ''
  drawerViewMode.value = listViewMode.value
  drawerOpen.value = true
}

const drawerViewModeOptions = [{ label: '按授权目标创建', value: 'PRINCIPAL' }, { label: '按数据表创建', value: 'RESOURCE' }]
const listViewModeOptions = [{ label: '授权目标视角', value: 'PRINCIPAL' }, { label: '数据表视角', value: 'RESOURCE' }]
const principalTypeOptions = [{ label: '角色', value: 'ROLE' }, { label: '用户组', value: 'GROUP' }, { label: '人员', value: 'USER' }, { label: '部门', value: 'DEPARTMENT' }, { label: '岗位', value: 'POSITION' }]
const principalOptions = computed(() => {
  if (draft.value.principalType === 'USER') {
    return props.userOptions
  }
  if (draft.value.principalType === 'DEPARTMENT') {
    return props.departmentOptions
  }
  if (draft.value.principalType === 'POSITION') {
    return props.positionOptions
  }
  const items = draft.value.principalType === 'ROLE' ? props.roles : props.groups
  return items.map(item => ({ label: `${item.name} · ${item.code}`, value: item.code }))
})
const principalPlaceholder = computed(() => {
  if (draft.value.principalType === 'USER') {
    return '选择人员'
  }
  if (draft.value.principalType === 'GROUP') {
    return '选择用户组'
  }
  if (draft.value.principalType === 'DEPARTMENT') {
    return '选择部门'
  }
  if (draft.value.principalType === 'POSITION') {
    return '选择岗位'
  }
  return '选择角色'
})
const operationCodeOptions = [
  { label: '查看', value: 'READ' },
  { label: '更新', value: 'UPDATE' },
  { label: '删除', value: 'DELETE' },
]
const templateOptions = [
  { label: '全部数据', value: 'ALL_DATA' },
  { label: '我的数据', value: 'MY_DATA' },
  { label: '我管理部门的数据', value: 'MANAGED_DEPARTMENTS' },
  { label: '指定部门数据', value: 'SPECIFIED_DEPARTMENTS' },
  { label: '分享某人的数据权限给某人', value: 'PERSON_SHARE' },
]
const anchorModeOptions = [
  { label: '本表', value: 'DIRECT' },
  { label: '关联表', value: 'ACCESS_PATH' },
]
const departmentScopeOptions = [
  { label: '直接管理部门', value: 'DIRECT_MANAGED' },
  { label: '管理的下级部门', value: 'SUB_MANAGED' },
  { label: '分管部门', value: 'PORTION_MANAGED' },
]
const depthOptions = [
  { label: '1级', value: '1' },
  { label: '2级', value: '2' },
  { label: '3级', value: '3' },
  { label: '全部', value: 'ALL' },
]
const grantColumns = [
  { title: '授权目标类型', dataIndex: 'principalTypeLabel', key: 'principalTypeLabel', width: 100 },
  { title: '授权目标', dataIndex: 'principalLabel', key: 'principalLabel', width: 140, ellipsis: true },
  { title: '数据表', dataIndex: 'resourceLabel', key: 'resourceLabel', width: 140, ellipsis: true },
  { title: '数据规则名称', dataIndex: 'policyLabel', key: 'policyLabel', width: 140, ellipsis: true },
  { title: '数据规则模板', dataIndex: 'ruleTemplateLabel', key: 'ruleTemplateLabel', width: 120 },
  { title: '状态', dataIndex: 'lifecycleState', key: 'lifecycleState', width: 80 },
  { title: '操作', key: 'action', width: 140 },
]
const resourceOptions = computed(() => resources.value.map(item => ({ label: `${item.displayName || item.resourceKey} · ${item.resourceKey}`, value: item.id })))
const fieldOptions = computed(() => resourceFields.value.filter(item => item.filterableFlag !== false).map(item => ({ label: `${item.displayName || item.fieldKey} · ${item.fieldKey}`, value: item.fieldKey })))
const relatedResourceOptions = computed(() => {
  const seen = new Set<string>()
  return accessPaths.value
    .filter(path => {
      if (seen.has(path.destinationRelationId)) {
        return false
      }
      seen.add(path.destinationRelationId)
      return true
    })
    .map(path => {
      const resource = resources.value.find(item => item.id === path.destinationRelationId)
      const label = resource ? `${resource.displayName || resource.resourceKey}${resource.physicalName ? ` · ${resource.physicalName}` : ''}` : path.displayName || path.destinationRelationId
      return { label, value: path.destinationRelationId }
    })
})
const relatedFieldOptions = computed(() => {
  const destinationRelationId = draft.value.relatedResourceId
  const fields = destinationRelationId ? resourceFieldCache.value[destinationRelationId] || [] : []
  return fields.filter(item => item.filterableFlag !== false).map(item => ({ label: `${item.displayName || item.fieldKey} · ${item.fieldKey}`, value: item.fieldKey }))
})
const anchorFieldOptions = computed(() => draft.value.anchorMode === 'ACCESS_PATH' ? relatedFieldOptions.value : fieldOptions.value)
const accessGrantRows = computed<AccessGrantRow[]>(() => accessGrants.value.map(grant => ({
  ...grant,
  principalTypeLabel: principalTypeLabel(grant.principalId),
  principalLabel: principalLabel(grant.principalId),
  resourceLabel: resourceLabel(grant.policyId),
  policyLabel: policyLabel(grant.policyId),
  ruleTemplateLabel: ruleTemplateLabel(grant.policyId),
})))
const filteredAccessGrantRows = computed<AccessGrantRow[]>(() => {
  // 未选授权目标时不显示数据权限规则
  if (!viewPrincipalId.value) { return [] }
  // 按当前查看的授权目标过滤：右上角选中某主体后，列表只显示该主体的数据权限规则
  let rows = accessGrantRows.value
  rows = rows.filter(row => row.principalId === viewPrincipalId.value)
  const keyword = grantKeyword.value.trim().toLowerCase()
  if (keyword) {
    rows = rows.filter(row => `${row.grantKey} ${row.principalLabel} ${row.policyLabel} ${row.lifecycleState || ''}`.toLowerCase().includes(keyword))
  }
  return rows
})

const resourceViewColumns = [
  { title: '授权目标类型', dataIndex: 'principalTypeLabel', key: 'principalTypeLabel', width: 100 },
  { title: '授权目标', dataIndex: 'principalLabel', key: 'principalLabel', width: 140, ellipsis: true },
  { title: '数据规则名称', dataIndex: 'policyLabel', key: 'policyLabel', width: 140, ellipsis: true },
  { title: '数据规则模板', dataIndex: 'ruleTemplateLabel', key: 'ruleTemplateLabel', width: 120 },
  { title: '状态', dataIndex: 'lifecycleState', key: 'lifecycleState', width: 80 },
  { title: '操作', key: 'action', width: 140 },
]
const resourceCardsWithGrantCount = computed(() => resources.value.map(resource => {
  const policyIds = Object.values(policiesById.value).filter(p => p.targetRelationId === resource.id).map(p => p.id)
  const grantCount = accessGrants.value.filter(g => policyIds.includes(g.policyId)).length
  return {
    ...resource,
    displayName: resource.displayName || resource.resourceKey,
    fieldCount: resourceFieldCache.value[resource.id]?.length ?? 0,
    grantCount,
  }
}))
const resourceViewRows = computed<AccessGrantRow[]>(() => {
  if (!selectedTableId.value) {
    return accessGrantRows.value
  }
  const policyIds = Object.values(policiesById.value).filter(p => p.targetRelationId === selectedTableId.value).map(p => p.id)
  return accessGrantRows.value.filter(row => policyIds.includes(row.policyId))
})
const filteredResourceViewRows = computed<AccessGrantRow[]>(() => {
  const keyword = resourceViewKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return resourceViewRows.value
  }
  return resourceViewRows.value.filter(row => `${row.grantKey} ${row.principalLabel} ${row.policyLabel} ${row.lifecycleState || ''}`.toLowerCase().includes(keyword))
})

watch(() => draft.value.principalType, () => {
  if (!principalOptions.value.some(option => draft.value.principalCodes.includes(option.value))) {
    draft.value.principalCodes = []
  }
})

async function loadDataPermissionResources(): Promise<void> {
  try {
    const response = await postJson<ApiResponse<DataResource[]>>('/center-api/authzcraft/api/v1/catalog/relation-resources/search', {
      tenantKey: 'platform',
      appKey: 'authzcraft-demo-oa',
    })
    resources.value = response.data ?? []
    if (!draft.value.resourceIds.length && resources.value.length) {
      draft.value.resourceIds = [resources.value[0].id]
      await loadResourceFields(resources.value[0].id)
    }
    await Promise.all(resources.value.filter(item => !draft.value.resourceIds.includes(item.id)).map(item => loadResourceFieldsForCache(item.id)))
  } catch {
    resources.value = []
    resourceFields.value = []
    accessPaths.value = []
  }
}

async function onResourceChange(resourceIds: string[]): Promise<void> {
  if (resourceIds.length) {
    await loadResourceFields(resourceIds[resourceIds.length - 1])
  }
}

async function loadResourceFields(resourceId: string): Promise<void> {
  draft.value.anchorMode = 'DIRECT'
  draft.value.relatedResourceId = ''
  draft.value.accessPathKey = ''
  draft.value.anchorField = ''
  try {
    resourceFields.value = await loadResourceFieldsForCache(resourceId)
    const firstFilterable = resourceFields.value.find(item => item.filterableFlag !== false)
    draft.value.anchorField = firstFilterable?.fieldKey ?? ''
    await loadAccessPaths(resourceId)
  } catch {
    resourceFields.value = []
    accessPaths.value = []
  }
}

async function loadResourceFieldsForCache(resourceId: string): Promise<DataResourceField[]> {
  if (resourceFieldCache.value[resourceId]) {
    return resourceFieldCache.value[resourceId]
  }
  const response = await postJson<ApiResponse<DataResourceField[]>>(`/center-api/authzcraft/api/v1/catalog/relation-resources/${resourceId}/fields/search`, {})
  const fields = response.data ?? []
  resourceFieldCache.value[resourceId] = fields
  const resource = resources.value.find(item => item.id === resourceId)
  if (resource) {
    emit('fieldsLoaded', resource.resourceKey, Object.fromEntries(fields.map(field => [field.fieldKey, field.displayName])))
  }
  return fields
}

async function loadAccessPaths(resourceId: string): Promise<void> {
  const response = await postJson<ApiResponse<DataAccessPath[]>>('/center-api/authzcraft/api/v1/catalog/access-paths/search', {
    tenantKey: 'platform',
    appKey: 'authzcraft-demo-oa',
    rootRelationId: resourceId,
    lifecycleState: 'ACTIVE',
  })
  accessPaths.value = response.data ?? []
}

async function onAnchorModeChange(): Promise<void> {
  draft.value.anchorField = ''
  if (draft.value.anchorMode === 'ACCESS_PATH') {
    draft.value.relatedResourceId = accessPaths.value[0]?.destinationRelationId ?? ''
    await loadRelatedResourceFields(draft.value.relatedResourceId)
    return
  }
  draft.value.relatedResourceId = ''
  draft.value.accessPathKey = ''
  draft.value.anchorField = resourceFields.value.find(item => item.filterableFlag !== false)?.fieldKey ?? ''
}

async function loadRelatedResourceFields(relatedResourceId: string): Promise<void> {
  draft.value.relatedResourceId = relatedResourceId
  draft.value.anchorField = ''
  const path = accessPaths.value.find(item => item.destinationRelationId === relatedResourceId)
  draft.value.accessPathKey = path?.pathKey ?? ''
  if (!path) {
    return
  }
  const fields = await loadResourceFieldsForCache(relatedResourceId)
  draft.value.anchorField = fields.find(item => item.filterableFlag !== false)?.fieldKey ?? ''
}

function checkRuleConflicts(): string {
  const isAllData = draft.value.ruleTemplate === 'ALL_DATA'
  const warnings: string[] = []
  for (const resourceId of draft.value.resourceIds) {
    const resource = resources.value.find(item => item.id === resourceId)
    const resourceLabel = resource?.displayName || resource?.resourceKey || resourceId
    for (const principalCode of draft.value.principalCodes) {
      const principal = Object.values(principalsById.value).find(p => p.principalKey === principalCode)
      if (!principal) continue
      for (const operationCode of draft.value.operationCodes) {
        const existingGrants = accessGrantRows.value.filter(row => {
          if (row.principalId !== principal.id) return false
          if (row.lifecycleState === 'REVOKED') return false
          const policy = policiesById.value[row.policyId]
          if (!policy) return false
          if (policy.targetRelationId !== resourceId) return false
          const policyOp = policy.operationCode || 'READ'
          if (policyOp !== operationCode) return false
          return true
        })
        if (existingGrants.length === 0) continue
        const existingTemplates = existingGrants.map(row => {
          const policy = policiesById.value[row.policyId]
          return ruleTemplateLabel(row.policyId) || policy?.ruleTemplate || '未知'
        })
        if (isAllData && existingTemplates.some(t => t !== '全部数据')) {
          warnings.push(`${resourceLabel}（${principalCode}）已配置了其它数据范围规则（${[...new Set(existingTemplates)].join('、')}），"全部数据"规则会使其失效。`)
        } else if (!isAllData && existingTemplates.includes('全部数据')) {
          warnings.push(`${resourceLabel}（${principalCode}）已配置了"全部数据"规则，新规则会被其覆盖而失效。`)
        }
      }
    }
  }
  return warnings.length ? warnings.join('\n') : ''
}

async function saveTemplate(): Promise<void> {
  if (!draft.value.principalCodes.length) {
    resultMessage.value = '请选择授权目标。'
    return
  }
  if (!draft.value.resourceIds.length) {
    resultMessage.value = '请选择数据表。'
    return
  }
  if (draft.value.ruleTemplate !== 'ALL_DATA' && !draft.value.anchorField) {
    resultMessage.value = '请选择数据归属字段。'
    return
  }
  if (draft.value.anchorMode === 'ACCESS_PATH' && !draft.value.relatedResourceId) {
    resultMessage.value = '请选择关联表。'
    return
  }
  if (draft.value.anchorMode === 'ACCESS_PATH' && !draft.value.accessPathKey) {
    resultMessage.value = '当前数据表到该关联表的访问路径不可用。'
    return
  }
  if (draft.value.ruleTemplate === 'SPECIFIED_DEPARTMENTS' && draft.value.departmentCodes.length === 0) {
    resultMessage.value = '请至少指定一个部门。'
    return
  }    if (!draft.value.operationCodes.length) {
      resultMessage.value = '请选择数据操作。'
      return
    }  if (draft.value.ruleTemplate === 'PERSON_SHARE' && !draft.value.fromUserKeys.length) {
    resultMessage.value = '请选择数据归属人。'
    return
  }
  // Check for rule conflicts: ALL_DATA vs other templates
  const conflictWarnings = checkRuleConflicts()
  if (conflictWarnings) {
    const confirmed = await new Promise<boolean>(resolve => {
      Modal.confirm({
        title: '数据规则冲突提示',
        content: conflictWarnings,
        okText: '继续保存',
        cancelText: '取消',
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      })
    })
    if (!confirmed) return
  }
  loading.value = true
  try {
    const results: string[] = []
    for (const resourceId of draft.value.resourceIds) {
      const res = resources.value.find(item => item.id === resourceId)
      if (!res) continue
      for (const principalCode of draft.value.principalCodes) {
      for (const operationCode of draft.value.operationCodes) {
      const editingGrantId = drawerMode.value === 'edit' ? filteredAccessGrantRows.value.find(r => r.principalId === Object.entries(principalsById.value).find(([, p]) => p.principalKey === principalCode)?.[0])?.id ?? '' : ''
      const response = await postJsonWithAuth<ApiResponse<SaveResult>>('/demo-api/authzcraft-demo-oa/api/v1/rbac/data-permissions/templates/save', {
        principalType: draft.value.principalType,
        principalCode,
        resourceId,
        resourceKey: res.resourceKey,
        operationCode,
        ruleTemplate: draft.value.ruleTemplate,
        accessPathKey: draft.value.anchorMode === 'ACCESS_PATH' ? draft.value.accessPathKey : '',
        anchorField: draft.value.ruleTemplate === 'ALL_DATA' ? 'id' : draft.value.anchorField,
        departmentScopes: draft.value.departmentScopes,
        subDepartmentDepth: draft.value.departmentScopes.includes('SUB_MANAGED') ? draft.value.subDepartmentDepth : '',
        departmentCodes: draft.value.departmentCodes,
        fromUserKey: draft.value.fromUserKeys.join(','),
        allowReshare: draft.value.allowReshare,
        validUntil: draft.value.validUntil,
        simulationRequesterKey: draft.value.simulationRequesterKey || props.personaKey,
      }, props.personaKey)
      const result = response.data
      // If editing and the new grantId differs from the old one, revoke the old grant
      if (drawerMode.value === 'edit' && editingGrantId && result?.grantId && editingGrantId !== result.grantId) {
        await postJson<ApiResponse<AccessGrant>>(`/center-api/authzcraft/api/v1/grants/access-grants/${editingGrantId}/revoke`, {})
      }
      const simulationText = result?.simulation?.assertionPassed === false ? '测试未通过' : '测试已通过'
      results.push(result ? `${result.summary}；${simulationText}` : `${principalCode} 保存完成`)
      }
      }
    }
    resultMessage.value = results.join('；')
    drawerOpen.value = false
    await loadAccessGrants()
  } finally {
    loading.value = false
  }
}

async function loadAccessGrants(): Promise<void> {
  grantLoading.value = true
  try {
    const [grantResponse, roleResponse, groupResponse, userResponse, orgResponse, positionResponse, policyResponse] = await Promise.all([
      postJson<ApiResponse<AccessGrant[]>>('/center-api/authzcraft/api/v1/grants/access-grants/search', {
        tenantKey: 'platform',
        appKey: 'authzcraft-demo-oa',
      }),
      postJson<ApiResponse<PrincipalInfo[]>>('/center-api/authzcraft/api/v1/principals/search', {
        tenantKey: 'platform', principalKind: 'ROLE',
      }),
      postJson<ApiResponse<PrincipalInfo[]>>('/center-api/authzcraft/api/v1/principals/search', {
        tenantKey: 'platform', principalKind: 'GROUP',
      }),
      postJson<ApiResponse<PrincipalInfo[]>>('/center-api/authzcraft/api/v1/principals/users/search', {
        tenantKey: 'platform',
      }),
      postJson<ApiResponse<PrincipalInfo[]>>('/center-api/authzcraft/api/v1/principals/search', {
        tenantKey: 'platform', principalKind: 'ORGANIZATION',
      }),
      postJson<ApiResponse<PrincipalInfo[]>>('/center-api/authzcraft/api/v1/principals/positions/search', {
        tenantKey: 'platform',
      }),
      postJson<ApiResponse<AccessPolicyInfo[]>>('/center-api/authzcraft/api/v1/policies/access-policies/search', {
        tenantKey: 'platform',
        appKey: 'authzcraft-demo-oa',
      }),
    ])
    accessGrants.value = grantResponse.data ?? []
    const allPrincipals = [
      ...(roleResponse.data ?? []),
      ...(groupResponse.data ?? []),
      ...(userResponse.data ?? []),
      ...(orgResponse.data ?? []),
      ...(positionResponse.data ?? []),
    ]
    principalsById.value = Object.fromEntries(allPrincipals.map(item => [item.id, item]))
    policiesById.value = Object.fromEntries((policyResponse.data ?? []).map(item => [item.id, item]))
    emitGrantCount()
  } finally {
    grantLoading.value = false
  }
}

async function updateGrantLifecycle(grant: AccessGrant, action: GrantLifecycleAction): Promise<void> {
  if (!grant.id) {
    return
  }
  await postJson<ApiResponse<AccessGrant>>(`/center-api/authzcraft/api/v1/grants/access-grants/${grant.id}/${action}`, {})
  await loadAccessGrants()
}

function grantStateColor(state: string | undefined): string {
  if (state === 'ACTIVE') {
    return 'success'
  }
  if (state === 'SUSPENDED') {
    return 'warning'
  }
  if (state === 'REVOKED') {
    return 'default'
  }
  return 'processing'
}

function grantStateLabel(state: string | undefined): string {
  if (state === 'ACTIVE') {
    return '生效'
  }
  if (state === 'SUSPENDED') {
    return '已禁用'
  }
  if (state === 'REVOKED') {
    return '已作废'
  }
  return state || '-'
}

function principalTypeLabel(principalId: string): string {
  const principal = principalsById.value[principalId]
  if (!principal) {
    return '-'
  }
  if (principal.principalKind === 'GROUP') {
    return '用户组'
  }
  if (principal.principalKind === 'ROLE') {
    return '角色'
  }
  if (principal.principalKind === 'DEPARTMENT') {
    return '部门'
  }
  if (principal.principalKind === 'POSITION') {
    return '岗位'
  }
  return '人员'
}

function principalLabel(principalId: string): string {
  const principal = principalsById.value[principalId]
  if (!principal) {
    return principalId
  }
  return principal.displayName || principal.principalKey
}

function resourceLabel(policyId: string): string {
  const policy = policiesById.value[policyId]
  if (!policy) {
    return '-'
  }
  // Prefer targetRelationId to look up resource
  if (policy.targetRelationId) {
    const resource = resources.value.find(item => item.id === policy.targetRelationId)
    if (resource) {
      return resource.displayName || resource.resourceKey
    }
  }
  // Fallback: parse from displayName (format: "模板 - 表名 - 字段")
  if (policy.displayName) {
    const parts = policy.displayName.split(' - ')
    if (parts.length >= 2) {
      return parts[1]
    }
  }
  return '-'
}

function ruleTemplateLabel(policyId: string): string {
  const policy = policiesById.value[policyId]
  if (!policy) {
    return '-'
  }
  // 数据规则模板取自 authzcraft_rule_blueprint.display_name（后端通过 ACTIVE 修订关联返回 ruleBlueprintDisplayName）
  if (policy.ruleBlueprintDisplayName) {
    return policy.ruleBlueprintDisplayName
  }
  // Fallback: 从策略 displayName 解析（格式: "模板 - 表名 - 字段"）
  if (policy.displayName) {
    const parts = policy.displayName.split(' - ')
    if (parts.length >= 1) {
      return parts[0]
    }
  }
  return '-'
}

function policyLabel(policyId: string): string {
  const policy = policiesById.value[policyId]
  if (!policy) {
    return policyId
  }
  return policy.displayName || policy.policyKey
}

defineExpose({
  loadDataPermissionResources,
  selectPrincipal,
  clearPrincipal,
  grantCountForPrincipal,
})

function grantCountForPrincipal(principalCode: string): number {
  const principal = Object.values(principalsById.value).find(p => p.principalKey === principalCode)
  if (!principal) { return 0 }
  return accessGrants.value.filter(g => g.principalId === principal.id && g.lifecycleState !== 'REVOKED').length
}

function emitGrantCount(): void {
  const code = draft.value.principalCodes[0] ?? ''
  emit('grantCountChanged', code ? grantCountForPrincipal(code) : 0)
}

function selectPrincipal(principalType: PrincipalType, principalCode: string): void {
  drawerViewMode.value = 'PRINCIPAL'
  draft.value.principalType = principalType
  draft.value.principalCodes = [principalCode]
  // 按选中的授权目标过滤列表：找到该主体的 principalId，仅显示其数据权限规则
  const principal = Object.values(principalsById.value).find(p => p.principalKey === principalCode)
  viewPrincipalId.value = principal?.id ?? ''
  emitGrantCount()
}

// 清除当前查看的授权目标：未选授权目标时调用，列表不再按任何主体过滤（显示空）
function clearPrincipal(): void {
  viewPrincipalId.value = ''
  draft.value.principalCodes = []
  emitGrantCount()
}

// principalsById 异步加载完成后，若已选中授权目标但尚未解析出 principalId（时序竞态），补一次解析
watch(principalsById, () => {
  emitGrantCount()
  if (!viewPrincipalId.value && draft.value.principalCodes.length) {
    const principal = Object.values(principalsById.value).find(p => p.principalKey === draft.value.principalCodes[0])
    viewPrincipalId.value = principal?.id ?? ''
  }
}, { deep: true })

onMounted(() => {
  void loadDataPermissionResources()
  void loadAccessGrants()
  void loadBlueprintSchemas()
})

async function loadBlueprintSchemas(): Promise<void> {
  try {
    const response = await postJson<ApiResponse<RuleBlueprintInfo[]>>('/center-api/authzcraft/api/v1/policies/rule-blueprints/search', {
      tenantKey: 'platform',
      lifecycleState: 'PUBLISHED',
    })
    const blueprints = response.data ?? []
    const schemas: Record<string, BlueprintInputSchema> = {}
    for (const bp of blueprints) {
      if (!bp.inputSchema || bp.inputSchema === '{}') continue
      try {
        const parsed = JSON.parse(bp.inputSchema) as BlueprintInputSchema
        if (parsed.fieldInputs || parsed.argumentInputs || parsed.attributeInputs) {
          schemas[bp.blueprintKey] = parsed
        }
      } catch {
        // skip invalid JSON
      }
    }
    blueprintSchemas.value = schemas
  } catch {
    // PAP may not be online yet; fallback to hardcoded behavior
  }
}
</script>

<style scoped>
.data-permission-view-alert {
  margin-bottom: 12px;
}

.data-resource-list {
  overflow: hidden;
}

.data-resource-list-item {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.data-resource-item-content {
  flex: 1;
  min-width: 0;
}

.data-resource-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.data-resource-item-name {
  font-weight: 500;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-resource-item-key {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-resource-list-item.active,
.data-resource-list-item:hover {
  background: #f2faf6;
}

.data-permission-form {
  display: flex;
  flex-direction: column;
}

/* 按数据表创建：数据表 → 可见范围 → 归属字段 → 规则参数 → 数据操作 → 失效时间 → 授权目标 */
.data-permission-form.resource-first .dp-field-resource { order: 1; }
.data-permission-form.resource-first .dp-field-template { order: 2; }
.data-permission-form.resource-first .dp-field-anchor-mode { order: 3; }
.data-permission-form.resource-first .dp-field-related-resource { order: 4; }
.data-permission-form.resource-first .dp-field-anchor-field { order: 5; }
.data-permission-form.resource-first .dp-field-rule-params { order: 6; }
.data-permission-form.resource-first .dp-field-operation { order: 7; }
.data-permission-form.resource-first .dp-field-valid-until { order: 8; }
.data-permission-form.resource-first .dp-field-principal-type { order: 9; }
.data-permission-form.resource-first .dp-field-principal { order: 10; }

/* 按授权目标创建（默认）：授权目标 → 数据表 → 可见范围 → 归属字段 → 规则参数 → 数据操作 → 失效时间 */
.data-permission-form.principal-first .dp-field-principal-type { order: 1; }
.data-permission-form.principal-first .dp-field-principal { order: 2; }
.data-permission-form.principal-first .dp-field-resource { order: 3; }
.data-permission-form.principal-first .dp-field-template { order: 4; }
.data-permission-form.principal-first .dp-field-anchor-mode { order: 5; }
.data-permission-form.principal-first .dp-field-related-resource { order: 6; }
.data-permission-form.principal-first .dp-field-anchor-field { order: 7; }
.data-permission-form.principal-first .dp-field-rule-params { order: 8; }
.data-permission-form.principal-first .dp-field-operation { order: 9; }
.data-permission-form.principal-first .dp-field-valid-until { order: 10; }

.list-view-switch {
  margin-bottom: 12px;
}

.resource-view-layout {
  display: flex;
  gap: 12px;
}

.resource-view-sidebar {
  flex: 0 0 240px;
}

.resource-view-main {
  flex: 1;
  min-width: 0;
  overflow: auto;
}

.drawer-view-switch {
  margin-bottom: 16px;
}

.rbac-panel-embedded {
  max-height: none;
  overflow: visible;
  border: none;
  box-shadow: none;
}

.rbac-panel-embedded :deep(.ant-card-body) {
  padding: 0;
}

.grant-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.drawer-footer {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  padding: 10px 16px;
  text-align: right;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
</style>
