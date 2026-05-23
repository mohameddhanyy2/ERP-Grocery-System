import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useStores } from '../hooks/useStores';
import PageHeader from '../components/PageHeader';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import { Plus } from 'lucide-react';

const ROLES = ['Cashier', 'Manager', 'Supervisor', 'Stock Clerk', 'Security', 'Cleaner'];

export default function HR() {
  const stores = useStores();
  const [employees, setEmployees]   = useState([]);
  const [shifts, setShifts]         = useState([]);
  const [payroll, setPayroll]       = useState([]);
  const [attendance, setAttendance] = useState([]);
  const [tab, setTab]               = useState('employees');
  const [loading, setLoading]       = useState(true);
  const [showAddEmp, setShowAddEmp] = useState(false);
  const [showAddShift, setShowAddShift]         = useState(false);
  const [showAddAttendance, setShowAddAttendance] = useState(false);
  const [showRunPayroll, setShowRunPayroll]      = useState(false);
  const [msg, setMsg]               = useState('');

  const [empForm, setEmpForm] = useState({ name: '', storeId: '', role: 'Cashier', hourlyRate: '' });
  const [shiftForm, setShiftForm] = useState({ employeeId: '', storeId: '', shiftStart: '', shiftEnd: '', hoursWorked: '' });
  const [attEmployeeId, setAttEmployeeId] = useState('');
  const [payForm, setPayForm]   = useState({ employeeId: '', period: new Date().toISOString().slice(0, 7) });

  const load = () => {
    setLoading(true);
    Promise.all([api.employees(), api.shifts(), api.payroll(), api.attendance()])
      .then(([e, s, p, a]) => { setEmployees(e); setShifts(s); setPayroll(p); setAttendance(a); })
      .catch(console.error)
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  const handleAddEmp = async (e) => {
    e.preventDefault();
    try {
      await api.addEmployee({ ...empForm, hourlyRate: Number(empForm.hourlyRate) });
      setMsg('Employee added.');
      setShowAddEmp(false);
      setEmpForm({ name: '', storeId: '', role: 'Cashier', hourlyRate: '' });
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const handleAddShift = async (e) => {
    e.preventDefault();
    try {
      await api.addShift({ ...shiftForm, hoursWorked: Number(shiftForm.hoursWorked) });
      setMsg('Shift recorded.');
      setShowAddShift(false);
      setShiftForm({ employeeId: '', storeId: '', shiftStart: '', shiftEnd: '', hoursWorked: '' });
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const handleAddAttendance = async (e) => {
    e.preventDefault();
    try {
      await api.addAttendance({ employeeId: attEmployeeId });
      setMsg('Attendance recorded.');
      setShowAddAttendance(false);
      setAttEmployeeId('');
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const handleRunPayroll = async (e) => {
    e.preventDefault();
    try {
      const r = await api.runPayroll(payForm);
      setMsg(`Payroll complete — Net Pay: EGP ${Number(r.netPay).toFixed(2)}`);
      setShowRunPayroll(false);
      setPayForm({ employeeId: '', period: new Date().toISOString().slice(0, 7) });
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  // Filter employees by selected store in shift/attendance forms
  const empForStore = (storeId) => employees.filter(e => e.storeId === storeId);

  const tabs = [
    { key: 'employees',  label: `Employees (${employees.length})` },
    { key: 'shifts',     label: `Shifts (${shifts.length})` },
    { key: 'payroll',    label: `Payroll (${payroll.length})` },
    { key: 'attendance', label: `Attendance (${attendance.length})` },
  ];

  const tabActions = {
    employees:  <button className="btn-primary flex items-center gap-2" onClick={() => setShowAddEmp(true)}><Plus size={14} /> Add Employee</button>,
    shifts:     <button className="btn-primary flex items-center gap-2" onClick={() => setShowAddShift(true)}><Plus size={14} /> Add Shift</button>,
    payroll:    <button className="btn-primary flex items-center gap-2" onClick={() => setShowRunPayroll(true)}><Plus size={14} /> Run Payroll</button>,
    attendance: <button className="btn-primary flex items-center gap-2" onClick={() => setShowAddAttendance(true)}><Plus size={14} /> Record Attendance</button>,
  };

  return (
    <div>
      <PageHeader
        title="HR & Staff Management"
        subtitle="Employees, shifts, payroll and attendance"
        action={tabActions[tab]}
      />

      {msg && <div className="mb-4 p-3 bg-brand-900/30 border border-brand-700 rounded-lg text-sm text-brand-300">{msg}</div>}

      <div className="flex gap-1 mb-4 bg-gray-900 rounded-lg p-1 w-fit flex-wrap">
        {tabs.map(t => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${tab === t.key ? 'bg-gray-700 text-white' : 'text-gray-400 hover:text-gray-200'}`}>
            {t.label}
          </button>
        ))}
      </div>

      {loading ? <LoadingSpinner /> : (
        <>
          {tab === 'employees' && (
            employees.length === 0 ? <EmptyState text="No employees yet. Add your first employee." /> : (
              <div className="card p-0 overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-800 text-left">
                      {['Name', 'Store', 'Role', 'Hourly Rate', 'Start Date'].map(h => (
                        <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {employees.map((e, i) => (
                      <tr key={i} className="table-row">
                        <td className="px-4 py-3">
                          <p className="font-medium text-white">{e.name}</p>
                          <p className="text-xs text-gray-500">{e.employeeId}</p>
                        </td>
                        <td className="px-4 py-3 text-gray-400">{stores.find(s => s.id === e.storeId)?.name || e.storeId}</td>
                        <td className="px-4 py-3"><span className="badge-blue">{e.role}</span></td>
                        <td className="px-4 py-3 text-emerald-400">EGP {e.hourlyRate}/hr</td>
                        <td className="px-4 py-3 text-gray-500 text-xs">{e.startDate || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}

          {tab === 'shifts' && (
            shifts.length === 0 ? <EmptyState text="No shifts recorded yet." /> : (
              <div className="card p-0 overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-800 text-left">
                      {['Employee', 'Store', 'Start', 'End', 'Hours'].map(h => (
                        <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {shifts.map((s, i) => (
                      <tr key={i} className="table-row">
                        <td className="px-4 py-3 font-medium text-white">{s.employeeName || s.employeeId}</td>
                        <td className="px-4 py-3 text-gray-400">{stores.find(st => st.id === s.storeId)?.name || s.storeId}</td>
                        <td className="px-4 py-3 text-gray-400 text-xs">{s.shiftStart?.slice(0, 16)}</td>
                        <td className="px-4 py-3 text-gray-400 text-xs">{s.shiftEnd?.slice(0, 16)}</td>
                        <td className="px-4 py-3 font-semibold text-white">{s.hoursWorked}h</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}

          {tab === 'payroll' && (
            payroll.length === 0 ? <EmptyState text="No payroll records yet. Run payroll for an employee." /> : (
              <div className="card p-0 overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-800 text-left">
                      {['Employee', 'Period', 'Gross Pay', 'Deductions (15%)', 'Net Pay'].map(h => (
                        <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {payroll.map((p, i) => (
                      <tr key={i} className="table-row">
                        <td className="px-4 py-3 font-medium text-white">{p.employeeName || p.employeeId}</td>
                        <td className="px-4 py-3 text-gray-400">{p.period}</td>
                        <td className="px-4 py-3 text-gray-300">EGP {Number(p.grossPay).toFixed(2)}</td>
                        <td className="px-4 py-3 text-red-400">-EGP {Number(p.deductions).toFixed(2)}</td>
                        <td className="px-4 py-3 font-semibold text-emerald-400">EGP {Number(p.netPay).toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}

          {tab === 'attendance' && (
            attendance.length === 0 ? <EmptyState text="No attendance records yet." /> : (
              <div className="card p-0 overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-800 text-left">
                      {['Employee', 'Store', 'Date', 'Status'].map(h => (
                        <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {attendance.map((a, i) => (
                      <tr key={i} className="table-row">
                        <td className="px-4 py-3 font-medium text-white">{a.employeeName || a.employeeId}</td>
                        <td className="px-4 py-3 text-gray-400">{stores.find(s => s.id === a.storeId)?.name || a.storeId}</td>
                        <td className="px-4 py-3 text-gray-400 text-xs">{a.date}</td>
                        <td className="px-4 py-3">{a.present ? <span className="badge-green">Present</span> : <span className="badge-red">Absent</span>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}
        </>
      )}

      {/* Add Employee */}
      {showAddEmp && (
        <Modal title="Add Employee" onClose={() => setShowAddEmp(false)}>
          <form onSubmit={handleAddEmp} className="space-y-3">
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Full Name</label>
              <input className="input" placeholder="Employee full name" value={empForm.name} onChange={e => setEmpForm({ ...empForm, name: e.target.value })} required />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Store</label>
                <select className="input" value={empForm.storeId} onChange={e => setEmpForm({ ...empForm, storeId: e.target.value })}>
                  {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Role</label>
                <select className="input" value={empForm.role} onChange={e => setEmpForm({ ...empForm, role: e.target.value })}>
                  {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>
            </div>
            <div>
              <label className="text-xs text-gray-400 mb-1 block">Hourly Rate (EGP)</label>
              <input type="number" min="1" step="0.5" className="input" placeholder="e.g. 25" value={empForm.hourlyRate} onChange={e => setEmpForm({ ...empForm, hourlyRate: e.target.value })} required />
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Add Employee</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => setShowAddEmp(false)}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}

      {/* Add Shift */}
      {showAddShift && (
        <Modal title="Record Shift" onClose={() => setShowAddShift(false)}>
          {employees.length === 0 ? (
            <p className="text-sm text-amber-400">Add employees before recording shifts.</p>
          ) : (
            <form onSubmit={handleAddShift} className="space-y-3">
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Store</label>
                <select className="input" value={shiftForm.storeId}
                  onChange={e => setShiftForm({ ...shiftForm, storeId: e.target.value, employeeId: '' })}>
                  {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Employee</label>
                <select className="input" value={shiftForm.employeeId} onChange={e => setShiftForm({ ...shiftForm, employeeId: e.target.value })} required>
                  <option value="">Select employee…</option>
                  {empForStore(shiftForm.storeId).map(e => <option key={e.employeeId} value={e.employeeId}>{e.name} ({e.role})</option>)}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Shift Start</label>
                  <input type="datetime-local" className="input" value={shiftForm.shiftStart} onChange={e => setShiftForm({ ...shiftForm, shiftStart: e.target.value })} required />
                </div>
                <div>
                  <label className="text-xs text-gray-400 mb-1 block">Shift End</label>
                  <input type="datetime-local" className="input" value={shiftForm.shiftEnd} onChange={e => setShiftForm({ ...shiftForm, shiftEnd: e.target.value })} required />
                </div>
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Hours Worked</label>
                <input type="number" min="0.5" step="0.5" max="24" className="input" placeholder="e.g. 8" value={shiftForm.hoursWorked} onChange={e => setShiftForm({ ...shiftForm, hoursWorked: e.target.value })} required />
              </div>
              <div className="flex gap-2 pt-2">
                <button type="submit" className="btn-primary flex-1">Save Shift</button>
                <button type="button" className="btn-secondary flex-1" onClick={() => setShowAddShift(false)}>Cancel</button>
              </div>
            </form>
          )}
        </Modal>
      )}

      {/* Record Attendance */}
      {showAddAttendance && (
        <Modal title="Record Attendance" onClose={() => setShowAddAttendance(false)}>
          {employees.length === 0 ? (
            <p className="text-sm text-amber-400">Add employees before recording attendance.</p>
          ) : (
            <form onSubmit={handleAddAttendance} className="space-y-3">
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Employee</label>
                <select className="input" value={attEmployeeId} onChange={e => setAttEmployeeId(e.target.value)} required>
                  <option value="">Select employee…</option>
                  {employees.map(e => <option key={e.employeeId} value={e.employeeId}>{e.name} ({e.role})</option>)}
                </select>
              </div>
              <p className="text-xs text-gray-500">Date and time will be recorded automatically.</p>
              <div className="flex gap-2 pt-2">
                <button type="submit" className="btn-primary flex-1">Record Attendance</button>
                <button type="button" className="btn-secondary flex-1" onClick={() => setShowAddAttendance(false)}>Cancel</button>
              </div>
            </form>
          )}
        </Modal>
      )}

      {/* Run Payroll */}
      {showRunPayroll && (
        <Modal title="Run Payroll" onClose={() => setShowRunPayroll(false)}>
          {employees.length === 0 ? (
            <p className="text-sm text-amber-400">Add employees before running payroll.</p>
          ) : (
            <form onSubmit={handleRunPayroll} className="space-y-3">
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Employee</label>
                <select className="input" value={payForm.employeeId} onChange={e => setPayForm({ ...payForm, employeeId: e.target.value })} required>
                  <option value="">Select employee…</option>
                  {employees.map(e => <option key={e.employeeId} value={e.employeeId}>{e.name} — {stores.find(s => s.id === e.storeId)?.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Period</label>
                <input type="month" className="input" value={payForm.period} onChange={e => setPayForm({ ...payForm, period: e.target.value })} required />
              </div>
              <div className="flex gap-2 pt-2">
                <button type="submit" className="btn-primary flex-1">Run Payroll</button>
                <button type="button" className="btn-secondary flex-1" onClick={() => setShowRunPayroll(false)}>Cancel</button>
              </div>
            </form>
          )}
        </Modal>
      )}
    </div>
  );
}
