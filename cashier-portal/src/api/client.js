const BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api';

async function get(path) {
  const res = await fetch(`${BASE}${path}`);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

async function post(path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export const api = {
  stores:         () => get('/inventory/stores'),
  customers:      () => get('/customer/customers'),
  customerPoints: (customerId) => get(`/customer/points?customerId=${customerId}`),
  lookupBarcode:  (barcode, storeId) => get(`/inventory/lookup?barcode=${encodeURIComponent(barcode)}&storeId=${storeId}`),
  processSale:    (b) => post('/pos/sale', b),
  receipt:        (saleId) => get(`/pos/receipt?saleId=${saleId}`),
  redeemPoints:   (b) => post('/customer/redeempoints', b),
};
