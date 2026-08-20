import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Grid,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import DeleteIcon from '@mui/icons-material/Delete'
import AddIcon from '@mui/icons-material/Add'
import { apiErrorMessage } from '../api/client'
import { PurchaseRequisitionsApi } from '../api/endpoints'
import type { PurchaseRequisition } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ItemSelect } from '../components/selects/ItemSelect'
import { SupplierSelect } from '../components/selects/SupplierSelect'
import { DepartmentSelect } from '../components/selects/DepartmentSelect'
import { MachineSelect } from '../components/selects/MachineSelect'

interface DraftLine {
  itemId: number | null
  quantity: string
  estimatedPrice: string
  supplierId: number | null
}

const STATUS_COLOR: Record<string, 'default' | 'info' | 'success' | 'error' | 'warning'> = {
  DRAFT: 'default',
  SUBMITTED: 'info',
  APPROVED: 'success',
  REJECTED: 'error',
  ORDERED: 'warning',
  RECEIVED: 'success',
  CLOSED: 'default',
}

export function PurchaseRequisitionFormPage() {
  const { id } = useParams()
  const isNew = id === 'new'

  if (isNew) {
    return <CreateForm />
  }
  return <DetailView id={Number(id)} />
}

function CreateForm() {
  const navigate = useNavigate()
  const [departmentId, setDepartmentId] = useState<number | null>(null)
  const [priority, setPriority] = useState('NORMAL')
  const [reason, setReason] = useState('')
  const [lines, setLines] = useState<DraftLine[]>([{ itemId: null, quantity: '', estimatedPrice: '', supplierId: null }])
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const updateLine = (index: number, patch: Partial<DraftLine>) => {
    setLines((prev) => prev.map((l, i) => (i === index ? { ...l, ...patch } : l)))
  }

  const handleSubmit = async () => {
    setError(null)
    const validLines = lines.filter((l) => l.itemId && l.quantity && l.estimatedPrice)
    if (validLines.length === 0) {
      setError('Add at least one line item with item, quantity, and estimated price.')
      return
    }
    setSubmitting(true)
    try {
      const res = await PurchaseRequisitionsApi.create({
        departmentId: departmentId ?? undefined,
        priority,
        reason: reason || undefined,
        items: validLines.map((l) => ({
          itemId: l.itemId!,
          quantity: Number(l.quantity),
          estimatedPrice: Number(l.estimatedPrice),
          supplierId: l.supplierId ?? undefined,
        })),
      })
      navigate(`/purchase-requisitions/${res.data.id}`)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Paper sx={{ p: 3, maxWidth: 900 }}>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        New Purchase Requisition
      </Typography>
      <Stack spacing={2}>
        {error && <Alert severity="error">{error}</Alert>}
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DepartmentSelect value={departmentId} onChange={setDepartmentId} canCreate={false} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField select fullWidth label="Priority" value={priority} onChange={(e) => setPriority(e.target.value)}>
              {['LOW', 'NORMAL', 'HIGH', 'URGENT'].map((p) => (
                <MenuItem key={p} value={p}>
                  {p}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
        </Grid>
        <TextField label="Reason" value={reason} onChange={(e) => setReason(e.target.value)} multiline minRows={2} />

        <Typography variant="subtitle1" sx={{ fontWeight: 600, mt: 1 }}>
          Line Items
        </Typography>
        {lines.map((line, i) => (
          <Grid container spacing={2} key={i} sx={{ alignItems: 'center' }}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <ItemSelect value={line.itemId} onChange={(v) => updateLine(i, { itemId: v })} required />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField
                label="Quantity"
                type="number"
                fullWidth
                value={line.quantity}
                onChange={(e) => updateLine(i, { quantity: e.target.value })}
              />
            </Grid>
            <Grid size={{ xs: 6, sm: 2 }}>
              <TextField
                label="Est. Price"
                type="number"
                fullWidth
                value={line.estimatedPrice}
                onChange={(e) => updateLine(i, { estimatedPrice: e.target.value })}
              />
            </Grid>
            <Grid size={{ xs: 10, sm: 3 }}>
              <SupplierSelect value={line.supplierId} onChange={(v) => updateLine(i, { supplierId: v })} canCreate />
            </Grid>
            <Grid size={{ xs: 2, sm: 1 }}>
              <IconButton
                onClick={() => setLines((prev) => prev.filter((_, idx) => idx !== i))}
                disabled={lines.length === 1}
              >
                <DeleteIcon />
              </IconButton>
            </Grid>
          </Grid>
        ))}
        <Box>
          <Button
            startIcon={<AddIcon />}
            onClick={() => setLines((prev) => [...prev, { itemId: null, quantity: '', estimatedPrice: '', supplierId: null }])}
          >
            Add Line
          </Button>
        </Box>

        <Box>
          <Button variant="contained" size="large" onClick={handleSubmit} disabled={submitting}>
            Save as Draft
          </Button>
        </Box>
      </Stack>
    </Paper>
  )
}

function DetailView({ id }: { id: number }) {
  const { isAdmin, isIssuer } = useAuth()
  const [pr, setPr] = useState<PurchaseRequisition | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [rejectOpen, setRejectOpen] = useState(false)
  const [rejectReason, setRejectReason] = useState('')
  const [receiveOpen, setReceiveOpen] = useState(false)
  const [busy, setBusy] = useState(false)

  const load = () => PurchaseRequisitionsApi.get(id).then((res) => setPr(res.data)).catch((e) => setError(apiErrorMessage(e)))

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  const act = async (fn: () => Promise<unknown>) => {
    setError(null)
    setBusy(true)
    try {
      await fn()
      await load()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  if (!pr) {
    return error ? <Alert severity="error">{error}</Alert> : <Typography>Loading…</Typography>
  }

  const canSubmit = pr.status === 'DRAFT' && (isAdmin || isIssuer)
  const canApprove = pr.status === 'SUBMITTED' && isAdmin
  const canMarkOrdered = pr.status === 'APPROVED' && (isAdmin || isIssuer)
  const canReceive = pr.status === 'ORDERED' && (isAdmin || isIssuer)
  const canClose = pr.status === 'RECEIVED' && isAdmin

  return (
    <Paper sx={{ p: 3, maxWidth: 1100 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 600 }}>
          {pr.prNo}
        </Typography>
        <Chip label={pr.status} color={STATUS_COLOR[pr.status]} />
      </Box>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid size={{ xs: 12, sm: 3 }}>
          <Typography variant="body2" color="text.secondary">Requested By</Typography>
          <Typography>{pr.requestedByUsername}</Typography>
        </Grid>
        <Grid size={{ xs: 12, sm: 3 }}>
          <Typography variant="body2" color="text.secondary">Department</Typography>
          <Typography>{pr.departmentName ?? '-'}</Typography>
        </Grid>
        <Grid size={{ xs: 12, sm: 3 }}>
          <Typography variant="body2" color="text.secondary">Priority</Typography>
          <Typography>{pr.priority ?? '-'}</Typography>
        </Grid>
        <Grid size={{ xs: 12, sm: 3 }}>
          <Typography variant="body2" color="text.secondary">Approved By</Typography>
          <Typography>{pr.approvedByUsername ?? '-'}</Typography>
        </Grid>
        {pr.reason && (
          <Grid size={12}>
            <Typography variant="body2" color="text.secondary">Reason</Typography>
            <Typography>{pr.reason}</Typography>
          </Grid>
        )}
      </Grid>

      <Table size="small" sx={{ mb: 2 }}>
        <TableHead>
          <TableRow>
            <TableCell>Item</TableCell>
            <TableCell align="right">Qty</TableCell>
            <TableCell align="right">Est. Price</TableCell>
            <TableCell>Supplier</TableCell>
            <TableCell align="right">Received</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {pr.items.map((line) => (
            <TableRow key={line.id}>
              <TableCell>{line.itemCode} — {line.itemName}</TableCell>
              <TableCell align="right">{line.quantity}</TableCell>
              <TableCell align="right">{line.estimatedPrice}</TableCell>
              <TableCell>{line.supplierName ?? '-'}</TableCell>
              <TableCell align="right">{line.receivedQty}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Stack direction="row" spacing={2}>
        {canSubmit && (
          <Button variant="contained" disabled={busy} onClick={() => act(() => PurchaseRequisitionsApi.submit(pr.id))}>
            Submit
          </Button>
        )}
        {canApprove && (
          <>
            <Button variant="contained" color="success" disabled={busy} onClick={() => act(() => PurchaseRequisitionsApi.approve(pr.id))}>
              Approve
            </Button>
            <Button variant="outlined" color="error" disabled={busy} onClick={() => setRejectOpen(true)}>
              Reject
            </Button>
          </>
        )}
        {canMarkOrdered && (
          <Button variant="contained" disabled={busy} onClick={() => act(() => PurchaseRequisitionsApi.markOrdered(pr.id))}>
            Mark as Ordered
          </Button>
        )}
        {canReceive && (
          <Button variant="contained" disabled={busy} onClick={() => setReceiveOpen(true)}>
            Receive Goods
          </Button>
        )}
        {canClose && (
          <Button variant="contained" disabled={busy} onClick={() => act(() => PurchaseRequisitionsApi.close(pr.id))}>
            Close
          </Button>
        )}
      </Stack>

      <Dialog open={rejectOpen} onClose={() => setRejectOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Reject {pr.prNo}</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            multiline
            minRows={2}
            label="Reason"
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectOpen(false)}>Cancel</Button>
          <Button
            color="error"
            disabled={!rejectReason.trim() || busy}
            onClick={async () => {
              await act(() => PurchaseRequisitionsApi.reject(pr.id, rejectReason))
              setRejectOpen(false)
              setRejectReason('')
            }}
          >
            Reject
          </Button>
        </DialogActions>
      </Dialog>

      {receiveOpen && (
        <ReceiveDialog
          pr={pr}
          onClose={() => setReceiveOpen(false)}
          onDone={() => {
            setReceiveOpen(false)
            load()
          }}
        />
      )}
    </Paper>
  )
}

interface ReceiveLineState {
  prItemId: number
  receivedQty: string
  unitCost: string
  directToFloor: boolean
  machineId: number | null
}

function ReceiveDialog({ pr, onClose, onDone }: { pr: PurchaseRequisition; onClose: () => void; onDone: () => void }) {
  const [lines, setLines] = useState<ReceiveLineState[]>(
    pr.items
      .filter((l) => l.receivedQty < l.quantity)
      .map((l) => ({ prItemId: l.id, receivedQty: String(l.quantity - l.receivedQty), unitCost: String(l.estimatedPrice), directToFloor: false, machineId: null })),
  )
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const updateLine = (index: number, patch: Partial<ReceiveLineState>) => {
    setLines((prev) => prev.map((l, i) => (i === index ? { ...l, ...patch } : l)))
  }

  const handleSubmit = async () => {
    setError(null)
    const validLines = lines.filter((l) => Number(l.receivedQty) > 0 && l.unitCost !== '')
    if (validLines.length === 0) {
      setError('Enter a received quantity and unit cost for at least one line.')
      return
    }
    setSubmitting(true)
    try {
      await PurchaseRequisitionsApi.receive(
        pr.id,
        validLines.map((l) => ({
          prItemId: l.prItemId,
          receivedQty: Number(l.receivedQty),
          unitCost: Number(l.unitCost),
          directToFloor: l.directToFloor,
          machineId: l.directToFloor ? l.machineId ?? undefined : undefined,
        })),
      )
      onDone()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>Receive Goods — {pr.prNo}</DialogTitle>
      <DialogContent>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <Stack spacing={2} sx={{ mt: 1 }}>
          {pr.items.map((item) => {
            const line = lines.find((l) => l.prItemId === item.id)
            if (!line) {
              return (
                <Typography key={item.id} variant="body2" color="text.secondary">
                  {item.itemCode} — {item.itemName}: fully received ({item.receivedQty}/{item.quantity})
                </Typography>
              )
            }
            const lineIndex = lines.findIndex((l) => l.prItemId === item.id)
            return (
              <Paper key={item.id} variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" gutterBottom>
                  {item.itemCode} — {item.itemName} (ordered {item.quantity}, received so far {item.receivedQty})
                </Typography>
                <Grid container spacing={2} sx={{ alignItems: 'center' }}>
                  <Grid size={{ xs: 6, sm: 3 }}>
                    <TextField
                      label="Received Qty"
                      type="number"
                      fullWidth
                      value={line.receivedQty}
                      onChange={(e) => updateLine(lineIndex, { receivedQty: e.target.value })}
                    />
                  </Grid>
                  <Grid size={{ xs: 6, sm: 3 }}>
                    <TextField
                      label="Unit Cost"
                      type="number"
                      fullWidth
                      value={line.unitCost}
                      onChange={(e) => updateLine(lineIndex, { unitCost: e.target.value })}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 3 }}>
                    <FormControlLabel
                      control={
                        <Checkbox
                          checked={line.directToFloor}
                          onChange={(e) => updateLine(lineIndex, { directToFloor: e.target.checked })}
                        />
                      }
                      label="Direct-to-floor"
                    />
                  </Grid>
                  {line.directToFloor && (
                    <Grid size={{ xs: 12, sm: 3 }}>
                      <MachineSelect value={line.machineId} onChange={(v) => updateLine(lineIndex, { machineId: v })} canCreate={false} />
                    </Grid>
                  )}
                </Grid>
              </Paper>
            )
          })}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={submitting} onClick={handleSubmit}>
          Confirm Receipt
        </Button>
      </DialogActions>
    </Dialog>
  )
}
