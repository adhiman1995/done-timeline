const base = {
  width: 18,
  height: 18,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
};

/** The Ticked mark: a ring with a tick breaking out through its gap. Drawn in currentColor. */
export const TickedMark = p => (
  <svg viewBox="0 0 64 64" width={20} height={20} fill="none" stroke="currentColor" strokeWidth={5.4}
    strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...p}>
    <g transform="translate(-1.2 1)">
      <path d="M37.3 17.4A15.5 15.5 0 1 0 47.44 30.65" />
      <path d="M23.8 31.6l6.8 6.8 17.9-19.9" />
    </g>
  </svg>
);

/** Blue rounded-square app logo. */
export const Logo = ({ size = 36 }) => (
  <span className="logo" style={{ width: size, height: size }}>
    <TickedMark width={size * 0.72} height={size * 0.72} />
  </span>
);

export const PlusIcon = p => (<svg {...base} {...p}><path d="M12 5v14M5 12h14" /></svg>);
export const CheckIcon = p => (<svg {...base} {...p}><path d="M5 12.5l4.5 4.5L19 7.5" /></svg>);
export const SearchIcon = p => (<svg {...base} {...p}><circle cx="11" cy="11" r="7" /><path d="M20 20l-3.5-3.5" /></svg>);
export const EditIcon = p => (<svg {...base} {...p}><path d="M4 20h4L19 9l-4-4L4 16v4z" /><path d="M13.5 6.5l4 4" /></svg>);
export const TrashIcon = p => (<svg {...base} {...p}><path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3" /></svg>);
export const CloseIcon = p => (<svg {...base} {...p}><path d="M6 6l12 12M18 6L6 18" /></svg>);
export const FlameIcon = p => (<svg {...base} {...p}><path d="M12 3c1 3.5 5 5.5 5 10a5 5 0 01-10 0c0-2 1-3.5 2-4.5 0 2 1 3 2 3 0-3-1-5.5 1-8.5z" /></svg>);
export const SunIcon = p => (<svg {...base} {...p}><circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" /></svg>);
export const MoonIcon = p => (<svg {...base} {...p}><path d="M20 14.5A8 8 0 019.5 4a8 8 0 1010.5 10.5z" /></svg>);
export const StarIcon = ({ filled, ...p }) => (<svg {...base} {...p} fill={filled ? 'currentColor' : 'none'}><path d="M12 3.5l2.6 5.3 5.8.8-4.2 4.1 1 5.8L12 16.8l-5.2 2.7 1-5.8-4.2-4.1 5.8-.8z" /></svg>);
export const ChevronDownIcon = p => (<svg {...base} {...p}><path d="M6 9l6 6 6-6" /></svg>);
export const ArrowUpRightIcon = p => (<svg {...base} {...p}><path d="M7 17L17 7M8 7h9v9" /></svg>);
export const FilterIcon = p => (<svg {...base} {...p}><path d="M4 6h16M7 12h10M10 18h4" /></svg>);
export const LayersIcon = p => (<svg {...base} {...p}><path d="M12 3l9 5-9 5-9-5 9-5z" /><path d="M3 13l9 5 9-5" /></svg>);
export const TimelineIcon = p => (<svg {...base} {...p}><circle cx="6" cy="6" r="2" /><circle cx="6" cy="18" r="2" /><path d="M6 8v8M11 6h9M11 18h9M11 12h6" /></svg>);
export const DownloadIcon = p => (<svg {...base} {...p}><path d="M12 4v11M7 10l5 5 5-5M5 20h14" /></svg>);
