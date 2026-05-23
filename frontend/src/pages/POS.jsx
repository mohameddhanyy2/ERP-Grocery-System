import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useStores } from '../hooks/useStores';
import PageHeader from '../components/PageHeader';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import { Plus, Trash2, Receipt, ShoppingCart } from 'lucide-react';
const fmt = (n) => `EGP ${Number(n).toFixed(2)}`;

export default function POS() {
  const stores = useStores();
  const [sales, setSales]         = useState([]);
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading]     = useState(true);
  const [showSale, setShowSale]   = useState(false);
  const [receipt, setReceipt]     = useState('');
  const [msg, setMsg]             = useState('');

  // Cart state
  const [storeId, setStoreId]         = useState('');
  const [customerId, setCustomerId]   = useState('');
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [amountPaid, setAmountPaid]   = useState('');
  const [cart, setCart]               = useState([]);           // [{productId, name, unitPrice, quantity, stock}]
  const [storeProducts, setStoreProducts] = useState([]);       // products available at selected store
  const [loadingProducts, setLoadingProducts] = useState(false);
  const [selectedProductId, setSelectedProductId] = useState('');

  const [discountType, setDiscountType]   = useState('PERCENTAGE');
  const [discountValue, setDiscountValue] = useState('');

  const TAX_RATE = 0.14;
  const subtotal  = cart.reduce((s, i) => s + i.unitPrice * i.quantity, 0);
  const discountAmt = (() => {
    const v = Number(discountValue) || 0;
    if (v <= 0) return 0;
    if (discountType === 'PERCENTAGE') return Math.min(subtotal, subtotal * v / 100);
    return Math.min(subtotal, v);
  })();
  const discountedSubtotal = subtotal - discountAmt;
  const tax        = Math.round(discountedSubtotal * TAX_RATE * 100) / 100;
  const grandTotal = Math.round((discountedSubtotal + tax) * 100) / 100;

  const load = () => {
    setLoading(true);
    Promise.all([api.sales(), api.customers()])
      .then(([s, c]) => { setSales(s); setCustomers(c); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  // Reload store products when store changes (while modal is open)
  const loadStoreProducts = (sid) => {
    setLoadingProducts(true);
    setCart([]);
    setSelectedProductId('');
    api.storeAvailable(sid)
      .then(setStoreProducts)
      .catch(console.error)
      .finally(() => setLoadingProducts(false));
  };

  const openSaleModal = () => {
    setCart([]);
    setSelectedProductId('');
    setAmountPaid('');
    setDiscountType('PERCENTAGE');
    setDiscountValue('');
    setStoreId(stores[0]?.id ?? '');
    setCustomerId('');
    setPaymentMethod('CASH');
    if (stores[0]?.id) loadStoreProducts(stores[0].id);
    setShowSale(true);
  };

  const handleStoreChange = (sid) => {
    setStoreId(sid);
    loadStoreProducts(sid);
  };

  const addToCart = () => {
    if (!selectedProductId) return;
    const prod = storeProducts.find(p => p.productId === selectedProductId);
    if (!prod) return;
    const existing = cart.find(i => i.productId === selectedProductId);
    if (existing) {
      setCart(cart.map(i =>
        i.productId === selectedProductId
          ? { ...i, quantity: Math.min(i.quantity + 1, i.stock) }
          : i
      ));
    } else {
      setCart([...cart, {
        productId: prod.productId,
        name: prod.name,
        unitPrice: prod.price,
        quantity: 1,
        stock: prod.quantity,
      }]);
    }
    setSelectedProductId('');
  };

  const updateQty = (productId, qty) => {
    const item = cart.find(i => i.productId === productId);
    if (!item) return;
    const clamped = Math.max(1, Math.min(Number(qty), item.stock));
    setCart(cart.map(i => i.productId === productId ? { ...i, quantity: clamped } : i));
  };

  const removeFromCart = (productId) => {
    setCart(cart.filter(i => i.productId !== productId));
  };

  const handleSale = async (e) => {
    e.preventDefault();
    if (cart.length === 0) { setMsg('Add at least one item to the cart.'); return; }
    try {
      const body = {
        storeId,
        customerId: customerId || 'GUEST',
        paymentMethod,
        amountPaid: Number(amountPaid),
        items: cart.map(i => ({ productId: i.productId, quantity: i.quantity })),
      };
      const dv = Number(discountValue);
      if (dv > 0) {
        body.discountType = discountType;
        body.discountValue = dv;
      }
      const sale = await api.processSale(body);
      setMsg(`Sale ${sale.saleId} — Total: ${fmt(sale.totalAmount)}`);
      setShowSale(false);
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const viewReceipt = async (saleId) => {
    try {
      const r = await api.receipt(saleId);
      setReceipt(r.receipt);
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const paymentColor = (m) => {
    if (m === 'CASH')   return 'badge-green';
    if (m === 'CARD')   return 'badge-blue';
    return 'badge-purple';
  };

  const availableToAdd = storeProducts.filter(
    p => p.quantity > 0 && !cart.find(i => i.productId === p.productId && i.quantity >= p.quantity)
  );

  return (
    <div>
      <PageHeader
        title="Point of Sale"
        subtitle="Process sales and view transaction history"
        action={
          <button className="btn-primary flex items-center gap-2" onClick={openSaleModal}>
            <ShoppingCart size={14} /> New Sale
          </button>
        }
      />

      {msg && <div className="mb-4 p-3 bg-brand-900/30 border border-brand-700 rounded-lg text-sm text-brand-300 whitespace-pre-wrap">{msg}</div>}

      {receipt && (
        <div className="mb-4 p-4 bg-gray-900 border border-gray-700 rounded-lg">
          <div className="flex justify-between items-center mb-2">
            <p className="text-sm font-semibold text-white flex items-center gap-2"><Receipt size={14} /> Receipt</p>
            <button className="text-xs text-gray-500 hover:text-gray-300" onClick={() => setReceipt('')}>✕ Close</button>
          </div>
          <pre className="text-xs text-gray-300 font-mono leading-relaxed">{receipt}</pre>
        </div>
      )}

      {loading ? <LoadingSpinner /> : sales.length === 0 ? <EmptyState text="No sales yet. Add products and customers first, then process a sale." /> : (
        <div className="card p-0 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-800 text-left">
                {['Sale ID', 'Store', 'Customer', 'Items', 'Total', 'Payment', 'Time', ''].map(h => (
                  <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {sales.map((s, i) => (
                <tr key={i} className="table-row">
                  <td className="px-4 py-3 font-mono text-xs text-gray-400">{s.saleId?.slice(0, 20)}</td>
                  <td className="px-4 py-3 text-gray-300">{stores.find(st => st.id === s.storeId)?.name || s.storeId}</td>
                  <td className="px-4 py-3 text-gray-400">{s.customerId === 'GUEST' ? 'Guest' : s.customerId}</td>
                  <td className="px-4 py-3 text-gray-400 text-xs">{s.itemCount ?? '—'}</td>
                  <td className="px-4 py-3 font-semibold text-emerald-400">{fmt(s.totalAmount)}</td>
                  <td className="px-4 py-3"><span className={paymentColor(s.paymentMethod)}>{s.paymentMethod}</span></td>
                  <td className="px-4 py-3 text-gray-500 text-xs">{s.timestamp?.slice(0, 19)}</td>
                  <td className="px-4 py-3">
                    <button className="btn-secondary py-1 px-2 text-xs" onClick={() => viewReceipt(s.saleId)}>Receipt</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showSale && (
        <Modal title="New Sale" onClose={() => setShowSale(false)}>
          <form onSubmit={handleSale} className="space-y-4">
            {/* Store & Customer */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Store</label>
                <select className="input" value={storeId} onChange={e => handleStoreChange(e.target.value)}>
                  {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Customer (optional)</label>
                <select className="input" value={customerId} onChange={e => setCustomerId(e.target.value)}>
                  <option value="">Walk-in / Guest</option>
                  {customers.map(c => <option key={c.customerId} value={c.customerId}>{c.name}</option>)}
                </select>
              </div>
            </div>

            {/* Product picker */}
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Add Product to Cart</label>
              {loadingProducts ? (
                <p className="text-xs text-gray-500">Loading products…</p>
              ) : storeProducts.length === 0 ? (
                <p className="text-xs text-amber-400">No products in stock at this store. Add inventory first.</p>
              ) : (
                <div className="flex gap-2">
                  <select className="input flex-1" value={selectedProductId} onChange={e => setSelectedProductId(e.target.value)}>
                    <option value="">Select a product…</option>
                    {availableToAdd.map(p => (
                      <option key={p.productId} value={p.productId}>
                        {p.name} — {fmt(p.price)} ({p.quantity} in stock)
                      </option>
                    ))}
                  </select>
                  <button type="button" className="btn-secondary px-3" onClick={addToCart} disabled={!selectedProductId}>
                    <Plus size={14} />
                  </button>
                </div>
              )}
            </div>

            {/* Cart */}
            {cart.length > 0 && (
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Cart</label>
                <div className="border border-gray-700 rounded-lg overflow-hidden">
                  <table className="w-full text-xs">
                    <thead>
                      <tr className="border-b border-gray-700 bg-gray-800/50">
                        <th className="px-3 py-2 text-left text-gray-500">Item</th>
                        <th className="px-3 py-2 text-right text-gray-500">Unit</th>
                        <th className="px-3 py-2 text-center text-gray-500">Qty</th>
                        <th className="px-3 py-2 text-right text-gray-500">Total</th>
                        <th className="px-3 py-2"></th>
                      </tr>
                    </thead>
                    <tbody>
                      {cart.map(item => (
                        <tr key={item.productId} className="border-b border-gray-800">
                          <td className="px-3 py-2 text-white font-medium">{item.name}</td>
                          <td className="px-3 py-2 text-right text-gray-400">{fmt(item.unitPrice)}</td>
                          <td className="px-3 py-2 text-center">
                            <input
                              type="number" min="1" max={item.stock}
                              className="input py-0.5 px-1 text-center w-16 text-xs"
                              value={item.quantity}
                              onChange={e => updateQty(item.productId, e.target.value)}
                            />
                            <span className="text-gray-600 ml-1">/{item.stock}</span>
                          </td>
                          <td className="px-3 py-2 text-right font-semibold text-emerald-400">
                            {fmt(item.unitPrice * item.quantity)}
                          </td>
                          <td className="px-3 py-2 text-center">
                            <button type="button" onClick={() => removeFromCart(item.productId)}
                              className="text-gray-500 hover:text-red-400 transition-colors">
                              <Trash2 size={12} />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {/* Totals */}
                  <div className="bg-gray-800/30 px-3 py-2 space-y-1">
                    <div className="flex justify-between text-xs text-gray-400">
                      <span>Subtotal</span><span>{fmt(subtotal)}</span>
                    </div>
                    {discountAmt > 0 && (
                      <div className="flex justify-between text-xs text-rose-400">
                        <span>Discount</span><span>- {fmt(discountAmt)}</span>
                      </div>
                    )}
                    <div className="flex justify-between text-xs text-gray-400">
                      <span>Tax (14%)</span><span>{fmt(tax)}</span>
                    </div>
                    <div className="flex justify-between text-sm font-bold text-white border-t border-gray-700 pt-1 mt-1">
                      <span>Grand Total</span><span className="text-emerald-400">{fmt(grandTotal)}</span>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Discount */}
            {cart.length > 0 && (
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Discount Type</label>
                  <select className="input" value={discountType} onChange={e => setDiscountType(e.target.value)}>
                    <option value="PERCENTAGE">Percentage (%)</option>
                    <option value="FIXED">Fixed (EGP)</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">
                    Discount {discountType === 'PERCENTAGE' ? '(%)' : '(EGP)'} — optional
                  </label>
                  <input
                    type="number" min="0" step="0.01" className="input"
                    placeholder="0"
                    value={discountValue}
                    onChange={e => setDiscountValue(e.target.value)}
                  />
                </div>
              </div>
            )}

            {/* Payment */}
            {cart.length > 0 && (
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Payment Method</label>
                  <select className="input" value={paymentMethod} onChange={e => setPaymentMethod(e.target.value)}>
                    <option value="CASH">Cash</option>
                    <option value="CARD">Card</option>
                    <option value="WALLET">Wallet</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Amount Paid (EGP)</label>
                  <input
                    type="number" min="0" step="0.01" className="input"
                    placeholder={`≥ ${grandTotal.toFixed(2)}`}
                    value={amountPaid}
                    onChange={e => setAmountPaid(e.target.value)}
                    required
                  />
                </div>
              </div>
            )}

            <div className="flex gap-2 pt-1">
              <button type="submit" className="btn-primary flex-1" disabled={cart.length === 0}>
                Process Sale
              </button>
              <button type="button" className="btn-secondary flex-1" onClick={() => setShowSale(false)}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
