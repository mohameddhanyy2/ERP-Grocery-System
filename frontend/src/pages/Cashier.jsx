import { useEffect, useRef, useState } from 'react';
import { api } from '../api/client';
import { fmt } from '../utils/fmt';
import { ShoppingCart, Trash2, Barcode, CreditCard, Banknote, Wallet, CheckCircle, ArrowLeft, AlertTriangle } from 'lucide-react';

const TAX_RATE = 0.14;

// ── Store selector screen ──────────────────────────────────────────────────
function StoreSelect({ onSelect }) {
  const [stores, setStores] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.stores()
      .then(setStores)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="min-h-screen bg-gray-950 flex flex-col items-center justify-center p-8">
      {/* Brand */}
      <div className="mb-10 text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-[#c8102e] mb-4 shadow-lg">
          <ShoppingCart size={32} className="text-white" />
        </div>
        <h1 className="text-3xl font-bold text-white tracking-tight">Cashier Portal</h1>
        <p className="text-gray-500 mt-1 text-sm">Select your store to begin</p>
      </div>

      {loading ? (
        <p className="text-gray-500">Loading stores…</p>
      ) : stores.length === 0 ? (
        <p className="text-amber-400 text-sm">No stores found. Create one in the ERP first.</p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 w-full max-w-2xl">
          {stores.map(s => (
            <button
              key={s.storeId}
              onClick={() => onSelect({ id: s.storeId, name: s.storeName })}
              className="bg-gray-900 border border-gray-800 hover:border-[#c8102e] hover:bg-gray-800 rounded-2xl p-6 text-left transition-all group"
            >
              <div className="w-10 h-10 rounded-xl bg-gray-800 group-hover:bg-[#c8102e]/20 flex items-center justify-center mb-3 transition-colors">
                <ShoppingCart size={20} className="text-gray-400 group-hover:text-[#c8102e]" />
              </div>
              <p className="text-white font-semibold text-lg leading-tight">{s.storeName}</p>
              <p className="text-gray-600 text-xs mt-1 font-mono">{s.storeId}</p>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

// ── Payment screen ─────────────────────────────────────────────────────────
function PaymentScreen({ grandTotal, onPay, onBack }) {
  const [method, setMethod] = useState('CASH');
  const [amountPaid, setAmountPaid] = useState('');
  const change = Math.max(0, Number(amountPaid) - grandTotal);

  const methods = [
    { id: 'CASH',   label: 'Cash',   icon: Banknote },
    { id: 'CARD',   label: 'Card',   icon: CreditCard },
    { id: 'WALLET', label: 'Wallet', icon: Wallet },
  ];

  const canPay = method !== 'CASH' || Number(amountPaid) >= grandTotal;

  const handleSubmit = (e) => {
    e.preventDefault();
    onPay(method, method === 'CASH' ? Number(amountPaid) : grandTotal);
  };

  return (
    <div className="min-h-screen bg-gray-950 flex flex-col items-center justify-center p-6">
      <div className="w-full max-w-sm">
        <button onClick={onBack} className="flex items-center gap-1 text-gray-500 hover:text-white text-sm mb-6 transition-colors">
          <ArrowLeft size={14} /> Back to cart
        </button>

        <h2 className="text-2xl font-bold text-white mb-1">Payment</h2>
        <p className="text-gray-500 text-sm mb-6">Total due</p>

        <div className="bg-gray-900 rounded-2xl p-6 mb-6 text-center">
          <p className="text-5xl font-bold text-white">{fmt(grandTotal)}</p>
          <p className="text-gray-500 text-xs mt-1">incl. {TAX_RATE * 100}% VAT</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Method selector */}
          <div className="grid grid-cols-3 gap-3">
            {methods.map(m => {
              const Icon = m.icon;
              return (
                <button
                  key={m.id}
                  type="button"
                  onClick={() => { setMethod(m.id); setAmountPaid(''); }}
                  className={`flex flex-col items-center gap-2 p-4 rounded-xl border transition-all ${
                    method === m.id
                      ? 'border-[#c8102e] bg-[#c8102e]/10 text-white'
                      : 'border-gray-800 bg-gray-900 text-gray-400 hover:border-gray-600'
                  }`}
                >
                  <Icon size={22} />
                  <span className="text-xs font-medium">{m.label}</span>
                </button>
              );
            })}
          </div>

          {/* Cash amount field */}
          {method === 'CASH' && (
            <div>
              <label className="text-xs text-gray-500 mb-1 block">Amount Received (EGP)</label>
              <input
                type="number"
                min={grandTotal}
                step="0.01"
                autoFocus
                className="w-full bg-gray-900 border border-gray-700 text-white rounded-xl px-4 py-3 text-lg font-mono focus:outline-none focus:border-[#c8102e]"
                placeholder={grandTotal.toFixed(2)}
                value={amountPaid}
                onChange={e => setAmountPaid(e.target.value)}
                required
              />
              {Number(amountPaid) > 0 && Number(amountPaid) >= grandTotal && (
                <p className="text-emerald-400 text-sm mt-2 font-medium">
                  Change: {fmt(change)}
                </p>
              )}
              {Number(amountPaid) > 0 && Number(amountPaid) < grandTotal && (
                <p className="text-red-400 text-xs mt-1">Amount is less than total due</p>
              )}
            </div>
          )}

          <button
            type="submit"
            disabled={!canPay}
            className="w-full py-4 rounded-xl font-bold text-white text-lg transition-all disabled:opacity-40 disabled:cursor-not-allowed"
            style={{ backgroundColor: canPay ? '#c8102e' : undefined, background: !canPay ? undefined : '#c8102e' }}
          >
            {method === 'CARD' ? 'Confirm Card Payment' : method === 'WALLET' ? 'Confirm Wallet Payment' : 'Collect Cash'}
          </button>
        </form>
      </div>
    </div>
  );
}

// ── Receipt screen ─────────────────────────────────────────────────────────
function ReceiptScreen({ sale, change, store, onNewSale }) {
  return (
    <div className="min-h-screen bg-gray-950 flex flex-col items-center justify-center p-6">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center mb-6">
          <div className="w-16 h-16 rounded-full bg-emerald-500/20 flex items-center justify-center mb-3">
            <CheckCircle size={36} className="text-emerald-400" />
          </div>
          <h2 className="text-2xl font-bold text-white">Sale Complete</h2>
          <p className="text-gray-500 text-sm mt-1">{store.name}</p>
        </div>

        {/* Receipt card */}
        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-5 mb-6 font-mono text-sm">
          <div className="text-center mb-4">
            <p className="text-white font-bold text-base">MASRI GROCERY</p>
            <p className="text-gray-500 text-xs">{store.name}</p>
            <p className="text-gray-600 text-xs">{new Date().toLocaleString()}</p>
          </div>
          <div className="border-t border-dashed border-gray-700 my-3" />
          {sale.items.map((item, i) => (
            <div key={i} className="flex justify-between text-xs mb-1">
              <span className="text-gray-300 truncate max-w-[65%]">
                {item.name} × {item.quantity}
              </span>
              <span className="text-gray-400">{fmt(item.lineTotal)}</span>
            </div>
          ))}
          <div className="border-t border-dashed border-gray-700 my-3" />
          <div className="flex justify-between text-xs text-gray-500 mb-1">
            <span>Subtotal</span><span>{fmt(sale.subtotal)}</span>
          </div>
          {sale.discountAmt > 0 && (
            <div className="flex justify-between text-xs text-rose-400 mb-1">
              <span>Discount</span><span>- {fmt(sale.discountAmt)}</span>
            </div>
          )}
          <div className="flex justify-between text-xs text-gray-500 mb-1">
            <span>VAT (14%)</span><span>{fmt(sale.tax)}</span>
          </div>
          <div className="flex justify-between text-white font-bold text-base mt-2">
            <span>TOTAL</span><span>{fmt(sale.grandTotal)}</span>
          </div>
          {sale.paymentMethod === 'CASH' && change >= 0 && (
            <div className="flex justify-between text-xs text-emerald-400 mt-2">
              <span>Change</span><span>{fmt(change)}</span>
            </div>
          )}
          <div className="border-t border-dashed border-gray-700 my-3" />
          <p className="text-center text-gray-600 text-xs">{sale.paymentMethod}</p>
          <p className="text-center text-gray-700 text-xs mt-1">{sale.saleId}</p>
        </div>

        <button
          onClick={onNewSale}
          className="w-full py-4 rounded-xl font-bold text-white text-lg"
          style={{ backgroundColor: '#c8102e' }}
        >
          New Sale
        </button>
      </div>
    </div>
  );
}

// ── Main Cashier screen ────────────────────────────────────────────────────
function CashierMain({ store, onExit }) {
  const [cart, setCart]             = useState([]);
  const [barcodeInput, setBarcodeInput] = useState('');
  const [scanning, setScanning]     = useState(false);
  const [scanError, setScanError]   = useState('');
  const [customers, setCustomers]   = useState([]);
  const [customerId, setCustomerId] = useState('');
  const [discountType, setDiscountType]   = useState('PERCENTAGE');
  const [discountValue, setDiscountValue] = useState('');
  const [screen, setScreen]         = useState('cart'); // 'cart' | 'payment' | 'receipt'
  const [completedSale, setCompletedSale] = useState(null);
  const [changeGiven, setChangeGiven]     = useState(0);
  const [msg, setMsg]               = useState('');
  const inputRef = useRef(null);

  useEffect(() => {
    api.customers().then(setCustomers).catch(console.error);
  }, []);

  // Keep barcode input focused on the cart screen
  useEffect(() => {
    if (screen === 'cart' && inputRef.current) {
      inputRef.current.focus();
    }
  }, [screen, cart]);

  const subtotal = cart.reduce((s, i) => s + i.price * i.quantity, 0);
  const discountAmt = (() => {
    const v = Number(discountValue) || 0;
    if (v <= 0) return 0;
    if (discountType === 'PERCENTAGE') return Math.min(subtotal, subtotal * v / 100);
    return Math.min(subtotal, v);
  })();
  const discounted = subtotal - discountAmt;
  const tax        = Math.round(discounted * TAX_RATE * 100) / 100;
  const grandTotal = Math.round((discounted + tax) * 100) / 100;

  const scanBarcode = async (e) => {
    e.preventDefault();
    const code = barcodeInput.trim();
    if (!code) return;
    setScanError('');
    setScanning(true);
    try {
      const product = await api.lookupBarcode(code, store.id);
      if (product.quantity <= 0) {
        setScanError(`"${product.name}" is out of stock at this store.`);
        setBarcodeInput('');
        setScanning(false);
        return;
      }
      setCart(prev => {
        const existing = prev.find(i => i.productId === product.productId);
        if (existing) {
          if (existing.quantity >= product.quantity) {
            setScanError(`Max stock reached for "${product.name}" (${product.quantity} units).`);
            return prev;
          }
          return prev.map(i =>
            i.productId === product.productId
              ? { ...i, quantity: i.quantity + 1 }
              : i
          );
        }
        return [...prev, {
          productId: product.productId,
          name:      product.name,
          price:     product.price,
          quantity:  1,
          stock:     product.quantity,
          barcode:   code,
        }];
      });
    } catch (err) {
      const body = err.message;
      try {
        const parsed = JSON.parse(body);
        setScanError(parsed.error || body);
      } catch {
        setScanError(`Barcode not found: ${code}`);
      }
    } finally {
      setBarcodeInput('');
      setScanning(false);
      inputRef.current?.focus();
    }
  };

  const updateQty = (productId, qty) => {
    setCart(prev => prev.map(i => {
      if (i.productId !== productId) return i;
      const clamped = Math.max(1, Math.min(Number(qty), i.stock));
      return { ...i, quantity: clamped };
    }));
  };

  const removeItem = (productId) => {
    setCart(prev => prev.filter(i => i.productId !== productId));
  };

  const handlePay = async (method, amountPaid) => {
    try {
      const body = {
        storeId: store.id,
        customerId: customerId || 'GUEST',
        paymentMethod: method,
        amountPaid,
        items: cart.map(i => ({ productId: i.productId, quantity: i.quantity })),
      };
      const dv = Number(discountValue);
      if (dv > 0) { body.discountType = discountType; body.discountValue = dv; }

      const sale = await api.processSale(body);

      // Build receipt-ready sale object
      const itemsWithNames = cart.map(i => ({
        name:      i.name,
        quantity:  i.quantity,
        unitPrice: i.price,
        lineTotal: i.price * i.quantity,
      }));

      setCompletedSale({
        saleId:        sale.saleId,
        items:         itemsWithNames,
        subtotal,
        discountAmt,
        tax,
        grandTotal,
        paymentMethod: method,
      });
      setChangeGiven(method === 'CASH' ? Math.max(0, amountPaid - grandTotal) : 0);
      setScreen('receipt');
    } catch (err) {
      setMsg('Payment failed: ' + err.message);
      setScreen('cart');
    }
  };

  const resetSale = () => {
    setCart([]);
    setBarcodeInput('');
    setScanError('');
    setCustomerId('');
    setDiscountValue('');
    setDiscountType('PERCENTAGE');
    setCompletedSale(null);
    setMsg('');
    setScreen('cart');
  };

  if (screen === 'payment') {
    return <PaymentScreen grandTotal={grandTotal} onPay={handlePay} onBack={() => setScreen('cart')} />;
  }

  if (screen === 'receipt' && completedSale) {
    return <ReceiptScreen sale={completedSale} change={changeGiven} store={store} onNewSale={resetSale} />;
  }

  // ── Cart screen ──
  return (
    <div className="min-h-screen bg-gray-950 flex flex-col lg:flex-row">

      {/* Left — scanner + cart */}
      <div className="flex-1 flex flex-col p-6 overflow-y-auto">

        {/* Top bar */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#c8102e] flex items-center justify-center">
              <ShoppingCart size={18} className="text-white" />
            </div>
            <div>
              <h1 className="text-white font-bold text-lg leading-tight">Cashier</h1>
              <p className="text-gray-600 text-xs">{store.name}</p>
            </div>
          </div>
          <button
            onClick={onExit}
            className="text-xs text-gray-600 hover:text-gray-300 flex items-center gap-1 transition-colors"
          >
            <ArrowLeft size={12} /> Switch store
          </button>
        </div>

        {/* Barcode scanner input */}
        <form onSubmit={scanBarcode} className="mb-4">
          <div className="relative">
            <Barcode size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-600 pointer-events-none" />
            <input
              ref={inputRef}
              type="text"
              autoFocus
              className="w-full bg-gray-900 border border-gray-800 focus:border-[#c8102e] text-white rounded-xl pl-11 pr-4 py-3.5 text-base font-mono placeholder-gray-700 focus:outline-none transition-colors"
              placeholder="Scan barcode or type and press Enter…"
              value={barcodeInput}
              onChange={e => { setBarcodeInput(e.target.value); setScanError(''); }}
              disabled={scanning}
            />
          </div>
          {scanError && (
            <div className="flex items-center gap-2 mt-2 text-red-400 text-sm">
              <AlertTriangle size={14} />
              <span>{scanError}</span>
            </div>
          )}
        </form>

        {msg && (
          <div className="mb-4 p-3 bg-red-900/30 border border-red-800 rounded-xl text-red-300 text-sm">
            {msg}
          </div>
        )}

        {/* Customer selector */}
        <div className="mb-4">
          <select
            className="w-full bg-gray-900 border border-gray-800 text-gray-300 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-gray-600"
            value={customerId}
            onChange={e => setCustomerId(e.target.value)}
          >
            <option value="">Walk-in / Guest</option>
            {customers.map(c => (
              <option key={c.customerId} value={c.customerId}>{c.name}</option>
            ))}
          </select>
        </div>

        {/* Cart items */}
        {cart.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center text-gray-700 gap-3 py-16">
            <Barcode size={48} strokeWidth={1} />
            <p className="text-sm">Scan a barcode to add items</p>
          </div>
        ) : (
          <div className="space-y-2 flex-1">
            {cart.map(item => (
              <div key={item.productId} className="bg-gray-900 border border-gray-800 rounded-xl px-4 py-3 flex items-center gap-3">
                <div className="flex-1 min-w-0">
                  <p className="text-white font-medium text-sm truncate">{item.name}</p>
                  <p className="text-gray-500 text-xs font-mono">{item.barcode}</p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => updateQty(item.productId, item.quantity - 1)}
                    disabled={item.quantity <= 1}
                    className="w-7 h-7 rounded-lg bg-gray-800 text-gray-400 hover:bg-gray-700 disabled:opacity-30 text-lg leading-none flex items-center justify-center transition-colors"
                  >−</button>
                  <span className="text-white font-bold w-6 text-center">{item.quantity}</span>
                  <button
                    type="button"
                    onClick={() => updateQty(item.productId, item.quantity + 1)}
                    disabled={item.quantity >= item.stock}
                    className="w-7 h-7 rounded-lg bg-gray-800 text-gray-400 hover:bg-gray-700 disabled:opacity-30 text-lg leading-none flex items-center justify-center transition-colors"
                  >+</button>
                </div>
                <div className="text-right min-w-[72px]">
                  <p className="text-white font-semibold text-sm">{fmt(item.price * item.quantity)}</p>
                  <p className="text-gray-600 text-xs">{fmt(item.price)} each</p>
                </div>
                <button
                  type="button"
                  onClick={() => removeItem(item.productId)}
                  className="text-gray-700 hover:text-red-500 transition-colors ml-1"
                >
                  <Trash2 size={15} />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Right — totals + checkout */}
      <div className="w-full lg:w-80 bg-gray-900 border-t lg:border-t-0 lg:border-l border-gray-800 p-6 flex flex-col">
        <h2 className="text-gray-400 text-xs font-semibold uppercase tracking-wider mb-4">Order Summary</h2>

        {/* Discount */}
        <div className="mb-4 space-y-2">
          <div className="flex gap-2">
            <select
              className="bg-gray-800 border border-gray-700 text-gray-300 rounded-lg px-2 py-2 text-xs focus:outline-none flex-shrink-0"
              value={discountType}
              onChange={e => setDiscountType(e.target.value)}
            >
              <option value="PERCENTAGE">% off</option>
              <option value="FIXED">EGP off</option>
            </select>
            <input
              type="number"
              min="0"
              step="0.01"
              className="flex-1 bg-gray-800 border border-gray-700 text-gray-300 rounded-lg px-3 py-2 text-xs focus:outline-none"
              placeholder="Discount (optional)"
              value={discountValue}
              onChange={e => setDiscountValue(e.target.value)}
            />
          </div>
        </div>

        {/* Totals */}
        <div className="space-y-2 mb-6">
          <div className="flex justify-between text-sm text-gray-500">
            <span>Subtotal</span><span>{fmt(subtotal)}</span>
          </div>
          {discountAmt > 0 && (
            <div className="flex justify-between text-sm text-rose-400">
              <span>Discount</span><span>− {fmt(discountAmt)}</span>
            </div>
          )}
          <div className="flex justify-between text-sm text-gray-500">
            <span>VAT (14%)</span><span>{fmt(tax)}</span>
          </div>
          <div className="border-t border-gray-800 pt-3 flex justify-between">
            <span className="text-white font-bold text-lg">Total</span>
            <span className="text-white font-bold text-lg">{fmt(grandTotal)}</span>
          </div>
        </div>

        {/* Items count */}
        <p className="text-gray-600 text-xs mb-6">
          {cart.reduce((s, i) => s + i.quantity, 0)} item{cart.reduce((s, i) => s + i.quantity, 0) !== 1 ? 's' : ''} in cart
        </p>

        <div className="mt-auto space-y-2">
          <button
            onClick={() => setScreen('payment')}
            disabled={cart.length === 0}
            className="w-full py-4 rounded-xl font-bold text-white text-base disabled:opacity-30 disabled:cursor-not-allowed transition-opacity"
            style={{ backgroundColor: '#c8102e' }}
          >
            Charge {fmt(grandTotal)}
          </button>
          {cart.length > 0 && (
            <button
              onClick={resetSale}
              className="w-full py-2.5 rounded-xl text-gray-600 hover:text-gray-300 text-sm transition-colors"
            >
              Clear cart
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Top-level Cashier router ───────────────────────────────────────────────
export default function Cashier() {
  const [store, setStore] = useState(null);

  if (!store) return <StoreSelect onSelect={setStore} />;
  return <CashierMain store={store} onExit={() => setStore(null)} />;
}
