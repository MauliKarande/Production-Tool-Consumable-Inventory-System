import { useState } from 'react'
import { CreatableSelect } from '../CreatableSelect'
import { QuickAddDialog } from '../QuickAddDialog'
import { EmployeesApi } from '../../api/endpoints'
import { useCreatableEntity } from './useCreatableEntity'

interface Props {
  value: number | null
  onChange: (id: number | null) => void
  canCreate: boolean
  required?: boolean
  error?: string
}

export function EmployeeSelect({ value, onChange, canCreate, required, error }: Props) {
  const { options, reload } = useCreatableEntity(
    () => EmployeesApi.list(0, 200).then((r) => r.data.content),
    (e) => ({ id: e.id, label: `${e.name} (${e.employeeCode})` }),
  )
  const [dialogOpen, setDialogOpen] = useState(false)
  const [typed, setTyped] = useState('')

  return (
    <>
      <CreatableSelect
        label="Employee"
        options={options}
        value={value}
        onChange={onChange}
        canCreate={canCreate}
        required={required}
        error={error}
        onCreateNew={(text) => {
          setTyped(text)
          setDialogOpen(true)
        }}
      />
      <QuickAddDialog
        open={dialogOpen}
        title="Add Employee"
        fields={[
          { name: 'name', label: 'Name', required: true },
          { name: 'employeeCode', label: 'Employee Code', required: true },
        ]}
        initialValue={typed}
        onCancel={() => setDialogOpen(false)}
        onSubmit={async (values) => {
          const res = await EmployeesApi.create({
            employeeCode: values.employeeCode,
            name: values.name,
            departmentId: null,
            active: true,
          })
          await reload()
          onChange(res.data.id)
          setDialogOpen(false)
        }}
      />
    </>
  )
}
