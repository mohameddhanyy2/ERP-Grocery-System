import { useEffect, useState } from 'react';
import { api, subscribePosEvents } from '../api/client';
import StatCard from '../components/StatCard';
import PageHeader from '../components/PageHeader';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  Package, Users, UserCheck, Truck, ShoppingCart,
  DollarSign, TrendingUp, AlertTriangle
} from 'lucide-react';
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, Tooltip, ResponsiveContainer, Legend
} from 'recharts';

const COLORS = ['#c8102e', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

import { fmt } from '../utils/fmt';

export default function Dashboard() {
  const [summary, setSummary]       = useState(null);
  const [charts, setCharts]         = useState(null);
  const [loading, setLoading]       = useState(true);
  const [confirmReset, setConfirmReset] = useState(false);
  const [resetMsg, setResetMsg]     = useState('');

  const load = () => {
    Promise.all([api.dashboard(), api.charts()])
      .then(([s, c]) => { setSummary(s); setCharts(c); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  useEffect(() => {
    const unsub = subscribePosEvents(() => load());
    return unsub;
  }, []);

  const handleReset = async () => {
    try {
      await api.resetDatabase();
      window.location.reload();
    } catch (err) {
      setResetMsg('Reset failed: ' + err.message);
    }
  };

  if (loading) return <LoadingSpinner text="Loading dashboard..." />;
  if (!summary) return <p className="text-gray-500 text-center py-20">Could not load dashboard. Is the backend running?</p>;

  return (
    <div>
      <PageHeader
        title="Dashboard"
        subtitle={`Period: ${summary.period}`}
        action={
          <div className="flex flex-col items-end gap-1">
            {!confirmReset ? (
              <button className="btn-secondary text-xs text-red-600 border-red-900 hover:border-red-600" onClick={() => setConfirmReset(true)}>
                Reset Database
              </button>
            ) : (
              <div className="flex items-center gap-2">
                <span className="text-xs text-red-600">Delete all data?</span>
                <button className="btn-secondary text-xs text-red-600 border-red-700" onClick={handleReset}>Yes, reset</button>
                <button className="btn-secondary text-xs" onClick={() => setConfirmReset(false)}>Cancel</button>
              </div>
            )}
            {resetMsg && <p className="text-xs text-amber-600">{resetMsg}</p>}
          </div>
        }
      />

      {/* KPI row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <StatCard label="Products"    value={summary.totalProducts}   icon={Package}      color="brand" />
        <StatCard label="Customers"   value={summary.totalCustomers}  icon={UserCheck}    color="green" />
        <StatCard label="Employees"   value={summary.totalEmployees}  icon={Users}        color="purple" />
        <StatCard label="Suppliers"   value={summary.totalSuppliers}  icon={Truck}        color="blue" />
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <StatCard label="Today's Sales"   value={summary.todayTransactions} icon={ShoppingCart} color="yellow" sub="transactions today" />
        <StatCard label="Today's Revenue" value={fmt(summary.todayRevenue)}  icon={DollarSign}   color="green" />
        <StatCard label="Period Profit"   value={fmt(summary.periodProfit)}  icon={TrendingUp}   color={summary.periodProfit >= 0 ? 'green' : 'red'} />
        <StatCard label="Low Stock Stores" value={summary.lowStockCount}    icon={AlertTriangle} color="red" sub={summary.lowStockStores.join(', ') || 'none'} />
      </div>

      {/* Charts */}
      {charts && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
          {/* Sales by day */}
          <div className="card">
            <h3 className="text-sm font-semibold text-gray-700 mb-4">Daily Revenue (Last 30 Days)</h3>
            <ResponsiveContainer width="100%" height={200}>
              <AreaChart data={charts.salesByDay}>
                <defs>
                  <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor="#c8102e" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#c8102e" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="day" tick={{ fontSize: 10, fill: '#6b7280' }} tickLine={false} axisLine={false} />
                <YAxis tick={{ fontSize: 10, fill: '#6b7280' }} tickLine={false} axisLine={false} />
                <Tooltip contentStyle={{ background: '#ffffff', border: '1px solid #e5e7eb', borderRadius: 8, fontSize: 12, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }} labelStyle={{ color: '#111827', fontWeight: 600 }} itemStyle={{ color: '#374151' }} />
                <Area type="monotone" dataKey="revenue" stroke="#c8102e" fill="url(#rev)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          {/* Revenue by store */}
          <div className="card">
            <h3 className="text-sm font-semibold text-gray-700 mb-4">Revenue by Store</h3>
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={charts.revenueByStore}>
                <XAxis dataKey="storeId" tick={{ fontSize: 11, fill: '#6b7280' }} tickLine={false} axisLine={false} />
                <YAxis tick={{ fontSize: 10, fill: '#6b7280' }} tickLine={false} axisLine={false} />
                <Tooltip contentStyle={{ background: '#ffffff', border: '1px solid #e5e7eb', borderRadius: 8, fontSize: 12, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }} labelStyle={{ color: '#111827', fontWeight: 600 }} itemStyle={{ color: '#374151' }} />
                <Bar dataKey="revenue" fill="#c8102e" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Stock by category */}
          <div className="card">
            <h3 className="text-sm font-semibold text-gray-700 mb-4">Stock by Category</h3>
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie data={charts.stockByCategory} dataKey="quantity" nameKey="category" cx="50%" cy="50%" outerRadius={75} label={({ category }) => category}>
                  {charts.stockByCategory.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip contentStyle={{ background: '#ffffff', border: '1px solid #e5e7eb', borderRadius: 8, fontSize: 12, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }} labelStyle={{ color: '#111827', fontWeight: 600 }} itemStyle={{ color: '#374151' }} />
              </PieChart>
            </ResponsiveContainer>
          </div>

          {/* Finance summary */}
          <div className="card flex flex-col gap-4">
            <h3 className="text-sm font-semibold text-gray-700">Finance Summary — {summary.period}</h3>
            {[
              { label: 'Revenue',  value: summary.periodRevenue,  color: 'text-emerald-600' },
              { label: 'Expenses', value: summary.periodExpenses, color: 'text-amber-600' },
              { label: 'Profit',   value: summary.periodProfit,   color: summary.periodProfit >= 0 ? 'text-emerald-600' : 'text-red-600' },
            ].map(r => (
              <div key={r.label} className="flex items-center justify-between">
                <span className="text-sm text-gray-400">{r.label}</span>
                <span className={`text-sm font-semibold ${r.color}`}>{fmt(r.value)}</span>
              </div>
            ))}
            <div className="border-t border-gray-200 pt-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-400">Orders</span>
                <span className="text-sm font-semibold text-gray-900">{summary.totalOrders}</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
