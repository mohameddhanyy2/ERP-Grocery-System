import { useState } from 'react';
import { api } from '../api/client';
import { useStores } from '../hooks/useStores';
import PageHeader from '../components/PageHeader';
import { BarChart3 } from 'lucide-react';

export default function Reporting() {
  const stores = useStores();
  const [type, setType]     = useState('SALES');
  const [period, setPeriod] = useState(new Date().toISOString().slice(0, 7));
  const [storeId, setStoreId] = useState('');
  const [report, setReport] = useState('');
  const [loading, setLoading] = useState(false);

  const generate = async () => {
    setLoading(true);
    try {
      const r = await api.report(type, period, storeId);
      setReport(r.report);
    } catch (err) {
      setReport('Error: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <PageHeader title="Reports" subtitle="Generate sales and finance reports" />

      <div className="card max-w-lg mb-6">
        <div className="space-y-4">
          <div>
            <label className="text-xs text-gray-400 mb-1 block">Report Type</label>
            <select className="input" value={type} onChange={e => setType(e.target.value)}>
              <option value="SALES">Sales Report</option>
              <option value="FINANCE">Finance Report</option>
              <option value="INVENTORY">Inventory Report</option>
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Period</label>
              <input type="month" className="input" value={period} onChange={e => setPeriod(e.target.value)} />
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Store</label>
              <select className="input" value={storeId} onChange={e => setStoreId(e.target.value)}>
                <option value="">All stores</option>
                {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
          </div>
          <button className="btn-primary w-full flex items-center justify-center gap-2" onClick={generate} disabled={loading}>
            <BarChart3 size={14} />
            {loading ? 'Generating...' : 'Generate Report'}
          </button>
        </div>
      </div>

      {report && (
        <div className="card">
          <h3 className="text-sm font-semibold text-gray-300 mb-3">Report Output — {type} / {period} / {storeId}</h3>
          <pre className="text-sm text-gray-300 whitespace-pre-wrap font-mono bg-gray-950 rounded-lg p-4 border border-gray-800">
            {report}
          </pre>
        </div>
      )}
    </div>
  );
}
