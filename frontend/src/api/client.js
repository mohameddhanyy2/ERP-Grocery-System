const BASE = '/api';

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
  addProduct:    (b) => post('/products/add', b),
  updateProduct: (b) => post('/products/update', b),

  // Inventory
  stock:         (storeId) => get(storeId ? `/inventory/stock?storeId=${storeId}` : '/inventory/stock'),
  storeAvailable:(storeId) => get(`/inventory/available?storeId=${storeId}`),
  stores:        () => get('/inventory/stores'),
  addStore:      (b) => post('/inventory/store', b),
  lowStock:      () => get('/inventory/lowstock'),
  alerts:        () => get('/inventory/alerts'),
  restock:       (b) => post('/inventory/restock', b),

  // POS
  sales:         (storeId) => get(storeId ? `/pos/sales?storeId=${storeId}` : '/pos/sales'),
  processSale:   (b) => post('/pos/sale', b),
  applyDiscount: (b) => post('/pos/discount', b),
  processPayment:(b) => post('/pos/payment', b),
  receipt:       (saleId) => get(`/pos/receipt?saleId=${saleId}`),

  // Supplier
  suppliers:     () => get('/supplier/suppliers'),
  orders:        (storeId) => get(storeId ? `/supplier/orders?storeId=${storeId}` : '/supplier/orders'),
  placeOrder:    (b) => post('/supplier/order', b),
  orderLines:    (orderId) => get(`/supplier/orderlines?orderId=${orderId}`),
  recordDelivery:(b) => post('/supplier/delivery', b),
  addSupplier:   (b) => post('/supplier/add', b),

  // HR
  employees:     (storeId) => get(storeId ? `/hr/employees?storeId=${storeId}` : '/hr/employees'),
  shifts:        () => get('/hr/shifts'),
  payroll:       () => get('/hr/payroll'),
  attendance:    () => get('/hr/attendance'),
  addEmployee:   (b) => post('/hr/employee', b),
  addShift:      (b) => post('/hr/shift', b),
  runPayroll:    (b) => post('/hr/runpayroll', b),
  addAttendance: (b) => post('/hr/attendance', b),

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
  addCustomer:   (b) => post('/customer/add', b),
  addPoints:     (b) => post('/customer/points', b),

  // Reporting
  report:        (type, period, storeId) => get(`/reporting/report?type=${type}&period=${period}&storeId=${storeId}`),
};
