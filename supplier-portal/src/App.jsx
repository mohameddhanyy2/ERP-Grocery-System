import { useEffect, useState } from 'react';
import { api, subscribeAlerts } from './api';
import { AlertTriangle, Truck, CheckCircle, Package, RefreshCw, LogOut } from 'lucide-react';

const fmt = (n) => `EGP ${Number(n || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const STATUS_META = {
  QUOTED:           { label: 'Quoted — awaiting acceptance', color: 'bg-yellow-900/40 text-yellow-300 border-yellow-700' },
  ACCEPTED:         { label: 'Accepted', color: 'bg-blue-900/40 text-blue-300 border-blue-700' },
  OUT_FOR_DELIVERY: { label: 'Out for Delivery', color: 'bg-purple-900/40 text-purple-300 border-purple-700' },
  DELIVERED:        { label: 'Completed', color: 'bg-green-900/40 text-green-300 border-green-700' },
};

function StatusBadge({ status }) {
  const m = STATUS_META[status] || { label: status, color: 'bg-gray-800 text-gray-400 border-gray-700' };
  return <span className={`text-xs px-2 py-1 rounded-full border font-medium ${m.color}`}>{m.label}</span>;
}

// ── Login screen ─────────────────────────────────────────────────────────────
function Login({ suppliers, onLogin }) {
  const [id, setId] = useState('');
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-950">
      <div className="bg-gray-900 border border-gray-800 rounded-2xl p-8 w-full max-w-sm shadow-xl">
        <div className="flex items-center gap-3 mb-6">
          <div className="bg-indigo-600 rounded-xl p-2"><Truck size={22} className="text-white" /></div>
          <div>
            <h1 className="text-lg font-bold text-white">Supplier Portal</h1>
            <p className="text-xs text-gray-400">ERP Grocery System</p>
          </div>
        </div>
        <label className="text-xs text-gray-400 block mb-1">Select your supplier account</label>
        <select
          className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-white mb-4 focus:outline-none focus:border-indigo-500"
          value={id}
          onChange={e => setId(e.target.value)}
        >
          <option value="">Choose supplier…</option>
          {suppliers.map(s => <option key={s.supplierId} value={s.supplierId}>{s.name}</option>)}
        </select>
        <button
          className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-lg py-2 text-sm transition-colors disabled:opacity-40"
          disabled={!id}
          onClick={() => onLogin(suppliers.find(s => s.supplierId === id))}
        >
          Enter Portal
        </button>
      </div>
    </div>
  );
}

// ── Alert card with quote form ────────────────────────────────────────────────
function AlertCard({ alert, supplierId, onQuoted }) {
  const [open, setOpen] = useState(false);
  const [qty, setQty]   = useState(alert.threshold * 5);
  const [price, setPrice] = useState('');
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState('');

  const hasOrder = !!alert.orderId;

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true); setErr('');
    try {
      await api.quote({ alertId: alert.alertId, supplierId, quantity: Number(qty), unitPrice: Number(price) });
      setOpen(false);
      onQuoted();
    } catch (ex) { setErr(ex.message); }
    finally { setLoading(false); }
  };

  return (
    <div className="bg-gray-900 border border-gray-800 rounded-xl p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="bg-red-900/40 border border-red-800 rounded-lg p-2 flex-shrink-0">
            <AlertTriangle size={16} className="text-red-400" />
          </div>
          <div>
            <p className="font-semibold text-white text-sm">{alert.productName}</p>
            <p className="text-xs text-gray-400">{alert.storeName} · alerted {alert.alertDate}</p>
            <p className="text-xs text-red-400 mt-0.5">
              Stock: <span className="font-bold">{alert.currentQty}</span> / threshold {alert.threshold}
            </p>
          </div>
        </div>
        <div className="flex-shrink-0">
          {hasOrder
            ? <StatusBadge status={alert.orderStatus} />
            : (
              <button
                className="text-xs bg-indigo-600 hover:bg-indigo-500 text-white px-3 py-1.5 rounded-lg font-medium transition-colors"
                onClick={() => setOpen(o => !o)}
              >
                {open ? 'Cancel' : 'Submit Quote'}
              </button>
            )
          }
        </div>
      </div>

      {open && !hasOrder && (
        <form onSubmit={submit} className="mt-4 border-t border-gray-800 pt-4 space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-gray-400 block mb-1">Quantity to supply</label>
              <input
                type="number" min="1" required
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:border-indigo-500"
                value={qty} onChange={e => setQty(e.target.value)}
              />
            </div>
            <div>
              <label className="text-xs text-gray-400 block mb-1">Unit price (EGP)</label>
              <input
                type="number" min="0.01" step="0.01" required
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-1.5 text-sm text-white focus:outline-none focus:border-indigo-500"
                placeholder="e.g. 12.50"
                value={price} onChange={e => setPrice(e.target.value)}
              />
            </div>
          </div>
          {price && (
            <p className="text-xs text-gray-400">
              Total: <span className="text-white font-semibold">{fmt(qty * price)}</span>
            </p>
          )}
          {err && <p className="text-xs text-red-400">{err}</p>}
          <button
            type="submit" disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-lg py-2 text-sm transition-colors disabled:opacity-40"
          >
            {loading ? 'Submitting…' : 'Submit Quote'}
          </button>
        </form>
      )}
    </div>
  );
}

// ── Order card ────────────────────────────────────────────────────────────────
function OrderCard({ order, onUpdated }) {
  const [lines, setLines] = useState([]);
  const [expanded, setExpanded] = useState(false);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState('');

  const loadLines = async () => {
    if (lines.length) { setExpanded(e => !e); return; }
    try {
      const data = await api.orderLines(order.orderId);
      setLines(data);
      setExpanded(true);
    } catch (ex) { setErr(ex.message); }
  };

  const markOutForDelivery = async () => {
    setLoading(true); setErr('');
    try {
      await api.outForDelivery(order.orderId);
      onUpdated();
    } catch (ex) { setErr(ex.message); }
    finally { setLoading(false); }
  };

  return (
    <div className="bg-gray-900 border border-gray-800 rounded-xl p-4">
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <div>
          <p className="font-semibold text-white text-sm">{order.storeName || order.storeId}</p>
          <p className="text-xs text-gray-400 font-mono">{order.orderId}</p>
          <p className="text-xs text-gray-400 mt-0.5">Ordered {order.orderDate?.slice(0, 10)} · {fmt(order.totalCost)}</p>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <StatusBadge status={order.status} />
          {order.status === 'ACCEPTED' && (
            <button
              onClick={markOutForDelivery} disabled={loading}
              className="text-xs bg-purple-600 hover:bg-purple-500 text-white px-3 py-1.5 rounded-lg font-medium transition-colors flex items-center gap-1 disabled:opacity-40"
            >
              <Truck size={12} /> {loading ? '…' : 'Mark Out for Delivery'}
            </button>
          )}
          {order.status === 'OUT_FOR_DELIVERY' && (
            <span className="text-xs text-gray-400 italic">Awaiting delivery confirmation from store</span>
          )}
          {order.status === 'DELIVERED' && (
            <span className="flex items-center gap-1 text-xs text-green-400"><CheckCircle size={12} /> Completed</span>
          )}
          <button onClick={loadLines} className="text-xs text-gray-500 hover:text-gray-300 underline">
            {expanded ? 'Hide items' : 'View items'}
          </button>
        </div>
      </div>

      {err && <p className="text-xs text-red-400 mt-2">{err}</p>}

      {expanded && lines.length > 0 && (
        <div className="mt-3 border-t border-gray-800 pt-3">
          <table className="w-full text-xs">
            <thead>
              <tr className="text-gray-500">
                <th className="text-left pb-1">Product</th>
                <th className="text-right pb-1">Qty</th>
                <th className="text-right pb-1">Unit Price</th>
                <th className="text-right pb-1">Total</th>
              </tr>
            </thead>
            <tbody>
              {lines.map((l, i) => (
                <tr key={i} className="border-t border-gray-800">
                  <td className="py-1 text-gray-300">{l.productName || l.productId}</td>
                  <td className="py-1 text-right text-gray-400">{l.quantity}</td>
                  <td className="py-1 text-right text-gray-400">{fmt(l.unitPrice)}</td>
                  <td className="py-1 text-right text-white font-medium">{fmt(l.unitPrice * l.quantity)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// ── Main portal ───────────────────────────────────────────────────────────────
export default function App() {
  const [suppliers, setSuppliers] = useState([]);
  const [supplier, setSupplier]   = useState(null);
  const [alerts, setAlerts]       = useState([]);
  const [orders, setOrders]       = useState([]);
  const [tab, setTab]             = useState('alerts');
  const [loading, setLoading]     = useState(false);

  // Load supplier list for login screen
  useEffect(() => {
    api.suppliers().then(setSuppliers).catch(console.error);
  }, []);

  const load = async (sup) => {
    if (!sup) return;
    setLoading(true);
    try {
      const [a, o] = await Promise.all([
        api.stockAlerts(sup.supplierId),
        api.orders(sup.supplierId),
      ]);
      setAlerts(a);
      setOrders(o);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  };

  const handleLogin = (sup) => { setSupplier(sup); load(sup); };
  const refresh = () => load(supplier);

  // SSE: subscribe when logged in, unsubscribe on logout/unmount
  useEffect(() => {
    if (!supplier) return;
    const unsubscribe = subscribeAlerts(supplier.supplierId, () => load(supplier));
    return unsubscribe;
  }, [supplier]);

  if (!supplier) return <Login suppliers={suppliers} onLogin={handleLogin} />;

  const openAlerts  = alerts.filter(a => !a.orderId);
  const quotedAlerts = alerts.filter(a => a.orderId && a.orderStatus === 'QUOTED');
  const activeOrders = orders.filter(o => ['ACCEPTED','OUT_FOR_DELIVERY','DELIVERED'].includes(o.status));

  return (
    <div className="min-h-screen bg-gray-950">
      {/* Header */}
      <header className="bg-gray-900 border-b border-gray-800 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-indigo-600 rounded-xl p-2"><Truck size={18} className="text-white" /></div>
          <div>
            <h1 className="text-base font-bold text-white">Supplier Portal</h1>
            <p className="text-xs text-gray-400">{supplier.name}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={refresh} className="text-gray-400 hover:text-white p-2 rounded-lg hover:bg-gray-800 transition-colors">
            <RefreshCw size={15} />
          </button>
          <button onClick={() => { setSupplier(null); setAlerts([]); setOrders([]); }}
            className="flex items-center gap-1.5 text-xs text-gray-400 hover:text-white px-3 py-1.5 rounded-lg hover:bg-gray-800 transition-colors">
            <LogOut size={13} /> Sign out
          </button>
        </div>
      </header>

      <div className="max-w-3xl mx-auto px-4 py-6">
        {/* Stats row */}
        <div className="grid grid-cols-3 gap-3 mb-6">
          {[
            { label: 'Open Alerts',    value: openAlerts.length,   icon: AlertTriangle, color: 'text-red-400',    bg: 'bg-red-900/30 border-red-800' },
            { label: 'Pending Quotes', value: quotedAlerts.length, icon: Package,       color: 'text-yellow-400', bg: 'bg-yellow-900/30 border-yellow-800' },
            { label: 'Active Orders',  value: activeOrders.length, icon: Truck,         color: 'text-blue-400',   bg: 'bg-blue-900/30 border-blue-800' },
          ].map(s => (
            <div key={s.label} className={`border rounded-xl p-4 ${s.bg}`}>
              <s.icon size={18} className={s.color + ' mb-2'} />
              <p className={`text-2xl font-bold ${s.color}`}>{s.value}</p>
              <p className="text-xs text-gray-400 mt-0.5">{s.label}</p>
            </div>
          ))}
        </div>

        {/* Tabs */}
        <div className="flex gap-1 bg-gray-900 rounded-lg p-1 w-fit mb-4">
          {[
            { key: 'alerts', label: `Low Stock Alerts (${openAlerts.length})` },
            { key: 'orders', label: `My Orders (${orders.length})` },
          ].map(t => (
            <button key={t.key} onClick={() => setTab(t.key)}
              className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${tab === t.key ? 'bg-gray-700 text-white' : 'text-gray-400 hover:text-gray-200'}`}>
              {t.label}
            </button>
          ))}
        </div>

        {loading ? (
          <p className="text-center text-gray-500 py-12">Loading…</p>
        ) : tab === 'alerts' ? (
          openAlerts.length === 0
            ? <p className="text-center text-gray-500 py-12">No open low-stock alerts right now.</p>
            : <div className="space-y-3">
                {openAlerts.map(a => (
                  <AlertCard key={a.alertId} alert={a} supplierId={supplier.supplierId} onQuoted={refresh} />
                ))}
              </div>
        ) : (
          orders.length === 0
            ? <p className="text-center text-gray-500 py-12">No orders yet.</p>
            : <div className="space-y-3">
                {orders.map(o => (
                  <OrderCard key={o.orderId} order={o} onUpdated={refresh} />
                ))}
              </div>
        )}
      </div>
    </div>
  );
}
