import { InboxIcon } from 'lucide-react';

export default function EmptyState({ text = 'No data found' }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 gap-3 text-gray-600">
      <InboxIcon size={36} />
      <p className="text-sm">{text}</p>
    </div>
  );
}
