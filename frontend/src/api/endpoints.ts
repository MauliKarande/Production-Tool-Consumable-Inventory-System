import { apiClient } from './client'
import type {
  AssignmentResponse,
  AttributeDef,
  CategoryConsumption,
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
  currentStock: (itemId: number) => apiClient.get<CurrentStockResponse>(`/api/inventory/current-stock/${itemId}`),
}

export const AccountabilityApi = {
  search: (params: { employeeId?: number; machineId?: number; itemId?: number; openOnly?: boolean; page?: number; size?: number }) =>
    apiClient.get<Page<AssignmentResponse>>('/api/accountability', { params }),
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
