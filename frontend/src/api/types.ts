export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export type Role = 'ADMIN' | 'ISSUER' | 'VIEWER'

export interface Department {
  id: number
  name: string
  active: boolean
}

export interface Employee {
  id: number
  employeeCode: string
  name: string
  departmentId: number | null
  departmentName: string | null
  designation: string | null
  contact: string | null
  active: boolean
}

export interface UnitOfMeasure {
  id: number
  code: string
  name: string
  active: boolean
}

export interface Manufacturer {
  id: number
  name: string
  active: boolean
}

export interface Supplier {
  id: number
  name: string
  contactPerson: string | null
  phone: string | null
  email: string | null
  address: string | null
  gstNumber: string | null
  active: boolean
}

export interface Machine {
  id: number
  machineCode: string
  machineName: string
  machineType: string | null
  departmentId: number | null
  departmentName: string | null
  location: string | null
  manufacturer: string | null
  model: string | null
  status: string
  installationDate: string | null
  remarks: string | null
  active: boolean
}

export interface AttributeDef {
  id: number
  attributeName: string
  dataType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE'
  required: boolean
  displayOrder: number
}

export interface ItemCategory {
  id: number
  name: string
  parentCategoryId: number | null
  parentCategoryName: string | null
  active: boolean
  attributes: AttributeDef[]
}

export interface AttributeValue {
  attributeDefId: number
  attributeName: string
  dataType: string
  value: string
}

export interface Item {
  id: number
  itemCode: string
  name: string
  description: string | null
  categoryId: number
  categoryName: string
  manufacturerId: number | null
  manufacturerName: string | null
  preferredSupplierId: number | null
  supplierName: string | null
  uomId: number
  uomCode: string
  partNumber: string | null
  specification: string | null
  safeStock: number
  maxStock: number | null
  currentUnitCost: number
  currency: string
  legacyDescription: string | null
  barcodeValue: string | null
  active: boolean
  attributes: AttributeValue[]
}

export interface TransactionResponse {
  id: number
  txnNo: string
  itemId: number
  itemCode: string
  itemName: string
  txnType: string
  quantity: number
  unitCostAtTxn: number
  totalValue: number
  machineId: number | null
  machineCode: string | null
  employeeId: number | null
  employeeName: string | null
  performedByUsername: string
  purpose: string | null
  remark: string | null
  itemCondition: string | null
  reversalOfTxnId: number | null
  source: string
  txnDate: string
  createdAt: string
}

export interface AssignmentResponse {
  id: number
  itemId: number
  itemCode: string
  itemName: string
  employeeId: number
  employeeName: string
  machineId: number | null
  machineCode: string | null
  assignedQty: number
  returnedQty: number
  remainingQty: number
  status: 'ASSIGNED' | 'PARTIALLY_RETURNED' | 'CLOSED'
  openedAt: string
  closedAt: string | null
}

export interface CurrentStockResponse {
  itemId: number
  itemCode: string
  itemName: string
  currentStock: number
  safeStock: number
  maxStock: number | null
  unitCost: number
  inventoryValue: number
  status: 'NORMAL' | 'LOW_STOCK' | 'OUT_OF_STOCK'
}

export interface StockSummaryResponse {
  itemId: number
  itemCode: string
  itemName: string
  opening: number
  purchased: number
  issued: number
  returned: number
  adjustmentIn: number
  adjustmentOut: number
  damaged: number
  scrapped: number
  currentStock: number
}

export interface ItemConsumption {
  itemId: number
  itemCode: string
  itemName: string
  categoryId: number
  categoryName: string
  quantity: number
  value: number
}

export interface MachineConsumption {
  machineId: number
  machineCode: string
  machineName: string
  quantity: number
  value: number
}

export interface CategoryConsumption {
  categoryId: number
  categoryName: string
  quantity: number
  value: number
}

export interface MachineConsumptionDetail {
  machineId: number
  machineCode: string
  machineName: string
  from: string
  to: string
  totalQuantity: number
  totalValue: number
  items: ItemConsumption[]
}
