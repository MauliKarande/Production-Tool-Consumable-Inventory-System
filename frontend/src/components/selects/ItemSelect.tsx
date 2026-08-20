import { useEffect, useState } from 'react'
import { CreatableSelect } from '../CreatableSelect'
import { ItemsApi } from '../../api/endpoints'
import type { SelectOption } from '../CreatableSelect'

interface Props {
  value: number | null
  onChange: (id: number | null) => void
  required?: boolean
  error?: string
}

/** Item creation is a full form, not a quick-add, so this select never offers "+ Add New". */
export function ItemSelect({ value, onChange, required, error }: Props) {
  const [options, setOptions] = useState<SelectOption[]>([])

  useEffect(() => {
    ItemsApi.search({ active: true, size: 500 }).then((res) =>
      setOptions(res.data.content.map((i) => ({ id: i.id, label: `${i.itemCode} - ${i.name}` }))),
    )
  }, [])

  return (
    <CreatableSelect
      label="Item"
      options={options}
      value={value}
      onChange={onChange}
      canCreate={false}
      onCreateNew={() => {}}
      required={required}
      error={error}
    />
  )
}
