import { clsx } from 'clsx';
import {
  AlertTriangle,
  ArrowLeftRight,
  Bell,
  Cpu,
  FileText,
  LayoutDashboard,
  Shield,
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';

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
  '/trades': 'Trades',
  '/breaches': 'Breach Management',
  '/reports': 'Commentary Report',
  '/ai-usage': 'AI Usage & Cost',
  '/audit': 'Audit Trail',
};

export function AppLayout() {
  const location = useLocation();
  const [clock, setClock] = useState(new Date());

  useEffect(() => {
    const interval = setInterval(() => setClock(new Date()), 1000);
    return () => clearInterval(interval);
  }, []);

  const pageTitle = useMemo(() => pageTitleMap[location.pathname] ?? 'Trade Settlement', [location.pathname]);

  return (
    <div className="flex h-screen overflow-hidden bg-[#0A0B0D]">
      <aside className="h-full w-64 flex-none border-r border-[#2A2D35] bg-[#111318]">
        <div className="flex h-full flex-col">
          <div className="border-b border-[#2A2D35] px-5 py-5">
            <div className="mb-1 flex items-center justify-between">
              <h1 className="font-mono text-3xl font-semibold text-primary">TSC</h1>
              <span className="inline-flex items-center gap-2 rounded-full border border-primary/30 bg-primary/10 px-2 py-1 text-[10px] text-primary">
                <span className="h-1.5 w-1.5 rounded-full bg-primary" />
                LIVE
              </span>
            </div>
            <p className="text-xs text-text-secondary">Trade Settlement</p>
          </div>

          <nav className="flex-1 overflow-y-auto py-4">
            {navItems.map((item) => {
              const active = item.to === '/' ? location.pathname === '/' : location.pathname.startsWith(item.to);
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={clsx(
                    'mx-2 flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all duration-150 ease-in-out',
                    'hover:bg-[#1A1D24] hover:text-text-primary',
                    active
                      ? 'border-l-2 border-primary bg-[#1A1D24] text-primary'
                      : 'text-text-secondary',
                  )}
                >
                  <item.icon className="h-5 w-5" />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>

          <div className="border-t border-[#2A2D35] p-4">
            <div className="flex items-center gap-3 rounded-lg bg-[#1A1D24] px-3 py-2">
              <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/20 font-semibold text-primary">
                OA
              </div>
              <div>
                <p className="text-sm font-medium text-text-primary">Ops Analyst</p>
                <span className="rounded-full border border-[#2A2D35] bg-[#0A0B0D] px-2 py-0.5 text-[10px] text-text-secondary">
                  ops-user
                </span>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <main className="flex flex-1 flex-col overflow-hidden">
        <header className="h-14 border-b border-[#2A2D35] bg-[#111318] px-6">
          <div className="flex h-full items-center justify-between">
            <h2 className="text-lg font-bold text-text-primary">{pageTitle}</h2>
            <div className="flex items-center gap-4">
              <p className="tabular-nums font-mono text-sm text-text-secondary">
                {clock.toLocaleTimeString()}
              </p>
              <span className="inline-flex items-center gap-2 rounded-full border border-info/30 bg-info/10 px-2 py-1 text-xs text-info">
                <span className="h-1.5 w-1.5 rounded-full bg-info" />
                NYSE
              </span>
              <button
                className="rounded-lg border border-[#2A2D35] bg-[#1A1D24] p-2 text-text-secondary transition-colors hover:border-[#3A3D45] hover:text-text-primary"
                type="button"
              >
                <Bell className="h-4 w-4" />
              </button>
            </div>
          </div>
        </header>

        <div className="flex-1 overflow-y-auto bg-[#0A0B0D] p-6">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
