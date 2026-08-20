import { apiClient } from './client'
import type {
  AlertResponse,
  AppUser,
  AssignmentResponse,
  AttributeDef,
  CategoryConsumption,
  DashboardSummary,
  DeadStockRow,
  ImportResult,
  LowStockRow,
  PurchasePipelineRow,
  PurchaseRequisition,
  StockValuationRow,
  SupplierPriceRow,
  SupplierSpendRow,
  CurrentStockResponse,
  Department,
  Employee,
  Item,
  ItemCategory,
  ItemConsumption,
  Machine,
  MachineConsumption,
  MachineConsumptionDetail,
  Manufacturer,
  Page,
  StockSummaryResponse,
  Supplier,
  TransactionResponse,
  UnitOfMeasure,
} from './types'

export const AuthApi = {
  login: (username: string, password: string) =>
    apiClient.post<{ token: string; username: string; role: string; userId: number }>('/api/auth/login', {
      username,
      password,
    }),
  changeOwnPassword: (currentPassword: string, newPassword: string) =>
    apiClient.post('/api/users/me/change-password', { currentPassword, newPassword }),
}

export const DepartmentsApi = {
  list: () => apiClient.get<Department[]>('/api/departments'),
  create: (data: { name: string; active: boolean }) => apiClient.post<Department>('/api/departments', data),
  update: (id: number, data: { name: string; active: boolean }) =>
    apiClient.put<Department>(`/api/departments/${id}`, data),
}

export const UomApi = {
  list: () => apiClient.get<UnitOfMeasure[]>('/api/units-of-measure'),
  create: (data: { code: string; name: string; active: boolean }) =>
    apiClient.post<UnitOfMeasure>('/api/units-of-measure', data),
  update: (id: number, data: { code: string; name: string; active: boolean }) =>
    apiClient.put<UnitOfMeasure>(`/api/units-of-measure/${id}`, data),
}

export const ManufacturersApi = {
  list: () => apiClient.get<Manufacturer[]>('/api/manufacturers'),
  create: (data: { name: string; active: boolean }) => apiClient.post<Manufacturer>('/api/manufacturers', data),
  update: (id: number, data: { name: string; active: boolean }) =>
    apiClient.put<Manufacturer>(`/api/manufacturers/${id}`, data),
}

export const SuppliersApi = {
  list: (page = 0, size = 100) =>
    apiClient.get<Page<Supplier>>('/api/suppliers', { params: { page, size } }),
  create: (data: Partial<Supplier> & { name: string; active: boolean }) =>
    apiClient.post<Supplier>('/api/suppliers', data),
  update: (id: number, data: Partial<Supplier> & { name: string; active: boolean }) =>
    apiClient.put<Supplier>(`/api/suppliers/${id}`, data),
}

export const EmployeesApi = {
  list: (page = 0, size = 100) =>
    apiClient.get<Page<Employee>>('/api/employees', { params: { page, size } }),
  create: (data: {
    employeeCode: string
    name: string
    departmentId: number | null
    designation?: string
    contact?: string
    active: boolean
  }) => apiClient.post<Employee>('/api/employees', data),
  update: (
    id: number,
    data: {
      employeeCode: string
      name: string
      departmentId: number | null
      designation?: string
      contact?: string
      active: boolean
    },
  ) => apiClient.put<Employee>(`/api/employees/${id}`, data),
}

export const MachinesApi = {
  list: (page = 0, size = 100) => apiClient.get<Page<Machine>>('/api/machines', { params: { page, size } }),
  create: (data: {
    machineCode: string
    machineName: string
    machineType?: string
    departmentId?: number | null
    location?: string
    manufacturer?: string
    model?: string
    status?: string
    active: boolean
  }) => apiClient.post<Machine>('/api/machines', data),
  update: (
    id: number,
    data: {
      machineCode: string
      machineName: string
      machineType?: string
      departmentId?: number | null
      location?: string
      manufacturer?: string
      model?: string
      status?: string
      active: boolean
    },
  ) => apiClient.put<Machine>(`/api/machines/${id}`, data),
}

export const CategoriesApi = {
  list: () => apiClient.get<ItemCategory[]>('/api/item-categories'),
  create: (data: { name: string; parentCategoryId: number | null; active: boolean }) =>
    apiClient.post<ItemCategory>('/api/item-categories', data),
  addAttribute: (categoryId: number, data: Omit<AttributeDef, 'id'>) =>
    apiClient.post<AttributeDef>(`/api/item-categories/${categoryId}/attributes`, data),
  removeAttribute: (categoryId: number, attributeId: number) =>
    apiClient.delete(`/api/item-categories/${categoryId}/attributes/${attributeId}`),
}

export const ItemsApi = {
  search: (params: { q?: string; categoryId?: number; active?: boolean; page?: number; size?: number }) =>
    apiClient.get<Page<Item>>('/api/items', { params }),
  get: (id: number) => apiClient.get<Item>(`/api/items/${id}`),
  create: (data: Record<string, unknown>) => apiClient.post<Item>('/api/items', data),
  update: (id: number, data: Record<string, unknown>) => apiClient.put<Item>(`/api/items/${id}`, data),
  transactions: (id: number, page = 0, size = 20) =>
    apiClient.get<Page<TransactionResponse>>(`/api/items/${id}/transactions`, { params: { page, size } }),
  stockSummary: (id: number) => apiClient.get<StockSummaryResponse>(`/api/items/${id}/stock-summary`),
  currentStock: (id: number) => apiClient.get<CurrentStockResponse>(`/api/items/${id}/current-stock`),
}

export const InventoryApi = {
  openingBalance: (data: { itemId: number; quantity: number; unitCost: number; remark?: string }) =>
    apiClient.post('/api/inventory/opening-balance', data),
  issue: (data: {
    itemId: number
    quantity: number
    employeeId: number
    machineId?: number | null
    purpose?: string
    remark?: string
  }) => apiClient.post('/api/inventory/issue', data),
  returnStock: (data: { assignmentId: number; quantity: number; condition: string; remark?: string }) =>
    apiClient.post('/api/inventory/return', data),
  inward: (data: { itemId: number; quantity: number; unitCost: number; remark?: string }) =>
    apiClient.post('/api/inventory/inward', data),
  adjustment: (data: { itemId: number; direction: 'IN' | 'OUT'; quantity: number; reason: string }) =>
    apiClient.post('/api/inventory/adjustment', data),
  damageScrap: (data: { itemId: number; type: 'DAMAGE' | 'SCRAP'; quantity: number; reason: string }) =>
    apiClient.post('/api/inventory/damage-scrap', data),
  reversal: (data: { transactionId: number; reason: string }) => apiClient.post('/api/inventory/reversal', data),
  currentStock: (itemId: number) => apiClient.get<CurrentStockResponse>(`/api/inventory/current-stock/${itemId}`),
}

export const AccountabilityApi = {
  search: (params: { employeeId?: number; machineId?: number; itemId?: number; openOnly?: boolean; page?: number; size?: number }) =>
    apiClient.get<Page<AssignmentResponse>>('/api/accountability', { params }),
}

export const PurchaseRequisitionsApi = {
  search: (params: { status?: string; priority?: string; departmentId?: number; page?: number; size?: number }) =>
    apiClient.get<Page<PurchaseRequisition>>('/api/purchase-requisitions', { params }),
  get: (id: number) => apiClient.get<PurchaseRequisition>(`/api/purchase-requisitions/${id}`),
  create: (data: {
    departmentId?: number | null
    priority?: string
    reason?: string
    items: { itemId: number; quantity: number; estimatedPrice: number; supplierId?: number | null }[]
  }) => apiClient.post<PurchaseRequisition>('/api/purchase-requisitions', data),
  submit: (id: number) => apiClient.post<PurchaseRequisition>(`/api/purchase-requisitions/${id}/submit`),
  approve: (id: number) => apiClient.post<PurchaseRequisition>(`/api/purchase-requisitions/${id}/approve`),
  reject: (id: number, reason: string) =>
    apiClient.post<PurchaseRequisition>(`/api/purchase-requisitions/${id}/reject`, { reason }),
  markOrdered: (id: number) => apiClient.post<PurchaseRequisition>(`/api/purchase-requisitions/${id}/mark-ordered`),
  receive: (
    id: number,
    lines: { prItemId: number; receivedQty: number; unitCost: number; directToFloor: boolean; machineId?: number | null }[],
  ) => apiClient.post<PurchaseRequisition>(`/api/purchase-requisitions/${id}/receive`, { lines }),
  close: (id: number) => apiClient.post<PurchaseRequisition>(`/api/purchase-requisitions/${id}/close`),
}

export const ImportApi = {
  preview: (file: File, fileType: string) => {
    const form = new FormData()
    form.append('file', file)
    return apiClient.post<ImportResult>('/api/import/preview', form, { params: { fileType } })
  },
  commit: (file: File, fileType: string) => {
    const form = new FormData()
    form.append('file', file)
    return apiClient.post<ImportResult>('/api/import/commit', form, { params: { fileType } })
  },
}

export const ReportsApi = {
  dashboardSummary: () => apiClient.get<DashboardSummary>('/api/reports/dashboard-summary'),
  stockValuation: () => apiClient.get<StockValuationRow[]>('/api/reports/stock-valuation'),
  lowStock: () => apiClient.get<LowStockRow[]>('/api/reports/low-stock'),
  deadStock: (months = 3) => apiClient.get<DeadStockRow[]>('/api/reports/dead-stock', { params: { months } }),
  supplierPriceComparison: () => apiClient.get<SupplierPriceRow[]>('/api/reports/supplier-price-comparison'),
  supplierSpend: () => apiClient.get<SupplierSpendRow[]>('/api/reports/supplier-spend'),
  purchasePipeline: () => apiClient.get<PurchasePipelineRow[]>('/api/reports/purchase-pipeline'),
  download: (report: string, format: 'xlsx' | 'pdf', params: Record<string, unknown> = {}) =>
    apiClient.get<Blob>(`/api/reports/${report}`, { params: { ...params, format }, responseType: 'blob' }),
}

export const UsersApi = {
  list: (page = 0, size = 20) => apiClient.get<Page<AppUser>>('/api/users', { params: { page, size } }),
  get: (id: number) => apiClient.get<AppUser>(`/api/users/${id}`),
  create: (data: { username: string; password: string; employeeId?: number | null; roleName: string }) =>
    apiClient.post<AppUser>('/api/users', data),
  update: (id: number, data: { employeeId?: number | null; roleName: string; active: boolean }) =>
    apiClient.put<AppUser>(`/api/users/${id}`, data),
  resetPassword: (id: number, newPassword: string) => apiClient.post(`/api/users/${id}/reset-password`, { newPassword }),
}

export const AlertsApi = {
  list: (params: { status?: string; type?: string; page?: number; size?: number }) =>
    apiClient.get<Page<AlertResponse>>('/api/alerts', { params }),
  openCount: () => apiClient.get<number>('/api/alerts/open-count'),
  acknowledge: (id: number) => apiClient.post<AlertResponse>(`/api/alerts/${id}/acknowledge`),
  resolve: (id: number) => apiClient.post<AlertResponse>(`/api/alerts/${id}/resolve`),
  recompute: () => apiClient.post<{ raised: number; resolved: number }>('/api/alerts/recompute'),
}

export const ConsumptionApi = {
  machineDetail: (machineId: number, from: string, to: string) =>
    apiClient.get<MachineConsumptionDetail>(`/api/consumption/machine/${machineId}`, { params: { from, to } }),
  allMachines: (from: string, to: string) =>
    apiClient.get<MachineConsumption[]>('/api/consumption/machines', { params: { from, to } }),
  byCategory: (from: string, to: string, machineId?: number) =>
    apiClient.get<CategoryConsumption[]>('/api/consumption/category', { params: { from, to, machineId } }),
  topItems: (from: string, to: string, by: 'value' | 'quantity', limit = 10) =>
    apiClient.get<ItemConsumption[]>('/api/consumption/top-items', { params: { from, to, by, limit } }),
}
