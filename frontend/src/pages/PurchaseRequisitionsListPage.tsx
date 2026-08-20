import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
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
import { PurchaseRequisitionsApi } from '../api/endpoints'
import type { PurchaseRequisition } from '../api/types'
import { useAuth } from '../auth/AuthContext'

const STATUS_COLOR: Record<string, 'default' | 'info' | 'success' | 'error' | 'warning'> = {
  DRAFT: 'default',
  SUBMITTED: 'info',
  APPROVED: 'success',
  REJECTED: 'error',
  ORDERED: 'warning',
  RECEIVED: 'success',
  CLOSED: 'default',
}

export function PurchaseRequisitionsListPage() {
  const navigate = useNavigate()
  const { isAdmin, isIssuer } = useAuth()
  const [rows, setRows] = useState<PurchaseRequisition[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [status, setStatus] = useState('')

  useEffect(() => {
    PurchaseRequisitionsApi.search({ status: status || undefined, page, size }).then((res) => {
      setRows(res.data.content)
      setTotal(res.data.totalElements)
    })
  }, [status, page, size])

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 600 }}>
          Purchase Requisitions
        </Typography>
        {(isAdmin || isIssuer) && (
          <Button variant="contained" onClick={() => navigate('/purchase-requisitions/new')}>
            New Requisition
          </Button>
        )}
      </Box>
      <TextField
        select
        label="Status"
        size="small"
        value={status}
        onChange={(e) => {
          setPage(0)
          setStatus(e.target.value)
        }}
        sx={{ minWidth: 180, mb: 2 }}
      >
        <MenuItem value="">All</MenuItem>
        {['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'ORDERED', 'RECEIVED', 'CLOSED'].map((s) => (
          <MenuItem key={s} value={s}>
            {s}
          </MenuItem>
        ))}
      </TextField>
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>PR No.</TableCell>
              <TableCell>Requested By</TableCell>
              <TableCell>Department</TableCell>
              <TableCell>Priority</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Lines</TableCell>
              <TableCell>Created</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((pr) => (
              <TableRow key={pr.id} hover onClick={() => navigate(`/purchase-requisitions/${pr.id}`)} sx={{ cursor: 'pointer' }}>
                <TableCell>{pr.prNo}</TableCell>
                <TableCell>{pr.requestedByUsername}</TableCell>
                <TableCell>{pr.departmentName ?? '-'}</TableCell>
                <TableCell>{pr.priority ?? '-'}</TableCell>
                <TableCell>
                  <Chip label={pr.status} color={STATUS_COLOR[pr.status]} size="small" />
                </TableCell>
                <TableCell align="right">{pr.items.length}</TableCell>
                <TableCell>{new Date(pr.createdAt).toLocaleDateString()}</TableCell>
              </TableRow>
            ))}
            {rows.length === 0 && (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  No purchase requisitions found.
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
