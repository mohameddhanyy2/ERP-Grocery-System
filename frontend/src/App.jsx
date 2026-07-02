import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import Dashboard  from './pages/Dashboard';
import Inventory  from './pages/Inventory';
import POS        from './pages/POS';
import Supplier   from './pages/Supplier';
import HR         from './pages/HR';
import Finance    from './pages/Finance';
import Customers  from './pages/Customers';
import Reporting  from './pages/Reporting';

export default function App() {
  return (
    <BrowserRouter>
      <div className="flex min-h-screen bg-gray-50">
        <Sidebar />
        <main className="flex-1 ml-60 p-8 overflow-y-auto">
          <Routes>
            <Route path="/"          element={<Dashboard />} />
            <Route path="/inventory" element={<Inventory />} />
            <Route path="/pos"       element={<POS />} />
            <Route path="/supplier"  element={<Supplier />} />
            <Route path="/hr"        element={<HR />} />
            <Route path="/finance"   element={<Finance />} />
            <Route path="/customers" element={<Customers />} />
            <Route path="/reporting" element={<Reporting />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
