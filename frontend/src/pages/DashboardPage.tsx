import { useEffect, useState } from 'react'
import { Card, CardActionArea, CardContent, Grid, Paper, Typography } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { BarChart } from '@mui/x-charts/BarChart'
import { PieChart } from '@mui/x-charts/PieChart'
import { ConsumptionApi, ReportsApi } from '../api/endpoints'
import type { CategoryConsumption, DashboardSummary, MachineConsumption } from '../api/types'
import { useAuth } from '../auth/AuthContext'

function money(n: number): string {
  return `₹${n.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`
}

function firstOfMonth(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

export function DashboardPage() {
  const { auth } = useAuth()
  const navigate = useNavigate()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [machines, setMachines] = useState<MachineConsumption[]>([])
  const [categories, setCategories] = useState<CategoryConsumption[]>([])

  useEffect(() => {
    ReportsApi.dashboardSummary().then((res) => setSummary(res.data))
    ConsumptionApi.allMachines(firstOfMonth(), today()).then((res) => setMachines(res.data))
    ConsumptionApi.byCategory(firstOfMonth(), today()).then((res) => setCategories(res.data))
  }, [])

  const cards = [
    { title: 'Items', value: summary?.itemCount ?? '...', path: '/items' },
    { title: 'Stock Value', value: summary ? money(summary.totalStockValue) : '...', path: '/reports' },
    { title: 'Low Stock', value: summary?.lowStockCount ?? '...', path: '/reports', warn: (summary?.lowStockCount ?? 0) > 0 },
    { title: 'Out of Stock', value: summary?.outOfStockCount ?? '...', path: '/reports', warn: (summary?.outOfStockCount ?? 0) > 0 },
    { title: 'Open Alerts', value: summary?.openAlertCount ?? '...', path: '/alerts', warn: (summary?.openAlertCount ?? 0) > 0 },
    { title: 'Open Requisitions', value: summary?.openPrCount ?? '...', path: '/purchase-requisitions' },
    { title: 'This Month Consumption', value: summary ? money(summary.thisMonthConsumptionValue) : '...', path: '/consumption' },
  ]

  return (
    <>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        Welcome, {auth?.username}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Role: {auth?.role}
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {cards.map((card) => (
          <Grid size={{ xs: 12, sm: 6, md: 3 }} key={card.title}>
            <Card>
              <CardActionArea onClick={() => navigate(card.path)} sx={{ p: 2 }}>
                <CardContent sx={{ p: '8px !important' }}>
                  <Typography variant="overline" color="text.secondary">
                    {card.title}
                  </Typography>
                  <Typography variant="h5" sx={{ fontWeight: 700 }} color={card.warn ? 'error' : undefined}>
                    {card.value}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Machine-wise Consumption (this month)
            </Typography>
            {machines.length > 0 ? (
              <BarChart
                height={280}
                xAxis={[{ scaleType: 'band', data: machines.map((m) => m.machineCode) }]}
                series={[{ data: machines.map((m) => m.value), label: 'Value (₹)' }]}
              />
            ) : (
              <Typography variant="body2" color="text.secondary">
                No consumption recorded this month yet.
              </Typography>
            )}
          </Paper>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Category-wise Consumption (this month)
            </Typography>
            {categories.length > 0 ? (
              <PieChart
                height={280}
                series={[
                  {
                    data: categories.map((c) => ({ id: c.categoryId, value: c.value, label: c.categoryName })),
                    highlightScope: { fade: 'global', highlight: 'item' },
                  },
                ]}
              />
            ) : (
              <Typography variant="body2" color="text.secondary">
                No consumption recorded this month yet.
              </Typography>
            )}
          </Paper>
        </Grid>
      </Grid>
    </>
  )
}
