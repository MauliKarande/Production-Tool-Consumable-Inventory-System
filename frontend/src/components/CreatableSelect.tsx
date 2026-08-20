import { Autocomplete, TextField, createFilterOptions } from '@mui/material'

export interface SelectOption {
  id: number
  label: string
}

interface CreateOption extends SelectOption {
  __create__: true
  inputValue: string
}

const filter = createFilterOptions<SelectOption | CreateOption>()

interface CreatableSelectProps {
  label: string
  options: SelectOption[]
  value: number | null
  onChange: (id: number | null) => void
  /** Admin-only affordance: lets the user type a new name and add it without leaving the form. */
  canCreate: boolean
  onCreateNew: (typedText: string) => void
  disabled?: boolean
  required?: boolean
  error?: string
  helperText?: string
}

export function CreatableSelect({
  label,
  options,
  value,
  onChange,
  canCreate,
  onCreateNew,
  disabled,
  required,
  error,
  helperText,
}: CreatableSelectProps) {
  const selected = options.find((o) => o.id === value) ?? null

  return (
    <Autocomplete
      value={selected}
      disabled={disabled}
      options={options}
      getOptionLabel={(option) => ('label' in option ? option.label : '')}
      isOptionEqualToValue={(a, b) => a.id === b.id}
      onChange={(_event, newValue) => {
        if (newValue && (newValue as CreateOption).__create__) {
          onCreateNew((newValue as CreateOption).inputValue)
          return
        }
        onChange(newValue ? (newValue as SelectOption).id : null)
      }}
      filterOptions={(opts, params) => {
        const filtered = filter(opts, params)
        const inputValue = params.inputValue.trim()
        const exists = inputValue !== '' && opts.some((o) => o.label.toLowerCase() === inputValue.toLowerCase())
        if (canCreate && inputValue !== '' && !exists) {
          filtered.push({ id: -1, label: `+ Add "${inputValue}"`, __create__: true, inputValue })
        }
        return filtered
      }}
      renderInput={(params) => (
        <TextField {...params} label={label} required={required} error={!!error} helperText={error ?? helperText} />
      )}
    />
  )
}
