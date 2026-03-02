import { clsx } from 'clsx';
import {
  AlertTriangle,
  ArrowLeftRight,
  Bell,
  ChevronLeft,
  ChevronRight,
  Cpu,
  FileText,
  LayoutDashboard,
  Shield,
} from 'lucide-react';
import { format } from 'date-fns';
import { useEffect, useMemo, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAppStore } from '../../store/appStore';
import { useAuth } from '../../auth/AuthContext';

const navItems = [
  { icon: LayoutDashboard, to: '/', label: 'Dashboard' },
  { icon: ArrowLeftRight, to: '/trades', label: 'Trades' },
  { icon: AlertTriangle, to: '/breaches', label: 'Breaches' },
  { icon: FileText, to: '/reports', label: 'Reports' },
  { icon: Cpu, to: '/ai-usage', label: 'AI Usage' },
  { icon: Shield, to: '/audit', label: 'Audit' },
];

const pageTitleMap: Record<string, string> = {
  '/': 'Dashboard',
  '/trades': 'Trade Blotter',
  '/breaches': 'Settlement Breaches',
  '/reports': 'Management Reports',
  '/ai-usage': 'AI Usage & Cost',
  '/audit': 'Audit Trail',
};

export function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const [clock, setClock] = useState(new Date());
  const { sidebarCollapsed, toggleSidebar } = useAppStore();
  const { username, roles, logout, switchUser } = useAuth();
  const primaryRole = roles[0] ?? 'user';

  useEffect(() => {
    const interval = setInterval(() => setClock(new Date()), 1000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (!event.metaKey) return;
      const key = Number(event.key);
      if (key >= 1 && key <= navItems.length) {
        event.preventDefault();
        navigate(navItems[key - 1].to);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [navigate]);

  const pageTitle = useMemo(() => pageTitleMap[location.pathname] ?? 'Trade Settlement', [location.pathname]);

  return (
    <div className="flex h-screen overflow-hidden bg-bg-base">
      <aside
        className={clsx(
          'no-print h-full flex-none border-r border-border-subtle bg-bg-surface transition-all duration-200',
          sidebarCollapsed ? 'w-16' : 'w-[220px]',
        )}
      >
        <div className="flex h-full flex-col">
          <div className={clsx('border-b border-border-subtle py-5', sidebarCollapsed ? 'px-3' : 'px-4')}>
            <div className="mb-1 flex items-center justify-between">
              <h1 className={clsx('font-mono font-semibold text-text-primary', sidebarCollapsed ? 'text-lg' : 'text-2xl')}>TSC</h1>
              <span className={clsx('inline-flex items-center gap-2 rounded-full border border-success/30 bg-success/10 px-2 py-1 text-[10px] text-success', sidebarCollapsed && 'hidden')}>
                <span className="h-1.5 w-1.5 rounded-full bg-success" />
                LIVE
              </span>
            </div>
            {!sidebarCollapsed ? (
              <p className="text-[11px] uppercase tracking-[0.16em] text-text-secondary">
                Settlement Engine
              </p>
            ) : null}
          </div>

          <nav className="flex-1 overflow-y-auto py-3">
            {navItems.map((item) => {
              const active = item.to === '/' ? location.pathname === '/' : location.pathname.startsWith(item.to);
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={clsx(
                    'mx-2 flex items-center gap-3 px-2 py-2 text-[13px] tracking-[0.01em] font-medium transition-colors',
                    'hover:bg-bg-raised hover:text-text-primary',
                    active
                      ? 'border-l-2 border-border-focus bg-bg-raised text-text-primary'
                      : 'text-text-secondary',
                    sidebarCollapsed && 'justify-center',
                  )}
                  title={item.label}
                >
                  <item.icon className="h-4 w-4" />
                  {!sidebarCollapsed ? <span>{item.label}</span> : null}
                </Link>
              );
            })}
          </nav>

          <div className="border-t border-border-subtle p-3">
            <div className={clsx('mb-2 flex items-center gap-3 rounded-lg bg-bg-raised px-3 py-2', sidebarCollapsed && 'justify-center px-2')}>
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/20 text-xs font-semibold text-primary">
                OA
              </div>
              {!sidebarCollapsed ? (
                <div>
                  <p className="text-xs font-medium text-text-primary">{username}</p>
                  <span className="text-[11px] text-text-secondary">{primaryRole}</span>
                </div>
              ) : null}
            </div>
            {!sidebarCollapsed ? (
              <div className="mb-2 grid grid-cols-2 gap-2">
                <button
                  type="button"
                  className="rounded border border-border-subtle bg-bg-base px-2 py-1 text-[11px] text-text-secondary transition-colors hover:text-text-primary"
                  onClick={() => void switchUser()}
                >
                  Switch User
                </button>
                <button
                  type="button"
                  className="rounded border border-border-subtle bg-bg-base px-2 py-1 text-[11px] text-text-secondary transition-colors hover:text-text-primary"
                  onClick={() => void logout()}
                >
                  Logout
                </button>
              </div>
            ) : null}
            <button
              type="button"
              className="flex w-full items-center justify-center border border-border-subtle bg-bg-raised py-2 text-text-secondary transition-colors hover:border-text-tertiary hover:text-text-primary"
              onClick={toggleSidebar}
              title={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            >
              {sidebarCollapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
            </button>
          </div>
        </div>
      </aside>

      <main className="flex flex-1 flex-col overflow-hidden">
        <header className="no-print h-12 border-b border-border-subtle bg-bg-surface px-6">
          <div className="flex h-full items-center justify-between">
            <h2 className="text-sm font-medium text-text-primary">{pageTitle}</h2>
            <div className="flex items-center gap-4">
              <p className="tabular-nums font-mono text-xs text-text-secondary">
                {format(clock, 'EEE MMM dd')} · {format(clock, 'h:mm:ss a')}
              </p>
              <span className="inline-flex items-center gap-1 text-xs font-mono text-text-secondary">
                <span className="h-1.5 w-1.5 rounded-full bg-success" />
                NYSE
              </span>
              <button
                className="rounded border border-border-subtle bg-bg-raised p-1.5 text-text-secondary transition-colors hover:border-text-tertiary hover:text-text-primary"
                type="button"
              >
                <Bell className="h-4 w-4" />
              </button>
            </div>
          </div>
        </header>

        <div className="flex-1 overflow-y-auto bg-bg-base p-6 print-area">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
