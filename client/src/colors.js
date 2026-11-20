// Category → hue. Common names get hand-picked, clearly distinct hues;
// anything else falls back to a stable hash over a curated palette.
const PRESET = {
  work: 262,
  learning: 189,
  study: 189,
  health: 152,
  fitness: 152,
  personal: 38,
  home: 24,
  finance: 330,
  family: 345,
  side: 212,
};
const PALETTE = [212, 262, 189, 152, 38, 330, 12, 290];
const DEFAULT_HUE = 217;

export function categoryHue(name) {
  if (!name) return DEFAULT_HUE;
  const key = name.trim().toLowerCase();
  if (key in PRESET) return PRESET[key];
  let h = 0;
  for (const ch of key) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return PALETTE[h % PALETTE.length];
}
