import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useStores } from '../hooks/useStores';
import PageHeader from '../components/PageHeader';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import { Plus, RefreshCw } from 'lucide-react';

export default function Inventory() {
  const stores = useStores();
  const [stock, setStock]     = useState([]);
  const [alerts, setAlerts]   = useState([]);
  const [products, setProducts] = useState([]);
  const [filter, setFilter]   = useState('');
  const [storeFilter, setStoreFilter] = useState('');
  const [tab, setTab]         = useState('stock');
  const [loading, setLoading] = useState(true);
  const [showRestock, setShowRestock] = useState(false);
  const [showAddProduct, setShowAddProduct] = useState(false);
  const [showAddStore, setShowAddStore] = useState(false);
  const [storeForm, setStoreForm] = useState({ storeId: '', storeName: '' });
  const [form, setForm]       = useState({ storeId: '', productId: '', quantity: '' });
  const [productForm, setProductForm] = useState({ name: '', category: '', price: '', expiryDate: '' });
  const [msg, setMsg]         = useState('');

  const load = () => {
    setLoading(true);
    Promise.all([
      api.stock(storeFilter || undefined),
      api.alerts(),
      api.products(),
    ]).then(([s, a, p]) => { setStock(s); setAlerts(a); setProducts(p); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(load, [storeFilter]);

  const filtered = stock.filter(r =>
    !filter ||
    r.productName?.toLowerCase().includes(filter.toLowerCase()) ||
    r.productId?.toLowerCase().includes(filter.toLowerCase())
  );

  const handleRestock = async (e) => {
    e.preventDefault();
    try {
      await api.restock({ ...form, quantity: Number(form.quantity) });
      setMsg('Stock updated!');
      setShowRestock(false);
      setForm({ storeId: '', productId: '', quantity: '' });
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const handleAddProduct = async (e) => {
    e.preventDefault();
    try {
      const r = await api.addProduct({ ...productForm, price: Number(productForm.price) });
      setMsg(`Product added: ${r.name}`);
      setShowAddProduct(false);
      setProductForm({ name: '', category: '', price: '', expiryDate: '' });
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const handleAddStore = async (e) => {
    e.preventDefault();
    try {
      const r = await api.addStore(storeForm);
      setMsg(`Store created: ${r.storeName}`);
      setShowAddStore(false);
      setStoreForm({ storeId: '', storeName: '' });
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const CATEGORIES = ['Dairy', 'Bakery', 'Produce', 'Meat', 'Beverages', 'Frozen', 'Snacks', 'Cleaning', 'Personal Care', 'General'];

  return (
    <div>
      <PageHeader
        title="Inventory"
        subtitle="Stock levels across all branches"
        action={
          <div className="flex gap-2">
            <button className="btn-secondary flex items-center gap-2" onClick={() => setShowAddStore(true)}>
              <Plus size={14} /> New Store
            </button>
            <button className="btn-secondary flex items-center gap-2" onClick={() => setShowAddProduct(true)}>
              <Plus size={14} /> Add Product
            </button>
            <button className="btn-primary flex items-center gap-2" onClick={() => setShowRestock(true)}>
              <Plus size={14} /> Restock
            </button>
          </div>
        }
      />

      {msg && <div className="mb-4 p-3 bg-brand-900/30 border border-brand-700 rounded-lg text-sm text-brand-300">{msg}</div>}

      <div className="flex gap-1 mb-4 bg-gray-900 rounded-lg p-1 w-fit">
        {['stock', 'alerts'].map(t => (
          <button key={t} onClick={() => setTab(t)}
            className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${tab === t ? 'bg-gray-700 text-white' : 'text-gray-400 hover:text-gray-200'}`}>
            {t === 'alerts' ? `Low Stock Alerts (${alerts.length})` : 'Stock Levels'}
          </button>
        ))}
      </div>

      {tab === 'stock' && (
        <>
          <div className="flex gap-3 mb-4">
            <input className="input max-w-xs" placeholder="Search product…" value={filter} onChange={e => setFilter(e.target.value)} />
            <select className="input max-w-48" value={storeFilter} onChange={e => setStoreFilter(e.target.value)}>
              <option value="">All stores</option>
              {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
            <button className="btn-secondary flex items-center gap-1" onClick={load}><RefreshCw size={14} /></button>
          </div>

          {loading ? <LoadingSpinner /> : filtered.length === 0 ? <EmptyState text="No stock records found" /> : (
            <div className="card p-0 overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-800 text-left">
                    {['Product', 'Category', 'Store', 'Price', 'Qty', 'Status'].map(h => (
                      <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((r, i) => (
                    <tr key={i} className="table-row">
                      <td className="px-4 py-3">
                        <p className="font-medium text-white">{r.productName || r.productId}</p>
                        <p className="text-xs text-gray-500">{r.productId}</p>
                      </td>
                      <td className="px-4 py-3 text-gray-400">{r.category || '—'}</td>
                      <td className="px-4 py-3 text-gray-400">{stores.find(s => s.id === r.storeId)?.name || r.storeId}</td>
                      <td className="px-4 py-3 text-gray-300">EGP {r.price?.toFixed(2)}</td>
                      <td className="px-4 py-3 font-semibold text-white">{r.quantity}</td>
                      <td className="px-4 py-3">
                        {r.quantity === 0 ? <span className="badge-red">Out</span>
                          : r.quantity < 10 ? <span className="badge-yellow">Low</span>
                          : <span className="badge-green">OK</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {tab === 'alerts' && (
        alerts.length === 0 ? <EmptyState text="No low-stock alerts" /> : (
          <div className="card p-0 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-800 text-left">
                  {['Product', 'Store', 'Qty', 'Threshold', 'Date'].map(h => (
                    <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {alerts.map((a, i) => (
                  <tr key={i} className="table-row">
                    <td className="px-4 py-3 font-medium text-white">{a.productId}</td>
                    <td className="px-4 py-3 text-gray-400">{stores.find(s => s.id === a.storeId)?.name || a.storeId}</td>
                    <td className="px-4 py-3"><span className="badge-red">{a.currentQty}</span></td>
                    <td className="px-4 py-3 text-gray-400">{a.threshold}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{a.alertDate}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {showRestock && (
        <Modal title="Restock Product" onClose={() => setShowRestock(false)}>
          <form onSubmit={handleRestock} className="space-y-4">
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Store</label>
              <select className="input" value={form.storeId} onChange={e => setForm({ ...form, storeId: e.target.value })} required>
                <option value="">Select store…</option>
                {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Product</label>
              <select className="input" value={form.productId} onChange={e => setForm({ ...form, productId: e.target.value })} required>
                <option value="">Select product…</option>
                {products.map(p => <option key={p.productId} value={p.productId}>{p.name} — EGP {p.price}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Quantity to Add</label>
              <input type="number" min="1" className="input" placeholder="50" value={form.quantity} onChange={e => setForm({ ...form, quantity: e.target.value })} required />
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Add Stock</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => setShowRestock(false)}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}
      {showAddProduct && (
        <Modal title="Add New Product" onClose={() => setShowAddProduct(false)}>
          <form onSubmit={handleAddProduct} className="space-y-3">
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Product Name</label>
              <input className="input" placeholder="e.g. Full Cream Milk 1L" value={productForm.name} onChange={e => setProductForm({ ...productForm, name: e.target.value })} required />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Category</label>
                <select className="input" value={productForm.category} onChange={e => setProductForm({ ...productForm, category: e.target.value })} required>
                  <option value="">Select category…</option>
                  {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Price (EGP)</label>
                <input type="number" min="0.01" step="0.01" className="input" placeholder="e.g. 45.00" value={productForm.price} onChange={e => setProductForm({ ...productForm, price: e.target.value })} required />
              </div>
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Expiry Date (optional)</label>
              <input type="date" className="input" value={productForm.expiryDate} onChange={e => setProductForm({ ...productForm, expiryDate: e.target.value })} />
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Add Product</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => setShowAddProduct(false)}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}

      {showAddStore && (
        <Modal title="Create New Store" onClose={() => setShowAddStore(false)}>
          <form onSubmit={handleAddStore} className="space-y-3">
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Store ID</label>
              <input className="input" placeholder="e.g. STORE_D or ALEX_2" value={storeForm.storeId}
                onChange={e => setStoreForm({ ...storeForm, storeId: e.target.value })} required />
              <p className="text-xs text-gray-600 mt-1">Uppercase letters and underscores only. Will be auto-normalised.</p>
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Store Name</label>
              <input className="input" placeholder="e.g. Mansoura Branch" value={storeForm.storeName}
                onChange={e => setStoreForm({ ...storeForm, storeName: e.target.value })} required />
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Create Store</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => setShowAddStore(false)}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
