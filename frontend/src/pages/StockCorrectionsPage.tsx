import { useEffect, useState, type SyntheticEvent } from 'react'
import {
  Alert,
  Box,
  Button,
  MenuItem,
  Paper,
  Radio,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material'
import { apiErrorMessage } from '../api/client'
import { InventoryApi, ItemsApi } from '../api/endpoints'
import type { TransactionResponse } from '../api/types'
import { ItemSelect } from '../components/selects/ItemSelect'

export function StockCorrectionsPage() {
  const [tab, setTab] = useState(0)
  return (
    <>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        Stock Corrections
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Admin-only. Every correction here posts a new, reasoned ledger transaction — history is never edited in place.
      </Typography>
      <Tabs value={tab} onChange={(_e: SyntheticEvent, v: number) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Adjustment" />
        <Tab label="Damage / Scrap" />
        <Tab label="Reversal" />
      </Tabs>
      {tab === 0 && <AdjustmentForm />}
      {tab === 1 && <DamageScrapForm />}
      {tab === 2 && <ReversalForm />}
    </>
  )
}

function AdjustmentForm() {
  const [itemId, setItemId] = useState<number | null>(null)
  const [direction, setDirection] = useState<'IN' | 'OUT'>('IN')
  const [quantity, setQuantity] = useState('')
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async () => {
    setError(null)
    setSuccess(null)
    if (!itemId || !quantity || !reason) {
      setError('Item, quantity, and reason are all required.')
      return
    }
    setSubmitting(true)
    try {
      const res = await InventoryApi.adjustment({ itemId, direction, quantity: Number(quantity), reason })
      setSuccess(`Adjustment posted. New stock: ${res.data.newStock}`)
      setQuantity('')
      setReason('')
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
        <TextField select label="Direction" value={direction} onChange={(e) => setDirection(e.target.value as 'IN' | 'OUT')}>
          <MenuItem value="IN">Adjustment In (increase stock)</MenuItem>
          <MenuItem value="OUT">Adjustment Out (decrease stock)</MenuItem>
        </TextField>
        <TextField label="Quantity" type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} required />
        <TextField
          label="Reason"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          multiline
          minRows={2}
          required
          placeholder="e.g. Physical count correction, data migration correction"
        />
        <Box>
          <Button variant="contained" size="large" onClick={handleSubmit} disabled={submitting}>
            Post Adjustment
          </Button>
        </Box>
      </Stack>
    </Paper>
  )
}

function DamageScrapForm() {
  const [itemId, setItemId] = useState<number | null>(null)
  const [type, setType] = useState<'DAMAGE' | 'SCRAP'>('DAMAGE')
  const [quantity, setQuantity] = useState('')
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async () => {
    setError(null)
    setSuccess(null)
    if (!itemId || !quantity || !reason) {
      setError('Item, quantity, and reason are all required.')
      return
    }
    setSubmitting(true)
    try {
      const res = await InventoryApi.damageScrap({ itemId, type, quantity: Number(quantity), reason })
      setSuccess(`${type === 'DAMAGE' ? 'Damage' : 'Scrap'} posted. New stock: ${res.data.newStock}`)
      setQuantity('')
      setReason('')
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
        <TextField select label="Type" value={type} onChange={(e) => setType(e.target.value as 'DAMAGE' | 'SCRAP')}>
          <MenuItem value="DAMAGE">Damage</MenuItem>
          <MenuItem value="SCRAP">Scrap</MenuItem>
        </TextField>
        <TextField label="Quantity" type="number" value={quantity} onChange={(e) => setQuantity(e.target.value)} required />
        <TextField
          label="Reason"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          multiline
          minRows={2}
          required
          placeholder="e.g. Found damaged during physical count"
        />
        <Box>
          <Button variant="contained" color="error" size="large" onClick={handleSubmit} disabled={submitting}>
            Post Write-off
          </Button>
        </Box>
      </Stack>
    </Paper>
  )
}

function ReversalForm() {
  const [itemId, setItemId] = useState<number | null>(null)
  const [transactions, setTransactions] = useState<TransactionResponse[]>([])
  const [selectedTxnId, setSelectedTxnId] = useState<number | null>(null)
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    setSelectedTxnId(null)
    if (itemId) {
      ItemsApi.transactions(itemId, 0, 20).then((res) => setTransactions(res.data.content))
    } else {
      setTransactions([])
    }
  }, [itemId])

  const handleSubmit = async () => {
    setError(null)
    setSuccess(null)
    if (!selectedTxnId || !reason) {
      setError('Select a transaction to reverse and enter a reason.')
      return
    }
    setSubmitting(true)
    try {
      const res = await InventoryApi.reversal({ transactionId: selectedTxnId, reason })
      setSuccess(`Reversal posted (${res.data.transaction.txnNo}). New stock: ${res.data.newStock}`)
      setReason('')
      setSelectedTxnId(null)
      if (itemId) ItemsApi.transactions(itemId, 0, 20).then((r) => setTransactions(r.data.content))
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Paper sx={{ p: 3, maxWidth: 900 }}>
      <Stack spacing={2}>
        {error && <Alert severity="error">{error}</Alert>}
        {success && <Alert severity="success">{success}</Alert>}
        <ItemSelect value={itemId} onChange={setItemId} required />

        {itemId && (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell />
                <TableCell>Txn No.</TableCell>
                <TableCell>Type</TableCell>
                <TableCell align="right">Qty</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Remark</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {transactions
                .filter((t) => t.txnType !== 'REVERSAL')
                .map((t) => (
                  <TableRow key={t.id} hover onClick={() => setSelectedTxnId(t.id)} sx={{ cursor: 'pointer' }}>
                    <TableCell padding="checkbox">
                      <Radio checked={selectedTxnId === t.id} onChange={() => setSelectedTxnId(t.id)} />
                    </TableCell>
                    <TableCell>{t.txnNo}</TableCell>
                    <TableCell>{t.txnType}</TableCell>
                    <TableCell align="right">{t.quantity}</TableCell>
                    <TableCell>{t.txnDate}</TableCell>
                    <TableCell>{t.remark ?? '-'}</TableCell>
                  </TableRow>
                ))}
              {transactions.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    No transactions found for this item.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}

        <TextField
          label="Reason"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          multiline
          minRows={2}
          required
          disabled={!selectedTxnId}
          placeholder="Why is this transaction being reversed?"
        />
        <Box>
          <Button variant="contained" color="warning" size="large" onClick={handleSubmit} disabled={submitting || !selectedTxnId}>
            Post Reversal
          </Button>
        </Box>
      </Stack>
    </Paper>
  )
}
