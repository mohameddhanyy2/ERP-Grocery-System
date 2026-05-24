const BASE = '/api/supplier';

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
};

/** Opens an SSE connection for the given supplierId. Calls onAlert() whenever
 *  a new low-stock alert arrives. Returns a cleanup function to close the stream. */
export function subscribeAlerts(supplierId, onAlert) {
  const es = new EventSource(`/api/supplier/events?supplierId=${supplierId}`);
  es.addEventListener('alert', onAlert);
  return () => es.close();
}
