const DATE_ONLY_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/
const TIME_PATTERN = /^(\d{2}):(\d{2})(?::\d{2}(?:\.\d+)?)?$/

const DEFAULT_DATE_OPTIONS: Intl.DateTimeFormatOptions = {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
}

const DEFAULT_DATE_TIME_OPTIONS: Intl.DateTimeFormatOptions = {
  ...DEFAULT_DATE_OPTIONS,
  hour: '2-digit',
  minute: '2-digit',
}

export function normalizeTime(value?: string | null): string | null {
  if (!value) return null
  const match = value.match(TIME_PATTERN)
  if (!match) return null

  const hour = Number(match[1])
  const minute = Number(match[2])
  return hour <= 23 && minute <= 59 ? `${match[1]}:${match[2]}` : null
}

export function combineLocalDateTime(
  date: string,
  time?: string | null,
  fallbackTime?: string,
): string | null {
  const normalizedTime = normalizeTime(time) ?? normalizeTime(fallbackTime)
  if (!parseLocalDate(date) || !normalizedTime) return null
  return `${date}T${normalizedTime}:00`
}

export function formatDate(
  value: string,
  options: Intl.DateTimeFormatOptions = DEFAULT_DATE_OPTIONS,
): string {
  const date = parseLocalDate(value)
  return date ? date.toLocaleDateString('pt-BR', options) : value
}

export function formatTime(value?: string | null): string | null {
  const normalizedTime = normalizeTime(value)
  if (normalizedTime) return normalizedTime
  if (!value) return null

  const date = parseLocalDate(value)
  if (!date) return null
  return date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

export function formatDateTime(
  value: string,
  options: Intl.DateTimeFormatOptions = DEFAULT_DATE_TIME_OPTIONS,
): string {
  const date = parseLocalDate(value)
  return date ? date.toLocaleString('pt-BR', options) : value
}

export function formatTaskDateTime(
  date: string,
  time?: string | null,
  options: Intl.DateTimeFormatOptions = DEFAULT_DATE_TIME_OPTIONS,
): string {
  const dateTime = combineLocalDateTime(date, time)
  return dateTime ? formatDateTime(dateTime, options) : formatDate(date)
}

function parseLocalDate(value: string): Date | null {
  const dateOnlyMatch = value.match(DATE_ONLY_PATTERN)
  if (dateOnlyMatch) {
    const year = Number(dateOnlyMatch[1])
    const month = Number(dateOnlyMatch[2])
    const day = Number(dateOnlyMatch[3])
    const date = new Date(year, month - 1, day, 12)

    return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day
      ? date
      : null
  }

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}
