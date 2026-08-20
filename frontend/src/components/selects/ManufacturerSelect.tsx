import { useState } from 'react'
import { CreatableSelect } from '../CreatableSelect'
import { QuickAddDialog } from '../QuickAddDialog'
import { ManufacturersApi } from '../../api/endpoints'
import { useCreatableEntity } from './useCreatableEntity'

interface Props {
  value: number | null
  onChange: (id: number | null) => void
  canCreate: boolean
  required?: boolean
  error?: string
}

export function ManufacturerSelect({ value, onChange, canCreate, required, error }: Props) {
  const { options, reload } = useCreatableEntity(
    () => ManufacturersApi.list().then((r) => r.data),
    (m) => ({ id: m.id, label: m.name }),
  )
  const [dialogOpen, setDialogOpen] = useState(false)
  const [typed, setTyped] = useState('')

  return (
    <>
      <CreatableSelect
        label="Manufacturer"
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
        title="Add Manufacturer"
        fields={[{ name: 'name', label: 'Name', required: true }]}
        initialValue={typed}
        onCancel={() => setDialogOpen(false)}
        onSubmit={async (values) => {
          const res = await ManufacturersApi.create({ name: values.name, active: true })
          await reload()
          onChange(res.data.id)
          setDialogOpen(false)
        }}
      />
    </>
  )
}
