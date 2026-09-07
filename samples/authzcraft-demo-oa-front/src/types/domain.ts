export type BusinessRow = Record<string, unknown>

export type AuthzState = {
  userKey: string
  displayName: string
  roles: string[]
  permissions: string[]
  fieldPermissions: Array<{ code: string; resourceKey: string; fieldKey: string; accessLevel: string; name: string }>
  requestKey: string
}

export type RbacRole = { id: string; code: string; name: string; description?: string }
export type RbacGroup = { id: string; code: string; name: string; description?: string }
export type RbacPermission = { id: string; code: string; name: string; description?: string }
export type RbacFieldPermission = { id: string; code: string; resourceKey: string; fieldKey: string; accessLevel: string; securityRequirement: string; name: string; description?: string }
export type RbacUser = { id: string; userKey: string; displayName?: string; staffNo?: string; roles?: string[]; permissions?: string[] }
export type RbacUserMembership = { containerKind: string; containerCode: string; userKey: string; membershipId?: string; lifecycleState?: string }
export type BatchFailure = { itemKey: string; reason: string }
export type AssignmentTargetType = 'ROLE' | 'GROUP'
export type MenuKey = 'meeting-analytics' | 'expense-applications' | 'leave-applications' | 'rbac'
export type RbacSubMenuKey = 'roles' | 'groups' | 'memberships' | 'permissions' | 'data-permissions' | 'authorization'

export type BusinessPageConfig = {
  key: MenuKey
  title: string
  description: string
  permissionLabel: string
  requiredPermissions: string[]
  scenarioKeys: string[]
  dataScopeText: string
  masterDetail?: { childScenarioKey: string; joinField: string; childJoinField?: string }
}

export type Scenario = {
  key: string
  title: string
  resourceKey: string
  endpoint: string
  countEndpoint: string
  columns: string[]
  expectedAccessPathKeys: string[]
  accessPathText: string
  requiredPermission?: string
  formFields?: { field: string; label: string; type?: 'text' | 'select' | 'number'; options?: { label: string; value: string }[] }[]
}

export type ComparisonCheck = {
  name: string
  expected: string
  actual: string
  passed: boolean
}

export type RunResult = {
  scenarioKey: string
  scenarioTitle: string
  personaKey: string
  expectedDecision: string
  expectedAccessPathKeys: string[]
  businessStatus: number
  businessCode: string
  businessRows: BusinessRow[]
  totalCount: number
  pepDecision: string | null
  decisionKey: string | null
  requiredAccessPathKeys: string[]
  simulationDecision: string | null
  simulationPassed: boolean
  simulationRecordedProduction: boolean | null
  auditDecision: string | null
  auditFound: boolean
  checks: ComparisonCheck[]
  errorMessage: string | null
}

export type SelectOption = { label?: string; value: string; isDefault?: boolean }
