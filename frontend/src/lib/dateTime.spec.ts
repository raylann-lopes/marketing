import { describe, expect, it } from 'vitest'
import {
  combineLocalDateTime,
  formatDate,
  formatTaskDateTime,
  formatTime,
  normalizeTime,
} from './dateTime'

describe('dateTime', () => {
  it('remove os segundos do horario retornado pelo Spring', () => {
    expect(normalizeTime('09:30:00')).toBe('09:30')
    expect(formatTime('09:30:00')).toBe('09:30')
  })

  it('combina a data e o horario da tarefa sem duplicar segundos', () => {
    expect(combineLocalDateTime('2026-08-12', '09:30:00')).toBe('2026-08-12T09:30:00')
  })

  it('formata tarefa e post no mesmo padrao', () => {
    expect(formatTaskDateTime('2026-08-12', '09:30:00')).toBe('12/08/2026, 09:30')
  })

  it('mantem somente a data quando a tarefa nao possui horario', () => {
    expect(formatTaskDateTime('2026-08-12', null)).toBe('12/08/2026')
    expect(formatDate('2026-08-12')).toBe('12/08/2026')
  })

  it('rejeita datas e horarios invalidos', () => {
    expect(combineLocalDateTime('2026-02-31', '09:30')).toBeNull()
    expect(normalizeTime('25:00:00')).toBeNull()
  })
})
