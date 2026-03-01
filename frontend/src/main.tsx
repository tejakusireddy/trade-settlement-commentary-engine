import { StrictMode, Suspense, lazy } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppLayout } from './components/layout/AppLayout';
import { PageErrorBoundary } from './components/ui/PageErrorBoundary';
import { SkeletonLoader } from './components/ui/SkeletonLoader';
import './index.css';

const DashboardPage = lazy(() => import('./pages/Dashboard'));
const BreachesPage = lazy(() => import('./pages/Breaches'));
const ReportsPage = lazy(() => import('./pages/Reports'));
const AiUsagePage = lazy(() => import('./pages/AiUsage'));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
    },
  },
});

function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="rounded-xl border border-[#2A2D35] bg-[#111318] p-6">
      <h3 className="text-base font-semibold text-text-primary">{title}</h3>
    </div>
  );
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Suspense
          fallback={
            <div className="bg-[#0A0B0D] p-6">
              <SkeletonLoader variant="card" className="mb-4" />
              <SkeletonLoader variant="card" className="mb-4" />
              <SkeletonLoader variant="card" />
            </div>
          }
        >
          <Routes>
            <Route element={<AppLayout />}>
              <Route
                path="/"
                element={
                  <PageErrorBoundary>
                    <DashboardPage />
                  </PageErrorBoundary>
                }
              />
              <Route
                path="/trades"
                element={
                  <PageErrorBoundary>
                    <PlaceholderPage title="Trades — Coming Soon" />
                  </PageErrorBoundary>
                }
              />
              <Route
                path="/breaches"
                element={
                  <PageErrorBoundary>
                    <BreachesPage />
                  </PageErrorBoundary>
                }
              />
              <Route
                path="/breaches/:id"
                element={
                  <PageErrorBoundary>
                    <BreachesPage />
                  </PageErrorBoundary>
                }
              />
              <Route
                path="/reports"
                element={
                  <PageErrorBoundary>
                    <ReportsPage />
                  </PageErrorBoundary>
                }
              />
              <Route
                path="/ai-usage"
                element={
                  <PageErrorBoundary>
                    <AiUsagePage />
                  </PageErrorBoundary>
                }
              />
              <Route
                path="/audit"
                element={
                  <PageErrorBoundary>
                    <PlaceholderPage title="Audit — Coming Soon" />
                  </PageErrorBoundary>
                }
              />
            </Route>
          </Routes>
        </Suspense>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
