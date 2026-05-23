import { useEffect, useState } from 'react';
import { api } from '../api/client';

// Returns stores as [{id, name}] matching the shape all pages expect
export function useStores() {
  const [stores, setStores] = useState([]);
  useEffect(() => {
    api.stores()
      .then(raw => setStores(raw.map(s => ({ id: s.storeId, name: s.storeName }))))
      .catch(console.error);
  }, []);
  return stores;
}
