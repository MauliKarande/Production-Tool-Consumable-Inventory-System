import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Button,
  Chip,
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
import AddIcon from '@mui/icons-material/Add'
import { ItemsApi } from '../api/endpoints'
import type { Item } from '../api/types'
import { useAuth } from '../auth/AuthContext'

export function ItemsListPage() {
  const { isAdmin } = useAuth()
  const navigate = useNavigate()
  const [items, setItems] = useState<Item[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [query, setQuery] = useState('')

  useEffect(() => {
    ItemsApi.search({ q: query || undefined, page, size }).then((res) => {
      setItems(res.data.content)
      setTotal(res.data.totalElements)
    })
  }, [query, page, size])

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 600 }}>
          Items
        </Typography>
        {isAdmin && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/items/new')}>
            New Item
          </Button>
        )}
      </Box>
      <TextField
        label="Search by code or name"
        value={query}
        onChange={(e) => {
          setPage(0)
          setQuery(e.target.value)
        }}
        size="small"
        sx={{ mb: 2, width: 320 }}
      />
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Item Code</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Category</TableCell>
              <TableCell>Manufacturer</TableCell>
              <TableCell align="right">Safe Stock</TableCell>
              <TableCell align="right">Unit Cost</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {items.map((item) => (
              <TableRow
                key={item.id}
                hover
                sx={{ cursor: 'pointer' }}
                onClick={() => navigate(`/items/${item.id}`)}
              >
                <TableCell>{item.itemCode}</TableCell>
                <TableCell>{item.name}</TableCell>
                <TableCell>{item.categoryName}</TableCell>
                <TableCell>{item.manufacturerName ?? '-'}</TableCell>
                <TableCell align="right">{item.safeStock}</TableCell>
                <TableCell align="right">₹{item.currentUnitCost}</TableCell>
                <TableCell>
                  <Chip label={item.active ? 'Active' : 'Inactive'} color={item.active ? 'success' : 'default'} size="small" />
                </TableCell>
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
