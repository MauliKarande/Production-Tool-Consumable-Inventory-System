import { useState } from 'react'
import { CreatableSelect } from '../CreatableSelect'
import { QuickAddDialog } from '../QuickAddDialog'
import { MachinesApi } from '../../api/endpoints'
import { useCreatableEntity } from './useCreatableEntity'

interface Props {
  value: number | null
  onChange: (id: number | null) => void
  canCreate: boolean
  required?: boolean
  error?: string
  label?: string
}

export function MachineSelect({ value, onChange, canCreate, required, error, label = 'Machine / CNC' }: Props) {
  const { options, reload } = useCreatableEntity(
    () => MachinesApi.list(0, 200).then((r) => r.data.content),
    (m) => ({ id: m.id, label: m.machineName }),
  )
  const [dialogOpen, setDialogOpen] = useState(false)
  const [typed, setTyped] = useState('')

  return (
    <>
      <CreatableSelect
        label={label}
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
        title="Add Machine"
        fields={[
          { name: 'machineName', label: 'Machine Name (e.g. CNC-10)', required: true },
          { name: 'machineCode', label: 'Machine Code', required: true },
        ]}
        initialValue={typed}
        onCancel={() => setDialogOpen(false)}
        onSubmit={async (values) => {
          const res = await MachinesApi.create({
            machineCode: values.machineCode,
            machineName: values.machineName,
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
