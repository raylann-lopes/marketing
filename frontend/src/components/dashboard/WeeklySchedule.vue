<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import Card from '@/components/ui/Card.vue'
import Badge from '@/components/ui/Badge.vue'
import { type Post } from '@/services/postService'

interface Props {
  selectedWeekStart: Date
  selectedWeekDate: Date
  today: Date
  selectedDayPosts: Post[]
}

const props = defineProps<Props>()
defineEmits<{
  (e: 'prevWeek'): void
  (e: 'nextWeek'): void
  (e: 'selectDay', date: Date): void
}>()

const router = useRouter()
const weekDays = ['SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB', 'DOM']

const visibleWeekDates = computed(() => {
  const dates: Date[] = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(props.selectedWeekStart)
    d.setDate(d.getDate() + i)
    dates.push(d)
  }
  return dates
})

const weekNavLabel = computed(() => {
  const start = props.selectedWeekStart
  const end = new Date(props.selectedWeekStart)
  end.setDate(end.getDate() + 6)
  const m1 = start.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })
  const m2 = end.toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })
  return `${m1} — ${m2}`
})

function isSameDay(a: Date, b: Date) {
  return (
    a.getDate() === b.getDate() &&
    a.getMonth() === b.getMonth() &&
    a.getFullYear() === b.getFullYear()
  )
}

const selectedDayLabel = computed(() => {
  if (isSameDay(props.selectedWeekDate, props.today)) return 'Publicações de Hoje'
  return `Publicações de ${props.selectedWeekDate.toLocaleDateString('pt-BR', { weekday: 'short', day: '2-digit', month: 'short' })}`
})

const statusLabel: Record<string, string> = {
  DEMAND: 'Demanda',
  IN_PRODUCTION: 'Em Produção',
  WAITING_APPROVAL: 'Aguard. Aprovação',
  FINISHED: 'Finalizado',
  SCHEDULE: 'Agendado',
  PUBLISHED: 'Publicado',
}
</script>

<template>
  <Card class="p-5">
    <div class="flex items-center justify-between mb-4">
      <h2 class="font-semibold text-gray-900">Cronograma Semanal</h2>
      <div class="flex items-center gap-1">
        <button class="p-1 hover:bg-gray-100 rounded" @click="$emit('prevWeek')">
          <ChevronLeft class="w-4 h-4 text-gray-500" />
        </button>
        <span class="text-[10px] text-gray-400 font-medium px-1">{{ weekNavLabel }}</span>
        <button class="p-1 hover:bg-gray-100 rounded" @click="$emit('nextWeek')">
          <ChevronRight class="w-4 h-4 text-gray-500" />
        </button>
      </div>
    </div>

    <!-- Week days -->
    <div class="grid grid-cols-7 gap-1 text-center mb-3">
      <div v-for="(day, i) in weekDays" :key="day" class="space-y-1">
        <p class="text-xs text-gray-400">{{ day }}</p>
        <button
          :class="[
            'w-full aspect-square rounded-full text-sm font-medium flex items-center justify-center transition-colors relative',
            isSameDay(visibleWeekDates[i]!, selectedWeekDate)
              ? 'bg-primary text-white shadow-sm'
              : isSameDay(visibleWeekDates[i]!, today)
                ? 'ring-2 ring-primary text-primary'
                : 'text-gray-600 hover:bg-gray-100',
          ]"
          @click="$emit('selectDay', visibleWeekDates[i]!)"
        >
          {{ visibleWeekDates[i]!.getDate() }}
        </button>
      </div>
    </div>

    <!-- Posts do dia selecionado -->
    <div class="mt-4">
      <p class="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-3">
        {{ selectedDayLabel }}
      </p>
      <div v-if="selectedDayPosts.length === 0" class="text-sm text-gray-500 text-center py-4">
        Nenhum post agendado para este dia.
      </div>
      <div class="space-y-3">
        <div v-for="post in selectedDayPosts" :key="post.id" class="flex gap-3">
          <div class="w-1 rounded-full bg-primary shrink-0" />
          <div class="flex-1">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-primary">
                {{
                  new Date(post.scheduledAt).toLocaleTimeString('pt-BR', {
                    hour: '2-digit',
                    minute: '2-digit',
                  })
                }}
              </span>
              <Badge variant="outline" class="text-[10px]">{{
                statusLabel[post.status] || post.status
              }}</Badge>
            </div>
            <p class="text-sm font-medium text-gray-800 mt-0.5">{{ post.title }}</p>
            <p class="text-xs mt-0.5 text-gray-500">{{ post.theme }}</p>
          </div>
        </div>
      </div>
    </div>

    <button
      class="w-full mt-4 py-2 text-sm text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
      @click="router.push('/calendar')"
    >
      Ver Calendário Completo
    </button>
  </Card>
</template>
