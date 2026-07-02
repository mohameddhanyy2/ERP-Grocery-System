export default function StatCard({ label, value, icon: Icon, color = 'brand', sub }) {
  const colors = {
    brand:   'bg-brand-100 text-brand-600',
    green:   'bg-emerald-100 text-emerald-600',
    yellow:  'bg-amber-100 text-amber-600',
    red:     'bg-red-100 text-red-600',
    purple:  'bg-purple-100 text-purple-600',
    blue:    'bg-blue-100 text-blue-600',
  };
  return (
    <div className="card flex items-center gap-4">
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 ${colors[color]}`}>
        <Icon size={22} />
      </div>
      <div className="min-w-0">
        <p className="text-xs text-gray-500 font-medium uppercase tracking-wide truncate">{label}</p>
        <p className="text-2xl font-bold text-gray-900 mt-0.5">{value}</p>
        {sub && <p className="text-xs text-gray-500 mt-0.5">{sub}</p>}
      </div>
    </div>
  );
}
