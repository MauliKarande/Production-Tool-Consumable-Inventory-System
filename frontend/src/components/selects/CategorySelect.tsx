import { useState } from 'react'
import { CreatableSelect } from '../CreatableSelect'
import { QuickAddDialog } from '../QuickAddDialog'
import { CategoriesApi } from '../../api/endpoints'
import { useCreatableEntity } from './useCreatableEntity'

interface Props {
  value: number | null
  onChange: (id: number | null) => void
  canCreate: boolean
  required?: boolean
  error?: string
}

export function CategorySelect({ value, onChange, canCreate, required, error }: Props) {
  const { options, reload } = useCreatableEntity(
    () => CategoriesApi.list().then((r) => r.data),
    (c) => ({ id: c.id, label: c.name }),
  )
  const [dialogOpen, setDialogOpen] = useState(false)
  const [typed, setTyped] = useState('')

  return (
    <>
      <CreatableSelect
        label="Category"
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
        title="Add Item Category"
        fields={[{ name: 'name', label: 'Name (e.g. INSERTS, DRILLS)', required: true }]}
        initialValue={typed}
        onCancel={() => setDialogOpen(false)}
        onSubmit={async (values) => {
          const res = await CategoriesApi.create({ name: values.name, parentCategoryId: null, active: true })
          await reload()
          onChange(res.data.id)
          setDialogOpen(false)
        }}
      />
    </>
  )
}
