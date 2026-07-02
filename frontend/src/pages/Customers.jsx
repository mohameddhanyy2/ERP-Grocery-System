import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useStores } from '../hooks/useStores';
import PageHeader from '../components/PageHeader';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import { Plus, Star } from 'lucide-react';
import { fmt } from '../utils/fmt';

export default function Customers() {
  const stores = useStores();
  const [customers, setCustomers] = useState([]);
  const [selected, setSelected]   = useState(null);
  const [history, setHistory]     = useState([]);
  const [loading, setLoading]     = useState(true);
  const [filter, setFilter]       = useState('');
  const [showAdd, setShowAdd]     = useState(false);
  const [msg, setMsg]             = useState('');
  const [form, setForm]           = useState({ name: '', email: '', registeredStoreId: '' });

  const load = () => {
    setLoading(true);
    api.customers()
      .then(setCustomers)
      .catch(console.error)
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  const selectCustomer = async (c) => {
    setSelected(c);
    setHistory([]);
    try {
      const [points, hist] = await Promise.all([
        api.customerPoints(c.customerId),
        api.customerHistory(c.customerId),
      ]);
      setSelected({ ...c, ...points });
      setHistory(hist);
    } catch {}
  };

  const handleAdd = async (e) => {
    e.preventDefault();
    try {
      const r = await api.addCustomer(form);
      setMsg(`Customer added: ${r.customerId}`);
      setShowAdd(false);
      setForm({ name: '', email: '', registeredStoreId: '' });
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const tierColor = (t) => {
    if (t === 'GOLD')   return 'badge-yellow';
    if (t === 'SILVER') return 'text-gray-700 bg-gray-200 text-xs px-2 py-0.5 rounded-full font-medium';
    return 'badge-blue';
  };

  const filtered = customers.filter(c =>
    !filter ||
    c.name?.toLowerCase().includes(filter.toLowerCase()) ||
    c.email?.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div>
      <PageHeader
        title="Customers"
        subtitle="Customer directory and loyalty program"
        action={
          <button className="btn-primary flex items-center gap-2" onClick={() => setShowAdd(true)}>
            <Plus size={14} /> Add Customer
          </button>
        }
      />

      {msg && <div className="mb-4 p-3 bg-brand-100 border border-brand-200 rounded-lg text-sm text-brand-700">{msg}</div>}

      <div className="flex gap-3 mb-4">
        <input className="input max-w-xs" placeholder="Search by name or email…" value={filter} onChange={e => setFilter(e.target.value)} />
      </div>

      <div className={`grid gap-4 ${selected ? 'lg:grid-cols-2' : 'grid-cols-1'}`}>
        <div>
          {loading ? <LoadingSpinner /> : filtered.length === 0 ? <EmptyState text="No customers yet. Add your first customer." /> : (
            <div className="card p-0 overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-left">
                    {['Customer', 'Email', 'Home Store', 'Points', 'Tier'].map(h => (
                      <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((c, i) => (
                    <tr key={i}
                      className={`table-row cursor-pointer ${selected?.customerId === c.customerId ? 'bg-brand-50' : ''}`}
                      onClick={() => selectCustomer(c)}>
                      <td className="px-4 py-3">
                        <p className="font-medium text-gray-900">{c.name}</p>
                        <p className="text-xs text-gray-500">{c.customerId}</p>
                      </td>
                      <td className="px-4 py-3 text-gray-400 text-xs">{c.email || '—'}</td>
                      <td className="px-4 py-3 text-gray-400">{stores.find(s => s.id === c.registeredStoreId)?.name || c.registeredStoreId}</td>
                      <td className="px-4 py-3 font-semibold text-gray-900">
                        {c.points ?? 0} <Star size={10} className="inline text-amber-600" />
                      </td>
                      <td className="px-4 py-3"><span className={tierColor(c.tier)}>{c.tier || 'BRONZE'}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {selected && (
          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-base font-semibold text-gray-900">{selected.name}</h3>
                <p className="text-xs text-gray-500">{selected.customerId}</p>
              </div>
              <button className="text-gray-500 hover:text-gray-700 text-xs" onClick={() => { setSelected(null); setHistory([]); }}>✕ Close</button>
            </div>
            <div className="grid grid-cols-3 gap-3 mb-4">
              {[
                { label: 'Points', value: selected.points ?? 0 },
                { label: 'Tier',   value: selected.tier || 'BRONZE' },
                { label: 'Spent',  value: fmt(selected.totalSpend || 0) },
              ].map(s => (
                <div key={s.label} className="bg-gray-100 rounded-lg p-3 text-center">
                  <p className="text-xs text-gray-500">{s.label}</p>
                  <p className="text-base font-bold text-gray-900 mt-1">{s.value}</p>
                </div>
              ))}
            </div>
            <h4 className="text-xs text-gray-500 font-medium uppercase mb-2">Purchase History</h4>
            {history.length === 0
              ? <p className="text-xs text-gray-600">No purchases yet.</p>
              : (
                <div className="space-y-2 max-h-60 overflow-y-auto">
                  {history.map((h, i) => (
                    <div key={i} className="flex justify-between items-center text-xs bg-gray-100 rounded-lg px-3 py-2">
                      <span className="text-gray-400">{h.date?.slice(0, 10)}</span>
                      <span className="text-gray-500 font-mono">{h.saleId?.slice(0, 20)}</span>
                      <span className="text-emerald-600 font-semibold">{fmt(h.amount)}</span>
                    </div>
                  ))}
                </div>
              )}
          </div>
        )}
      </div>

      {showAdd && (
        <Modal title="Add Customer" onClose={() => setShowAdd(false)}>
          <form onSubmit={handleAdd} className="space-y-3">
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Full Name</label>
              <input className="input" placeholder="Customer name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required />
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Email (optional)</label>
              <input type="email" className="input" placeholder="customer@email.com" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Home Store</label>
              <select className="input" value={form.registeredStoreId} onChange={e => setForm({ ...form, registeredStoreId: e.target.value })}>
                {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Add Customer</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => setShowAdd(false)}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
