import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  Divider,
  FormControlLabel,
  Grid,
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
import { apiErrorMessage } from '../api/client'
import { ItemsApi } from '../api/endpoints'
import type { Item, StockSummaryResponse, TransactionResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { CategorySelect } from '../components/selects/CategorySelect'
import { ManufacturerSelect } from '../components/selects/ManufacturerSelect'
import { SupplierSelect } from '../components/selects/SupplierSelect'
import { UomSelect } from '../components/selects/UomSelect'

export function ItemFormPage() {
  const { id } = useParams()
  const isNew = id === 'new'
  const navigate = useNavigate()
  const { isAdmin } = useAuth()

  const [itemCode, setItemCode] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [manufacturerId, setManufacturerId] = useState<number | null>(null)
  const [supplierId, setSupplierId] = useState<number | null>(null)
  const [uomId, setUomId] = useState<number | null>(null)
  const [safeStock, setSafeStock] = useState('0')
  const [maxStock, setMaxStock] = useState('')
  const [openingUnitCost, setOpeningUnitCost] = useState('0')
  const [active, setActive] = useState(true)

  const [loadedItem, setLoadedItem] = useState<Item | null>(null)
  const [summary, setSummary] = useState<StockSummaryResponse | null>(null)
  const [history, setHistory] = useState<TransactionResponse[]>([])
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (isNew || !id) return
    const itemId = Number(id)
    ItemsApi.get(itemId).then((res) => {
      const item = res.data
      setLoadedItem(item)
      setItemCode(item.itemCode)
      setName(item.name)
      setDescription(item.description ?? '')
      setCategoryId(item.categoryId)
      setManufacturerId(item.manufacturerId)
      setSupplierId(item.preferredSupplierId)
      setUomId(item.uomId)
      setSafeStock(String(item.safeStock))
      setMaxStock(item.maxStock != null ? String(item.maxStock) : '')
      setActive(item.active)
    })
    ItemsApi.stockSummary(itemId).then((res) => setSummary(res.data))
    ItemsApi.transactions(itemId, 0, 20).then((res) => setHistory(res.data.content))
  }, [id, isNew])

  const handleSubmit = async () => {
    setError(null)
    if (!categoryId || !uomId) {
      setError('Category and Unit of Measure are required.')
      return
    }
    setSaving(true)
    try {
      if (isNew) {
        const res = await ItemsApi.create({
          itemCode,
          name,
          description: description || undefined,
          categoryId,
          manufacturerId,
          preferredSupplierId: supplierId,
          uomId,
          safeStock: Number(safeStock),
          maxStock: maxStock ? Number(maxStock) : null,
          openingUnitCost: Number(openingUnitCost || 0),
          active,
          attributeValues: {},
        })
        navigate(`/items/${res.data.id}`)
      } else if (id) {
        await ItemsApi.update(Number(id), {
          name,
          description: description || undefined,
          categoryId,
          manufacturerId,
          preferredSupplierId: supplierId,
          uomId,
          safeStock: Number(safeStock),
          maxStock: maxStock ? Number(maxStock) : null,
          active,
          attributeValues: {},
        })
        const res = await ItemsApi.get(Number(id))
        setLoadedItem(res.data)
      }
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        {isNew ? 'New Item' : loadedItem?.name ?? 'Item'}
      </Typography>

      <Paper sx={{ p: 3, mb: 3 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Item Code"
              value={itemCode}
              onChange={(e) => setItemCode(e.target.value)}
              required
              fullWidth
              disabled={!isNew || !isAdmin}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              fullWidth
              disabled={!isAdmin}
            />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField
              label="Description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              fullWidth
              multiline
              minRows={2}
              disabled={!isAdmin}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <CategorySelect value={categoryId} onChange={setCategoryId} canCreate={isAdmin} required />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <ManufacturerSelect value={manufacturerId} onChange={setManufacturerId} canCreate={isAdmin} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <SupplierSelect value={supplierId} onChange={setSupplierId} canCreate={isAdmin} />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <UomSelect value={uomId} onChange={setUomId} canCreate={isAdmin} required />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField
              label="Safe Stock"
              type="number"
              value={safeStock}
              onChange={(e) => setSafeStock(e.target.value)}
              fullWidth
              disabled={!isAdmin}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField
              label="Max Stock"
              type="number"
              value={maxStock}
              onChange={(e) => setMaxStock(e.target.value)}
              fullWidth
              disabled={!isAdmin}
            />
          </Grid>
          {isNew && (
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                label="Opening Unit Cost (₹)"
                type="number"
                value={openingUnitCost}
                onChange={(e) => setOpeningUnitCost(e.target.value)}
                fullWidth
                helperText="Set stock quantity afterwards via Opening Balance"
              />
            </Grid>
          )}
          <Grid size={{ xs: 12 }}>
            <FormControlLabel
              control={<Checkbox checked={active} onChange={(e) => setActive(e.target.checked)} disabled={!isAdmin} />}
              label="Active"
            />
          </Grid>
        </Grid>
        {isAdmin && (
          <Box sx={{ mt: 2 }}>
            <Button variant="contained" onClick={handleSubmit} disabled={saving}>
              {isNew ? 'Create Item' : 'Save Changes'}
            </Button>
          </Box>
        )}
      </Paper>

      {!isNew && summary && (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Stock Summary
          </Typography>
          <Grid container spacing={2}>
            {[
              ['Opening', summary.opening],
              ['Purchased', summary.purchased],
              ['Issued', summary.issued],
              ['Returned', summary.returned],
              ['Adj. In', summary.adjustmentIn],
              ['Adj. Out', summary.adjustmentOut],
              ['Damaged', summary.damaged],
              ['Scrapped', summary.scrapped],
            ].map(([label, val]) => (
              <Grid size={{ xs: 6, sm: 3 }} key={label as string}>
                <Typography variant="caption" color="text.secondary">
                  {label}
                </Typography>
                <Typography variant="h6">{val}</Typography>
              </Grid>
            ))}
          </Grid>
          <Divider sx={{ my: 2 }} />
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
            <Typography variant="body1">
              Current Stock: <strong>{summary.currentStock}</strong>
            </Typography>
            <Chip
              label={
                summary.currentStock <= 0 ? 'OUT OF STOCK' : summary.currentStock <= (loadedItem?.safeStock ?? 0) ? 'LOW STOCK' : 'NORMAL'
              }
              color={summary.currentStock <= 0 ? 'error' : summary.currentStock <= (loadedItem?.safeStock ?? 0) ? 'warning' : 'success'}
              size="small"
            />
          </Stack>
        </Paper>
      )}

      {!isNew && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Transaction History
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Date</TableCell>
                <TableCell>Type</TableCell>
                <TableCell align="right">Qty</TableCell>
                <TableCell>Employee</TableCell>
                <TableCell>Machine</TableCell>
                <TableCell>Remark</TableCell>
                <TableCell>By</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {history.map((t) => (
                <TableRow key={t.id}>
                  <TableCell>{t.txnDate}</TableCell>
                  <TableCell>{t.txnType}</TableCell>
                  <TableCell align="right">{t.quantity}</TableCell>
                  <TableCell>{t.employeeName ?? '-'}</TableCell>
                  <TableCell>{t.machineCode ?? '-'}</TableCell>
                  <TableCell>{t.remark ?? t.purpose ?? '-'}</TableCell>
                  <TableCell>{t.performedByUsername}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}
    </>
  )
}
