<script setup lang="ts">
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  Trash2,
  Plus,
} from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import { useIsMobile } from '@/lib/breakpoint'

const { isMobile } = useIsMobile()

type UploadStatus = 'PLANNED' | 'UPLOADED' | 'APPROVED'

interface ClientUploadItem {
  id: string
  date: string // YYYY-MM-DD
  title: string
  format: string
  channel: string
  notes: string
  status: UploadStatus
}

interface Props {
  currentMonthLabel: string
  currentDate: Date
  selectedDay: number
  calendarDays: (number | null)[]
  uploadsPerDay: Record<number, ClientUploadItem[]>
  selectedDayUploads: ClientUploadItem[]
}

defineProps<Props>()

defineEmits<{
  (e: 'prevMonth'): void
  (e: 'nextMonth'): void
  (e: 'today'): void
  (e: 'selectDay', day: number): void
  (e: 'cycleStatus', upload: ClientUploadItem): void
  (e: 'removeUpload', id: string): void
  (e: 'openUploadModal', day: number): void
}>()

const dayNames = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB']
const monthNames = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
]

function statusVariant(status: UploadStatus): 'secondary' | 'warning' | 'success' {
  if (status === 'PLANNED') return 'secondary'
  if (status === 'UPLOADED') return 'warning'
  return 'success'
}

function statusLabel(status: UploadStatus) {
  if (status === 'PLANNED') return 'Planejado'
  if (status === 'UPLOADED') return 'Enviado'
  return 'Aprovado'
}
</script>

<template>
  <div class="grid gap-6 lg:grid-cols-[1fr_360px]">
    <section v-if="!isMobile" class="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
      <div class="mb-5 flex items-center justify-between">
        <h2 class="text-xl font-bold text-gray-900">{{ currentMonthLabel }}</h2>
        <div class="flex items-center gap-2">
          <Button variant="outline" class="h-9 px-3" @click="$emit('today')"> Hoje </Button>
          <button class="rounded-lg p-1.5 hover:bg-gray-100" @click="$emit('prevMonth')">
            <ChevronLeft class="h-4 w-4" />
          </button>
          <button class="rounded-lg p-1.5 hover:bg-gray-100" @click="$emit('nextMonth')">
            <ChevronRight class="h-4 w-4" />
          </button>
        </div>
      </div>

      <div class="mb-2 grid grid-cols-7 text-center">
        <div v-for="d in dayNames" :key="d" class="py-1 text-xs font-semibold text-gray-400">
          {{ d }}
        </div>
      </div>

      <div
        class="grid grid-cols-7 gap-px overflow-hidden rounded-xl border border-gray-200 bg-gray-200"
      >
        <div
          v-for="(day, index) in calendarDays"
          :key="index"
          :class="[
            'min-h-[110px] bg-white p-2 transition-colors',
            day ? 'cursor-pointer hover:bg-primary/5' : '',
          ]"
          @click="day && $emit('selectDay', day)"
        >
          <div
            v-if="day"
            :class="[
              'mb-2 inline-flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold',
              day === selectedDay ? 'bg-primary text-white' : 'text-gray-600',
            ]"
          >
            {{ day }}
          </div>

          <div v-if="day && uploadsPerDay[day]" class="space-y-1">
            <div
              v-for="upload in uploadsPerDay[day].slice(0, 2)"
              :key="upload.id"
              class="truncate rounded-md bg-primary/10 px-2 py-1 text-[10px] font-medium text-primary"
            >
              {{ upload.title }}
            </div>
            <p v-if="uploadsPerDay[day].length > 2" class="text-[10px] font-semibold text-gray-400">
              +{{ uploadsPerDay[day].length - 2 }} itens
            </p>
          </div>
        </div>
      </div>
    </section>

    <aside
      class="flex min-h-[520px] flex-col rounded-2xl border border-gray-100 bg-white shadow-sm"
    >
      <div class="border-b border-gray-100 p-4">
        <p class="text-xs font-semibold uppercase tracking-wide text-gray-400">Agenda do dia</p>
        <p class="text-3xl font-bold text-gray-900">{{ selectedDay }}</p>
        <p class="text-sm text-gray-500">{{ monthNames[currentDate.getMonth()] }}</p>
      </div>

      <div class="flex-1 space-y-3 overflow-y-auto p-4">
        <div
          v-if="selectedDayUploads.length === 0"
          class="flex h-40 flex-col items-center justify-center text-center text-gray-400"
        >
          <CalendarDays class="mb-2 h-8 w-8 opacity-30" />
          <p class="text-sm">Nenhum post planejado para esse dia.</p>
        </div>

        <div
          v-for="upload in selectedDayUploads"
          :key="upload.id"
          class="rounded-xl border border-gray-100 bg-gray-50 p-3"
        >
          <div class="mb-2 flex items-center justify-between gap-2">
            <Badge :variant="statusVariant(upload.status)">{{ statusLabel(upload.status) }}</Badge>
            <div class="flex items-center gap-1">
              <button
                class="rounded-md p-1 text-gray-400 hover:bg-green-50 hover:text-green-600"
                title="Atualizar status"
                @click="$emit('cycleStatus', upload)"
              >
                <CheckCircle2 class="h-4 w-4" />
              </button>
              <button
                class="rounded-md p-1 text-gray-400 hover:bg-red-50 hover:text-red-600"
                title="Remover"
                @click="$emit('removeUpload', upload.id)"
              >
                <Trash2 class="h-4 w-4" />
              </button>
            </div>
          </div>

          <p class="text-sm font-semibold text-gray-800">{{ upload.title }}</p>
          <p class="mt-1 text-xs text-gray-500">{{ upload.channel }} · {{ upload.format }}</p>
          <p v-if="upload.notes" class="mt-2 text-xs text-gray-500">{{ upload.notes }}</p>
        </div>
      </div>

      <div class="border-t border-gray-100 p-4">
        <Button class="w-full gap-2" @click="$emit('openUploadModal', selectedDay)">
          <Plus class="h-4 w-4" />
          Agendar Post no Dia
        </Button>
      </div>
    </aside>
  </div>
</template>
