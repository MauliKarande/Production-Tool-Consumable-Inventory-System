import { useState } from 'react'
import { CreatableSelect } from '../CreatableSelect'
import { QuickAddDialog } from '../QuickAddDialog'
import { SuppliersApi } from '../../api/endpoints'
import { useCreatableEntity } from './useCreatableEntity'

interface Props {
  value: number | null
  onChange: (id: number | null) => void
  canCreate: boolean
  required?: boolean
  error?: string
}

export function SupplierSelect({ value, onChange, canCreate, required, error }: Props) {
  const { options, reload } = useCreatableEntity(
    () => SuppliersApi.list().then((r) => r.data.content),
    (s) => ({ id: s.id, label: s.name }),
  )
  const [dialogOpen, setDialogOpen] = useState(false)
  const [typed, setTyped] = useState('')

  return (
    <>
      <CreatableSelect
        label="Supplier"
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
        title="Add Supplier"
        fields={[
          { name: 'name', label: 'Name', required: true },
          { name: 'contactPerson', label: 'Contact Person' },
          { name: 'phone', label: 'Phone' },
        ]}
        initialValue={typed}
        onCancel={() => setDialogOpen(false)}
        onSubmit={async (values) => {
          const res = await SuppliersApi.create({
            name: values.name,
            contactPerson: values.contactPerson || undefined,
            phone: values.phone || undefined,
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
