import { type ReactNode, useEffect, useState } from 'react'
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom'
import {
  AppBar,
  Badge,
  Box,
  Chip,
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  IconButton,
  Divider,
} from '@mui/material'
import DashboardIcon from '@mui/icons-material/Dashboard'
import InventoryIcon from '@mui/icons-material/Inventory2'
import OutboxIcon from '@mui/icons-material/Outbox'
import AssignmentIndIcon from '@mui/icons-material/AssignmentInd'
import BarChartIcon from '@mui/icons-material/BarChart'
import NotificationsIcon from '@mui/icons-material/Notifications'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'
import AssessmentIcon from '@mui/icons-material/Assessment'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import SettingsIcon from '@mui/icons-material/Settings'
import LogoutIcon from '@mui/icons-material/Logout'
import { useAuth } from '../../auth/AuthContext'
import { AlertsApi } from '../../api/endpoints'

const DRAWER_WIDTH = 240

const NAV_ITEMS = [
  { label: 'Dashboard', path: '/', icon: <DashboardIcon /> },
  { label: 'Items', path: '/items', icon: <InventoryIcon /> },
  { label: 'Issue / Return', path: '/issue', icon: <OutboxIcon /> },
  { label: 'Accountability', path: '/accountability', icon: <AssignmentIndIcon /> },
  { label: 'Consumption', path: '/consumption', icon: <BarChartIcon /> },
  { label: 'Purchase Requisitions', path: '/purchase-requisitions', icon: <ShoppingCartIcon /> },
  { label: 'Reports', path: '/reports', icon: <AssessmentIcon /> },
  { label: 'Alerts', path: '/alerts', icon: <NotificationsIcon />, badge: true },
  { label: 'Masters', path: '/masters', icon: <SettingsIcon />, adminOnly: true },
  { label: 'Import Data', path: '/import', icon: <UploadFileIcon />, adminOnly: true },
]

export function AppLayout({ children }: { children: ReactNode }) {
  const { auth, logout, isAdmin } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [openAlerts, setOpenAlerts] = useState(0)

  useEffect(() => {
    const load = () => AlertsApi.openCount().then((res) => setOpenAlerts(res.data)).catch(() => {})
    load()
    const interval = setInterval(load, 60000)
    return () => clearInterval(interval)
  }, [])

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
          <Typography variant="h6" noWrap>
            Ameya Production Tool & Consumable Inventory
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Chip label={`${auth?.username} · ${auth?.role}`} size="small" color="secondary" />
            <IconButton
              color="inherit"
              onClick={() => {
                logout()
                navigate('/login')
              }}
              title="Log out"
            >
              <LogoutIcon />
            </IconButton>
          </Box>
        </Toolbar>
      </AppBar>
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <Divider />
        <List>
          {NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin).map((item) => (
            <ListItemButton
              key={item.path}
              component={RouterLink}
              to={item.path}
              selected={item.path === '/' ? location.pathname === '/' : location.pathname.startsWith(item.path)}
            >
              <ListItemIcon>
                {item.badge && openAlerts > 0 ? (
                  <Badge badgeContent={openAlerts} color="error">
                    {item.icon}
                  </Badge>
                ) : (
                  item.icon
                )}
              </ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, bgcolor: '#f4f6f8', minHeight: '100vh', p: 3 }}>
        <Toolbar />
        {children}
      </Box>
    </Box>
  )
}
