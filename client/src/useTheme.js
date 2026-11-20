import { useEffect, useState } from 'react';

const THEME_COLORS = { dark: '#0b0f16', light: '#f5f6f8' };

export function useTheme() {
  const [theme, setTheme] = useState(() => document.documentElement.dataset.theme || 'light');

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', THEME_COLORS[theme]);
    try { localStorage.setItem('theme', theme); } catch {}
  }, [theme]);

  const toggle = () => setTheme(t => (t === 'dark' ? 'light' : 'dark'));
  return [theme, toggle];
}
