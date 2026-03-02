import { StrictMode, Suspense, lazy } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AppLayout } from './components/layout/AppLayout';
import { ErrorBoundary } from './components/ErrorBoundary';
import { SkeletonLoader } from './components/ui/SkeletonLoader';
import { AuthProvider } from './auth/AuthContext';
import { queryClient } from './queryClient';
import './index.css';

const DashboardPage = lazy(() => import('./pages/Dashboard'));
const TradesPage = lazy(() => import('./pages/Trades'));
const BreachesPage = lazy(() => import('./pages/Breaches'));
const ReportsPage = lazy(() => import('./pages/Reports'));
const AiUsagePage = lazy(() => import('./pages/AiUsage'));
const AuditPage = lazy(() => import('./pages/Audit'));

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Suspense
            fallback={
              <div className="bg-bg-base p-6">
                <div className="mb-4 grid grid-cols-3 gap-4">
                  <SkeletonLoader variant="card" />
                  <SkeletonLoader variant="card" />
                  <SkeletonLoader variant="card" />
                </div>
                <SkeletonLoader variant="row" className="mb-3" />
                <SkeletonLoader variant="row" className="mb-3" />
                <SkeletonLoader variant="row" />
              </div>
            }
          >
            <Routes>
              <Route element={<AppLayout />}>
                <Route
                  path="/"
                  element={
                    <ErrorBoundary>
                      <DashboardPage />
                    </ErrorBoundary>
                  }
                />
                <Route
                  path="/trades"
                  element={
                    <ErrorBoundary>
                      <TradesPage />
                    </ErrorBoundary>
                  }
                />
                <Route
                  path="/breaches"
                  element={
                    <ErrorBoundary>
                      <BreachesPage />
                    </ErrorBoundary>
                  }
                />
                <Route
                  path="/breaches/:id"
                  element={
                    <ErrorBoundary>
                      <BreachesPage />
                    </ErrorBoundary>
                  }
                />
                <Route
                  path="/reports"
                  element={
                    <ErrorBoundary>
                      <ReportsPage />
                    </ErrorBoundary>
                  }
                />
                <Route
                  path="/ai-usage"
                  element={
                    <ErrorBoundary>
                      <AiUsagePage />
                    </ErrorBoundary>
                  }
                />
                <Route
                  path="/audit"
                  element={
                    <ErrorBoundary>
                      <AuditPage />
                    </ErrorBoundary>
                  }
                />
              </Route>
            </Routes>
          </Suspense>
          <Toaster
            position="bottom-right"
            toastOptions={{
              style: {
                background: '#0D1117',
                color: '#E6EDF3',
                border: '1px solid #21262D',
              },
            }}
          />
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
);
