import type { CSSProperties, ReactNode } from 'react'

type IconName = 'arrow' | 'eye' | 'eye-off' | 'check' | 'logout' | 'refresh' | 'lock' | 'alert'
const paths: Record<IconName, ReactNode> = {
  arrow: <><path d="M4 12h15M13 5l7 7-7 7" /></>,
  eye: <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></>,
  'eye-off': <><path d="m3 3 18 18M10.6 5.1 12 5c6.5 0 10 7 10 7a19 19 0 0 1-3.2 4M6.2 6.2A22 22 0 0 0 2 12s3.5 7 10 7a13 13 0 0 0 5.8-1.4M10 10a3 3 0 0 0 4 4" /></>,
  check: <path d="m5 12 4 4L19 6" />,
  logout: <><path d="M10 4H5v16h5M10 12h11m-4-4 4 4-4 4" /></>,
  refresh: <><path d="M20 7v5h-5M4 17v-5h5M19.5 12A7.5 7.5 0 0 0 6.7 6.7L4 9M4.5 12a7.5 7.5 0 0 0 12.8 5.3L20 15" /></>,
  lock: <><rect x="5" y="10" width="14" height="11" rx="3" /><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3" /></>,
  alert: <><circle cx="12" cy="12" r="9" /><path d="M12 7v6M12 16h.01" /></>,
}

export function Icon({ name, className, style }: { name: IconName; className?: string; style?: CSSProperties }) {
  return <svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" className={className} style={style}>{paths[name]}</svg>
}
