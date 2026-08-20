import { useState, type SyntheticEvent } from 'react'
import {
  Box,
  Button,
  Chip,
  Paper,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tabs,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import {
  DepartmentsApi,
  EmployeesApi,
  MachinesApi,
  ManufacturersApi,
  SuppliersApi,
  UomApi,
} from '../api/endpoints'
import { QuickAddDialog, type QuickAddField } from '../components/QuickAddDialog'
import { CategorySelect } from '../components/selects/CategorySelect'
import { useCreatableEntity } from '../components/selects/useCreatableEntity'

const TABS = ['Departments', 'Units of Measure', 'Manufacturers', 'Suppliers', 'Machines', 'Employees', 'Categories'] as const

export function MastersPage() {
  const [tab, setTab] = useState(0)

  return (
    <>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        Masters
      </Typography>
      <Tabs value={tab} onChange={(_e: SyntheticEvent, v: number) => setTab(v)} sx={{ mb: 2 }} variant="scrollable">
        {TABS.map((t) => (
          <Tab key={t} label={t} />
        ))}
      </Tabs>
      {tab === 0 && <DepartmentsTab />}
      {tab === 1 && <UomTab />}
      {tab === 2 && <ManufacturersTab />}
      {tab === 3 && <SuppliersTab />}
      {tab === 4 && <MachinesTab />}
      {tab === 5 && <EmployeesTab />}
      {tab === 6 && <CategoriesTab />}
    </>
  )
}

function MasterTable<T>({
  title,
  columns,
  rows,
  addFields,
  onAdd,
  renderRow,
}: {
  title: string
  columns: string[]
  rows: T[]
  addFields: QuickAddField[]
  onAdd: (values: Record<string, string>) => Promise<void>
  renderRow: (row: T) => React.ReactNode
}) {
  const [dialogOpen, setDialogOpen] = useState(false)
  return (
    <Paper sx={{ p: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">{title}</Typography>
        <Button startIcon={<AddIcon />} onClick={() => setDialogOpen(true)} variant="outlined" size="small">
          Add
        </Button>
      </Box>
      <Table size="small">
        <TableHead>
          <TableRow>
            {columns.map((c) => (
              <TableCell key={c}>{c}</TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row, idx) => (
            <TableRow key={idx}>{renderRow(row)}</TableRow>
          ))}
        </TableBody>
      </Table>
      <QuickAddDialog
        open={dialogOpen}
        title={`Add ${title.replace(/s$/, '')}`}
        fields={addFields}
        onCancel={() => setDialogOpen(false)}
        onSubmit={async (values) => {
          await onAdd(values)
          setDialogOpen(false)
        }}
      />
    </Paper>
  )
}

function ActiveChip({ active }: { active: boolean }) {
  return <Chip label={active ? 'Active' : 'Inactive'} color={active ? 'success' : 'default'} size="small" />
}

function DepartmentsTab() {
  const { items, reload } = useCreatableEntity(
    () => DepartmentsApi.list().then((r) => r.data),
    (d) => ({ id: d.id, label: d.name }),
  )
  return (
    <MasterTable
      title="Departments"
      columns={['Name', 'Status']}
      rows={items}
      addFields={[{ name: 'name', label: 'Name', required: true }]}
      onAdd={async (v) => {
        await DepartmentsApi.create({ name: v.name, active: true })
        await reload()
      }}
      renderRow={(d) => (
        <>
          <TableCell>{d.name}</TableCell>
          <TableCell>
            <ActiveChip active={d.active} />
          </TableCell>
        </>
      )}
    />
  )
}

function UomTab() {
  const { items, reload } = useCreatableEntity(
    () => UomApi.list().then((r) => r.data),
    (u) => ({ id: u.id, label: u.code }),
  )
  return (
    <MasterTable
      title="Units of Measure"
      columns={['Code', 'Name', 'Status']}
      rows={items}
      addFields={[
        { name: 'code', label: 'Code', required: true },
        { name: 'name', label: 'Name', required: true },
      ]}
      onAdd={async (v) => {
        await UomApi.create({ code: v.code, name: v.name, active: true })
        await reload()
      }}
      renderRow={(u) => (
        <>
          <TableCell>{u.code}</TableCell>
          <TableCell>{u.name}</TableCell>
          <TableCell>
            <ActiveChip active={u.active} />
          </TableCell>
        </>
      )}
    />
  )
}

function ManufacturersTab() {
  const { items, reload } = useCreatableEntity(
    () => ManufacturersApi.list().then((r) => r.data),
    (m) => ({ id: m.id, label: m.name }),
  )
  return (
    <MasterTable
      title="Manufacturers"
      columns={['Name', 'Status']}
      rows={items}
      addFields={[{ name: 'name', label: 'Name', required: true }]}
      onAdd={async (v) => {
        await ManufacturersApi.create({ name: v.name, active: true })
        await reload()
      }}
      renderRow={(m) => (
        <>
          <TableCell>{m.name}</TableCell>
          <TableCell>
            <ActiveChip active={m.active} />
          </TableCell>
        </>
      )}
    />
  )
}

function SuppliersTab() {
  const { items, reload } = useCreatableEntity(
    () => SuppliersApi.list().then((r) => r.data.content),
    (s) => ({ id: s.id, label: s.name }),
  )
  return (
    <MasterTable
      title="Suppliers"
      columns={['Name', 'Contact', 'Phone', 'Status']}
      rows={items}
      addFields={[
        { name: 'name', label: 'Name', required: true },
        { name: 'contactPerson', label: 'Contact Person' },
        { name: 'phone', label: 'Phone' },
      ]}
      onAdd={async (v) => {
        await SuppliersApi.create({ name: v.name, contactPerson: v.contactPerson, phone: v.phone, active: true })
        await reload()
      }}
      renderRow={(s) => (
        <>
          <TableCell>{s.name}</TableCell>
          <TableCell>{s.contactPerson ?? '-'}</TableCell>
          <TableCell>{s.phone ?? '-'}</TableCell>
          <TableCell>
            <ActiveChip active={s.active} />
          </TableCell>
        </>
      )}
    />
  )
}

function MachinesTab() {
  const { items, reload } = useCreatableEntity(
    () => MachinesApi.list().then((r) => r.data.content),
    (m) => ({ id: m.id, label: m.machineName }),
  )
  return (
    <MasterTable
      title="Machines"
      columns={['Code', 'Name', 'Type', 'Status']}
      rows={items}
      addFields={[
        { name: 'machineCode', label: 'Machine Code', required: true },
        { name: 'machineName', label: 'Machine Name', required: true },
        { name: 'machineType', label: 'Type (e.g. CNC, VMC)' },
      ]}
      onAdd={async (v) => {
        await MachinesApi.create({
          machineCode: v.machineCode,
          machineName: v.machineName,
          machineType: v.machineType,
          active: true,
        })
        await reload()
      }}
      renderRow={(m) => (
        <>
          <TableCell>{m.machineCode}</TableCell>
          <TableCell>{m.machineName}</TableCell>
          <TableCell>{m.machineType ?? '-'}</TableCell>
          <TableCell>
            <ActiveChip active={m.active} />
          </TableCell>
        </>
      )}
    />
  )
}

function EmployeesTab() {
  const { items, reload } = useCreatableEntity(
    () => EmployeesApi.list().then((r) => r.data.content),
    (e) => ({ id: e.id, label: e.name }),
  )
  return (
    <MasterTable
      title="Employees"
      columns={['Code', 'Name', 'Department', 'Status']}
      rows={items}
      addFields={[
        { name: 'employeeCode', label: 'Employee Code', required: true },
        { name: 'name', label: 'Name', required: true },
        { name: 'designation', label: 'Designation' },
      ]}
      onAdd={async (v) => {
        await EmployeesApi.create({
          employeeCode: v.employeeCode,
          name: v.name,
          departmentId: null,
          designation: v.designation,
          active: true,
        })
        await reload()
      }}
      renderRow={(e) => (
        <>
          <TableCell>{e.employeeCode}</TableCell>
          <TableCell>{e.name}</TableCell>
          <TableCell>{e.departmentName ?? '-'}</TableCell>
          <TableCell>
            <ActiveChip active={e.active} />
          </TableCell>
        </>
      )}
    />
  )
}

function CategoriesTab() {
  const [selected, setSelected] = useState<number | null>(null)
  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Item Categories
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Categories are configurable — use the dropdown below (or its "+ Add" option) to view or create one. Per-category
        custom attributes (e.g. Diameter for Drills) can be added from the API for now.
      </Typography>
      <Box sx={{ maxWidth: 320 }}>
        <CategorySelect value={selected} onChange={setSelected} canCreate />
      </Box>
    </Paper>
  )
}
