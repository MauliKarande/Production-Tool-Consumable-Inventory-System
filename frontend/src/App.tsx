import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { RequireAuth } from './auth/RequireAuth'
import { AppLayout } from './components/layout/AppLayout'
import { LoginPage } from './pages/LoginPage'
import { DashboardPage } from './pages/DashboardPage'
import { ItemsListPage } from './pages/ItemsListPage'
import { ItemFormPage } from './pages/ItemFormPage'
import { IssuePage } from './pages/IssuePage'
import { AccountabilityPage } from './pages/AccountabilityPage'
import { ConsumptionPage } from './pages/ConsumptionPage'
import { AlertsPage } from './pages/AlertsPage'
import { PurchaseRequisitionsListPage } from './pages/PurchaseRequisitionsListPage'
import { PurchaseRequisitionFormPage } from './pages/PurchaseRequisitionFormPage'
import { ReportsPage } from './pages/ReportsPage'
import { ImportPage } from './pages/ImportPage'
import { MastersPage } from './pages/MastersPage'

function LoginRoute() {
  const { auth } = useAuth()
  if (auth) return <Navigate to="/" replace />
  return <LoginPage />
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const { isAdmin } = useAuth()
  if (!isAdmin) return <Navigate to="/" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginRoute />} />
        <Route
          path="/*"
          element={
            <RequireAuth>
              <AppLayout>
                <Routes>
                  <Route path="/" element={<DashboardPage />} />
                  <Route path="/items" element={<ItemsListPage />} />
                  <Route path="/items/:id" element={<ItemFormPage />} />
                  <Route path="/issue" element={<IssuePage />} />
                  <Route path="/accountability" element={<AccountabilityPage />} />
                  <Route path="/consumption" element={<ConsumptionPage />} />
                  <Route path="/alerts" element={<AlertsPage />} />
                  <Route path="/purchase-requisitions" element={<PurchaseRequisitionsListPage />} />
                  <Route path="/purchase-requisitions/:id" element={<PurchaseRequisitionFormPage />} />
                  <Route path="/reports" element={<ReportsPage />} />
                  <Route
                    path="/masters"
                    element={
                      <AdminRoute>
                        <MastersPage />
                      </AdminRoute>
                    }
                  />
                  <Route
                    path="/import"
                    element={
                      <AdminRoute>
                        <ImportPage />
                      </AdminRoute>
                    }
                  />
                  <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
              </AppLayout>
            </RequireAuth>
          }
        />
      </Routes>
    </AuthProvider>
  )
}
