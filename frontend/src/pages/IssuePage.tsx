import { useEffect, useState, type SyntheticEvent } from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  Grid,
  MenuItem,
  Paper,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material'
import { apiErrorMessage } from '../api/client'
import { AccountabilityApi, InventoryApi } from '../api/endpoints'
import type { AssignmentResponse, CurrentStockResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { EmployeeSelect } from '../components/selects/EmployeeSelect'
import { ItemSelect } from '../components/selects/ItemSelect'
import { MachineSelect } from '../components/selects/MachineSelect'

export function IssuePage() {
  const [tab, setTab] = useState(0)
  return (
    <>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        Issue / Return
      </Typography>
      <Tabs value={tab} onChange={(_e: SyntheticEvent, v: number) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Issue" />
        <Tab label="Return" />
      </Tabs>
      {tab === 0 ? <IssueForm /> : <ReturnForm />}
    </>
  )
}

function IssueForm() {
  const { isAdmin } = useAuth()
  const [itemId, setItemId] = useState<number | null>(null)
  const [quantity, setQuantity] = useState('')
  const [employeeId, setEmployeeId] = useState<number | null>(null)
  const [machineId, setMachineId] = useState<number | null>(null)
  const [purpose, setPurpose] = useState('')
  const [remark, setRemark] = useState('')
  const [stock, setStock] = useState<CurrentStockResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (itemId) {
      InventoryApi.currentStock(itemId).then((res) => setStock(res.data))
    } else {
      setStock(null)
    }
  }, [itemId])

  const remaining = stock && quantity ? stock.currentStock - Number(quantity) : null

  const handleSubmit = async () => {
    setError(null)
    setSuccess(null)
    if (!itemId || !employeeId || !quantity) {
      setError('Item, quantity, and employee are required.')
      return
    }
    setSubmitting(true)
    try {
      const res = await InventoryApi.issue({
        itemId,
        quantity: Number(quantity),
        employeeId,
        machineId: machineId ?? undefined,
        purpose: purpose || undefined,
        remark: remark || undefined,
      })
      setSuccess(`Issued. Transaction ${res.data.transaction.txnNo}. Remaining stock: ${res.data.remainingStock}`)
      setQuantity('')
      setPurpose('')
      setRemark('')
      if (itemId) InventoryApi.currentStock(itemId).then((r) => setStock(r.data))
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Paper sx={{ p: 3, maxWidth: 640 }}>
      <Stack spacing={2}>
        {error && <Alert severity="error">{error}</Alert>}
        {success && <Alert severity="success">{success}</Alert>}
        <ItemSelect value={itemId} onChange={setItemId} required />
        {stock && (
          <Chip
            label={`Available: ${stock.currentStock} ${stock.status !== 'NORMAL' ? `(${stock.status})` : ''}`}
            color={stock.status === 'OUT_OF_STOCK' ? 'error' : stock.status === 'LOW_STOCK' ? 'warning' : 'default'}
            sx={{ alignSelf: 'flex-start' }}
          />
        )}
        <TextField
          label="Quantity to Issue"
          type="number"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
          required
        />
        {remaining !== null && (
          <Typography variant="body2" color={remaining < 0 ? 'error' : 'text.secondary'}>
            Remaining after issue: {remaining}
          </Typography>
        )}
        <EmployeeSelect value={employeeId} onChange={setEmployeeId} canCreate={isAdmin} required />
        <MachineSelect value={machineId} onChange={setMachineId} canCreate={isAdmin} />
        <TextField label="Purpose" value={purpose} onChange={(e) => setPurpose(e.target.value)} placeholder="e.g. Production" />
        <TextField label="Remark" value={remark} onChange={(e) => setRemark(e.target.value)} multiline minRows={2} />
        <Box>
          <Button variant="contained" size="large" onClick={handleSubmit} disabled={submitting}>
            Confirm Issue
          </Button>
        </Box>
      </Stack>
    </Paper>
  )
}

function ReturnForm() {
  const [employeeId, setEmployeeId] = useState<number | null>(null)
  const [assignments, setAssignments] = useState<AssignmentResponse[]>([])
  const [assignmentId, setAssignmentId] = useState<number | ''>('')
  const [quantity, setQuantity] = useState('')
  const [condition, setCondition] = useState('GOOD')
  const [remark, setRemark] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (employeeId) {
      AccountabilityApi.search({ employeeId, openOnly: true, size: 100 }).then((res) => setAssignments(res.data.content))
    } else {
      setAssignments([])
    }
    setAssignmentId('')
  }, [employeeId])

  const selected = assignments.find((a) => a.id === assignmentId)

  const handleSubmit = async () => {
    setError(null)
    setSuccess(null)
    if (!assignmentId || !quantity) {
      setError('Select an assignment and quantity.')
      return
    }
    setSubmitting(true)
    try {
      const res = await InventoryApi.returnStock({
        assignmentId: Number(assignmentId),
        quantity: Number(quantity),
        condition,
        remark: remark || undefined,
      })
      setSuccess(`Returned. Assignment status: ${res.data.assignment.status}`)
      setQuantity('')
      setRemark('')
      if (employeeId) {
        AccountabilityApi.search({ employeeId, openOnly: true, size: 100 }).then((r) => setAssignments(r.data.content))
      }
      setAssignmentId('')
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Paper sx={{ p: 3, maxWidth: 640 }}>
      <Stack spacing={2}>
        {error && <Alert severity="error">{error}</Alert>}
        {success && <Alert severity="success">{success}</Alert>}
        <EmployeeSelect value={employeeId} onChange={setEmployeeId} canCreate={false} required />
        <TextField
          select
          label="Assigned Item"
          value={assignmentId}
          onChange={(e) => setAssignmentId(e.target.value === '' ? '' : Number(e.target.value))}
          disabled={!employeeId}
          helperText={employeeId && assignments.length === 0 ? 'No open assignments for this employee' : undefined}
        >
          {assignments.map((a) => (
            <MenuItem key={a.id} value={a.id}>
              {a.itemCode} - {a.itemName} (remaining: {a.remainingQty})
            </MenuItem>
          ))}
        </TextField>
        {selected && (
          <Grid container spacing={2}>
            <Grid size={6}>
              <Typography variant="body2">Assigned: {selected.assignedQty}</Typography>
            </Grid>
            <Grid size={6}>
              <Typography variant="body2">Already returned: {selected.returnedQty}</Typography>
            </Grid>
          </Grid>
        )}
        <TextField
          label="Return Quantity"
          type="number"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
          required
          disabled={!selected}
        />
        <TextField select label="Condition" value={condition} onChange={(e) => setCondition(e.target.value)}>
          <MenuItem value="GOOD">Good</MenuItem>
          <MenuItem value="DAMAGED">Damaged</MenuItem>
          <MenuItem value="SCRAP">Scrap</MenuItem>
        </TextField>
        <TextField label="Remark" value={remark} onChange={(e) => setRemark(e.target.value)} multiline minRows={2} />
        <Box>
          <Button variant="contained" size="large" onClick={handleSubmit} disabled={submitting || !selected}>
            Confirm Return
          </Button>
        </Box>
      </Stack>
    </Paper>
  )
}
