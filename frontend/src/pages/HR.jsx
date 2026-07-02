import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useStores } from '../hooks/useStores';
import PageHeader from '../components/PageHeader';
import LoadingSpinner from '../components/LoadingSpinner';
import EmptyState from '../components/EmptyState';
import Modal from '../components/Modal';
import { Plus, CreditCard, Calendar, ChevronLeft, ChevronRight, Clock, AlertTriangle } from 'lucide-react';
import { fmt } from '../utils/fmt';

const ROLES    = ['Cashier', 'Manager', 'Supervisor', 'Stock Clerk', 'Security', 'Cleaner'];
const OFF_DAYS = ['Friday', 'Saturday', 'Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday'];
const MONTH_NAMES = ['January','February','March','April','May','June','July','August','September','October','November','December'];

// ── Attendance Calendar ───────────────────────────────────────────────────────
function AttendanceCalendar({ employee, onClose }) {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [year, setYear]   = useState(new Date().getFullYear());
  const [month, setMonth] = useState(new Date().getMonth()); // 0-based

  useEffect(() => {
    api.attendance(employee.employeeId)
      .then(setRecords)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [employee.employeeId]);

  // Build a Set of "YYYY-MM-DD" strings that employee was present
  const presentDays = new Set(
    records.filter(r => r.present).map(r => r.date?.slice(0, 10))
  );
  // Map date → attendance detail for tooltip
  const detailMap = {};
  records.forEach(r => { if (r.date) detailMap[r.date.slice(0, 10)] = r; });

  const prevMonth = () => { if (month === 0) { setMonth(11); setYear(y => y - 1); } else setMonth(m => m - 1); };
  const nextMonth = () => { if (month === 11) { setMonth(0);  setYear(y => y + 1); } else setMonth(m => m + 1); };

  // Build calendar grid
  const firstDay   = new Date(year, month, 1).getDay(); // 0=Sun
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const cells = [];
  for (let i = 0; i < firstDay; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);

  const pad = (n) => String(n).padStart(2, '0');
  const dateStr = (d) => `${year}-${pad(month + 1)}-${pad(d)}`;

  return (
    <Modal title={`Attendance — ${employee.name}`} onClose={onClose} wide>
      <div className="space-y-4">
        {/* Month navigator */}
        <div className="flex items-center justify-between">
          <button onClick={prevMonth} className="p-1 rounded hover:bg-gray-100"><ChevronLeft size={18} /></button>
          <span className="font-semibold text-gray-800">{MONTH_NAMES[month]} {year}</span>
          <button onClick={nextMonth} className="p-1 rounded hover:bg-gray-100"><ChevronRight size={18} /></button>
        </div>

        {loading ? <LoadingSpinner /> : (
          <>
            {/* Day headers */}
            <div className="grid grid-cols-7 text-center text-xs text-gray-400 font-medium mb-1">
              {['Sun','Mon','Tue','Wed','Thu','Fri','Sat'].map(d => <div key={d}>{d}</div>)}
            </div>

            {/* Calendar grid */}
            <div className="grid grid-cols-7 gap-1">
              {cells.map((d, i) => {
                if (!d) return <div key={`empty-${i}`} />;
                const ds = dateStr(d);
                const present = presentDays.has(ds);
                const detail  = detailMap[ds];
                const isToday = ds === new Date().toISOString().slice(0, 10);
                return (
                  <div key={ds} title={
                    detail
                      ? `In: ${detail.checkInTime || '—'}  Out: ${detail.checkOutTime || '—'}${detail.minutesLate > 0 ? `  Late: ${detail.minutesLate}min` : ''}${detail.penaltyAmount > 0 ? `  Penalty: ${fmt(detail.penaltyAmount)}` : ''}`
                      : undefined
                  }
                    className={`relative flex flex-col items-center justify-center rounded-lg h-10 text-sm font-medium cursor-default
                      ${isToday ? 'ring-2 ring-brand-500' : ''}
                      ${present ? 'bg-red-100 text-red-700' : 'bg-gray-50 text-gray-300'}
                    `}>
                    <span>{d}</span>
                    {present && <span className="text-xs leading-none text-red-500 font-bold">✗</span>}
                    {detail?.penaltyAmount > 0 && (
                      <span className="absolute top-0.5 right-0.5 w-1.5 h-1.5 rounded-full bg-amber-400" title="Penalty applied" />
                    )}
                  </div>
                );
              })}
            </div>

            {/* Legend */}
            <div className="flex items-center gap-4 text-xs text-gray-500 pt-2 border-t border-gray-100">
              <span className="flex items-center gap-1"><span className="w-3 h-3 rounded bg-red-100 inline-block" /> Attended</span>
              <span className="flex items-center gap-1"><span className="w-1.5 h-1.5 rounded-full bg-amber-400 inline-block" /> Late penalty</span>
              <span className="ml-auto text-gray-400">{presentDays.size} days this month</span>
            </div>

            {/* Attendance list for this month */}
            {records.filter(r => r.date?.startsWith(`${year}-${pad(month + 1)}`)).length > 0 && (
              <div className="border-t border-gray-100 pt-3 space-y-1 max-h-40 overflow-y-auto">
                {records
                  .filter(r => r.date?.startsWith(`${year}-${pad(month + 1)}`))
                  .map(r => (
                    <div key={r.attendanceId} className="flex items-center justify-between text-xs text-gray-600 py-0.5">
                      <span className="font-medium">{r.date?.slice(0, 10)}</span>
                      <span className="text-gray-400">In: {r.checkInTime || '—'}  Out: {r.checkOutTime || '—'}</span>
                      {r.minutesLate > 0
                        ? <span className="text-amber-600 flex items-center gap-0.5"><AlertTriangle size={10} /> {r.minutesLate}min late · -{fmt(r.penaltyAmount)}</span>
                        : <span className="text-emerald-600">On time</span>
                      }
                    </div>
                  ))}
              </div>
            )}
          </>
        )}
      </div>
    </Modal>
  );
}

// ── Pay Salary Confirm Modal ──────────────────────────────────────────────────
function PaySalaryModal({ employee, onClose, onPaid }) {
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState('');

  const confirm = async () => {
    setLoading(true);
    try {
      const r = await api.paySalary({ employeeId: employee.employeeId });
      onPaid(r);
      onClose();
    } catch (err) {
      setMsg(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title="Pay Salary" onClose={onClose}>
      <div className="space-y-4">
        <div className="bg-gray-50 rounded-lg p-4 space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Employee</span>
            <span className="font-medium">{employee.name}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Role</span>
            <span>{employee.role}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-500">Last Paid</span>
            <span className="text-gray-400">{employee.lastPaidDate || 'Never'}</span>
          </div>
          <div className="flex justify-between text-sm border-t border-gray-200 pt-2 mt-2">
            <span className="text-gray-700 font-semibold">Amount to Pay</span>
            <span className="text-emerald-600 font-bold text-base">{fmt(employee.pendingSalary)}</span>
          </div>
        </div>
        <p className="text-xs text-gray-400">This will post an expense to Finance and reset the pending salary to 0.</p>
        {msg && <p className="text-red-600 text-sm">{msg}</p>}
        <div className="flex gap-2">
          <button className="btn-primary flex-1 flex items-center justify-center gap-2" onClick={confirm} disabled={loading}>
            <CreditCard size={14} /> {loading ? 'Processing…' : `Pay ${fmt(employee.pendingSalary)}`}
          </button>
          <button className="btn-secondary flex-1" onClick={onClose}>Cancel</button>
        </div>
      </div>
    </Modal>
  );
}

// ── Check-In Modal ────────────────────────────────────────────────────────────
function CheckInModal({ employee, onClose, onCheckedIn }) {
  const now = new Date();
  const defaultTime = `${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`;
  const [checkInTime, setCheckInTime] = useState(defaultTime);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState('');

  const confirm = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const r = await api.checkIn({ employeeId: employee.employeeId, checkInTime });
      onCheckedIn(r);
      onClose();
    } catch (err) {
      setMsg(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={`Check In — ${employee.name}`} onClose={onClose}>
      <form onSubmit={confirm} className="space-y-4">
        <div className="bg-gray-50 rounded-lg p-3 text-sm space-y-1">
          <div className="flex justify-between"><span className="text-gray-500">Scheduled Start</span><span className="font-medium">{employee.shiftStart}</span></div>
          <div className="flex justify-between"><span className="text-gray-500">Scheduled End</span><span className="font-medium">{employee.shiftEnd}</span></div>
          <div className="flex justify-between"><span className="text-gray-500">Off Day</span><span className="text-gray-400">{employee.offDay}</span></div>
        </div>
        <div>
          <label className="text-xs text-gray-400 mb-1 block">Check-In Time</label>
          <input type="time" className="input" value={checkInTime} onChange={e => setCheckInTime(e.target.value)} required />
          <p className="text-xs text-gray-400 mt-1">Grace period: 30 minutes after {employee.shiftStart}. Late arrivals incur a proportional salary deduction.</p>
        </div>
        {msg && <p className="text-red-600 text-sm">{msg}</p>}
        <div className="flex gap-2">
          <button type="submit" className="btn-primary flex-1 flex items-center justify-center gap-2" disabled={loading}>
            <Clock size={14} /> {loading ? 'Recording…' : 'Record Check-In'}
          </button>
          <button type="button" className="btn-secondary flex-1" onClick={onClose}>Cancel</button>
        </div>
      </form>
    </Modal>
  );
}

// ── Main HR Page ──────────────────────────────────────────────────────────────
export default function HR() {
  const stores = useStores();
  const [employees, setEmployees] = useState([]);
  const [tab, setTab]             = useState('employees');
  const [loading, setLoading]     = useState(true);
  const [msg, setMsg]             = useState('');

  // Modal state
  const [showAddEmp, setShowAddEmp]           = useState(false);
  const [calendarEmp, setCalendarEmp]         = useState(null);
  const [payEmp, setPayEmp]                   = useState(null);
  const [checkInEmp, setCheckInEmp]           = useState(null);

  const [empForm, setEmpForm] = useState({
    name: '', phone: '', storeId: '', role: 'Cashier',
    salary: '', shiftStart: '09:00', shiftEnd: '17:00', offDay: 'Friday',
  });

  const load = () => {
    setLoading(true);
    api.employees()
      .then(setEmployees)
      .catch(console.error)
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  const handleAddEmp = async (e) => {
    e.preventDefault();
    try {
      await api.addEmployee({ ...empForm, salary: Number(empForm.salary) });
      setMsg('Employee added.');
      setShowAddEmp(false);
      setEmpForm({ name: '', phone: '', storeId: '', role: 'Cashier', salary: '', shiftStart: '09:00', shiftEnd: '17:00', offDay: 'Friday' });
      load();
    } catch (err) { setMsg('Error: ' + err.message); }
  };

  const handlePaid = (result) => {
    setMsg(`Paid ${fmt(result.amountPaid)} to employee. Finance expense recorded.`);
    load();
  };

  const handleCheckedIn = (result) => {
    const lateMsg = result.minutesLate > 0
      ? ` Late by ${result.minutesLate} min — penalty ${fmt(result.penaltyAmount)}.`
      : ' On time.';
    setMsg(`Check-in recorded.${lateMsg} Earned today: ${fmt(result.earnedToday)}.`);
    load();
  };

  const storeName = (storeId) => stores.find(s => s.id === storeId)?.name || storeId;

  return (
    <div>
      <PageHeader
        title="HR & Staff Management"
        subtitle="Employees, attendance, and salary payments"
        action={
          tab === 'employees'
            ? <button className="btn-primary flex items-center gap-2" onClick={() => setShowAddEmp(true)}><Plus size={14} /> Add Employee</button>
            : null
        }
      />

      {msg && (
        <div className="mb-4 p-3 bg-brand-50 border border-brand-200 rounded-lg text-sm text-brand-700 flex items-start justify-between gap-2">
          <span>{msg}</span>
          <button className="text-brand-400 hover:text-brand-600 text-xs shrink-0" onClick={() => setMsg('')}>✕</button>
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-1 mb-4 bg-white rounded-lg p-1 w-fit">
        {[
          { key: 'employees', label: `Employees (${employees.length})` },
          { key: 'summary',   label: 'Salary Summary' },
        ].map(t => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${tab === t.key ? 'bg-gray-200 text-gray-900' : 'text-gray-400 hover:text-gray-800'}`}>
            {t.label}
          </button>
        ))}
      </div>

      {loading ? <LoadingSpinner /> : (
        <>
          {/* ── Employees tab ── */}
          {tab === 'employees' && (
            employees.length === 0
              ? <EmptyState text="No employees yet. Add your first employee." />
              : (
                <div className="card p-0 overflow-hidden">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-gray-200 text-left bg-gray-50">
                        {['Name / Phone', 'Store', 'Role', 'Shift', 'Off Day', 'Monthly Salary', 'Pending', 'Last Paid', 'Actions'].map(h => (
                          <th key={h} className="px-4 py-3 text-xs text-gray-500 font-medium uppercase whitespace-nowrap">{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {employees.map(e => (
                        <tr key={e.employeeId} className="table-row border-b border-gray-100 last:border-0">
                          <td className="px-4 py-3">
                            <p className="font-medium text-gray-900">{e.name}</p>
                            <p className="text-xs text-gray-400">{e.phone || '—'}</p>
                          </td>
                          <td className="px-4 py-3 text-gray-400 text-xs">{storeName(e.storeId)}</td>
                          <td className="px-4 py-3"><span className="badge-blue">{e.role}</span></td>
                          <td className="px-4 py-3 text-gray-600 text-xs whitespace-nowrap">
                            <span className="font-mono">{e.shiftStart} – {e.shiftEnd}</span>
                          </td>
                          <td className="px-4 py-3 text-gray-400 text-xs">{e.offDay}</td>
                          <td className="px-4 py-3 text-gray-700 font-medium">{fmt(e.salary)}</td>
                          <td className="px-4 py-3">
                            <span className={`font-semibold ${e.pendingSalary > 0 ? 'text-emerald-600' : 'text-gray-300'}`}>
                              {fmt(e.pendingSalary)}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-gray-400 text-xs">{e.lastPaidDate || '—'}</td>
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-1">
                              {/* Check In */}
                              <button
                                className="px-2 py-1 rounded text-xs bg-blue-50 text-blue-700 hover:bg-blue-100 flex items-center gap-1 whitespace-nowrap"
                                onClick={() => setCheckInEmp(e)}
                                title="Record check-in">
                                <Clock size={11} /> Check In
                              </button>
                              {/* Attendance Calendar */}
                              <button
                                className="px-2 py-1 rounded text-xs bg-gray-100 text-gray-600 hover:bg-gray-200 flex items-center gap-1"
                                onClick={() => setCalendarEmp(e)}
                                title="View attendance calendar">
                                <Calendar size={11} />
                              </button>
                              {/* Pay Salary */}
                              <button
                                className={`px-2 py-1 rounded text-xs flex items-center gap-1 whitespace-nowrap ${
                                  e.pendingSalary > 0
                                    ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                                    : 'bg-gray-50 text-gray-300 cursor-not-allowed'
                                }`}
                                onClick={() => e.pendingSalary > 0 && setPayEmp(e)}
                                title={e.pendingSalary > 0 ? 'Pay pending salary' : 'No pending salary'}
                                disabled={e.pendingSalary <= 0}>
                                <CreditCard size={11} /> Pay
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )
          )}

          {/* ── Salary Summary tab ── */}
          {tab === 'summary' && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {employees.length === 0
                ? <EmptyState text="No employees yet." />
                : employees.map(e => (
                  <div key={e.employeeId} className="card space-y-3">
                    <div className="flex items-start justify-between">
                      <div>
                        <p className="font-semibold text-gray-900">{e.name}</p>
                        <p className="text-xs text-gray-400">{e.role} · {storeName(e.storeId)}</p>
                      </div>
                      <span className="badge-blue text-xs">{e.offDay} off</span>
                    </div>
                    <div className="space-y-1.5 text-sm">
                      <div className="flex justify-between">
                        <span className="text-gray-400">Monthly Salary</span>
                        <span className="font-medium">{fmt(e.salary)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Daily Rate</span>
                        <span className="text-gray-600">{fmt(e.salary / 26)}</span>
                      </div>
                      <div className="flex justify-between border-t border-gray-100 pt-1.5">
                        <span className="text-gray-700 font-medium">Pending Payment</span>
                        <span className={`font-bold ${e.pendingSalary > 0 ? 'text-emerald-600' : 'text-gray-300'}`}>{fmt(e.pendingSalary)}</span>
                      </div>
                      <div className="flex justify-between text-xs">
                        <span className="text-gray-400">Last Paid</span>
                        <span className="text-gray-500">{e.lastPaidDate || 'Never'}</span>
                      </div>
                    </div>
                    <div className="flex gap-2 pt-1">
                      <button
                        className="flex-1 px-3 py-1.5 rounded text-xs bg-gray-100 text-gray-600 hover:bg-gray-200 flex items-center justify-center gap-1"
                        onClick={() => setCalendarEmp(e)}>
                        <Calendar size={12} /> Attendance
                      </button>
                      <button
                        className={`flex-1 px-3 py-1.5 rounded text-xs flex items-center justify-center gap-1 ${
                          e.pendingSalary > 0
                            ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                            : 'bg-gray-100 text-gray-300 cursor-not-allowed'
                        }`}
                        onClick={() => e.pendingSalary > 0 && setPayEmp(e)}
                        disabled={e.pendingSalary <= 0}>
                        <CreditCard size={12} /> Pay {e.pendingSalary > 0 ? fmt(e.pendingSalary) : '—'}
                      </button>
                    </div>
                  </div>
                ))
              }
            </div>
          )}
        </>
      )}

      {/* ── Add Employee Modal ── */}
      {showAddEmp && (
        <Modal title="Add Employee" onClose={() => setShowAddEmp(false)}>
          <form onSubmit={handleAddEmp} className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div className="col-span-2">
                <label className="text-xs text-gray-400 mb-1 block">Full Name</label>
                <input className="input" placeholder="Employee full name" value={empForm.name}
                  onChange={e => setEmpForm({ ...empForm, name: e.target.value })} required />
              </div>
              <div className="col-span-2">
                <label className="text-xs text-gray-400 mb-1 block">Phone Number</label>
                <input className="input" placeholder="e.g. 010XXXXXXXX" value={empForm.phone}
                  onChange={e => setEmpForm({ ...empForm, phone: e.target.value })} />
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Store</label>
                <select className="input" value={empForm.storeId}
                  onChange={e => setEmpForm({ ...empForm, storeId: e.target.value })} required>
                  <option value="">Select store…</option>
                  {stores.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Role</label>
                <select className="input" value={empForm.role}
                  onChange={e => setEmpForm({ ...empForm, role: e.target.value })}>
                  {ROLES.map(r => <option key={r}>{r}</option>)}
                </select>
              </div>
              <div className="col-span-2">
                <label className="text-xs text-gray-400 mb-1 block">Monthly Salary (EGP)</label>
                <input type="number" min="1" step="1" className="input" placeholder="e.g. 5000"
                  value={empForm.salary} onChange={e => setEmpForm({ ...empForm, salary: e.target.value })} required />
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Shift Start</label>
                <input type="time" className="input" value={empForm.shiftStart}
                  onChange={e => setEmpForm({ ...empForm, shiftStart: e.target.value })} required />
              </div>
              <div>
                <label className="text-xs text-gray-400 mb-1 block">Shift End</label>
                <input type="time" className="input" value={empForm.shiftEnd}
                  onChange={e => setEmpForm({ ...empForm, shiftEnd: e.target.value })} required />
              </div>
              <div className="col-span-2">
                <label className="text-xs text-gray-400 mb-1 block">Off Day</label>
                <select className="input" value={empForm.offDay}
                  onChange={e => setEmpForm({ ...empForm, offDay: e.target.value })}>
                  {OFF_DAYS.map(d => <option key={d}>{d}</option>)}
                </select>
              </div>
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" className="btn-primary flex-1">Add Employee</button>
              <button type="button" className="btn-secondary flex-1" onClick={() => setShowAddEmp(false)}>Cancel</button>
            </div>
          </form>
        </Modal>
      )}

      {/* ── Attendance Calendar Modal ── */}
      {calendarEmp && (
        <AttendanceCalendar employee={calendarEmp} onClose={() => setCalendarEmp(null)} />
      )}

      {/* ── Pay Salary Modal ── */}
      {payEmp && (
        <PaySalaryModal employee={payEmp} onClose={() => setPayEmp(null)} onPaid={handlePaid} />
      )}

      {/* ── Check-In Modal ── */}
      {checkInEmp && (
        <CheckInModal employee={checkInEmp} onClose={() => setCheckInEmp(null)} onCheckedIn={handleCheckedIn} />
      )}
    </div>
  );
}
