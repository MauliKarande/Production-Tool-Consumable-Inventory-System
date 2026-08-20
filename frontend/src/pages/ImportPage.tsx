import { useRef, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import { ImportApi } from '../api/endpoints'
import { apiErrorMessage } from '../api/client'
import type { ImportResult } from '../api/types'

const FILE_TYPES = [
  { value: 'CONSUMPTION_SHEET', label: 'Insert / Drills / Taps consumption sheet (.xlsx)' },
  { value: 'OIL_CONSUMABLE', label: 'Machine oil & general consumables log (.xlsx)' },
  { value: 'PURCHASE_REQUISITION', label: 'Purchase requisition slip file (.xls / .xlsx)' },
]

export function ImportPage() {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [fileType, setFileType] = useState('CONSUMPTION_SHEET')
  const [previewResult, setPreviewResult] = useState<ImportResult | null>(null)
  const [commitResult, setCommitResult] = useState<ImportResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const reset = () => {
    setPreviewResult(null)
    setCommitResult(null)
    setError(null)
  }

  const handlePreview = async () => {
    if (!file) return
    reset()
    setBusy(true)
    try {
      const res = await ImportApi.preview(file, fileType)
      setPreviewResult(res.data)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const handleCommit = async () => {
    if (!file) return
    setBusy(true)
    setError(null)
    try {
      const res = await ImportApi.commit(file, fileType)
      setCommitResult(res.data)
      setPreviewResult(null)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const result = commitResult ?? previewResult

  return (
    <>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        Excel Data Import
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        One-time migration of the legacy Excel tracking sheets into the system. Preview runs the full import and shows
        exactly what it would do, then rolls everything back — nothing is written until you confirm.
      </Typography>

      <Paper sx={{ p: 3, maxWidth: 720, mb: 3 }}>
        <Stack spacing={2}>
          <TextField select label="File Type" value={fileType} onChange={(e) => { setFileType(e.target.value); reset(); setFile(null) }}>
            {FILE_TYPES.map((t) => (
              <MenuItem key={t.value} value={t.value}>
                {t.label}
              </MenuItem>
            ))}
          </TextField>

          <input
            ref={fileInputRef}
            type="file"
            accept=".xlsx,.xls"
            style={{ display: 'none' }}
            onChange={(e) => {
              setFile(e.target.files?.[0] ?? null)
              reset()
            }}
          />
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Button variant="outlined" startIcon={<UploadFileIcon />} onClick={() => fileInputRef.current?.click()}>
              Choose File
            </Button>
            {file && <Chip label={file.name} onDelete={() => { setFile(null); reset() }} />}
          </Box>

          {error && <Alert severity="error">{error}</Alert>}

          <Stack direction="row" spacing={2}>
            <Button variant="outlined" onClick={handlePreview} disabled={!file || busy}>
              Preview (dry run)
            </Button>
            <Button
              variant="contained"
              onClick={handleCommit}
              disabled={!previewResult || previewResult.errors.length > 0 || busy || !file}
            >
              Confirm Import
            </Button>
          </Stack>
        </Stack>
      </Paper>

      {result && (
        <Paper sx={{ p: 3, maxWidth: 900 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">{commitResult ? 'Import Committed' : 'Preview (nothing written yet)'}</Typography>
            <Chip
              label={commitResult ? 'COMMITTED' : 'DRY RUN'}
              color={commitResult ? 'success' : 'info'}
            />
          </Box>

          <Stack direction="row" spacing={3} sx={{ mb: 2, flexWrap: 'wrap' }}>
            <Stat label="Manufacturers created" value={result.manufacturersCreated} />
            <Stat label="Suppliers created" value={result.suppliersCreated} />
            <Stat label="Items created" value={result.itemsCreated} />
            <Stat label="Machines created" value={result.machinesCreated} />
            <Stat label="Requisitions created" value={result.purchaseRequisitionsCreated} />
            <Stat label="Ledger transactions posted" value={result.transactionsPosted} />
          </Stack>

          {result.errors.length > 0 && (
            <Alert severity="error" sx={{ mb: 2 }}>
              <Typography variant="subtitle2">Errors (import stopped)</Typography>
              <List dense>
                {result.errors.map((e, i) => (
                  <ListItem key={i}>
                    <ListItemText primary={e} />
                  </ListItem>
                ))}
              </List>
            </Alert>
          )}

          {result.warnings.length > 0 && (
            <Alert severity="warning">
              <Typography variant="subtitle2">Warnings / reconciliation notes ({result.warnings.length})</Typography>
              <List dense sx={{ maxHeight: 400, overflow: 'auto' }}>
                {result.warnings.map((w, i) => (
                  <ListItem key={i}>
                    <ListItemText primary={w} />
                  </ListItem>
                ))}
              </List>
            </Alert>
          )}

          {previewResult && !commitResult && result.errors.length === 0 && (
            <Box sx={{ mt: 2 }}>
              <Button variant="contained" onClick={handleCommit} disabled={busy}>
                Looks good — Confirm Import
              </Button>
            </Box>
          )}
        </Paper>
      )}
    </>
  )
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <Box>
      <Typography variant="overline" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="h5" sx={{ fontWeight: 700 }}>
        {value}
      </Typography>
    </Box>
  )
}
