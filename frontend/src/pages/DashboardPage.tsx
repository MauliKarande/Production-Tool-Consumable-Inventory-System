import { useEffect, useState } from 'react'
import { Card, CardActionArea, CardContent, Grid, Typography } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { ItemsApi } from '../api/endpoints'
import { useAuth } from '../auth/AuthContext'

export function DashboardPage() {
  const { auth } = useAuth()
  const navigate = useNavigate()
  const [itemCount, setItemCount] = useState<number | null>(null)

  useEffect(() => {
    ItemsApi.search({ page: 0, size: 1 }).then((res) => setItemCount(res.data.totalElements))
  }, [])

  const cards = [
    { title: 'Items', value: itemCount ?? '...', path: '/items', hint: 'Browse the item master, stock & lifecycle' },
    { title: 'Issue / Return', value: '→', path: '/issue', hint: 'Issue tools to production, or process a return' },
    { title: 'Accountability', value: '→', path: '/accountability', hint: 'See who currently has what' },
  ]

  return (
    <>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        Welcome, {auth?.username}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Role: {auth?.role}. Full KPI dashboard and charts are coming in a later phase — for now, use the quick links below.
      </Typography>
      <Grid container spacing={2}>
        {cards.map((card) => (
          <Grid size={{ xs: 12, sm: 6, md: 4 }} key={card.title}>
            <Card>
              <CardActionArea onClick={() => navigate(card.path)} sx={{ p: 2 }}>
                <CardContent>
                  <Typography variant="overline" color="text.secondary">
                    {card.title}
                  </Typography>
                  <Typography variant="h4" sx={{ fontWeight: 700 }}>
                    {card.value}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {card.hint}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </>
  )
}
