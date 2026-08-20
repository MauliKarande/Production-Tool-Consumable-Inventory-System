import { useEffect, useState, type SyntheticEvent } from 'react'
import {
  Box,
  Button,
  ButtonGroup,
  Paper,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from '@mui/material'
import DownloadIcon from '@mui/icons-material/Download'
import { ReportsApi } from '../api/endpoints'
import type {
  DeadStockRow,
  LowStockRow,
  PurchasePipelineRow,
  StockValuationRow,
  SupplierPriceRow,
  SupplierSpendRow,
} from '../api/types'

function money(n: number): string {
  return `₹${n.toLocaleString('en-IN', { maximumFractionDigits: 2 })}`
}

function download(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  window.URL.revokeObjectURL(url)
}

function ExportButtons({ report, params }: { report: string; params?: Record<string, unknown> }) {
  const handle = async (format: 'xlsx' | 'pdf') => {
    const res = await ReportsApi.download(report, format, params)
    download(res.data, `${report}.${format}`)
  }
  return (
    <ButtonGroup size="small" sx={{ mb: 2 }}>
      <Button startIcon={<DownloadIcon />} onClick={() => handle('xlsx')}>
        Excel
      </Button>
      <Button startIcon={<DownloadIcon />} onClick={() => handle('pdf')}>
        PDF
      </Button>
    </ButtonGroup>
  )
}

export function ReportsPage() {
  const [tab, setTab] = useState(0)
  return (
    <>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        Reports
      </Typography>
      <Tabs value={tab} onChange={(_e: SyntheticEvent, v: number) => setTab(v)} sx={{ mb: 2 }} variant="scrollable">
        <Tab label="Stock Valuation" />
        <Tab label="Low / Out of Stock" />
        <Tab label="Dead Stock" />
        <Tab label="Supplier Price Comparison" />
        <Tab label="Supplier Spend" />
        <Tab label="Purchase Pipeline" />
      </Tabs>
      {tab === 0 && <StockValuationTab />}
      {tab === 1 && <LowStockTab />}
      {tab === 2 && <DeadStockTab />}
      {tab === 3 && <SupplierPriceTab />}
      {tab === 4 && <SupplierSpendTab />}
      {tab === 5 && <PurchasePipelineTab />}
    </>
  )
}

function StockValuationTab() {
  const [rows, setRows] = useState<StockValuationRow[]>([])
  useEffect(() => {
    ReportsApi.stockValuation().then((res) => setRows(res.data))
  }, [])
  return (
    <Paper sx={{ p: 2 }}>
      <ExportButtons report="stock-valuation" />
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Code</TableCell>
            <TableCell>Name</TableCell>
            <TableCell>Category</TableCell>
            <TableCell align="right">Stock</TableCell>
            <TableCell align="right">Unit Cost</TableCell>
            <TableCell align="right">Value</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r) => (
            <TableRow key={r.itemId}>
              <TableCell>{r.itemCode}</TableCell>
              <TableCell>{r.itemName}</TableCell>
              <TableCell>{r.categoryName}</TableCell>
              <TableCell align="right">{r.currentStock} {r.uomCode}</TableCell>
              <TableCell align="right">{money(r.unitCost)}</TableCell>
              <TableCell align="right">{money(r.value)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  )
}

function LowStockTab() {
  const [rows, setRows] = useState<LowStockRow[]>([])
  useEffect(() => {
    ReportsApi.lowStock().then((res) => setRows(res.data))
  }, [])
  return (
    <Paper sx={{ p: 2 }}>
      <ExportButtons report="low-stock" />
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Code</TableCell>
            <TableCell>Name</TableCell>
            <TableCell align="right">Stock</TableCell>
            <TableCell align="right">Safe Stock</TableCell>
            <TableCell align="right">Reorder Qty</TableCell>
            <TableCell>Status</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r) => (
            <TableRow key={r.itemId}>
              <TableCell>{r.itemCode}</TableCell>
              <TableCell>{r.itemName}</TableCell>
              <TableCell align="right">{r.currentStock}</TableCell>
              <TableCell align="right">{r.safeStock}</TableCell>
              <TableCell align="right">{r.reorderQty}</TableCell>
              <TableCell>{r.status}</TableCell>
            </TableRow>
          ))}
          {rows.length === 0 && (
            <TableRow>
              <TableCell colSpan={6} align="center">Nothing low or out of stock.</TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </Paper>
  )
}

function DeadStockTab() {
  const [months, setMonths] = useState(3)
  const [rows, setRows] = useState<DeadStockRow[]>([])
  useEffect(() => {
    ReportsApi.deadStock(months).then((res) => setRows(res.data))
  }, [months])
  return (
    <Paper sx={{ p: 2 }}>
      <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', mb: 2 }}>
        <TextField
          label="Months of no consumption"
          type="number"
          size="small"
          value={months}
          onChange={(e) => setMonths(Number(e.target.value) || 3)}
          sx={{ width: 220 }}
        />
        <ExportButtons report="dead-stock" params={{ months }} />
      </Box>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Code</TableCell>
            <TableCell>Name</TableCell>
            <TableCell>Category</TableCell>
            <TableCell align="right">Stock</TableCell>
            <TableCell align="right">Value</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r) => (
            <TableRow key={r.itemId}>
              <TableCell>{r.itemCode}</TableCell>
              <TableCell>{r.itemName}</TableCell>
              <TableCell>{r.categoryName}</TableCell>
              <TableCell align="right">{r.currentStock}</TableCell>
              <TableCell align="right">{money(r.value)}</TableCell>
            </TableRow>
          ))}
          {rows.length === 0 && (
            <TableRow>
              <TableCell colSpan={5} align="center">No dead stock found.</TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </Paper>
  )
}

function SupplierPriceTab() {
  const [rows, setRows] = useState<SupplierPriceRow[]>([])
  useEffect(() => {
    ReportsApi.supplierPriceComparison().then((res) => setRows(res.data))
  }, [])
  return (
    <Paper sx={{ p: 2 }}>
      <ExportButtons report="supplier-price-comparison" />
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Code</TableCell>
            <TableCell>Item</TableCell>
            <TableCell>Supplier</TableCell>
            <TableCell align="right">Min</TableCell>
            <TableCell align="right">Max</TableCell>
            <TableCell align="right">Avg</TableCell>
            <TableCell align="right">Times Quoted</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r, i) => (
            <TableRow key={`${r.itemId}-${r.supplierId}-${i}`}>
              <TableCell>{r.itemCode}</TableCell>
              <TableCell>{r.itemName}</TableCell>
              <TableCell>{r.supplierName}</TableCell>
              <TableCell align="right">{money(r.minPrice)}</TableCell>
              <TableCell align="right">{money(r.maxPrice)}</TableCell>
              <TableCell align="right">{money(r.avgPrice)}</TableCell>
              <TableCell align="right">{r.timesQuoted}</TableCell>
            </TableRow>
          ))}
          {rows.length === 0 && (
            <TableRow>
              <TableCell colSpan={7} align="center">No purchase requisition price data yet.</TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </Paper>
  )
}

function SupplierSpendTab() {
  const [rows, setRows] = useState<SupplierSpendRow[]>([])
  useEffect(() => {
    ReportsApi.supplierSpend().then((res) => setRows(res.data))
  }, [])
  return (
    <Paper sx={{ p: 2 }}>
      <ExportButtons report="supplier-spend" />
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Supplier</TableCell>
            <TableCell align="right">Total Spend</TableCell>
            <TableCell align="right">Lines</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r) => (
            <TableRow key={r.supplierId}>
              <TableCell>{r.supplierName}</TableCell>
              <TableCell align="right">{money(r.totalSpend)}</TableCell>
              <TableCell align="right">{r.lineCount}</TableCell>
            </TableRow>
          ))}
          {rows.length === 0 && (
            <TableRow>
              <TableCell colSpan={3} align="center">No received purchases yet.</TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </Paper>
  )
}

function PurchasePipelineTab() {
  const [rows, setRows] = useState<PurchasePipelineRow[]>([])
  useEffect(() => {
    ReportsApi.purchasePipeline().then((res) => setRows(res.data))
  }, [])
  return (
    <Paper sx={{ p: 2 }}>
      <ExportButtons report="purchase-pipeline" />
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Status</TableCell>
            <TableCell align="right">Count</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r) => (
            <TableRow key={r.status}>
              <TableCell>{r.status}</TableCell>
              <TableCell align="right">{r.count}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  )
}
