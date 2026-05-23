export default function StatCard({ label, value, icon: Icon, color = 'brand', sub }) {
  const colors = {
    brand:   'bg-brand-900/40 text-brand-400',
    green:   'bg-emerald-900/40 text-emerald-400',
    yellow:  'bg-amber-900/40 text-amber-400',
    red:     'bg-red-900/40 text-red-400',
    purple:  'bg-purple-900/40 text-purple-400',
    blue:    'bg-blue-900/40 text-blue-400',
  };
  return (
    <div className="card flex items-center gap-4">
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 ${colors[color]}`}>
        <Icon size={22} />
      </div>
      <div className="min-w-0">
        <p className="text-xs text-gray-500 font-medium uppercase tracking-wide truncate">{label}</p>
        <p className="text-2xl font-bold text-white mt-0.5">{value}</p>
        {sub && <p className="text-xs text-gray-500 mt-0.5">{sub}</p>}
      </div>
    </div>
  );
}
