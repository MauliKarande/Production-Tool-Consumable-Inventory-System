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

export interface PurchaseRequisitionLine {
  id: number
  itemId: number
  itemCode: string
  itemName: string
  quantity: number
  estimatedPrice: number
  supplierId: number | null
  supplierName: string | null
  receivedQty: number
  receivedTxnId: number | null
}

export interface PurchaseRequisition {
  id: number
  prNo: string
  requestedByUserId: number
  requestedByUsername: string
  departmentId: number | null
  departmentName: string | null
  status: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'ORDERED' | 'RECEIVED' | 'CLOSED'
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT' | null
  reason: string | null
  approvedByUserId: number | null
  approvedByUsername: string | null
  approvedAt: string | null
  createdAt: string
  updatedAt: string
  items: PurchaseRequisitionLine[]
}

export interface DashboardSummary {
  itemCount: number
  totalStockValue: number
  lowStockCount: number
  outOfStockCount: number
  openAlertCount: number
  openPrCount: number
  thisMonthConsumptionValue: number
}

export interface StockValuationRow {
  itemId: number
  itemCode: string
  itemName: string
  categoryName: string
  uomCode: string
  currentStock: number
  unitCost: number
  value: number
}

export interface LowStockRow {
  itemId: number
  itemCode: string
  itemName: string
  currentStock: number
  safeStock: number
  maxStock: number | null
  reorderQty: number
  status: 'LOW_STOCK' | 'OUT_OF_STOCK'
}

export interface DeadStockRow {
  itemId: number
  itemCode: string
  itemName: string
  categoryName: string
  currentStock: number
  unitCost: number
  value: number
}

export interface SupplierPriceRow {
  itemId: number
  itemCode: string
  itemName: string
  supplierId: number
  supplierName: string
  minPrice: number
  maxPrice: number
  avgPrice: number
  timesQuoted: number
}

export interface SupplierSpendRow {
  supplierId: number
  supplierName: string
  totalSpend: number
  lineCount: number
}

export interface PurchasePipelineRow {
  status: string
  count: number
}

export interface ImportResult {
  committed: boolean
  sourceFile: string
  manufacturersCreated: number
  suppliersCreated: number
  itemsCreated: number
  machinesCreated: number
  purchaseRequisitionsCreated: number
  transactionsPosted: number
  warnings: string[]
  errors: string[]
}

export interface AlertResponse {
  id: number
  type: 'LOW_STOCK' | 'OUT_OF_STOCK' | 'HIGH_CONSUMPTION' | 'PENDING_RETURN' | 'UNUSUAL_CONSUMPTION' | 'PURCHASE_PENDING'
  itemId: number | null
  itemCode: string | null
  itemName: string | null
  message: string
  status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED'
  raisedAt: string
  acknowledgedByUsername: string | null
  acknowledgedAt: string | null
  resolvedByUsername: string | null
  resolvedAt: string | null
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
