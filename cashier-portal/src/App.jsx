import { useEffect, useRef, useState } from 'react';
import { api } from './api/client';
import {
  ShoppingCart, Trash2, Barcode, CreditCard, Banknote,
  Wallet, CheckCircle, ArrowLeft, AlertTriangle, ScanLine,
} from 'lucide-react';

const TAX_RATE = 0.14;

const fmt = (n) =>
  Number(n).toLocaleString('en-EG', { style: 'currency', currency: 'EGP', minimumFractionDigits: 2 });

// ── Store Select ────────────────────────────────────────────────────────────
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
      <div className="mb-10 text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-[#c8102e] mb-4 shadow-lg shadow-[#c8102e]/30">
          <ShoppingCart size={32} className="text-white" />
        </div>
        <h1 className="text-3xl font-bold text-white tracking-tight">Cashier Portal</h1>
        <p className="text-gray-500 mt-1 text-sm">Select your store to begin</p>
      </div>

      {loading ? (
        <p className="text-gray-600 text-sm animate-pulse">Loading stores…</p>
      ) : stores.length === 0 ? (
        <p className="text-amber-400 text-sm">No stores found. Create one in the ERP first.</p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 w-full max-w-2xl">
          {stores.map(s => (
            <button
              key={s.storeId}
              onClick={() => onSelect({ id: s.storeId, name: s.storeName })}
              className="bg-gray-900 border border-gray-800 hover:border-[#c8102e] hover:bg-gray-800/80 rounded-2xl p-6 text-left transition-all group cursor-pointer"
            >
              <div className="w-10 h-10 rounded-xl bg-gray-800 group-hover:bg-[#c8102e]/20 flex items-center justify-center mb-3 transition-colors">
                <ShoppingCart size={20} className="text-gray-500 group-hover:text-[#c8102e] transition-colors" />
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

// ── Payment Screen ──────────────────────────────────────────────────────────
function PaymentScreen({ grandTotal, onPay, onBack }) {
  const [method, setMethod] = useState('CASH');
  const [amountPaid, setAmountPaid] = useState('');
  const [paying, setPaying] = useState(false);
  const cashRef = useRef(null);
  const change = Math.max(0, Number(amountPaid) - grandTotal);
  const canPay = !paying && (method !== 'CASH' || Number(amountPaid) >= grandTotal);

  useEffect(() => {
    if (method === 'CASH') cashRef.current?.focus();
  }, [method]);

  const methods = [
    { id: 'CASH',   label: 'Cash',   icon: Banknote },
    { id: 'CARD',   label: 'Card',   icon: CreditCard },
    { id: 'WALLET', label: 'Wallet', icon: Wallet },
  ];

  const handleSubmit = (e) => {
    e.preventDefault();
    if (paying) return;
    setPaying(true);
    onPay(method, method === 'CASH' ? Number(amountPaid) : grandTotal);
  };

  return (
    <div className="min-h-screen bg-gray-950 flex flex-col items-center justify-center p-6">
      <div className="w-full max-w-sm">
        <button
          onClick={onBack}
          className="flex items-center gap-1.5 text-gray-600 hover:text-gray-300 text-sm mb-8 transition-colors"
        >
          <ArrowLeft size={14} /> Back to cart
        </button>

        <h2 className="text-2xl font-bold text-white mb-1">Payment</h2>
        <p className="text-gray-600 text-sm mb-6">Choose payment method</p>

        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 mb-6 text-center">
          <p className="text-gray-500 text-xs uppercase tracking-widest mb-2">Total Due</p>
          <p className="text-5xl font-bold text-white tabular-nums">{fmt(grandTotal)}</p>
          <p className="text-gray-600 text-xs mt-2">incl. {TAX_RATE * 100}% VAT</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
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
                      : 'border-gray-800 bg-gray-900 text-gray-500 hover:border-gray-600 hover:text-gray-300'
                  }`}
                >
                  <Icon size={22} />
                  <span className="text-xs font-medium">{m.label}</span>
                </button>
              );
            })}
          </div>

          {method === 'CASH' && (
            <div>
              <label className="text-xs text-gray-500 mb-1.5 block">Amount Received (EGP)</label>
              <input
                ref={cashRef}
                type="number"
                min={grandTotal}
                step="0.01"
                className="w-full bg-gray-900 border border-gray-700 focus:border-[#c8102e] text-white rounded-xl px-4 py-3.5 text-xl font-mono focus:outline-none transition-colors"
                placeholder={grandTotal.toFixed(2)}
                value={amountPaid}
                onChange={e => setAmountPaid(e.target.value)}
                required
              />
              {Number(amountPaid) >= grandTotal && Number(amountPaid) > 0 && (
                <p className="text-emerald-400 font-semibold text-sm mt-2">
                  Change: {fmt(change)}
                </p>
              )}
              {Number(amountPaid) > 0 && Number(amountPaid) < grandTotal && (
                <p className="text-red-400 text-xs mt-1.5">
                  Amount is {fmt(grandTotal - Number(amountPaid))} short
                </p>
              )}
            </div>
          )}

          <button
            type="submit"
            disabled={!canPay}
            className="w-full py-4 rounded-xl font-bold text-white text-lg transition-all disabled:opacity-30 disabled:cursor-not-allowed bg-[#c8102e] hover:bg-[#a60d26]"
          >
            {paying ? 'Processing…'
             : method === 'CASH'   ? 'Collect Cash'
             : method === 'CARD' ? 'Confirm Card Payment'
             :                     'Confirm Wallet Payment'}
          </button>
        </form>
      </div>
    </div>
  );
}

// ── Receipt Screen ──────────────────────────────────────────────────────────
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

        <div className="bg-gray-900 border border-gray-800 rounded-2xl p-5 mb-6 font-mono text-sm">
          <div className="text-center mb-4">
            <p className="text-white font-bold text-base tracking-wide">MASRI GROCERY</p>
            <p className="text-gray-500 text-xs">{store.name}</p>
            <p className="text-gray-600 text-xs mt-0.5">{new Date().toLocaleString()}</p>
          </div>
          <div className="border-t border-dashed border-gray-800 my-3" />
          {sale.items.map((item, i) => (
            <div key={i} className="flex justify-between text-xs mb-1.5">
              <span className="text-gray-300 truncate max-w-[65%]">
                {item.name}
                <span className="text-gray-600 ml-1">×{item.quantity}</span>
              </span>
              <span className="text-gray-400 tabular-nums">{fmt(item.lineTotal)}</span>
            </div>
          ))}
          <div className="border-t border-dashed border-gray-800 my-3" />
          <div className="flex justify-between text-xs text-gray-600 mb-1">
            <span>Subtotal</span><span className="tabular-nums">{fmt(sale.subtotal)}</span>
          </div>
          {(sale.discountAmt - (sale.pointsDiscountEgp || 0)) > 0 && (
            <div className="flex justify-between text-xs text-rose-400 mb-1">
              <span>Discount</span><span className="tabular-nums">− {fmt(sale.discountAmt - (sale.pointsDiscountEgp || 0))}</span>
            </div>
          )}
          {sale.pointsDiscountEgp > 0 && (
            <div className="flex justify-between text-xs text-amber-400 mb-1">
              <span>Points Redeemed</span><span className="tabular-nums">− {fmt(sale.pointsDiscountEgp)}</span>
            </div>
          )}
          <div className="flex justify-between text-xs text-gray-600 mb-1">
            <span>VAT (14%)</span><span className="tabular-nums">{fmt(sale.tax)}</span>
          </div>
          <div className="flex justify-between text-white font-bold text-base mt-2 pt-2 border-t border-gray-800">
            <span>TOTAL</span><span className="tabular-nums">{fmt(sale.grandTotal)}</span>
          </div>
          {sale.paymentMethod === 'CASH' && (
            <div className="flex justify-between text-xs text-emerald-400 mt-2">
              <span>Change</span><span className="tabular-nums">{fmt(change)}</span>
            </div>
          )}
          <div className="border-t border-dashed border-gray-800 my-3" />
          <p className="text-center text-gray-600 text-xs">{sale.paymentMethod}</p>
          <p className="text-center text-gray-700 text-[10px] mt-1 break-all">{sale.saleId}</p>
        </div>

        <button
          onClick={onNewSale}
          className="w-full py-4 rounded-xl font-bold text-white text-lg bg-[#c8102e] hover:bg-[#a60d26] transition-colors"
        >
          New Sale
        </button>
      </div>
    </div>
  );
}

// ── Cashier Main ────────────────────────────────────────────────────────────
function CashierMain({ store, onExit }) {
  const [cart, setCart]           = useState([]);
  const [barcodeInput, setBarcodeInput] = useState('');
  const [scanning, setScanning]   = useState(false);
  const [scanError, setScanError] = useState('');
  const [scanFlash, setScanFlash] = useState(null); // 'ok' | 'err'
  const [customers, setCustomers] = useState([]);
  const [customerId, setCustomerId] = useState('');
  const [customerPoints, setCustomerPoints] = useState(0);
  const [redeemPoints, setRedeemPoints] = useState(false);
  const [discountType, setDiscountType]   = useState('PERCENTAGE');
  const [discountValue, setDiscountValue] = useState('');
  const [screen, setScreen]       = useState('cart'); // 'cart' | 'payment' | 'receipt'
  const [completedSale, setCompletedSale] = useState(null);
  const [changeGiven, setChangeGiven]     = useState(0);
  const [msg, setMsg]             = useState('');
  const inputRef = useRef(null);
  const scanAudio = useRef(new Audio('/scan_sound.mp3'));

  useEffect(() => {
    api.customers().then(setCustomers).catch(console.error);
  }, []);

  // Fetch points whenever customer changes
  useEffect(() => {
    setRedeemPoints(false);
    setCustomerPoints(0);
    if (!customerId) return;
    api.customerPoints(customerId)
      .then(r => setCustomerPoints(r.points || 0))
      .catch(() => setCustomerPoints(0));
  }, [customerId]);

  useEffect(() => {
    if (screen === 'cart') inputRef.current?.focus();
  }, [screen, cart]);

  const subtotal = cart.reduce((s, i) => s + i.price * i.quantity, 0);
  // How much the redeemable points are worth in EGP (100 pts = 1 EGP)
  const maxPointsDiscount = Math.floor(customerPoints / 100);
  const pointsDiscountEgp = redeemPoints ? Math.min(maxPointsDiscount, subtotal) : 0;
  const manualDiscountAmt = (() => {
    const v = Number(discountValue) || 0;
    if (v <= 0) return 0;
    if (discountType === 'PERCENTAGE') return Math.min(subtotal, subtotal * v / 100);
    return Math.min(subtotal, v);
  })();
  const discountAmt = Math.round((manualDiscountAmt + pointsDiscountEgp) * 100) / 100;
  const discounted = subtotal - discountAmt;
  const tax        = Math.round(discounted * TAX_RATE * 100) / 100;
  const grandTotal = Math.round((discounted + tax) * 100) / 100;

  const flash = (type) => {
    setScanFlash(type);
    setTimeout(() => setScanFlash(null), 500);
  };

  const scanBarcode = async (e) => {
    e.preventDefault();
    const code = barcodeInput.trim();
    if (!code) return;
    scanAudio.current.currentTime = 0;
    scanAudio.current.play().catch(() => {});
    setScanError('');
    setScanning(true);
    try {
      const product = await api.lookupBarcode(code, store.id);
      if (product.quantity <= 0) {
        setScanError(`"${product.name}" is out of stock at this store.`);
        flash('err');
        setBarcodeInput('');
        setScanning(false);
        return;
      }
      setCart(prev => {
        const existing = prev.find(i => i.productId === product.productId);
        if (existing) {
          if (existing.quantity >= product.quantity) {
            setScanError(`Max stock reached for "${product.name}" (${product.quantity} units).`);
            flash('err');
            return prev;
          }
          return prev.map(i =>
            i.productId === product.productId ? { ...i, quantity: i.quantity + 1 } : i
          );
        }
        return [{
          productId: product.productId,
          name:      product.name,
          price:     product.price,
          quantity:  1,
          stock:     product.quantity,
          barcode:   code,
        }, ...prev];
      });
      flash('ok');
    } catch (err) {
      let msg = err.message;
      try { msg = JSON.parse(err.message).error; } catch { /* raw */ }
      setScanError(msg || `Barcode not found: ${code}`);
      flash('err');
    } finally {
      setBarcodeInput('');
      setScanning(false);
      inputRef.current?.focus();
    }
  };

  const updateQty = (productId, delta) => {
    setCart(prev => prev.map(i => {
      if (i.productId !== productId) return i;
      const next = Math.max(1, Math.min(i.quantity + delta, i.stock));
      return { ...i, quantity: next };
    }));
  };

  const removeItem = (productId) => setCart(prev => prev.filter(i => i.productId !== productId));

  const handlePay = async (method, amountPaid) => {
    try {
      // Combined discount: manual + points redemption
      const totalDiscount = discountAmt; // already includes pointsDiscountEgp
      const body = {
        storeId: store.id,
        customerId: customerId || 'GUEST',
        paymentMethod: method,
        amountPaid,
        items: cart.map(i => ({ productId: i.productId, quantity: i.quantity })),
      };
      if (totalDiscount > 0) { body.discountType = 'FIXED'; body.discountValue = totalDiscount; }

      const sale = await api.processSale(body);

      // Deduct redeemed points from customer account
      if (redeemPoints && customerId && pointsDiscountEgp > 0) {
        const pointsToDeduct = pointsDiscountEgp * 100;
        await api.redeemPoints({ customerId, pointsToRedeem: pointsToDeduct });
      }

      setCompletedSale({
        saleId: sale.saleId,
        items: cart.map(i => ({
          name: i.name,
          quantity: i.quantity,
          lineTotal: i.price * i.quantity,
        })),
        subtotal,
        discountAmt,
        pointsDiscountEgp,
        tax,
        grandTotal,
        paymentMethod: method,
      });
      setChangeGiven(method === 'CASH' ? Math.max(0, amountPaid - grandTotal) : 0);
      setScreen('receipt');
    } catch (err) {
      let m = err.message;
      try { m = JSON.parse(err.message).error; } catch { /* raw */ }
      setMsg('Payment failed: ' + m);
      setScreen('cart');
    }
  };

  const resetSale = () => {
    setCart([]); setBarcodeInput(''); setScanError('');
    setCustomerId(''); setCustomerPoints(0); setRedeemPoints(false);
    setDiscountValue(''); setDiscountType('PERCENTAGE');
    setCompletedSale(null); setMsg(''); setScreen('cart');
  };

  if (screen === 'payment') {
    return <PaymentScreen grandTotal={grandTotal} onPay={handlePay} onBack={() => setScreen('cart')} />;
  }
  if (screen === 'receipt' && completedSale) {
    return <ReceiptScreen sale={completedSale} change={changeGiven} store={store} onNewSale={resetSale} />;
  }

  const totalItems = cart.reduce((s, i) => s + i.quantity, 0);

  return (
    <div
      className={`min-h-screen bg-gray-950 flex flex-col lg:flex-row transition-colors duration-200 ${
        scanFlash === 'ok'  ? 'bg-emerald-950' :
        scanFlash === 'err' ? 'bg-red-950' : ''
      }`}
    >
      {/* ── Left: scanner + cart ── */}
      <div className="flex-1 flex flex-col p-6 min-h-0">

        {/* Top bar */}
        <div className="flex items-center justify-between mb-5 flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-[#c8102e] flex items-center justify-center shadow-lg shadow-[#c8102e]/30">
              <ShoppingCart size={17} className="text-white" />
            </div>
            <div>
              <h1 className="text-white font-bold text-base leading-tight">Cashier</h1>
              <p className="text-gray-600 text-xs">{store.name}</p>
            </div>
          </div>
          <button
            onClick={onExit}
            className="text-xs text-gray-700 hover:text-gray-400 flex items-center gap-1 transition-colors"
          >
            <ArrowLeft size={11} /> Switch store
          </button>
        </div>

        {/* Barcode input */}
        <form onSubmit={scanBarcode} className="mb-3 flex-shrink-0">
          <div className="relative">
            <ScanLine
              size={18}
              className={`absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none transition-colors ${
                scanning ? 'text-[#c8102e] animate-pulse' : 'text-gray-600'
              }`}
            />
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
        </form>

        {/* Scan feedback */}
        <div className="mb-3 flex-shrink-0 min-h-[20px]">
          {scanError && (
            <div className="flex items-center gap-2 text-red-400 text-xs">
              <AlertTriangle size={13} />
              <span>{scanError}</span>
            </div>
          )}
          {msg && (
            <div className="flex items-center gap-2 text-red-400 text-xs">
              <AlertTriangle size={13} />
              <span>{msg}</span>
            </div>
          )}
        </div>

        {/* Customer selector */}
        <div className="mb-4 flex-shrink-0">
          <select
            className="w-full bg-gray-900 border border-gray-800 text-gray-400 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-gray-600 transition-colors"
            value={customerId}
            onChange={e => setCustomerId(e.target.value)}
          >
            <option value="">Walk-in / Guest</option>
            {customers.map(c => (
              <option key={c.customerId} value={c.customerId}>{c.name}</option>
            ))}
          </select>
          {customerId && customerPoints > 0 && (
            <p className="text-xs text-amber-400 mt-1 pl-1">
              {customerPoints} pts available · worth {fmt(Math.floor(customerPoints / 100))} discount
            </p>
          )}
          {customerId && customerPoints === 0 && (
            <p className="text-xs text-gray-700 mt-1 pl-1">No loyalty points</p>
          )}
        </div>

        {/* Cart items */}
        {cart.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center text-gray-800 gap-3 py-12">
            <Barcode size={52} strokeWidth={1} />
            <p className="text-sm">Scan a barcode to add items to the cart</p>
          </div>
        ) : (
          <div className="flex-1 overflow-y-auto space-y-2 pr-1">
            {cart.map(item => (
              <div
                key={item.productId}
                className="bg-gray-900 border border-gray-800 rounded-xl px-4 py-3 flex items-center gap-3"
              >
                <div className="flex-1 min-w-0">
                  <p className="text-white font-medium text-sm truncate">{item.name}</p>
                  <p className="text-gray-700 text-[11px] font-mono">{item.barcode}</p>
                </div>
                {/* Qty controls */}
                <div className="flex items-center gap-1.5 flex-shrink-0">
                  <button
                    type="button"
                    onClick={() => updateQty(item.productId, -1)}
                    disabled={item.quantity <= 1}
                    className="w-7 h-7 rounded-lg bg-gray-800 text-gray-400 hover:bg-gray-700 disabled:opacity-30 text-base flex items-center justify-center transition-colors"
                  >−</button>
                  <span className="text-white font-bold w-5 text-center text-sm tabular-nums">{item.quantity}</span>
                  <button
                    type="button"
                    onClick={() => updateQty(item.productId, +1)}
                    disabled={item.quantity >= item.stock}
                    className="w-7 h-7 rounded-lg bg-gray-800 text-gray-400 hover:bg-gray-700 disabled:opacity-30 text-base flex items-center justify-center transition-colors"
                  >+</button>
                </div>
                {/* Price */}
                <div className="text-right min-w-[72px] flex-shrink-0">
                  <p className="text-white font-semibold text-sm tabular-nums">{fmt(item.price * item.quantity)}</p>
                  <p className="text-gray-600 text-[11px] tabular-nums">{fmt(item.price)} ea</p>
                </div>
                <button
                  type="button"
                  onClick={() => removeItem(item.productId)}
                  className="text-gray-700 hover:text-red-500 transition-colors ml-1 flex-shrink-0"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ── Right: summary + checkout ── */}
      <div className="w-full lg:w-80 bg-gray-900 border-t lg:border-t-0 lg:border-l border-gray-800 p-6 flex flex-col flex-shrink-0">
        <h2 className="text-gray-600 text-xs font-semibold uppercase tracking-widest mb-5">Order Summary</h2>

        {/* Discount */}
        <div className="mb-5">
          <p className="text-gray-600 text-xs mb-2">Discount (optional)</p>
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
              className="flex-1 bg-gray-800 border border-gray-700 text-gray-300 rounded-lg px-3 py-2 text-xs focus:outline-none placeholder-gray-600"
              placeholder="0"
              value={discountValue}
              onChange={e => setDiscountValue(e.target.value)}
            />
          </div>
        </div>

        {/* Points redemption toggle */}
        {customerId && customerPoints >= 100 && (
          <div className={`mb-5 rounded-xl border p-3 transition-colors ${redeemPoints ? 'border-amber-500/50 bg-amber-500/10' : 'border-gray-800 bg-gray-800/50'}`}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-amber-400">Redeem Points</p>
                <p className="text-[11px] text-gray-500 mt-0.5">
                  {customerPoints} pts → {fmt(Math.floor(customerPoints / 100))} off
                </p>
              </div>
              <button
                type="button"
                onClick={() => setRedeemPoints(r => !r)}
                className={`relative w-10 h-5 rounded-full transition-colors flex-shrink-0 ${redeemPoints ? 'bg-amber-500' : 'bg-gray-700'}`}
              >
                <span className={`absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform ${redeemPoints ? 'translate-x-5' : 'translate-x-0'}`} />
              </button>
            </div>
            {redeemPoints && (
              <p className="text-[11px] text-amber-300 mt-2">
                − {fmt(pointsDiscountEgp)} will be applied · {Math.floor(pointsDiscountEgp) * 100} pts used
              </p>
            )}
          </div>
        )}

        {/* Totals */}
        <div className="space-y-2 mb-5">
          <div className="flex justify-between text-sm text-gray-500">
            <span>Subtotal</span>
            <span className="tabular-nums">{fmt(subtotal)}</span>
          </div>
          {manualDiscountAmt > 0 && (
            <div className="flex justify-between text-sm text-rose-400">
              <span>Discount</span>
              <span className="tabular-nums">− {fmt(manualDiscountAmt)}</span>
            </div>
          )}
          {pointsDiscountEgp > 0 && (
            <div className="flex justify-between text-sm text-amber-400">
              <span>Points Redemption</span>
              <span className="tabular-nums">− {fmt(pointsDiscountEgp)}</span>
            </div>
          )}
          <div className="flex justify-between text-sm text-gray-500">
            <span>VAT (14%)</span>
            <span className="tabular-nums">{fmt(tax)}</span>
          </div>
          <div className="border-t border-gray-800 pt-3 flex justify-between">
            <span className="text-white font-bold text-xl">Total</span>
            <span className="text-white font-bold text-xl tabular-nums">{fmt(grandTotal)}</span>
          </div>
        </div>

        <p className="text-gray-700 text-xs mb-6">
          {totalItems} item{totalItems !== 1 ? 's' : ''} · {cart.length} line{cart.length !== 1 ? 's' : ''}
        </p>

        <div className="mt-auto space-y-2">
          <button
            onClick={() => setScreen('payment')}
            disabled={cart.length === 0}
            className="w-full py-4 rounded-xl font-bold text-white text-lg bg-[#c8102e] hover:bg-[#a60d26] disabled:opacity-30 disabled:cursor-not-allowed transition-all"
          >
            Charge {fmt(grandTotal)}
          </button>
          {cart.length > 0 && (
            <button
              onClick={resetSale}
              className="w-full py-2.5 rounded-xl text-gray-700 hover:text-gray-400 text-sm transition-colors"
            >
              Clear cart
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Root ────────────────────────────────────────────────────────────────────
export default function App() {
  const [store, setStore] = useState(null);
  if (!store) return <StoreSelect onSelect={setStore} />;
  return <CashierMain store={store} onExit={() => setStore(null)} />;
}
