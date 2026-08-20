import { useEffect, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper,
  Stack,
  Switch,
  FormControlLabel,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  TextField,
  Typography,
} from '@mui/material'
import { apiErrorMessage } from '../api/client'
import { UsersApi } from '../api/endpoints'
import type { AppUser } from '../api/types'
import { EmployeeSelect } from '../components/selects/EmployeeSelect'

const ROLES = ['ADMIN', 'ISSUER', 'VIEWER']

export function UsersPage() {
  const [rows, setRows] = useState<AppUser[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [createOpen, setCreateOpen] = useState(false)
  const [editUser, setEditUser] = useState<AppUser | null>(null)
  const [resetUser, setResetUser] = useState<AppUser | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = () =>
    UsersApi.list(page, size)
      .then((res) => {
        setRows(res.data.content)
        setTotal(res.data.totalElements)
      })
      .catch((e) => setError(apiErrorMessage(e)))

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size])

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 600 }}>
          Users
        </Typography>
        <Button variant="contained" onClick={() => setCreateOpen(true)}>
          New User
        </Button>
      </Box>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Username</TableCell>
              <TableCell>Employee</TableCell>
              <TableCell>Role</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Last Login</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((u) => (
              <TableRow key={u.id}>
                <TableCell>{u.username}</TableCell>
                <TableCell>{u.employeeName ?? '-'}</TableCell>
                <TableCell>{u.roleName}</TableCell>
                <TableCell>
                  <Chip label={u.active ? 'Active' : 'Disabled'} color={u.active ? 'success' : 'default'} size="small" />
                </TableCell>
                <TableCell>{u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString() : 'Never'}</TableCell>
                <TableCell align="right">
                  <Button size="small" onClick={() => setEditUser(u)}>
                    Edit
                  </Button>
                  <Button size="small" onClick={() => setResetUser(u)}>
                    Reset Password
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {rows.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  No users found.
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

      {createOpen && (
        <CreateUserDialog
          onClose={() => setCreateOpen(false)}
          onCreated={() => {
            setCreateOpen(false)
            load()
          }}
        />
      )}
      {editUser && (
        <EditUserDialog
          user={editUser}
          onClose={() => setEditUser(null)}
          onSaved={() => {
            setEditUser(null)
            load()
          }}
        />
      )}
      {resetUser && <ResetPasswordDialog user={resetUser} onClose={() => setResetUser(null)} />}
    </>
  )
}

function CreateUserDialog({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [employeeId, setEmployeeId] = useState<number | null>(null)
  const [roleName, setRoleName] = useState('ISSUER')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async () => {
    setError(null)
    if (!username || password.length < 8) {
      setError('Username is required and password must be at least 8 characters.')
      return
    }
    setSubmitting(true)
    try {
      await UsersApi.create({ username, password, employeeId, roleName })
      onCreated()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>New User</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField label="Username" value={username} onChange={(e) => setUsername(e.target.value)} required autoFocus />
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            helperText="At least 8 characters"
          />
          <EmployeeSelect value={employeeId} onChange={setEmployeeId} canCreate={false} />
          <TextField select label="Role" value={roleName} onChange={(e) => setRoleName(e.target.value)}>
            {ROLES.map((r) => (
              <MenuItem key={r} value={r}>
                {r}
              </MenuItem>
            ))}
          </TextField>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={submitting}>
          Create
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function EditUserDialog({ user, onClose, onSaved }: { user: AppUser; onClose: () => void; onSaved: () => void }) {
  const [employeeId, setEmployeeId] = useState<number | null>(user.employeeId)
  const [roleName, setRoleName] = useState(user.roleName)
  const [active, setActive] = useState(user.active)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async () => {
    setError(null)
    setSubmitting(true)
    try {
      await UsersApi.update(user.id, { employeeId, roleName, active })
      onSaved()
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Edit {user.username}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <EmployeeSelect value={employeeId} onChange={setEmployeeId} canCreate={false} />
          <TextField select label="Role" value={roleName} onChange={(e) => setRoleName(e.target.value)}>
            {ROLES.map((r) => (
              <MenuItem key={r} value={r}>
                {r}
              </MenuItem>
            ))}
          </TextField>
          <FormControlLabel control={<Switch checked={active} onChange={(e) => setActive(e.target.checked)} />} label="Active" />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={submitting}>
          Save
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function ResetPasswordDialog({ user, onClose }: { user: AppUser; onClose: () => void }) {
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async () => {
    setError(null)
    if (password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }
    setSubmitting(true)
    try {
      await UsersApi.resetPassword(user.id, password)
      setSuccess(true)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Reset Password — {user.username}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          {success ? (
            <Alert severity="success">Password reset. Share the new password with {user.username} securely.</Alert>
          ) : (
            <TextField
              label="New Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              helperText="At least 8 characters"
              autoFocus
            />
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{success ? 'Close' : 'Cancel'}</Button>
        {!success && (
          <Button variant="contained" onClick={handleSubmit} disabled={submitting}>
            Reset
          </Button>
        )}
      </DialogActions>
    </Dialog>
  )
}
