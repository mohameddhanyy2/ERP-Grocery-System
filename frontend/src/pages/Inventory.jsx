import { useEffect, useState } from 'react';
import { api, subscribePosEvents } from '../api/client';
import { useStores } from '../hooks/useStores';
import PageHeader from '../components/PageHeader';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import { Plus, RefreshCw, AlertTriangle, ShoppingCart, Pencil, Trash2 } from 'lucide-react';
import { fmt } from '../utils/fmt';

const LOW_STOCK_THRESHOLD = 10;
const CATEGORIES = ['Dairy', 'Bakery', 'Produce', 'Meat', 'Beverages', 'Frozen', 'Snacks', 'Cleaning', 'Personal Care', 'General'];

function ProductCard({ item, onOrder, onEdit, onDelete }) {
  const isLow = item.quantity > 0 && item.quantity < LOW_STOCK_THRESHOLD;
  const isOut = item.quantity === 0;
  const highlight = isLow || isOut;

  return (
    <div
      className={`rounded-xl border p-5 flex flex-col gap-3 shadow-sm transition-colors ${
        highlight ? 'bg-red-50 border-red-200' : 'bg-white border-gray-200'
      }`}
    >
      {/* Header */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            {highlight && <AlertTriangle size={14} className="text-red-500 flex-shrink-0" />}
            <h3 className="font-semibold text-gray-900 text-sm leading-tight truncate">
              {item.productName}
            </h3>
          </div>
          {highlight && (
            <p className="text-xs text-red-600 font-medium mt-0.5">
              {isOut ? 'Out of stock' : 'Low stock'}
            </p>
          )}
        </div>
        <div className="flex items-center gap-1 flex-shrink-0">
          <span className="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-full">
            {item.category || '—'}
          </span>
          <button
            onClick={() => onEdit(item)}
            className="p-1 rounded text-gray-400 hover:text-blue-600 hover:bg-blue-50 transition-colors"
            title="Edit product"
          >
            <Pencil size={12} />
          </button>
          <button
            onClick={() => onDelete(item)}
            className="p-1 rounded text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors"
            title="Delete product"
          >
            <Trash2 size={12} />
          </button>
        </div>
      </div>

      {/* Details grid */}
      <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-xs">
        <div>
          <dt className="text-gray-400">Supplier</dt>
          <dd className="font-medium text-gray-700 truncate">{item.supplierName || '—'}</dd>
        </div>
        <div>
          <dt className="text-gray-400">Store</dt>
          <dd className="font-medium text-gray-700 truncate">{item.storeName || item.storeId}</dd>
        </div>
        <div>
          <dt className="text-gray-400">Sell Price</dt>
          <dd className="font-medium text-gray-700">{fmt(item.price)}</dd>
        </div>
        <div>
          <dt className="text-gray-400">Unit Cost</dt>
          <dd className="font-medium text-gray-700">{item.unitCost > 0 ? fmt(item.unitCost) : '—'}</dd>
        </div>
        {item.barcode && (
          <div className="col-span-2">
            <dt className="text-gray-400">Barcode</dt>
            <dd className="font-mono text-gray-600">{item.barcode}</dd>
          </div>
        )}
      </dl>

      {/* Quantity + action */}
      <div className="flex items-center justify-between pt-1 border-t border-gray-100 mt-auto">
        <div className="flex items-baseline gap-1">
          <span className={`text-2xl font-bold ${isOut ? 'text-red-600' : isLow ? 'text-amber-600' : 'text-gray-900'}`}>
            {item.quantity}
          </span>
          <span className="text-xs text-gray-400">units</span>
        </div>
        {item.supplierId && (
          <button
            onClick={() => onOrder(item)}
            className="flex items-center gap-1.5 text-xs btn-primary py-1.5 px-3"
          >
            <ShoppingCart size={12} /> Order
          </button>
        )}
      </div>
    </div>
  );
}

export default function Inventory() {
  const stores = useStores();
  const [stock, setStock]           = useState([]);
  const [suppliers, setSuppliers]   = useState([]);
  const [filter, setFilter]         = useState('');
  const [storeFilter, setStoreFilter] = useState('');
  const [loading, setLoading]       = useState(true);
  const [msg, setMsg]               = useState('');

  // Store modals
  const [showAddStore, setShowAddStore]     = useState(false);
  const [showEditStore, setShowEditStore]   = useState(false);
  const [showDeleteStore, setShowDeleteStore] = useState(false);
  const [storeForm, setStoreForm]           = useState({ storeId: '', storeName: '' });
  const [editingStore, setEditingStore]     = useState(null);
  const [deletingStore, setDeletingStore]   = useState(null);

  // Product modals
  const [showOrder, setShowOrder]       = useState(false);
  const [orderItem, setOrderItem]       = useState(null);
  const [orderForm, setOrderForm]       = useState({ supplierId: '', productId: '', quantity: '', storeId: '' });
  const [showEditProduct, setShowEditProduct] = useState(false);
  const [editingProduct, setEditingProduct]   = useState(null);
  const [editProductForm, setEditProductForm] = useState({ name: '', category: '', price: '', unitCost: '', barcode: '', expiryDate: '' });

  const load = () => {
    setLoading(true);
    Promise.all([
      api.stock(storeFilter || undefined),
      api.suppliers(),
    ]).then(([s, sup]) => { setStock(s); setSuppliers(sup); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(load, [storeFilter]);

  useEffect(() => {
    const unsub = subscribePosEvents(() => load());
    return unsub;
  }, []);

  const filtered = stock
    .filter(r => {
      if (!filter) return true;
      const q = filter.toLowerCase();
      return (
        r.productName?.toLowerCase().includes(q) ||
        r.supplierName?.toLowerCase().includes(q) ||
        r.category?.toLowerCase().includes(q)
      );
    })
    .sort((a, b) => a.quantity - b.quantity);

  // ── Order ──────────────────────────────────────────────────────────
  const openOrder = (item) => {
    setOrderItem(item);
    setOrderForm({ supplierId: item.supplierId || '', productId: item.productId, quantity: '', storeId: item.storeId });
    setShowOrder(true);
  };

  const handleOrder = async (e) => {
    e.preventDefault();
    try {
      const r = await api.placeOrder({ ...orderForm, quantity: Number(orderForm.quantity) });
      setMsg(`Order placed: ${r.result}`);
      setShowOrder(false);
      setOrderItem(null);
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  // ── Edit product ───────────────────────────────────────────────────
  const openEditProduct = (item) => {
    setEditingProduct(item);
    setEditProductForm({
      name: item.productName || '',
      category: item.category || '',
      price: String(item.price ?? ''),
      unitCost: String(item.unitCost ?? ''),
      barcode: item.barcode || '',
      expiryDate: item.expiryDate || '',
    });
    setShowEditProduct(true);
  };

  const handleEditProduct = async (e) => {
    e.preventDefault();
    try {
      await api.updateProduct({
        productId: editingProduct.productId,
        name: editProductForm.name,
        category: editProductForm.category,
        price: Number(editProductForm.price),
        unitCost: Number(editProductForm.unitCost),
        barcode: editProductForm.barcode,
        expiryDate: editProductForm.expiryDate,
      });
      setMsg(`Product "${editProductForm.name}" updated.`);
      setShowEditProduct(false);
      setEditingProduct(null);
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  // ── Delete product ─────────────────────────────────────────────────
  const handleDeleteProduct = async (item) => {
    if (!window.confirm(`Delete "${item.productName}"? This cannot be undone.`)) return;
    try {
      await api.deleteProduct({ productId: item.productId });
      setMsg(`Product "${item.productName}" deleted.`);
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  // ── Store CRUD ─────────────────────────────────────────────────────
  const handleAddStore = async (e) => {
    e.preventDefault();
    try {
      const r = await api.addStore(storeForm);
      setMsg(`Store created: ${r.storeName}`);
      setShowAddStore(false);
      setStoreForm({ storeId: '', storeName: '' });
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const openEditStore = (store) => {
    setEditingStore(store);
    setShowEditStore(true);
  };

  const handleEditStore = async (e) => {
    e.preventDefault();
    try {
      await api.updateStore({ storeId: editingStore.id, storeName: editingStore.name });
      setMsg(`Store "${editingStore.name}" updated.`);
      setShowEditStore(false);
      setEditingStore(null);
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const openDeleteStore = (store) => {
    setDeletingStore(store);
    setShowDeleteStore(true);
  };

  const handleDeleteStore = async () => {
    try {
      await api.deleteStore({ storeId: deletingStore.id });
      setMsg(`Store "${deletingStore.name}" deleted.`);
      setShowDeleteStore(false);
      setDeletingStore(null);
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

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
          </div>
        }
      />

      {msg && (
        <div className="mb-4 p-3 bg-brand-100 border border-brand-200 rounded-lg text-sm text-brand-700 flex items-center justify-between">
          <span>{msg}</span>
          <button onClick={() => setMsg('')} className="text-brand-500 hover:text-brand-700 ml-4 text-xs">✕</button>
        </div>
      )}

      {/* Store list with edit/delete */}
      {stores.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-5">
          {stores.map(s => (
            <div key={s.id} className="flex items-center gap-1 bg-white border border-gray-200 rounded-lg px-3 py-1.5 text-xs text-gray-700 shadow-sm">
              <span className="font-medium">{s.name}</span>
              <span className="text-gray-400 ml-1">({s.id})</span>
              <button
                onClick={() => openEditStore(s)}
                className="ml-1 p-0.5 rounded text-gray-400 hover:text-blue-600 transition-colors"
                title="Rename store"
              >
                <Pencil size={11} />
              </button>
              <button
                onClick={() => openDeleteStore(s)}
                className="p-0.5 rounded text-gray-400 hover:text-red-600 transition-colors"
                title="Delete store"
              >
                <Trash2 size={11} />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Filters */}
      <div className="flex gap-3 mb-5">
        <input
          className="input max-w-xs"
          placeholder="Search product, supplier…"
          value={filter}
          onChange={e => setFilter(e.target.value)}
        />
        <select
          className="input max-w-48"
          value={storeFilter}
          onChange={e => setStoreFilter(e.target.value)}
        >
          <option value="">All stores</option>
          {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <button className="btn-secondary flex items-center gap-1" onClick={load}>
          <RefreshCw size={14} />
        </button>
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : filtered.length === 0 ? (
        <EmptyState text="No products found. Add products from the Supplier tab." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filtered.map((item, i) => (
            <ProductCard
              key={i}
              item={item}
              onOrder={openOrder}
              onEdit={openEditProduct}
              onDelete={handleDeleteProduct}
            />
          ))}
        </div>
      )}

      {/* Place Order modal */}
      {showOrder && orderItem && (
        <Modal title="Place Purchase Order" onClose={() => { setShowOrder(false); setOrderItem(null); }}>
          <form onSubmit={handleOrder} className="space-y-3">
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Product</label>
              <input className="input bg-gray-50" value={orderItem.productName} readOnly />
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Supplier</label>
              <input className="input bg-gray-50" value={orderItem.supplierName} readOnly />
            </div>
            {orderItem.unitCost > 0 && (
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Unit Cost</label>
                <input className="input bg-gray-50" value={fmt(orderItem.unitCost)} readOnly />
              </div>
            )}
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Store</label>
              <select
                className="input"
                value={orderForm.storeId}
                onChange={e => setOrderForm({ ...orderForm, storeId: e.target.value })}
                required
              >
                <option value="">Select store…</option>
                {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Quantity</label>
              <input
                type="number"
                min="1"
                className="input"
                placeholder="e.g. 50"
                value={orderForm.quantity}
                onChange={e => setOrderForm({ ...orderForm, quantity: e.target.value })}
                required
              />
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Place Order</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => { setShowOrder(false); setOrderItem(null); }}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}

      {/* Edit Product modal */}
      {showEditProduct && editingProduct && (
        <Modal title={`Edit Product — ${editingProduct.productName}`} onClose={() => { setShowEditProduct(false); setEditingProduct(null); }}>
          <form onSubmit={handleEditProduct} className="space-y-3">
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Product Name</label>
              <input
                className="input"
                value={editProductForm.name}
                onChange={e => setEditProductForm({ ...editProductForm, name: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Category</label>
              <select
                className="input"
                value={editProductForm.category}
                onChange={e => setEditProductForm({ ...editProductForm, category: e.target.value })}
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
                  value={editProductForm.unitCost}
                  onChange={e => setEditProductForm({ ...editProductForm, unitCost: e.target.value })}
                />
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Selling Price (EGP)</label>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  className="input"
                  value={editProductForm.price}
                  onChange={e => setEditProductForm({ ...editProductForm, price: e.target.value })}
                />
              </div>
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Barcode</label>
              <input
                className="input"
                placeholder="e.g. 6900000000001"
                value={editProductForm.barcode}
                onChange={e => setEditProductForm({ ...editProductForm, barcode: e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Expiry Date</label>
              <input
                type="date"
                className="input"
                value={editProductForm.expiryDate}
                onChange={e => setEditProductForm({ ...editProductForm, expiryDate: e.target.value })}
              />
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Save Changes</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => { setShowEditProduct(false); setEditingProduct(null); }}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}

      {/* Create Store modal */}
      {showAddStore && (
        <Modal title="Create New Store" onClose={() => setShowAddStore(false)}>
          <form onSubmit={handleAddStore} className="space-y-3">
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Store ID</label>
              <input
                className="input"
                placeholder="e.g. STORE_D or ALEX_2"
                value={storeForm.storeId}
                onChange={e => setStoreForm({ ...storeForm, storeId: e.target.value })}
                required
              />
              <p className="text-xs text-gray-600 mt-1">Will be auto-normalised to uppercase.</p>
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Store Name</label>
              <input
                className="input"
                placeholder="e.g. Mansoura Branch"
                value={storeForm.storeName}
                onChange={e => setStoreForm({ ...storeForm, storeName: e.target.value })}
                required
              />
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Create Store</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => setShowAddStore(false)}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}

      {/* Edit Store modal */}
      {showEditStore && editingStore && (
        <Modal title={`Rename Store — ${editingStore.id}`} onClose={() => { setShowEditStore(false); setEditingStore(null); }}>
          <form onSubmit={handleEditStore} className="space-y-3">
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Store Name</label>
              <input
                className="input"
                value={editingStore.name}
                onChange={e => setEditingStore({ ...editingStore, name: e.target.value })}
                required
              />
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Save</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => { setShowEditStore(false); setEditingStore(null); }}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}

      {/* Delete Store confirmation */}
      {showDeleteStore && deletingStore && (
        <Modal title="Delete Store" onClose={() => { setShowDeleteStore(false); setDeletingStore(null); }}>
          <p className="text-sm text-gray-600 mb-4">
            Are you sure you want to delete <span className="font-semibold text-gray-900">{deletingStore.name}</span>?
            This will fail if the store has any sales history or remaining stock.
          </p>
          <div className="flex gap-2">
            <button className="btn-primary flex-1 bg-red-600 hover:bg-red-700" onClick={handleDeleteStore}>Delete</button>
            <button className="btn-secondary flex-1" onClick={() => { setShowDeleteStore(false); setDeletingStore(null); }}>Cancel</button>
          </div>
        </Modal>
      )}
    </div>
  );
}
