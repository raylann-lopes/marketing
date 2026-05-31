<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import { useFeedback } from '@/lib/feedback'

import ClientDetailsTab from '@/components/client-workspace/ClientDetailsTab.vue'
import ClientCalendarTab from '@/components/client-workspace/ClientCalendarTab.vue'
import ClientAiTab from '@/components/client-workspace/ClientAiTab.vue'
import UploadModal from '@/components/client-workspace/UploadModal.vue'

const route = useRoute()
const router = useRouter()
const feedback = useFeedback()

const UPLOADS_STORAGE_KEY = 'client_workspace_uploads_v1'
const DETAILS_STORAGE_KEY = 'client_workspace_details_v1'

type WorkspaceTabId = 'details' | 'calendar' | 'ai'

type UploadStatus = 'PLANNED' | 'UPLOADED' | 'APPROVED'

type ClientUploadItem = {
  id: string
  date: string // YYYY-MM-DD
  title: string
  format: string
  channel: string
  notes: string
  status: UploadStatus
}

type ClientWorkspaceDetails = {
  mainGoal: string
  targetAudience: string
  preferredChannels: string
  contentPillars: string
  approvalSla: string
  seasonality: string
  reminders: string
}

type ClientSnapshot = {
  id: string
  name: string
  email: string
  niche: string
  status: string
  voiceTone: string
}

type RecordMap<T> = Record<string, T>

const activeTab = ref<WorkspaceTabId>('details')
const tabItems: { id: WorkspaceTabId; label: string; helper: string }[] = [
  { id: 'details', label: 'Dados do Cliente', helper: 'Briefing essencial para posts' },
  { id: 'calendar', label: 'Posts e Agendamento', helper: 'Planejar e acompanhar agenda' },
  { id: 'ai', label: 'Recomendações IA', helper: 'Análise simples baseada no nicho' },
]

const today = new Date()
const currentDate = ref(new Date(today.getFullYear(), today.getMonth(), 1))
const selectedDay = ref(today.getDate())

const isUploadModalOpen = ref(false)
const isSavingUpload = ref(false)
const uploadFieldErrors = ref<Record<string, string>>({})
const uploadDraft = ref<ClientUploadItem>({
  id: '',
  date: '',
  title: '',
  format: 'Feed',
  channel: 'Instagram',
  notes: '',
  status: 'PLANNED',
})

const isSavingDetails = ref(false)
const detailsDraft = ref<ClientWorkspaceDetails>({
  mainGoal: '',
  targetAudience: '',
  preferredChannels: '',
  contentPillars: '',
  approvalSla: '',
  seasonality: '',
  reminders: '',
})

const uploadsByClient = ref<RecordMap<ClientUploadItem[]>>(readStorage<RecordMap<ClientUploadItem[]>>(UPLOADS_STORAGE_KEY, {}))
const detailsByClient = ref<RecordMap<ClientWorkspaceDetails>>(readStorage<RecordMap<ClientWorkspaceDetails>>(DETAILS_STORAGE_KEY, {}))

const monthNames = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro']

const clientId = computed(() => String(route.params.id ?? ''))

function getQueryString(value: unknown) {
  if (Array.isArray(value)) return value[0] ? String(value[0]) : ''
  return typeof value === 'string' ? value : ''
}

const client = computed<ClientSnapshot>(() => ({
  id: clientId.value,
  name: getQueryString(route.query.name) || `Cliente #${clientId.value}`,
  email: getQueryString(route.query.email) || 'Sem e-mail',
  niche: getQueryString(route.query.niche) || 'Sem nicho',
  status: getQueryString(route.query.status) || 'ACTIVE',
  voiceTone: getQueryString(route.query.voiceTone),
}))

const currentMonthLabel = computed(() => `${monthNames[currentDate.value.getMonth()]} ${currentDate.value.getFullYear()}`)

const clientUploads = computed(() => {
  const list = uploadsByClient.value[clientId.value] || []
  return [...list].sort((a, b) => a.date.localeCompare(b.date) || a.title.localeCompare(b.title))
})

const calendarDays = computed<(number | null)[]>(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  const firstWeekDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const days: (number | null)[] = []

  for (let i = 0; i < firstWeekDay; i++) days.push(null)
  for (let day = 1; day <= daysInMonth; day++) days.push(day)

  return days
})

const uploadsPerDay = computed<Record<number, ClientUploadItem[]>>(() => {
  const map: Record<number, ClientUploadItem[]> = {}
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()

  for (const upload of clientUploads.value) {
    const date = new Date(`${upload.date}T12:00:00`)
    if (Number.isNaN(date.getTime())) continue
    if (date.getFullYear() !== year || date.getMonth() !== month) continue

    const day = date.getDate()
    if (!map[day]) map[day] = []
    map[day].push(upload)
  }

  return map
})

const selectedDayUploads = computed(() => uploadsPerDay.value[selectedDay.value] || [])

const monthUploadMetrics = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  const monthItems = clientUploads.value.filter((item) => {
    const d = new Date(`${item.date}T12:00:00`)
    return !Number.isNaN(d.getTime()) && d.getFullYear() === year && d.getMonth() === month
  })
  const planned = monthItems.length
  const approved = monthItems.filter(i => i.status === 'APPROVED').length
  const completion = planned === 0 ? 0 : Math.round((approved / planned) * 100)
  return { planned, approved, completion }
})

const aiFieldChecks = computed(() => {
  const fields = [
    detailsDraft.value.mainGoal,
    detailsDraft.value.targetAudience,
    detailsDraft.value.preferredChannels,
    detailsDraft.value.contentPillars,
  ]
  const filled = fields.filter(v => v.trim().length > 0).length
  const total = fields.length
  return {
    filled,
    total,
    percent: Math.round((filled / total) * 100),
  }
})

const aiMissingInputs = computed(() => {
  const missing: string[] = []
  if (!detailsDraft.value.mainGoal.trim()) missing.push('Objetivo principal')
  if (!detailsDraft.value.targetAudience.trim()) missing.push('Público-alvo')
  if (!detailsDraft.value.preferredChannels.trim()) missing.push('Canais principais')
  if (!detailsDraft.value.contentPillars.trim()) missing.push('Pilares de conteúdo')
  return missing
})

type RecommendationCard = {
  title: string
  priority: 'ALTA' | 'MEDIA' | 'BAIXA'
  details: string
}

const aiRecommendations = computed<RecommendationCard[]>(() => {
  const recs: RecommendationCard[] = []
  const niche = client.value.niche.toLowerCase()

  if (niche.includes('imobili')) {
    recs.push({
      title: 'Conteúdo para imobiliário',
      priority: 'ALTA',
      details: 'Use tour rápido, prova social e diferenciais de localização para melhorar resposta do público.',
    })
  } else if (niche.includes('clin') || niche.includes('saúde') || niche.includes('saude')) {
    recs.push({
      title: 'Conteúdo para saúde e serviços',
      priority: 'ALTA',
      details: 'Priorize educação curta, confiança e CTA de agendamento com linguagem clara.',
    })
  } else if (niche.includes('ecom') || niche.includes('loja') || niche.includes('varejo')) {
    recs.push({
      title: 'Conteúdo para varejo/e-commerce',
      priority: 'ALTA',
      details: 'Combine oferta semanal com criativos diretos para acelerar decisão de compra.',
    })
  } else {
    recs.push({
      title: 'Estrutura base de conteúdo',
      priority: 'MEDIA',
      details: 'Mantenha 3 pilares fixos por mês e repita os formatos com melhor aprovação.',
    })
  }

  recs.push({
    title: 'Cadência de aprovação',
    priority: monthUploadMetrics.value.completion < 50 ? 'ALTA' : 'BAIXA',
    details: monthUploadMetrics.value.completion < 50
      ? 'A aprovação está baixa. Reduza lote semanal e alinhe prévia de copy antes de enviar ao cliente.'
      : 'A aprovação está estável. Mantenha frequência e teste variações de headline por formato.',
  })

  if (aiMissingInputs.value.length > 0) {
    recs.push({
      title: 'Contexto incompleto',
      priority: 'MEDIA',
      details: `Complete os campos: ${aiMissingInputs.value.join(', ')} para melhorar as recomendações.`,
    })
  }

  return recs
})

const aiPromptContext = computed(() => {
  return [
    `Cliente: ${client.value.name}`,
    `Nicho: ${client.value.niche || 'não informado'}`,
    `Objetivo: ${detailsDraft.value.mainGoal || 'não informado'}`,
    `Público: ${detailsDraft.value.targetAudience || 'não informado'}`,
    `Canais: ${detailsDraft.value.preferredChannels || 'não informado'}`,
    `Pilares: ${detailsDraft.value.contentPillars || 'não informado'}`,
    `Posts no mês: ${monthUploadMetrics.value.planned}`,
    `Taxa de aprovação: ${monthUploadMetrics.value.completion}%`,
  ].join('\n')
})

watch(clientId, () => {
  if (!detailsByClient.value[clientId.value]) {
    detailsByClient.value = {
      ...detailsByClient.value,
      [clientId.value]: defaultDetailsFromClient(client.value),
    }
    persistDetails()
  }

  detailsDraft.value = {
    ...defaultDetailsFromClient(client.value),
    ...detailsByClient.value[clientId.value],
  }
}, { immediate: true })

function defaultDetailsFromClient(snapshot: ClientSnapshot): ClientWorkspaceDetails {
  return {
    mainGoal: '',
    targetAudience: '',
    preferredChannels: 'Instagram',
    contentPillars: snapshot.niche ? `Conteúdo sobre ${snapshot.niche}` : '',
    approvalSla: '24h úteis',
    seasonality: '',
    reminders: '',
  }
}

function readStorage<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return fallback
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

function persistUploads() {
  localStorage.setItem(UPLOADS_STORAGE_KEY, JSON.stringify(uploadsByClient.value))
}

function persistDetails() {
  localStorage.setItem(DETAILS_STORAGE_KEY, JSON.stringify(detailsByClient.value))
}

function prevMonth() {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  currentDate.value = new Date(year, month - 1, 1)
}

function nextMonth() {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  currentDate.value = new Date(year, month + 1, 1)
}

function toDateInputValue(date: Date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}

function openUploadModal(day?: number | null) {
  const base = new Date(currentDate.value)
  base.setDate(day || selectedDay.value || 1)

  uploadDraft.value = {
    id: '',
    date: toDateInputValue(base),
    title: '',
    format: 'Feed',
    channel: 'Instagram',
    notes: '',
    status: 'PLANNED',
  }
  uploadFieldErrors.value = {}
  isUploadModalOpen.value = true
}

function upsertClientUploads(nextList: ClientUploadItem[]) {
  uploadsByClient.value = {
    ...uploadsByClient.value,
    [clientId.value]: nextList,
  }
  persistUploads()
}

async function saveUpload() {
  uploadFieldErrors.value = {}

  if (!uploadDraft.value.date) uploadFieldErrors.value.date = 'Selecione a data.'
  if (!uploadDraft.value.title.trim()) uploadFieldErrors.value.title = 'Defina um título.'
  if (!uploadDraft.value.channel.trim()) uploadFieldErrors.value.channel = 'Defina o canal.'

  if (Object.keys(uploadFieldErrors.value).length > 0) return

  isSavingUpload.value = true
  try {
    const next: ClientUploadItem = {
      ...uploadDraft.value,
      id: uploadDraft.value.id || (globalThis.crypto?.randomUUID?.() || String(Date.now())),
      title: uploadDraft.value.title.trim(),
      channel: uploadDraft.value.channel.trim(),
      notes: uploadDraft.value.notes.trim(),
    }

    upsertClientUploads([...clientUploads.value, next])
    isUploadModalOpen.value = false
    feedback.success('Post agendado salvo localmente.')
  } finally {
    isSavingUpload.value = false
  }
}

function removeUpload(id: string) {
  const filtered = clientUploads.value.filter(item => item.id !== id)
  upsertClientUploads(filtered)
  feedback.info('Item removido da agenda local.')
}

function cycleStatus(upload: ClientUploadItem) {
  const nextStatus: UploadStatus =
    upload.status === 'PLANNED'
      ? 'UPLOADED'
      : upload.status === 'UPLOADED'
        ? 'APPROVED'
        : 'PLANNED'

  const nextList = clientUploads.value.map(item =>
    item.id === upload.id ? { ...item, status: nextStatus } : item,
  )
  upsertClientUploads(nextList)
}

async function saveDetails() {
  isSavingDetails.value = true
  try {
    detailsByClient.value = {
      ...detailsByClient.value,
      [clientId.value]: { ...detailsDraft.value },
    }
    persistDetails()
    feedback.success('Dados do cliente salvos localmente.')
  } finally {
    isSavingDetails.value = false
  }
}

function goBackToClients() {
  router.push('/clients')
}

function selectDay(day: number) {
  selectedDay.value = day
}

function selectToday() {
  currentDate.value = new Date(today.getFullYear(), today.getMonth(), 1)
  selectedDay.value = today.getDate()
}
</script>

<template>
  <AppLayout topbar-placeholder="Buscar posts agendados...">
    <div class="space-y-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="flex items-center gap-3">
          <Button variant="outline" class="gap-2" @click="goBackToClients">
            <ArrowLeft class="h-4 w-4" />
            Voltar para Clientes
          </Button>
          <div>
            <h1 class="text-2xl font-bold text-gray-900">Workspace do Cliente</h1>
            <p class="text-sm text-gray-500">{{ client.name }} · {{ client.niche }}</p>
          </div>
        </div>
        <Badge variant="secondary">Somente posts e agendamento</Badge>
      </div>

      <section class="rounded-2xl border border-gray-100 bg-white p-2 shadow-sm">
        <div class="flex gap-2 overflow-x-auto">
          <button
            v-for="tab in tabItems"
            :key="tab.id"
            @click="activeTab = tab.id"
            :class="[
              'min-w-[220px] rounded-xl border px-3 py-2 text-left transition-colors',
              activeTab === tab.id
                ? 'border-primary bg-primary/10'
                : 'border-gray-200 bg-white hover:border-primary/40 hover:bg-primary/5'
            ]"
          >
            <p :class="['text-sm font-semibold', activeTab === tab.id ? 'text-primary' : 'text-gray-700']">{{ tab.label }}</p>
            <p class="mt-0.5 text-xs text-gray-500">{{ tab.helper }}</p>
          </button>
        </div>
      </section>

      <div class="grid gap-3 sm:grid-cols-3">
        <div class="rounded-xl border border-gray-100 bg-white p-3 shadow-sm">
          <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">Posts no mês</p>
          <p class="mt-1 text-2xl font-bold text-gray-900">{{ monthUploadMetrics.planned }}</p>
        </div>
        <div class="rounded-xl border border-gray-100 bg-white p-3 shadow-sm">
          <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">Aprovados</p>
          <p class="mt-1 text-2xl font-bold text-emerald-600">{{ monthUploadMetrics.approved }}</p>
        </div>
        <div class="rounded-xl border border-gray-100 bg-white p-3 shadow-sm">
          <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">Taxa de aprovação</p>
          <p class="mt-1 text-2xl font-bold text-primary">{{ monthUploadMetrics.completion }}%</p>
        </div>
      </div>

      <ClientDetailsTab
        v-if="activeTab === 'details'"
        :client="client"
        v-model:details-draft="detailsDraft"
        :is-saving-details="isSavingDetails"
        @save-details="saveDetails"
      />

      <ClientCalendarTab
        v-else-if="activeTab === 'calendar'"
        :current-month-label="currentMonthLabel"
        :current-date="currentDate"
        :selected-day="selectedDay"
        :calendar-days="calendarDays"
        :uploads-per-day="uploadsPerDay"
        :selected-day-uploads="selectedDayUploads"
        @prev-month="prevMonth"
        @next-month="nextMonth"
        @today="selectToday"
        @select-day="selectDay"
        @cycle-status="cycleStatus"
        @remove-upload="removeUpload"
        @open-upload-modal="openUploadModal"
      />

      <ClientAiTab
        v-else-if="activeTab === 'ai'"
        :ai-field-checks="aiFieldChecks"
        :ai-missing-inputs="aiMissingInputs"
        :ai-prompt-context="aiPromptContext"
        :ai-recommendations="aiRecommendations"
      />
    </div>

    <UploadModal
      :is-open="isUploadModalOpen"
      :is-saving-upload="isSavingUpload"
      v-model:upload-draft="uploadDraft"
      :upload-field-errors="uploadFieldErrors"
      @close="isUploadModalOpen = false"
      @save-upload="saveUpload"
    />
  </AppLayout>
</template>
