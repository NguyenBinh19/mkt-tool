import { useEffect, useRef, useState } from 'react';

export function usePersistentState(key, initialValue) {
  const [value, setValue] = useState(() => {
    try {
      const stored = localStorage.getItem(key);
      return stored ? JSON.parse(stored) : initialValue;
    } catch {
      return initialValue;
    }
  });
  const previousKey = useRef(key);
  const skipPersist = useRef(false);

  useEffect(() => {
    if (previousKey.current === key) return;
    previousKey.current = key;
    skipPersist.current = true;
    try {
      const stored = localStorage.getItem(key);
      setValue(stored ? JSON.parse(stored) : initialValue);
    } catch {
      setValue(initialValue);
    }
  }, [key, initialValue]);

  useEffect(() => {
    if (skipPersist.current) {
      skipPersist.current = false;
      return;
    }
    localStorage.setItem(key, JSON.stringify(value));
  }, [key, value]);

  return [value, setValue];
}
