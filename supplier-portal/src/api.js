// Absolute API base so the built static site talks to WildFly directly,
// without depending on a dev-server proxy. Override with VITE_API_BASE.
const API_ROOT = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api';
const BASE = `${API_ROOT}/supplier`;

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
  suppliers:        ()           => get('/suppliers'),
  stockAlerts:      (supplierId) => get(`/stockalerts?supplierId=${supplierId}`),
  allAlerts:        ()           => get('/stockalerts'),
  orders:           (supplierId) => get(`/orders?supplierId=${supplierId}`),
  orderLines:       (orderId)    => get(`/orderlines?orderId=${orderId}`),
  quote:            (body)       => post('/quote', body),
  outForDelivery:   (orderId)    => post('/outfordelivery', { orderId }),
  lastPrice:        (supplierId, productId) => get(`/lastprice?supplierId=${supplierId}&productId=${productId}`),
};

/** Opens an SSE connection for the given supplierId. Calls onEvent() whenever a
 *  low-stock alert ('alert') OR an order change ('order') for this supplier
 *  arrives — e.g. the manager accepting a quote or recording delivery — so the
 *  portal refreshes live. Returns a cleanup function to close the stream. */
export function subscribeAlerts(supplierId, onEvent) {
  const es = new EventSource(`${BASE}/events?supplierId=${supplierId}`);
  ['alert', 'order'].forEach(ev => es.addEventListener(ev, onEvent));
  return () => es.close();
}
