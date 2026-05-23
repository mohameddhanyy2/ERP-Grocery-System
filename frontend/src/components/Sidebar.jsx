import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard, ShoppingCart, Package, Truck,
  Users, DollarSign, BarChart3, UserCheck, Store
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
    <aside className="fixed top-0 left-0 h-screen w-60 bg-gray-900 border-r border-gray-800 flex flex-col z-40">
      {/* Logo */}
      <div className="flex items-center gap-3 px-5 py-5 border-b border-gray-800">
        <div className="w-8 h-8 bg-brand-600 rounded-lg flex items-center justify-center">
          <Store size={16} className="text-white" />
        </div>
        <div>
          <p className="text-sm font-semibold text-white leading-tight">ERP Grocery</p>
          <p className="text-xs text-gray-500">Store Management</p>
        </div>
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
                  ? 'bg-brand-600 text-white'
                  : 'text-gray-400 hover:text-gray-100 hover:bg-gray-800'
              }`
            }
          >
            <Icon size={16} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="px-5 py-4 border-t border-gray-800">
        <p className="text-xs text-gray-600">Multi-Store ERP v1.0</p>
      </div>
    </aside>
  );
}
