import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useStores } from '../hooks/useStores';
import PageHeader from '../components/PageHeader';
import StatCard from '../components/StatCard';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import { DollarSign, TrendingUp, TrendingDown, Plus } from 'lucide-react';

const fmt = (n) => `EGP ${Number(n).toFixed(2)}`;

export default function Finance() {
  const stores = useStores();
  const [summary, setSummary]   = useState(null);
  const [revenue, setRevenue]   = useState([]);
  const [expenses, setExpenses] = useState([]);
  const [tab, setTab]           = useState('summary');
  const [loading, setLoading]   = useState(true);
  const [period, setPeriod]     = useState(new Date().toISOString().slice(0, 7));
  const [showExpense, setShowExpense] = useState(false);
  const [msg, setMsg]           = useState('');
  const [form, setForm]         = useState({ storeId: '', category: 'MISC', amount: '', date: new Date().toISOString().slice(0, 10) });

  const load = () => {
    setLoading(true);
    Promise.all([api.financeSummary(period), api.revenue(), api.expenses()])
      .then(([s, r, e]) => { setSummary(s); setRevenue(r); setExpenses(e); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };
  useEffect(load, [period]);

  const handleAddExpense = async (e) => {
    e.preventDefault();
    try {
      await api.addExpense({ ...form, amount: Number(form.amount) });
      setMsg('Expense recorded.');
      setShowExpense(false);
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  return (
    <div>
      <PageHeader
        title="Finance"
        subtitle="Revenue, expenses and profit analysis"
        action={
          <div className="flex gap-2 items-center">
            <input type="month" className="input w-36" value={period} onChange={e => setPeriod(e.target.value)} />
            <button className="btn-primary flex items-center gap-2" onClick={() => setShowExpense(true)}><Plus size={14} /> Expense</button>
          </div>
        }
      />

      {msg && <div className="mb-4 p-3 bg-brand-900/30 border border-brand-700 rounded-lg text-sm text-brand-300">{msg}</div>}

      {loading ? <LoadingSpinner /> : (
        <>
          {summary && (
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
              <StatCard label={`Revenue (${summary.period})`}  value={fmt(summary.totalRevenue)}  icon={DollarSign}  color="green" />
              <StatCard label={`Expenses (${summary.period})`} value={fmt(summary.totalExpenses)} icon={TrendingDown} color="red" />
              <StatCard label={`Net Profit (${summary.period})`} value={fmt(summary.netProfit)}   icon={TrendingUp}  color={summary.netProfit >= 0 ? 'green' : 'red'} />
            </div>
          )}

          <div className="flex gap-1 mb-4 bg-gray-900 rounded-lg p-1 w-fit">
            {['summary', 'revenue', 'expenses'].map(t => (
              <button key={t} onClick={() => setTab(t)}
                className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors capitalize ${tab === t ? 'bg-gray-700 text-white' : 'text-gray-400 hover:text-gray-200'}`}>
                {t}
              </button>
            ))}
          </div>

          {tab === 'revenue' && (
            revenue.length === 0 ? <EmptyState text="No revenue records" /> : (
              <div className="card p-0 overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-800 text-left">
                      {['Period', 'Store', 'Gross Revenue'].map(h => <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>)}
                    </tr>
                  </thead>
                  <tbody>
                    {revenue.map((r, i) => (
                      <tr key={i} className="table-row">
                        <td className="px-4 py-3 text-gray-300">{r.period}</td>
                        <td className="px-4 py-3 text-gray-400">{r.storeId}</td>
                        <td className="px-4 py-3 font-semibold text-emerald-400">{fmt(r.grossRevenue)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}

          {tab === 'expenses' && (
            expenses.length === 0 ? <EmptyState text="No expense records" /> : (
              <div className="card p-0 overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-800 text-left">
                      {['Store', 'Category', 'Amount', 'Date'].map(h => <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>)}
                    </tr>
                  </thead>
                  <tbody>
                    {expenses.map((e, i) => (
                      <tr key={i} className="table-row">
                        <td className="px-4 py-3 text-gray-400">{e.storeId}</td>
                        <td className="px-4 py-3"><span className="badge-blue">{e.category}</span></td>
                        <td className="px-4 py-3 font-semibold text-red-400">{fmt(e.amount)}</td>
                        <td className="px-4 py-3 text-gray-500 text-xs">{e.date}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}

          {tab === 'summary' && summary && (
            <div className="card max-w-sm">
              <h3 className="text-sm font-semibold text-gray-300 mb-4">Period: {summary.period}</h3>
              {[
                { label: 'Total Revenue',  value: fmt(summary.totalRevenue),  cls: 'text-emerald-400' },
                { label: 'Total Expenses', value: fmt(summary.totalExpenses), cls: 'text-red-400' },
              ].map(r => (
                <div key={r.label} className="flex justify-between py-2 border-b border-gray-800">
                  <span className="text-sm text-gray-400">{r.label}</span>
                  <span className={`text-sm font-semibold ${r.cls}`}>{r.value}</span>
                </div>
              ))}
              <div className="flex justify-between pt-3">
                <span className="text-sm font-semibold text-gray-300">Net Profit</span>
                <span className={`text-sm font-bold ${summary.netProfit >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>{fmt(summary.netProfit)}</span>
              </div>
            </div>
          )}
        </>
      )}

      {showExpense && (
        <Modal title="Record Expense" onClose={() => setShowExpense(false)}>
          <form onSubmit={handleAddExpense} className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Store</label>
                <select className="input" value={form.storeId} onChange={e => setForm({...form, storeId: e.target.value})} required>
                  <option value="">Select store…</option>
                  {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Category</label>
                <input className="input" placeholder="MISC" value={form.category} onChange={e => setForm({...form, category: e.target.value})} />
              </div>
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Amount (EGP)</label>
              <input type="number" min="0" step="0.01" className="input" value={form.amount} onChange={e => setForm({...form, amount: e.target.value})} required />
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Date</label>
              <input type="date" className="input" value={form.date} onChange={e => setForm({...form, date: e.target.value})} />
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Save Expense</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => setShowExpense(false)}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
