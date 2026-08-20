import { useState } from 'react'
import { CreatableSelect } from '../CreatableSelect'
import { QuickAddDialog } from '../QuickAddDialog'
import { UomApi } from '../../api/endpoints'
import { useCreatableEntity } from './useCreatableEntity'

interface Props {
  value: number | null
  onChange: (id: number | null) => void
  canCreate: boolean
  required?: boolean
  error?: string
}

export function UomSelect({ value, onChange, canCreate, required, error }: Props) {
  const { options, reload } = useCreatableEntity(
    () => UomApi.list().then((r) => r.data),
    (u) => ({ id: u.id, label: `${u.code} - ${u.name}` }),
  )
  const [dialogOpen, setDialogOpen] = useState(false)
  const [typed, setTyped] = useState('')

  return (
    <>
      <CreatableSelect
        label="Unit of Measure"
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
        title="Add Unit of Measure"
        fields={[
          { name: 'code', label: 'Code (e.g. PCS)', required: true },
          { name: 'name', label: 'Name (e.g. Pieces)', required: true },
        ]}
        initialValue={typed}
        onCancel={() => setDialogOpen(false)}
        onSubmit={async (values) => {
          const res = await UomApi.create({ code: values.code, name: values.name || values.code, active: true })
          await reload()
          onChange(res.data.id)
          setDialogOpen(false)
        }}
      />
    </>
  )
}
