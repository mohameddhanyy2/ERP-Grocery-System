import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard, ShoppingCart, Package, Truck,
  Users, DollarSign, BarChart3, UserCheck, ScanBarcode, ExternalLink
} from 'lucide-react';

const nav = [
  { to: '/',          label: 'Dashboard',  icon: LayoutDashboard },
  { to: '/inventory', label: 'Inventory',  icon: Package },
  { to: '/pos',       label: 'Point of Sale', icon: ShoppingCart },
  { to: '/supplier',  label: 'Suppliers',  icon: Truck },
  { to: '/hr',        label: 'HR & Staff', icon: Users },
  { to: '/finance',   label: 'Finance',    icon: DollarSign },
  { to: '/customers', label: 'Customers',  icon: UserCheck },
  { to: '/reporting', label: 'Reports',    icon: BarChart3 },
];

export default function Sidebar() {
  return (
    <aside className="fixed top-0 left-0 h-screen w-60 bg-white border-r border-gray-200 flex flex-col z-40">
      {/* Logo */}
      <div className="flex items-center justify-center px-2 py-4 border-b border-gray-100">
        <img src="/masri-logo.png" alt="Masri" className="w-full max-h-28 object-contain" />
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {nav.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-brand-600 text-white shadow-sm'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
              }`
            }
          >
            <Icon size={16} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="px-3 py-4 border-t border-gray-100 space-y-2">
        <a
          href="http://localhost:5175"
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors text-gray-600 hover:text-gray-900 hover:bg-gray-100"
        >
          <ScanBarcode size={16} />
          Cashier Portal
          <ExternalLink size={11} className="ml-auto text-gray-400" />
        </a>
        <p className="text-xs text-gray-400 px-3">Masri ERP · since 1938</p>
      </div>
    </aside>
  );
}
