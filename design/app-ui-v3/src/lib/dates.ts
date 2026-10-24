const pad = (n: number) => String(n).padStart(2, '0')

export const toISO = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
export const todayISO = () => toISO(new Date())

const parse = (iso: string) => {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d, 12)
}

export const addDays = (iso: string, days: number) => {
  const d = parse(iso)
  d.setDate(d.getDate() + days)
  return toISO(d)
}

export const diffDays = (from: string, to: string) => Math.round((parse(to).getTime() - parse(from).getTime()) / 86_400_000)
