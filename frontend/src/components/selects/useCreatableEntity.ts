import { useCallback, useEffect, useState } from 'react'
import type { SelectOption } from '../CreatableSelect'

export function useCreatableEntity<T>(fetchList: () => Promise<T[]>, toOption: (item: T) => SelectOption) {
  const [items, setItems] = useState<T[]>([])
  const [loading, setLoading] = useState(true)

  const reload = useCallback(async () => {
    setLoading(true)
    try {
      setItems(await fetchList())
    } finally {
      setLoading(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    reload()
  }, [reload])

  return { items, options: items.map(toOption), loading, reload }
}
