import { useEffect, useState } from 'react';
import { api, subscribeManagerEvents } from '../api/client';
import { useStores } from '../hooks/useStores';
import PageHeader from '../components/PageHeader';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import { Plus, AlertTriangle, ChevronDown, ChevronRight, X, Pencil, Trash2 } from 'lucide-react';
import { fmt } from '../utils/fmt';

const CATEGORIES = ['Dairy', 'Bakery', 'Produce', 'Meat', 'Beverages', 'Frozen', 'Snacks', 'Cleaning', 'Personal Care', 'General'];

function SupplierRow({ supplier, products, onMsg, onRefresh }) {
  const [expanded, setExpanded]             = useState(false);
  const [assigned, setAssigned]             = useState(null);
  const [showAddProduct, setShowAddProduct]  = useState(false);
  const [showEdit, setShowEdit]             = useState(false);
  const [productForm, setProductForm]       = useState({ name: '', category: '', price: '', unitCost: '', expiryDate: '', barcode: '' });
  const [editForm, setEditForm]             = useState({ name: supplier.name, contactEmail: supplier.contactEmail || '', leadTimeDays: String(supplier.leadTimeDays || 3) });

  const loadAssigned = async () => {
    if (assigned !== null) { setExpanded(e => !e); return; }
    try {
      const data = await api.supplierProducts(supplier.supplierId);
      setAssigned(data);
      setExpanded(true);
    } catch (err) { onMsg('Error: ' + err.message); }
  };

  const handleRemove = async (productId) => {
    try {
      await api.removeProduct({ supplierId: supplier.supplierId, productId });
      setAssigned(a => a.filter(p => p.productId !== productId));
    } catch (err) { onMsg('Error: ' + err.message); }
  };

  const handleAddProduct = async (e) => {
    e.preventDefault();
    try {
      await api.addProduct({
        name: productForm.name,
        category: productForm.category,
        price: Number(productForm.price),
        unitCost: Number(productForm.unitCost),
        expiryDate: productForm.expiryDate,
        barcode: productForm.barcode,
        supplierId: supplier.supplierId,
      });
      onMsg(`Product "${productForm.name}" added under ${supplier.name}`);
      setShowAddProduct(false);
      setProductForm({ name: '', category: '', price: '', unitCost: '', expiryDate: '', barcode: '' });
      const data = await api.supplierProducts(supplier.supplierId);
      setAssigned(data);
      if (!expanded) setExpanded(true);
      onRefresh();
    } catch (err) { onMsg('Error: ' + err.message); }
  };

  const handleEditSupplier = async (e) => {
    e.preventDefault();
    try {
      await api.updateSupplier({ supplierId: supplier.supplierId, ...editForm, leadTimeDays: Number(editForm.leadTimeDays) });
      onMsg(`Supplier "${editForm.name}" updated.`);
      setShowEdit(false);
      onRefresh();
    } catch (err) { onMsg('Error: ' + err.message); }
  };

  const handleDeleteSupplier = async () => {
    if (!window.confirm(`Delete supplier "${supplier.name}"? This cannot be undone.`)) return;
    try {
      await api.deleteSupplier({ supplierId: supplier.supplierId });
      onMsg(`Supplier "${supplier.name}" deleted.`);
      onRefresh();
    } catch (err) { onMsg('Error: ' + err.message); }
  };

  return (
    <>
      <tr className="table-row cursor-pointer" onClick={loadAssigned}>
        <td className="px-4 py-3 font-medium text-gray-900 flex items-center gap-2">
          {expanded ? <ChevronDown size={13} className="text-gray-500" /> : <ChevronRight size={13} className="text-gray-500" />}
          {supplier.name}
        </td>
        <td className="px-4 py-3 text-gray-400">{supplier.contactEmail || '—'}</td>
        <td className="px-4 py-3 text-gray-400">{supplier.leadTimeDays} days</td>
        <td className="px-4 py-3 text-gray-500 text-xs">
          {assigned !== null ? `${assigned.length} product${assigned.length !== 1 ? 's' : ''}` : ''}
        </td>
        <td className="px-4 py-3" onClick={e => e.stopPropagation()}>
          <div className="flex items-center gap-2">
            <button
              className="btn-primary text-xs py-1 px-3 flex items-center gap-1"
              onClick={() => setShowAddProduct(true)}
            >
              <Plus size={11} /> Add Product
            </button>
            <button
              className="p-1.5 rounded text-gray-400 hover:text-blue-600 hover:bg-blue-50 transition-colors"
              onClick={() => setShowEdit(true)}
              title="Edit supplier"
            >
              <Pencil size={13} />
            </button>
            <button
              className="p-1.5 rounded text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors"
              onClick={handleDeleteSupplier}
              title="Delete supplier"
            >
              <Trash2 size={13} />
            </button>
          </div>
        </td>
      </tr>

      {expanded && assigned !== null && (
        <tr className="bg-gray-50">
          <td colSpan={5} className="px-6 pb-4 pt-2">
            <p className="text-xs text-gray-500 uppercase font-medium mb-2">Products supplied</p>
            {assigned.length === 0
              ? <p className="text-xs text-gray-600 mb-2">No products assigned yet. Click "Add Product" to create one.</p>
              : (
                <div className="flex flex-wrap gap-2 mb-2">
                  {assigned.map(p => (
                    <span key={p.productId} className="flex items-center gap-2 text-xs bg-white border border-gray-300 rounded-lg px-3 py-1.5 text-gray-700 shadow-sm">
                      <span>
                        <span className="font-medium">{p.productName}</span>
                        {p.unitCost > 0 && <span className="text-gray-400 ml-1">· {fmt(p.unitCost)}</span>}
                      </span>
                      <button
                        onClick={e => { e.stopPropagation(); handleRemove(p.productId); }}
                        className="text-gray-400 hover:text-red-600 ml-1"
                      >
                        <X size={11} />
                      </button>
                    </span>
                  ))}
                </div>
              )
            }
          </td>
        </tr>
      )}

      {showAddProduct && (
        <tr>
          <td colSpan={5} onClick={e => e.stopPropagation()}>
            <Modal title={`Add Product — ${supplier.name}`} onClose={() => setShowAddProduct(false)}>
              <form onSubmit={handleAddProduct} className="space-y-3">
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Product Name</label>
                  <input
                    className="input"
                    placeholder="e.g. Full Cream Milk 1L"
                    value={productForm.name}
                    onChange={e => setProductForm({ ...productForm, name: e.target.value })}
                    required
                  />
                </div>
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Category</label>
                  <select
                    className="input"
                    value={productForm.category}
                    onChange={e => setProductForm({ ...productForm, category: e.target.value })}
                    required
                  >
                    <option value="">Select category…</option>
                    {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                  </select>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs text-gray-400 mb-1 block">Unit Cost (EGP)</label>
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      className="input"
                      placeholder="e.g. 45.00"
                      value={productForm.unitCost}
                      onChange={e => setProductForm({ ...productForm, unitCost: e.target.value })}
                      required
                    />
                  </div>
                  <div>
                    <label className="text-xs text-gray-400 mb-1 block">Selling Price (EGP)</label>
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      className="input"
                      placeholder="e.g. 60.00"
                      value={productForm.price}
                      onChange={e => setProductForm({ ...productForm, price: e.target.value })}
                      required
                    />
                  </div>
                </div>
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Barcode (optional)</label>
                  <input
                    className="input"
                    placeholder="e.g. 6900000000001"
                    value={productForm.barcode}
                    onChange={e => setProductForm({ ...productForm, barcode: e.target.value })}
                  />
                </div>
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Expiry Date (optional)</label>
                  <input
                    type="date"
                    className="input"
                    value={productForm.expiryDate}
                    onChange={e => setProductForm({ ...productForm, expiryDate: e.target.value })}
                  />
                </div>
                <p className="text-xs text-gray-500">
                  Supplier: <span className="font-medium text-gray-700">{supplier.name}</span>
                </p>
                <div className="flex gap-2 pt-2">
                  <button type="submit" className="btn-primary flex-1">Add Product</button>
                  <button type="button" className="btn-secondary flex-1" onClick={() => setShowAddProduct(false)}>Cancel</button>
                </div>
              </form>
            </Modal>
          </td>
        </tr>
      )}

      {showEdit && (
        <tr>
          <td colSpan={5} onClick={e => e.stopPropagation()}>
            <Modal title={`Edit Supplier — ${supplier.name}`} onClose={() => setShowEdit(false)}>
              <form onSubmit={handleEditSupplier} className="space-y-3">
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Name</label>
                  <input className="input" value={editForm.name} onChange={e => setEditForm({ ...editForm, name: e.target.value })} required />
                </div>
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Contact Email</label>
                  <input type="email" className="input" value={editForm.contactEmail} onChange={e => setEditForm({ ...editForm, contactEmail: e.target.value })} />
                </div>
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Lead Time (days)</label>
                  <input type="number" min="1" className="input" value={editForm.leadTimeDays} onChange={e => setEditForm({ ...editForm, leadTimeDays: e.target.value })} />
                </div>
                <div className="flex gap-2 pt-2">
                  <button type="submit" className="btn-primary flex-1">Save Changes</button>
                  <button type="button" className="btn-secondary flex-1" onClick={() => setShowEdit(false)}>Cancel</button>
                </div>
              </form>
            </Modal>
          </td>
        </tr>
      )}
    </>
  );
}

export default function Supplier() {
  const stores = useStores();
  const [suppliers, setSuppliers]   = useState([]);
  const [orders, setOrders]         = useState([]);
  const [alerts, setAlerts]         = useState([]);
  const [products, setProducts]     = useState([]);
  const [tab, setTab]               = useState('alerts');
  const [loading, setLoading]       = useState(true);
  const [showSupplier, setShowSupplier] = useState(false);
  const [msg, setMsg]               = useState('');
  const [supForm, setSupForm]       = useState({ name: '', contactEmail: '', leadTimeDays: '3' });
  const [deliverOrder, setDeliverOrder] = useState(null);
  const [orderLines, setOrderLines] = useState([]);
  const [delivering, setDelivering] = useState(false);

  const load = () => {
    setLoading(true);
    Promise.all([api.suppliers(), api.orders(), api.stockAlerts(), api.products()])
      .then(([s, o, a, p]) => { setSuppliers(s); setOrders(o); setAlerts(a); setProducts(p); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  useEffect(() => {
    const unsubscribe = subscribeManagerEvents(() => load());
    return unsubscribe;
  }, []);

  const openDelivery = async (order) => {
    try {
      const lines = await api.orderLines(order.orderId);
      setOrderLines(lines);
      setDeliverOrder(order);
    } catch (err) { setMsg('Error fetching order lines: ' + err.message); }
  };

  const handleDeliver = async () => {
    if (delivering) return;
    setDelivering(true);
    try {
      for (const line of orderLines) {
        await api.recordDelivery({ orderId: deliverOrder.orderId, productId: line.productId, quantity: line.quantity });
      }
      setMsg(`Order ${deliverOrder.orderId} marked as delivered.`);
      setDeliverOrder(null);
      load();
    } catch (err) { setMsg('Delivery error: ' + err.message); }
    finally { setDelivering(false); }
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

  const STATUS_ORDER = ['OUT_FOR_DELIVERY', 'QUOTED', 'ACCEPTED', 'DELIVERED'];
  const statusRank = (s) => { const i = STATUS_ORDER.indexOf(s); return i === -1 ? STATUS_ORDER.length : i; };

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
    if (!alert.orderId)                           return <span className="badge-yellow">Awaiting Quote</span>;
    if (alert.orderStatus === 'QUOTED')           return <span className="badge-yellow">Quote Received</span>;
    if (alert.orderStatus === 'ACCEPTED')         return <span className="badge-blue">Accepted</span>;
    if (alert.orderStatus === 'OUT_FOR_DELIVERY') return <span className="badge-purple">Out for Delivery</span>;
    if (alert.orderStatus === 'DELIVERED')        return <span className="badge-green">Delivered</span>;
    return <span className="badge-blue">{alert.orderStatus}</span>;
  };

  return (
    <div>
      <PageHeader
        title="Supplier Management"
        subtitle="Low-stock alerts, purchase orders, and supplier directory"
        action={
          <button className="btn-secondary flex items-center gap-2" onClick={() => setShowSupplier(true)}>
            <Plus size={14} /> Add Supplier
          </button>
        }
      />

      {msg && (
        <div className="mb-4 p-3 bg-brand-100 border border-brand-200 rounded-lg text-sm text-brand-700">
          {msg}
        </div>
      )}

      <div className="flex gap-1 mb-4 bg-white rounded-lg p-1 w-fit">
        {[
          { key: 'alerts',    label: `Low Stock Alerts (${alerts.length})` },
          { key: 'orders',    label: `Orders (${orders.length})` },
          { key: 'suppliers', label: `Suppliers (${suppliers.length})` },
        ].map(t => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${tab === t.key ? 'bg-gray-200 text-gray-900' : 'text-gray-400 hover:text-gray-800'}`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {loading ? <LoadingSpinner /> : tab === 'alerts' ? (
        alerts.length === 0
          ? <EmptyState text="No low-stock alerts. All products are sufficiently stocked." />
          : (
            <div className="card p-0 overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-left">
                    {['Product', 'Store', 'Current Qty', 'Threshold', 'Alert Date', 'Order', 'Status'].map(h => (
                      <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {alerts.map((a, i) => (
                    <tr key={i} className="table-row">
                      <td className="px-4 py-3 text-gray-900 flex items-center gap-2">
                        <AlertTriangle size={13} className="text-amber-600 flex-shrink-0" />
                        {a.productName}
                      </td>
                      <td className="px-4 py-3 text-gray-400">{a.storeName}</td>
                      <td className="px-4 py-3 text-red-600 font-semibold">{a.currentQty}</td>
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
        orders.length === 0
          ? <EmptyState text="No orders yet. Suppliers submit quotes via the Supplier Portal." />
          : (
            <div className="card p-0 overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-left">
                    {['Order ID', 'Supplier', 'Store', 'Date', 'Cost', 'Status', ''].map(h => (
                      <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {[...orders].sort((a, b) => statusRank(a.status) - statusRank(b.status)).map((o, i) => (
                    <tr key={i} className="table-row">
                      <td className="px-4 py-3 font-mono text-xs text-gray-400">{o.orderId}</td>
                      <td className="px-4 py-3 text-gray-900">{o.supplierName || o.supplierId}</td>
                      <td className="px-4 py-3 text-gray-400">{o.storeName || o.storeId}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{o.orderDate?.slice(0, 10)}</td>
                      <td className="px-4 py-3 font-semibold text-amber-600">{fmt(o.totalCost)}</td>
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
        /* Suppliers tab */
        suppliers.length === 0
          ? <EmptyState text="No suppliers yet. Add one to get started." />
          : (
            <div className="card p-0 overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-200 text-left">
                    {['Name', 'Email', 'Lead Time', 'Products', ''].map(h => (
                      <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {suppliers.map(s => (
                    <SupplierRow
                      key={s.supplierId}
                      supplier={s}
                      products={products}
                      onMsg={setMsg}
                      onRefresh={load}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          )
      )}

      {/* Confirm Delivery modal */}
      {deliverOrder && (
        <Modal title={`Confirm Delivery — ${deliverOrder.orderId}`} onClose={() => setDeliverOrder(null)}>
          <p className="text-sm text-gray-400 mb-3">The following items will be restocked at the store:</p>
          {orderLines.length === 0 ? (
            <p className="text-sm text-amber-600">No line items found for this order.</p>
          ) : (
            <table className="w-full text-sm mb-4">
              <thead>
                <tr className="border-b border-gray-300">
                  <th className="text-left py-1 text-xs text-gray-500">Product</th>
                  <th className="text-right py-1 text-xs text-gray-500">Qty</th>
                  <th className="text-right py-1 text-xs text-gray-500">Unit Price</th>
                </tr>
              </thead>
              <tbody>
                {orderLines.map((l, i) => (
                  <tr key={i} className="border-b border-gray-200">
                    <td className="py-1 text-gray-900">{l.productName || l.productId}</td>
                    <td className="py-1 text-right text-gray-700">{l.quantity}</td>
                    <td className="py-1 text-right text-amber-600">{fmt(l.unitPrice)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <div className="flex gap-2">
            <button className="btn-primary flex-1" onClick={handleDeliver} disabled={orderLines.length === 0 || delivering}>{delivering ? 'Recording…' : 'Confirm Delivery'}</button>
            <button className="btn-secondary flex-1" onClick={() => setDeliverOrder(null)}>Cancel</button>
          </div>
        </Modal>
      )}

      {/* Add Supplier modal */}
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
