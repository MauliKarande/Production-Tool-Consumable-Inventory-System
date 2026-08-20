import { useEffect, useMemo, useState } from 'react'
import {
  Box,
  Grid,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { ConsumptionApi } from '../api/endpoints'
import type { CategoryConsumption, ItemConsumption, MachineConsumption, MachineConsumptionDetail } from '../api/types'

function firstOfMonth(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

function money(n: number): string {
  return `₹${n.toLocaleString('en-IN', { maximumFractionDigits: 2 })}`
}

export function ConsumptionPage() {
  const [from, setFrom] = useState(firstOfMonth())
  const [to, setTo] = useState(today())
  const [machines, setMachines] = useState<MachineConsumption[]>([])
  const [categories, setCategories] = useState<CategoryConsumption[]>([])
  const [topByValue, setTopByValue] = useState<ItemConsumption[]>([])
  const [selectedMachineId, setSelectedMachineId] = useState<number | ''>('')
  const [machineDetail, setMachineDetail] = useState<MachineConsumptionDetail | null>(null)

  useEffect(() => {
    if (!from || !to) return
    ConsumptionApi.allMachines(from, to).then((res) => setMachines(res.data))
    ConsumptionApi.byCategory(from, to).then((res) => setCategories(res.data))
    ConsumptionApi.topItems(from, to, 'value', 10).then((res) => setTopByValue(res.data))
  }, [from, to])

  useEffect(() => {
    if (selectedMachineId && from && to) {
      ConsumptionApi.machineDetail(Number(selectedMachineId), from, to).then((res) => setMachineDetail(res.data))
    } else {
      setMachineDetail(null)
    }
  }, [selectedMachineId, from, to])

  const grandTotal = useMemo(() => machines.reduce((sum, m) => sum + m.value, 0), [machines])

  return (
    <>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        CNC-wise Consumption
      </Typography>
      <Paper sx={{ p: 2, mb: 3, display: 'flex', gap: 2 }}>
        <TextField
          label="From"
          type="date"
          value={from}
          onChange={(e) => setFrom(e.target.value)}
          slotProps={{ inputLabel: { shrink: true } }}
        />
        <TextField
          label="To"
          type="date"
          value={to}
          onChange={(e) => setTo(e.target.value)}
          slotProps={{ inputLabel: { shrink: true } }}
        />
      </Paper>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Machine-wise Comparison
            </Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Machine</TableCell>
                  <TableCell align="right">Qty</TableCell>
                  <TableCell align="right">Value</TableCell>
                  <TableCell align="right">Share</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {machines.map((m) => (
                  <TableRow
                    key={m.machineId}
                    hover
                    selected={selectedMachineId === m.machineId}
                    sx={{ cursor: 'pointer' }}
                    onClick={() => setSelectedMachineId(m.machineId)}
                  >
                    <TableCell>{m.machineName}</TableCell>
                    <TableCell align="right">{m.quantity}</TableCell>
                    <TableCell align="right">{money(m.value)}</TableCell>
                    <TableCell align="right">{grandTotal ? `${((m.value / grandTotal) * 100).toFixed(1)}%` : '-'}</TableCell>
                  </TableRow>
                ))}
                {machines.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4}>
                      <Typography variant="body2" color="text.secondary">
                        No consumption recorded in this date range.
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Paper>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Category-wise Consumption
            </Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Category</TableCell>
                  <TableCell align="right">Qty</TableCell>
                  <TableCell align="right">Value</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {categories.map((c) => (
                  <TableRow key={c.categoryId}>
                    <TableCell>{c.categoryName}</TableCell>
                    <TableCell align="right">{c.quantity}</TableCell>
                    <TableCell align="right">{money(c.value)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        </Grid>

        {machineDetail && (
          <Grid size={{ xs: 12 }}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                {machineDetail.machineName} — Item Breakdown ({machineDetail.from} to {machineDetail.to})
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Item Code</TableCell>
                    <TableCell>Item</TableCell>
                    <TableCell>Category</TableCell>
                    <TableCell align="right">Qty</TableCell>
                    <TableCell align="right">Value</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {machineDetail.items.map((i) => (
                    <TableRow key={i.itemId}>
                      <TableCell>{i.itemCode}</TableCell>
                      <TableCell>{i.itemName}</TableCell>
                      <TableCell>{i.categoryName}</TableCell>
                      <TableCell align="right">{i.quantity}</TableCell>
                      <TableCell align="right">{money(i.value)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <Box sx={{ mt: 1, textAlign: 'right' }}>
                <Typography variant="body2">
                  Total: {machineDetail.totalQuantity} units, {money(machineDetail.totalValue)}
                </Typography>
              </Box>
            </Paper>
          </Grid>
        )}

        <Grid size={{ xs: 12 }}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Top 10 Consumed Items (by value)
            </Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Item Code</TableCell>
                  <TableCell>Item</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell align="right">Qty</TableCell>
                  <TableCell align="right">Value</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {topByValue.map((i) => (
                  <TableRow key={i.itemId}>
                    <TableCell>{i.itemCode}</TableCell>
                    <TableCell>{i.itemName}</TableCell>
                    <TableCell>{i.categoryName}</TableCell>
                    <TableCell align="right">{i.quantity}</TableCell>
                    <TableCell align="right">{money(i.value)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        </Grid>
      </Grid>
    </>
  )
}
