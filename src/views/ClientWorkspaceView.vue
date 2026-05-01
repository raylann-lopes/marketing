<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  Clock3,
  Plus,
  Save,
  Trash2,
} from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'
import Badge from '@/components/ui/Badge.vue'
import { useFeedback } from '@/lib/feedback'

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
const dayNames = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SÁB']

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
    ...(detailsByClient.value[clientId.value] || {}),
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

      <section v-if="activeTab === 'details'" class="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
        <div class="mb-5 flex items-start justify-between gap-4">
          <div>
            <h2 class="text-xl font-bold text-gray-900">Dados Essenciais do Cliente</h2>
            <p class="text-sm text-gray-500">Somente contexto necessário para planejar posts e melhorar recomendações.</p>
          </div>
          <Button class="gap-2" :disabled="isSavingDetails" @click="saveDetails">
            <Save class="h-4 w-4" />
            {{ isSavingDetails ? 'Salvando...' : 'Salvar Dados' }}
          </Button>
        </div>

        <div class="mb-4 grid gap-3 rounded-xl border border-gray-100 bg-gray-50 p-3 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">Nome</p>
            <p class="mt-1 text-sm font-semibold text-gray-800">{{ client.name }}</p>
          </div>
          <div>
            <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">Nicho</p>
            <p class="mt-1 text-sm font-semibold text-gray-800">{{ client.niche }}</p>
          </div>
          <div>
            <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">E-mail</p>
            <p class="mt-1 text-sm font-semibold text-gray-800">{{ client.email }}</p>
          </div>
          <div>
            <p class="text-[11px] font-semibold uppercase tracking-wide text-gray-400">Tom de voz</p>
            <p class="mt-1 text-sm font-semibold text-gray-800">{{ client.voiceTone || 'Não informado' }}</p>
          </div>
        </div>

        <div class="grid gap-4 md:grid-cols-2">
          <div class="space-y-1.5 md:col-span-2">
            <label class="text-xs font-semibold uppercase text-gray-500">Objetivo Principal</label>
            <textarea v-model="detailsDraft.mainGoal" rows="3" class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" placeholder="Ex: gerar mais leads qualificados com conteúdo semanal." />
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold uppercase text-gray-500">Público-alvo</label>
            <input v-model="detailsDraft.targetAudience" type="text" class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" placeholder="Ex: donos de negócio local 30-50 anos" />
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold uppercase text-gray-500">Canais Prioritários</label>
            <input v-model="detailsDraft.preferredChannels" type="text" class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" placeholder="Ex: Instagram, Facebook" />
          </div>

          <div class="space-y-1.5 md:col-span-2">
            <label class="text-xs font-semibold uppercase text-gray-500">Pilares de Conteúdo</label>
            <input v-model="detailsDraft.contentPillars" type="text" class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" placeholder="Ex: prova social, bastidores, oferta" />
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold uppercase text-gray-500">SLA de Aprovação</label>
            <input v-model="detailsDraft.approvalSla" type="text" class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" placeholder="Ex: 24h úteis" />
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold uppercase text-gray-500">Sazonalidade</label>
            <input v-model="detailsDraft.seasonality" type="text" class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" placeholder="Ex: campanhas fortes em novembro e dezembro" />
          </div>

          <div class="space-y-1.5 md:col-span-2">
            <label class="text-xs font-semibold uppercase text-gray-500">Lembretes da Equipe</label>
            <textarea v-model="detailsDraft.reminders" rows="3" class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" placeholder="Ex: sempre validar CTA com comercial antes de enviar." />
          </div>
        </div>
      </section>

      <div v-else-if="activeTab === 'calendar'" class="grid gap-6 lg:grid-cols-[1fr_360px]">
        <section class="rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
          <div class="mb-5 flex items-center justify-between">
            <h2 class="text-xl font-bold text-gray-900">{{ currentMonthLabel }}</h2>
            <div class="flex items-center gap-2">
              <Button variant="outline" class="h-9 px-3" @click="currentDate = new Date(today.getFullYear(), today.getMonth(), 1); selectedDay = today.getDate()">
                Hoje
              </Button>
              <button class="rounded-lg p-1.5 hover:bg-gray-100" @click="prevMonth"><ChevronLeft class="h-4 w-4" /></button>
              <button class="rounded-lg p-1.5 hover:bg-gray-100" @click="nextMonth"><ChevronRight class="h-4 w-4" /></button>
            </div>
          </div>

          <div class="mb-2 grid grid-cols-7 text-center">
            <div v-for="d in dayNames" :key="d" class="py-1 text-xs font-semibold text-gray-400">{{ d }}</div>
          </div>

          <div class="grid grid-cols-7 gap-px overflow-hidden rounded-xl border border-gray-200 bg-gray-200">
            <div
              v-for="(day, index) in calendarDays"
              :key="index"
              :class="[
                'min-h-[110px] bg-white p-2 transition-colors',
                day ? 'cursor-pointer hover:bg-primary/5' : ''
              ]"
              @click="day && (selectedDay = day)"
            >
              <div
                v-if="day"
                :class="[
                  'mb-2 inline-flex h-7 w-7 items-center justify-center rounded-full text-xs font-semibold',
                  day === selectedDay ? 'bg-primary text-white' : 'text-gray-600'
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

        <aside class="flex min-h-[520px] flex-col rounded-2xl border border-gray-100 bg-white shadow-sm">
          <div class="border-b border-gray-100 p-4">
            <p class="text-xs font-semibold uppercase tracking-wide text-gray-400">Agenda do dia</p>
            <p class="text-3xl font-bold text-gray-900">{{ selectedDay }}</p>
            <p class="text-sm text-gray-500">{{ monthNames[currentDate.getMonth()] }}</p>
          </div>

          <div class="flex-1 space-y-3 overflow-y-auto p-4">
            <div v-if="selectedDayUploads.length === 0" class="flex h-40 flex-col items-center justify-center text-center text-gray-400">
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
                    @click="cycleStatus(upload)"
                  >
                    <CheckCircle2 class="h-4 w-4" />
                  </button>
                  <button
                    class="rounded-md p-1 text-gray-400 hover:bg-red-50 hover:text-red-600"
                    title="Remover"
                    @click="removeUpload(upload.id)"
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
            <Button class="w-full gap-2" @click="openUploadModal(selectedDay)">
              <Plus class="h-4 w-4" />
              Agendar Post no Dia
            </Button>
          </div>
        </aside>
      </div>

      <section v-else class="grid gap-6 xl:grid-cols-[380px_1fr]">
        <div class="space-y-4 rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
          <div>
            <h2 class="text-lg font-bold text-gray-900">Qualidade do Contexto</h2>
            <p class="text-xs text-gray-500">Recomendações ficam melhores quando os dados essenciais estão completos.</p>
          </div>

          <div>
            <div class="mb-1 flex items-center justify-between text-xs font-semibold text-gray-500">
              <span>Campos preenchidos</span>
              <span>{{ aiFieldChecks.filled }}/{{ aiFieldChecks.total }}</span>
            </div>
            <div class="h-2 rounded-full bg-gray-100">
              <div class="h-2 rounded-full bg-primary transition-all" :style="{ width: `${aiFieldChecks.percent}%` }" />
            </div>
            <p class="mt-1 text-xs text-gray-500">{{ aiFieldChecks.percent }}% do briefing principal</p>
          </div>

          <div class="rounded-xl border border-gray-200 bg-gray-50 p-3">
            <p class="text-xs font-semibold uppercase text-gray-500">Pendências</p>
            <ul v-if="aiMissingInputs.length > 0" class="mt-2 space-y-1 text-xs text-gray-700">
              <li v-for="missing in aiMissingInputs" :key="missing">• {{ missing }}</li>
            </ul>
            <p v-else class="mt-2 text-xs text-emerald-600">Contexto essencial preenchido.</p>
          </div>

          <div class="rounded-xl border border-gray-200 bg-gray-50 p-3">
            <p class="text-xs font-semibold uppercase text-gray-500">Resumo para Prompt</p>
            <pre class="mt-2 whitespace-pre-wrap text-xs text-gray-700">{{ aiPromptContext }}</pre>
          </div>
        </div>

        <div class="space-y-4 rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
          <div>
            <h2 class="text-lg font-bold text-gray-900">Recomendações de IA por Nicho</h2>
            <p class="text-xs text-gray-500">Sugestões simples com base no nicho e no calendário já planejado.</p>
          </div>

          <div class="grid gap-3">
            <article
              v-for="(rec, index) in aiRecommendations"
              :key="`${rec.title}-${index}`"
              class="rounded-xl border border-gray-100 bg-gray-50 p-4"
            >
              <div class="mb-2 flex items-center justify-between gap-2">
                <p class="text-sm font-semibold text-gray-900">{{ rec.title }}</p>
                <span
                  :class="[
                    'rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase',
                    rec.priority === 'ALTA'
                      ? 'border border-red-200 bg-red-50 text-red-700'
                      : rec.priority === 'MEDIA'
                        ? 'border border-amber-200 bg-amber-50 text-amber-700'
                        : 'border border-emerald-200 bg-emerald-50 text-emerald-700'
                  ]"
                >
                  {{ rec.priority }}
                </span>
              </div>
              <p class="text-xs leading-relaxed text-gray-600">{{ rec.details }}</p>
            </article>
          </div>
        </div>
      </section>
    </div>

    <div v-if="isUploadModalOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div class="w-full max-w-lg overflow-hidden rounded-2xl bg-white shadow-xl">
        <div class="flex items-center justify-between border-b border-gray-100 p-5">
          <h3 class="text-lg font-bold text-gray-900">Agendar Post</h3>
          <button class="rounded-lg p-1.5 text-gray-400 hover:bg-gray-100" @click="isUploadModalOpen = false">×</button>
        </div>

        <div class="space-y-4 p-5">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold uppercase text-gray-500">Data</label>
            <input v-model="uploadDraft.date" type="date" :class="['w-full rounded-lg border bg-gray-50 p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20', uploadFieldErrors.date ? 'border-red-500' : 'border-gray-200']" />
            <p v-if="uploadFieldErrors.date" class="text-[10px] font-medium text-red-500">{{ uploadFieldErrors.date }}</p>
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold uppercase text-gray-500">Título do Post</label>
            <input v-model="uploadDraft.title" type="text" :class="['w-full rounded-lg border bg-gray-50 p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20', uploadFieldErrors.title ? 'border-red-500' : 'border-gray-200']" placeholder="Ex: Reels - oferta da semana" />
            <p v-if="uploadFieldErrors.title" class="text-[10px] font-medium text-red-500">{{ uploadFieldErrors.title }}</p>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-1.5">
              <label class="text-xs font-semibold uppercase text-gray-500">Formato</label>
              <select v-model="uploadDraft.format" class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20">
                <option>Feed</option>
                <option>Reels</option>
                <option>Story</option>
                <option>Carrossel</option>
                <option>Vídeo Curto</option>
              </select>
            </div>
            <div class="space-y-1.5">
              <label class="text-xs font-semibold uppercase text-gray-500">Canal</label>
              <input v-model="uploadDraft.channel" type="text" :class="['w-full rounded-lg border bg-gray-50 p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20', uploadFieldErrors.channel ? 'border-red-500' : 'border-gray-200']" placeholder="Instagram" />
              <p v-if="uploadFieldErrors.channel" class="text-[10px] font-medium text-red-500">{{ uploadFieldErrors.channel }}</p>
            </div>
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold uppercase text-gray-500">Status inicial</label>
            <select v-model="uploadDraft.status" class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20">
              <option value="PLANNED">Planejado</option>
              <option value="UPLOADED">Enviado</option>
              <option value="APPROVED">Aprovado</option>
            </select>
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold uppercase text-gray-500">Observações</label>
            <textarea v-model="uploadDraft.notes" rows="3" class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" placeholder="Briefing curto, CTA ou observações para equipe." />
          </div>
        </div>

        <div class="flex gap-3 border-t border-gray-100 bg-gray-50 p-5">
          <Button variant="outline" class="flex-1" @click="isUploadModalOpen = false">Cancelar</Button>
          <Button class="flex-1 gap-2" :disabled="isSavingUpload" @click="saveUpload">
            <Clock3 class="h-4 w-4" />
            {{ isSavingUpload ? 'Salvando...' : 'Salvar Agendamento' }}
          </Button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
