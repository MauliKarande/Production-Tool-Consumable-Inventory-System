import { useEffect, useState } from 'react'
import {
  Chip,
  FormControlLabel,
  Paper,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Typography,
} from '@mui/material'
import { AccountabilityApi } from '../api/endpoints'
import type { AssignmentResponse } from '../api/types'

const STATUS_COLOR: Record<string, 'warning' | 'info' | 'success'> = {
  ASSIGNED: 'warning',
  PARTIALLY_RETURNED: 'info',
  CLOSED: 'success',
}

export function AccountabilityPage() {
  const [rows, setRows] = useState<AssignmentResponse[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [openOnly, setOpenOnly] = useState(true)

  useEffect(() => {
    AccountabilityApi.search({ openOnly, page, size }).then((res) => {
      setRows(res.data.content)
      setTotal(res.data.totalElements)
    })
  }, [openOnly, page, size])

  return (
    <>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        Accountability — Who Has What
      </Typography>
      <FormControlLabel
        control={
          <Switch
            checked={openOnly}
            onChange={(e) => {
              setPage(0)
              setOpenOnly(e.target.checked)
            }}
          />
        }
        label="Currently assigned only"
        sx={{ mb: 2 }}
      />
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Employee</TableCell>
              <TableCell>Item</TableCell>
              <TableCell>Machine</TableCell>
              <TableCell align="right">Assigned</TableCell>
              <TableCell align="right">Returned</TableCell>
              <TableCell align="right">Remaining</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Opened</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((a) => (
              <TableRow key={a.id}>
                <TableCell>{a.employeeName}</TableCell>
                <TableCell>
                  {a.itemCode} - {a.itemName}
                </TableCell>
                <TableCell>{a.machineCode ?? '-'}</TableCell>
                <TableCell align="right">{a.assignedQty}</TableCell>
                <TableCell align="right">{a.returnedQty}</TableCell>
                <TableCell align="right">{a.remainingQty}</TableCell>
                <TableCell>
                  <Chip label={a.status} color={STATUS_COLOR[a.status]} size="small" />
                </TableCell>
                <TableCell>{new Date(a.openedAt).toLocaleString()}</TableCell>
              </TableRow>
            ))}
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
