import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useStores } from '../hooks/useStores';
import PageHeader from '../components/PageHeader';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import { Plus, AlertTriangle, ChevronDown, ChevronRight, X } from 'lucide-react';
import { fmt } from '../utils/fmt';

function SupplierRow({ supplier, products, onMsg }) {
  const [expanded, setExpanded]         = useState(false);
  const [assigned, setAssigned]         = useState(null); // null = not loaded
  const [addingProduct, setAddingProduct] = useState('');

  const loadAssigned = async () => {
    if (assigned !== null) { setExpanded(e => !e); return; }
    try {
      const data = await api.supplierProducts(supplier.supplierId);
      setAssigned(data);
      setExpanded(true);
    } catch (err) { onMsg('Error: ' + err.message); }
  };

  const handleAdd = async () => {
    if (!addingProduct) return;
    try {
      await api.assignProduct({ supplierId: supplier.supplierId, productId: addingProduct });
      const data = await api.supplierProducts(supplier.supplierId);
      setAssigned(data);
      setAddingProduct('');
    } catch (err) { onMsg('Error: ' + err.message); }
  };

  const handleRemove = async (productId) => {
    try {
      await api.removeProduct({ supplierId: supplier.supplierId, productId });
      setAssigned(a => a.filter(p => p.productId !== productId));
    } catch (err) { onMsg('Error: ' + err.message); }
  };

  const unassignedProducts = products.filter(
    p => !assigned?.some(a => a.productId === p.productId)
  );

  return (
    <>
      <tr className="table-row cursor-pointer" onClick={loadAssigned}>
        <td className="px-4 py-3 font-medium text-white flex items-center gap-2">
          {expanded ? <ChevronDown size={13} className="text-gray-500" /> : <ChevronRight size={13} className="text-gray-500" />}
          {supplier.name}
        </td>
        <td className="px-4 py-3 text-gray-400">{supplier.contactEmail || '—'}</td>
        <td className="px-4 py-3 text-gray-400">{supplier.leadTimeDays} days</td>
        <td className="px-4 py-3 text-gray-500 text-xs">
          {assigned !== null ? `${assigned.length} product${assigned.length !== 1 ? 's' : ''}` : ''}
        </td>
      </tr>
      {expanded && assigned !== null && (
        <tr className="bg-gray-900/50">
          <td colSpan={4} className="px-6 pb-4 pt-2">
            <p className="text-xs text-gray-500 uppercase font-medium mb-2">Products supplied</p>
            {assigned.length === 0
              ? <p className="text-xs text-gray-600 mb-2">No products assigned yet.</p>
              : (
                <div className="flex flex-wrap gap-2 mb-3">
                  {assigned.map(p => (
                    <span key={p.productId} className="flex items-center gap-1 text-xs bg-gray-800 border border-gray-700 rounded-full px-3 py-1 text-gray-300">
                      {p.productName}
                      <button onClick={e => { e.stopPropagation(); handleRemove(p.productId); }} className="text-gray-500 hover:text-red-400 ml-1">
                        <X size={11} />
                      </button>
                    </span>
                  ))}
                </div>
              )
            }
            {unassignedProducts.length > 0 && (
              <div className="flex gap-2 items-center" onClick={e => e.stopPropagation()}>
                <select
                  className="input text-xs py-1 h-8 flex-1 max-w-xs"
                  value={addingProduct}
                  onChange={e => setAddingProduct(e.target.value)}
                >
                  <option value="">Assign a product…</option>
                  {unassignedProducts.map(p => (
                    <option key={p.productId} value={p.productId}>{p.name}</option>
                  ))}
                </select>
                <button
                  className="btn-primary text-xs py-1 px-3 h-8"
                  disabled={!addingProduct}
                  onClick={handleAdd}
                >
                  Assign
                </button>
              </div>
            )}
          </td>
        </tr>
      )}
    </>
  );
}

export default function Supplier() {
  const stores = useStores();
  const [suppliers, setSuppliers] = useState([]);
  const [orders, setOrders]       = useState([]);
  const [alerts, setAlerts]       = useState([]);
  const [products, setProducts]   = useState([]);
  const [tab, setTab]             = useState('alerts');
  const [loading, setLoading]     = useState(true);
  const [showSupplier, setShowSupplier] = useState(false);
  const [showOrder, setShowOrder] = useState(false);
  const [msg, setMsg]             = useState('');
  const [supForm, setSupForm]     = useState({ name: '', contactEmail: '', leadTimeDays: '3' });
  const [orderForm, setOrderForm] = useState({ supplierId: '', productId: '', quantity: '', storeId: '' });
  const [deliverOrder, setDeliverOrder] = useState(null);
  const [orderLines, setOrderLines]     = useState([]);

  const load = () => {
    setLoading(true);
    Promise.all([api.suppliers(), api.orders(), api.stockAlerts(), api.products()])
      .then(([s, o, a, p]) => { setSuppliers(s); setOrders(o); setAlerts(a); setProducts(p); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  const handleOrder = async (e) => {
    e.preventDefault();
    try {
      const r = await api.placeOrder({ ...orderForm, quantity: Number(orderForm.quantity) });
      setMsg(`Order placed: ${r.result}`);
      setShowOrder(false);
      setOrderForm({ supplierId: '', productId: '', quantity: '', storeId: '' });
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const openDelivery = async (order) => {
    try {
      const lines = await api.orderLines(order.orderId);
      setOrderLines(lines);
      setDeliverOrder(order);
    } catch (err) { setMsg('Error fetching order lines: ' + err.message); }
  };

  const handleDeliver = async () => {
    try {
      for (const line of orderLines) {
        await api.recordDelivery({ orderId: deliverOrder.orderId, productId: line.productId, quantity: line.quantity });
      }
      setMsg(`Order ${deliverOrder.orderId} marked as delivered.`);
      setDeliverOrder(null);
      load();
    } catch (err) { setMsg('Delivery error: ' + err.message); }
  };

  const handleAddSupplier = async (e) => {
    e.preventDefault();
    try {
      const r = await api.addSupplier({ ...supForm, leadTimeDays: Number(supForm.leadTimeDays) });
      setMsg(`Supplier added: ${r.name}`);
      setShowSupplier(false);
      setSupForm({ name: '', contactEmail: '', leadTimeDays: '3' });
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const statusBadge = (s) => {
    if (s === 'DELIVERED')        return <span className="badge-green">Delivered</span>;
    if (s === 'OUT_FOR_DELIVERY') return <span className="badge-purple">Out for Delivery</span>;
    if (s === 'ACCEPTED')         return <span className="badge-blue">Accepted</span>;
    if (s === 'QUOTED')           return <span className="badge-yellow">Quoted</span>;
    return <span className="badge-blue">{s}</span>;
  };

  const handleAccept = async (orderId) => {
    try {
      await api.acceptOrder(orderId);
      setMsg('Order accepted.');
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const alertStatusBadge = (alert) => {
    if (!alert.orderId)                          return <span className="badge-yellow">Awaiting Quote</span>;
    if (alert.orderStatus === 'QUOTED')          return <span className="badge-yellow">Quote Received</span>;
    if (alert.orderStatus === 'ACCEPTED')        return <span className="badge-blue">Accepted</span>;
    if (alert.orderStatus === 'OUT_FOR_DELIVERY') return <span className="badge-purple">Out for Delivery</span>;
    if (alert.orderStatus === 'DELIVERED')       return <span className="badge-green">Delivered</span>;
    return <span className="badge-blue">{alert.orderStatus}</span>;
  };

  return (
    <div>
      <PageHeader
        title="Supplier Management"
        subtitle="Low-stock alerts, purchase orders, and supplier directory"
        action={
          <div className="flex gap-2">
            <button className="btn-secondary flex items-center gap-2" onClick={() => setShowSupplier(true)}><Plus size={14} /> Add Supplier</button>
            <button className="btn-primary flex items-center gap-2" onClick={() => setShowOrder(true)}><Plus size={14} /> Place Order</button>
          </div>
        }
      />

      {msg && <div className="mb-4 p-3 bg-brand-900/30 border border-brand-700 rounded-lg text-sm text-brand-300">{msg}</div>}

      <div className="flex gap-1 mb-4 bg-gray-900 rounded-lg p-1 w-fit">
        {[
          { key: 'alerts',    label: `Low Stock Alerts (${alerts.length})` },
          { key: 'orders',    label: `Orders (${orders.length})` },
          { key: 'suppliers', label: `Suppliers (${suppliers.length})` },
        ].map(t => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${tab === t.key ? 'bg-gray-700 text-white' : 'text-gray-400 hover:text-gray-200'}`}>
            {t.label}
          </button>
        ))}
      </div>

      {loading ? <LoadingSpinner /> : tab === 'alerts' ? (
        alerts.length === 0 ? <EmptyState text="No low-stock alerts. All products are sufficiently stocked." /> : (
          <div className="card p-0 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-800 text-left">
                  {['Product', 'Store', 'Current Qty', 'Threshold', 'Alert Date', 'Order', 'Status'].map(h => (
                    <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {alerts.map((a, i) => (
                  <tr key={i} className="table-row">
                    <td className="px-4 py-3 text-white flex items-center gap-2">
                      <AlertTriangle size={13} className="text-amber-400 flex-shrink-0" />
                      {a.productName}
                    </td>
                    <td className="px-4 py-3 text-gray-400">{a.storeName}</td>
                    <td className="px-4 py-3 text-red-400 font-semibold">{a.currentQty}</td>
                    <td className="px-4 py-3 text-gray-500">{a.threshold}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{a.alertDate}</td>
                    <td className="px-4 py-3 font-mono text-xs text-gray-500">{a.orderId || '—'}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        {alertStatusBadge(a)}
                        {a.orderStatus === 'QUOTED' && (
                          <button className="btn-primary text-xs py-1 px-2" onClick={() => handleAccept(a.orderId)}>Accept</button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : tab === 'orders' ? (
        orders.length === 0 ? <EmptyState text="No orders yet. Suppliers submit quotes via the Supplier Portal." /> : (
          <div className="card p-0 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-800 text-left">
                  {['Order ID', 'Supplier', 'Store', 'Date', 'Cost', 'Status', ''].map(h => (
                    <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {orders.map((o, i) => (
                  <tr key={i} className="table-row">
                    <td className="px-4 py-3 font-mono text-xs text-gray-400">{o.orderId}</td>
                    <td className="px-4 py-3 text-white">{o.supplierName || o.supplierId}</td>
                    <td className="px-4 py-3 text-gray-400">{o.storeName || o.storeId}</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{o.orderDate?.slice(0, 10)}</td>
                    <td className="px-4 py-3 font-semibold text-amber-400">{fmt(o.totalCost)}</td>
                    <td className="px-4 py-3">{statusBadge(o.status)}</td>
                    <td className="px-4 py-3">
                      {o.status === 'QUOTED' && (
                        <button className="btn-primary text-xs py-1 px-2" onClick={() => handleAccept(o.orderId)}>Accept</button>
                      )}
                      {o.status === 'OUT_FOR_DELIVERY' && (
                        <button className="btn-secondary text-xs py-1 px-2" onClick={() => openDelivery(o)}>Mark Delivered</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : (
        suppliers.length === 0 ? <EmptyState text="No suppliers yet. Add one to get started." /> : (
          <div className="card p-0 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-800 text-left">
                  {['Name', 'Email', 'Lead Time', 'Products'].map(h => (
                    <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {suppliers.map(s => (
                  <SupplierRow key={s.supplierId} supplier={s} products={products} onMsg={setMsg} />
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {deliverOrder && (
        <Modal title={`Confirm Delivery — ${deliverOrder.orderId}`} onClose={() => setDeliverOrder(null)}>
          <p className="text-sm text-gray-400 mb-3">The following items will be restocked at the store:</p>
          {orderLines.length === 0 ? (
            <p className="text-sm text-amber-400">No line items found for this order.</p>
          ) : (
            <table className="w-full text-sm mb-4">
              <thead>
                <tr className="border-b border-gray-700">
                  <th className="text-left py-1 text-xs text-gray-500">Product</th>
                  <th className="text-right py-1 text-xs text-gray-500">Qty</th>
                  <th className="text-right py-1 text-xs text-gray-500">Unit Price</th>
                </tr>
              </thead>
              <tbody>
                {orderLines.map((l, i) => (
                  <tr key={i} className="border-b border-gray-800">
                    <td className="py-1 text-white">{l.productName || l.productId}</td>
                    <td className="py-1 text-right text-gray-300">{l.quantity}</td>
                    <td className="py-1 text-right text-amber-400">{fmt(l.unitPrice)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <div className="flex gap-2">
            <button className="btn-primary flex-1" onClick={handleDeliver} disabled={orderLines.length === 0}>Confirm Delivery</button>
            <button className="btn-secondary flex-1" onClick={() => setDeliverOrder(null)}>Cancel</button>
          </div>
        </Modal>
      )}

      {showOrder && (
        <Modal title="Place Purchase Order" onClose={() => setShowOrder(false)}>
          {suppliers.length === 0 || products.length === 0 ? (
            <p className="text-sm text-amber-400">You need at least one supplier and one product before placing an order.</p>
          ) : (
            <form onSubmit={handleOrder} className="space-y-3">
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Supplier</label>
                <select className="input" value={orderForm.supplierId} onChange={e => setOrderForm({ ...orderForm, supplierId: e.target.value })} required>
                  <option value="">Select supplier…</option>
                  {suppliers.map(s => <option key={s.supplierId} value={s.supplierId}>{s.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Product</label>
                <select className="input" value={orderForm.productId} onChange={e => setOrderForm({ ...orderForm, productId: e.target.value })} required>
                  <option value="">Select product…</option>
                  {products.map(p => <option key={p.productId} value={p.productId}>{p.name} — {fmt(p.price)}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Store</label>
                <select className="input" value={orderForm.storeId} onChange={e => setOrderForm({ ...orderForm, storeId: e.target.value })} required>
                  <option value="">Select store…</option>
                  {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Quantity</label>
                <input type="number" min="1" className="input" value={orderForm.quantity} onChange={e => setOrderForm({ ...orderForm, quantity: e.target.value })} required />
              </div>
              <div className="flex gap-2 pt-2">
                <button type="submit" className="btn-primary flex-1">Place Order</button>
                <button type="button" className="btn-secondary flex-1" onClick={() => setShowOrder(false)}>Cancel</button>
              </div>
            </form>
          )}
        </Modal>
      )}

      {showSupplier && (
        <Modal title="Add Supplier" onClose={() => setShowSupplier(false)}>
          <form onSubmit={handleAddSupplier} className="space-y-3">
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Name</label>
              <input className="input" placeholder="Supplier name" value={supForm.name} onChange={e => setSupForm({ ...supForm, name: e.target.value })} required />
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Contact Email</label>
              <input type="email" className="input" placeholder="supplier@email.com" value={supForm.contactEmail} onChange={e => setSupForm({ ...supForm, contactEmail: e.target.value })} />
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Lead Time (days)</label>
              <input type="number" min="1" className="input" value={supForm.leadTimeDays} onChange={e => setSupForm({ ...supForm, leadTimeDays: e.target.value })} />
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Add Supplier</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => setShowSupplier(false)}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
