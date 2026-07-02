// Absolute API base so the built static site talks to WildFly directly,
// without depending on a dev-server proxy. Override at build time with
// VITE_API_BASE if the backend ever moves.
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
  // Dashboard
  dashboard:     () => get('/dashboard/summary'),
  charts:        () => get('/dashboard/charts'),

  // Products
  products:      () => get('/products/list'),
  productSuppliers: () => get('/products/suppliers'),
  addProduct:    (b) => post('/products/add', b),
  updateProduct: (b) => post('/products/update', b),
  deleteProduct: (b) => post('/products/delete', b),

  // Inventory
  stock:         (storeId) => get(storeId ? `/inventory/stock?storeId=${storeId}` : '/inventory/stock'),
  storeAvailable:(storeId) => get(`/inventory/available?storeId=${storeId}`),
  stores:        () => get('/inventory/stores'),
  addStore:      (b) => post('/inventory/store', b),
  updateStore:   (b) => post('/inventory/store/update', b),
  deleteStore:   (b) => post('/inventory/store/delete', b),
  lowStock:      () => get('/inventory/lowstock'),
  alerts:        () => get('/inventory/alerts'),
  restock:       (b) => post('/inventory/restock', b),
  lookupBarcode: (barcode, storeId) => get(`/inventory/lookup?barcode=${encodeURIComponent(barcode)}&storeId=${storeId}`),

  // POS
  sales:         (storeId) => get(storeId ? `/pos/sales?storeId=${storeId}` : '/pos/sales'),
  processSale:   (b) => post('/pos/sale', b),
  applyDiscount: (b) => post('/pos/discount', b),
  processPayment:(b) => post('/pos/payment', b),
  receipt:       (saleId) => get(`/pos/receipt?saleId=${saleId}`),

  // Supplier
  suppliers:          () => get('/supplier/suppliers'),
  orders:             (storeId) => get(storeId ? `/supplier/orders?storeId=${storeId}` : '/supplier/orders'),
  placeOrder:         (b) => post('/supplier/order', b),
  stockAlerts:        () => get('/supplier/stockalerts'),
  orderLines:         (orderId) => get(`/supplier/orderlines?orderId=${orderId}`),
  acceptOrder:        (orderId) => post('/supplier/accept', { orderId }),
  recordDelivery:     (b) => post('/supplier/delivery', b),
  addSupplier:        (b) => post('/supplier/add', b),
  updateSupplier:     (b) => post('/supplier/update', b),
  deleteSupplier:     (b) => post('/supplier/delete', b),
  supplierProducts:   (supplierId) => get(`/supplier/products?supplierId=${supplierId}`),
  assignProduct:      (b) => post('/supplier/products', b),
  removeProduct:      (b) => post('/supplier/removeproduct', b),
  lastPrice:          (supplierId, productId) => get(`/supplier/lastprice?supplierId=${supplierId}&productId=${productId}`),

  // HR
  employees:        (storeId) => get(storeId ? `/hr/employees?storeId=${storeId}` : '/hr/employees'),
  shifts:           () => get('/hr/shifts'),
  payroll:          () => get('/hr/payroll'),
  attendance:       (employeeId) => get(employeeId ? `/hr/attendance?employeeId=${employeeId}` : '/hr/attendance'),
  addEmployee:      (b) => post('/hr/employee', b),
  addShift:         (b) => post('/hr/shift', b),
  runPayroll:       (b) => post('/hr/runpayroll', b),
  addAttendance:    (b) => post('/hr/attendance', b),
  checkIn:          (b) => post('/hr/checkin', b),
  paySalary:        (b) => post('/hr/paysalary', b),

  // Finance
  financeSummary:(period) => get(`/finance/summary?period=${period}`),
  revenue:       () => get('/finance/revenue'),
  expenses:      () => get('/finance/expenses'),
  addExpense:    (b) => post('/finance/expense', b),
  addRevenue:    (b) => post('/finance/revenue', b),

  // Customer
  customers:     () => get('/customer/customers'),
  loyalty:       () => get('/customer/loyalty'),
  customerPoints:(id) => get(`/customer/points?customerId=${id}`),
  customerHistory:(id) => get(`/customer/history?customerId=${id}`),
  addCustomer:   (b) => post('/customer/add', b),
  addPoints:     (b) => post('/customer/points', b),

  // Reporting
  report:        (type, period, storeId) => get(`/reporting/report?type=${type}&period=${period}&storeId=${storeId}`),

  // Admin
  resetDatabase: () => post('/reset', {}),
};

/**
 * Opens the POS SSE stream. Fires a "sale" event whenever a cashier completes
 * a sale, so the ERP pages (POS history, Inventory, Dashboard) refresh live.
 */
export function subscribePosEvents(onSale) {
  const es = new EventSource(`${BASE}/pos/events`);
  es.addEventListener('sale', onSale);
  return () => es.close();
}

/**
 * Opens the manager SSE stream. The backend pushes a named event on every
 * supplier-order change (quote / accept / out-for-delivery / delivery) and on
 * low-stock alerts, so the Supplier page can refresh live without polling.
 * onEvent() is called for any of those; returns a cleanup function.
 */
export function subscribeManagerEvents(onEvent) {
  const es = new EventSource(`${BASE}/supplier/manager-events`);
  ['order', 'alert'].forEach(ev => es.addEventListener(ev, onEvent));
  return () => es.close();
}
