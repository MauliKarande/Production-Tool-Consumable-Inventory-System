import { useCallback, useEffect, useState } from 'react'
import {
  Alert as MuiAlert,
  Box,
  Button,
  Chip,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import { AlertsApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { AlertResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'

const STATUS_COLOR: Record<string, 'error' | 'warning' | 'success'> = {
  OPEN: 'error',
  ACKNOWLEDGED: 'warning',
  RESOLVED: 'success',
}

const TYPE_LABEL: Record<string, string> = {
  LOW_STOCK: 'Low Stock',
  OUT_OF_STOCK: 'Out of Stock',
  HIGH_CONSUMPTION: 'High Consumption',
  PENDING_RETURN: 'Pending Return',
  UNUSUAL_CONSUMPTION: 'Unusual Consumption',
  PURCHASE_PENDING: 'Purchase Pending',
}

export function AlertsPage() {
  const { isAdmin, isIssuer } = useAuth()
  const [rows, setRows] = useState<AlertResponse[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [status, setStatus] = useState('OPEN')
  const [type, setType] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [recomputing, setRecomputing] = useState(false)

  const load = useCallback(() => {
    AlertsApi.list({ status: status || undefined, type: type || undefined, page, size })
      .then((res) => {
        setRows(res.data.content)
        setTotal(res.data.totalElements)
      })
      .catch((e) => setError(apiErrorMessage(e)))
  }, [status, type, page, size])

  useEffect(() => {
    load()
  }, [load])

  const act = (action: 'acknowledge' | 'resolve', id: number) => {
    const call = action === 'acknowledge' ? AlertsApi.acknowledge(id) : AlertsApi.resolve(id)
    call.then(load).catch((e) => setError(apiErrorMessage(e)))
  }

  const recompute = () => {
    setRecomputing(true)
    AlertsApi.recompute()
      .then(load)
      .catch((e) => setError(apiErrorMessage(e)))
      .finally(() => setRecomputing(false))
  }

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 600 }}>
          Alerts
        </Typography>
        {isAdmin && (
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={recompute} disabled={recomputing}>
            Recompute Now
          </Button>
        )}
      </Box>
      {error && (
        <MuiAlert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </MuiAlert>
      )}
      <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
        <TextField
          select
          label="Status"
          size="small"
          value={status}
          onChange={(e) => {
            setPage(0)
            setStatus(e.target.value)
          }}
          sx={{ minWidth: 160 }}
        >
          <MenuItem value="">All</MenuItem>
          <MenuItem value="OPEN">Open</MenuItem>
          <MenuItem value="ACKNOWLEDGED">Acknowledged</MenuItem>
          <MenuItem value="RESOLVED">Resolved</MenuItem>
        </TextField>
        <TextField
          select
          label="Type"
          size="small"
          value={type}
          onChange={(e) => {
            setPage(0)
            setType(e.target.value)
          }}
          sx={{ minWidth: 200 }}
        >
          <MenuItem value="">All</MenuItem>
          {Object.entries(TYPE_LABEL).map(([value, label]) => (
            <MenuItem key={value} value={value}>
              {label}
            </MenuItem>
          ))}
        </TextField>
      </Box>
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Type</TableCell>
              <TableCell>Item</TableCell>
              <TableCell>Message</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Raised</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((a) => (
              <TableRow key={a.id}>
                <TableCell>{TYPE_LABEL[a.type] ?? a.type}</TableCell>
                <TableCell>{a.itemCode ? `${a.itemCode} — ${a.itemName}` : '-'}</TableCell>
                <TableCell>{a.message}</TableCell>
                <TableCell>
                  <Chip label={a.status} color={STATUS_COLOR[a.status]} size="small" />
                </TableCell>
                <TableCell>{new Date(a.raisedAt).toLocaleString()}</TableCell>
                <TableCell align="right">
                  {(isAdmin || isIssuer) && a.status !== 'RESOLVED' && (
                    <>
                      {a.status === 'OPEN' && (
                        <Button size="small" onClick={() => act('acknowledge', a.id)}>
                          Acknowledge
                        </Button>
                      )}
                      <Button size="small" onClick={() => act('resolve', a.id)}>
                        Resolve
                      </Button>
                    </>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {rows.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  No alerts found.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={total}
          page={page}
          onPageChange={(_e, newPage) => setPage(newPage)}
          rowsPerPage={size}
          onRowsPerPageChange={(e) => {
            setSize(parseInt(e.target.value, 10))
            setPage(0)
          }}
        />
      </TableContainer>
    </>
  )
}
