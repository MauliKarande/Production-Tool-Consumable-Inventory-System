import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material'
import { apiErrorMessage } from '../api/client'

export interface QuickAddField {
  name: string
  label: string
  required?: boolean
}

interface QuickAddDialogProps {
  open: boolean
  title: string
  fields: QuickAddField[]
  initialValue?: string
  onCancel: () => void
  onSubmit: (values: Record<string, string>) => Promise<void>
}

export function QuickAddDialog({ open, title, fields, initialValue, onCancel, onSubmit }: QuickAddDialogProps) {
  const [values, setValues] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      const first = fields[0]?.name
      setValues(first ? { [first]: initialValue ?? '' } : {})
      setError(null)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  const handleSubmit = async () => {
    setSubmitting(true)
    setError(null)
    try {
      await onSubmit(values)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onClose={submitting ? undefined : onCancel} fullWidth maxWidth="xs">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          {fields.map((field) => (
            <TextField
              key={field.name}
              label={field.label}
              required={field.required}
              value={values[field.name] ?? ''}
              onChange={(e) => setValues((prev) => ({ ...prev, [field.name]: e.target.value }))}
              fullWidth
              autoFocus={field === fields[0]}
            />
          ))}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button onClick={handleSubmit} variant="contained" disabled={submitting}>
          Add
        </Button>
      </DialogActions>
    </Dialog>
  )
}
