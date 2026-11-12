import type { Fmt } from './bn'

export function captureMoney(value: number, fmt: Fmt, template: string) {
  const amount = value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return template.replace('{value}', fmt.d(amount))
}
