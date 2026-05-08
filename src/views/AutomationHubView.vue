<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  AlertCircle,
  ArrowRight,
  Bot,
  CalendarClock,
  CheckCircle2,
  Clock3,
  FileText,
  Lightbulb,
  MessageCircle,
  Play,
  RefreshCw,
  Search,
  Send,
  Settings2,
  Sparkles,
  Zap,
} from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Badge from '@/components/ui/Badge.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import type { Component } from 'vue'

type ClientHealth = 'READY' | 'ATTENTION' | 'BLOCKED'
type AutomationStatus = 'ACTIVE' | 'PAUSED' | 'ERROR' | 'MISSING_SETUP'

type AutomationClient = {
  id: number
  name: string
  niche: string
  health: ClientHealth
  score: number
  summary: string
  nextAction: string
  missing: string[]
}

type AutomationModule = {
  id: number
  clientId: number
  title: string
  description: string
  icon: Component
  status: AutomationStatus
  lastRun: string
  nextRun: string
  impact: string
  primaryAction: string
}

type ActivityItem = {
  id: number
  clientId: number
  title: string
  description: string
  time: string
  status: 'SUCCESS' | 'ERROR' | 'INFO'
}

const searchTerm = ref('')
const selectedClientId = ref(1)
const selectedStatus = ref<AutomationStatus | 'ALL'>('ALL')

const clients = ref<AutomationClient[]>([
  {
    id: 1,
    name: 'Clínica Aurora',
    niche: 'Saúde e estética',
    health: 'ATTENTION',
    score: 78,
    summary: 'Aprovação e legenda estão prontas, mas houve falha no último envio pelo WhatsApp.',
    nextAction: 'Reprocessar envio de aprovação',
    missing: ['Revisar envio WhatsApp'],
  },
  {
    id: 2,
    name: 'Imobiliária Norte',
    niche: 'Imobiliário',
    health: 'READY',
    score: 94,
    summary: 'Fluxo principal configurado e rodando sem falhas nas últimas execuções.',
    nextAction: 'Acompanhar próximas demandas',
    missing: [],
  },
  {
    id: 3,
    name: 'Studio Forma',
    niche: 'Arquitetura',
    health: 'BLOCKED',
    score: 46,
    summary: 'Automações de aprovação estão bloqueadas porque o grupo do WhatsApp ainda não foi vinculado.',
    nextAction: 'Configurar grupo do WhatsApp',
    missing: ['Grupo do WhatsApp', 'Coleta Apify'],
  },
  {
    id: 4,
    name: 'Café Jardim',
    niche: 'Gastronomia',
    health: 'READY',
    score: 88,
    summary: 'Coleta de ideias e resumo semanal estão funcionando. Existem ideias novas para converter.',
    nextAction: 'Converter ideias em demandas',
    missing: [],
  },
])

const automationModules = ref<AutomationModule[]>([
  {
    id: 1,
    clientId: 1,
    title: 'Enviar aprovação no WhatsApp',
    description: 'Quando a legenda estiver pronta, envia a demanda para o grupo do cliente.',
    icon: MessageCircle,
    status: 'ERROR',
    lastRun: 'Hoje, 09:20',
    nextRun: 'Manual',
    impact: 'Evita envio manual de aprovação',
    primaryAction: 'Reprocessar',
  },
  {
    id: 2,
    clientId: 1,
    title: 'Gerar legenda com IA',
    description: 'Gera legenda usando briefing da demanda, nicho e tom de voz do cliente.',
    icon: Sparkles,
    status: 'ACTIVE',
    lastRun: 'Hoje, 09:12',
    nextRun: 'Ao finalizar arte',
    impact: 'Reduz tempo de copy',
    primaryAction: 'Rodar agora',
  },
  {
    id: 3,
    clientId: 1,
    title: 'Buscar ideias virais',
    description: 'Usa Apify e IA para sugerir temas adaptados ao nicho do cliente.',
    icon: Lightbulb,
    status: 'PAUSED',
    lastRun: 'Ontem, 06:00',
    nextRun: 'Pausado',
    impact: 'Alimenta novas demandas',
    primaryAction: 'Ativar',
  },
  {
    id: 4,
    clientId: 1,
    title: 'Resumo semanal',
    description: 'Gera resumo de posts, aprovações, atrasos e pendências do cliente.',
    icon: FileText,
    status: 'ACTIVE',
    lastRun: 'Segunda, 08:00',
    nextRun: 'Próxima segunda',
    impact: 'Facilita acompanhamento',
    primaryAction: 'Ver resumo',
  },
  {
    id: 5,
    clientId: 2,
    title: 'Enviar aprovação no WhatsApp',
    description: 'Envia automaticamente posts prontos para aprovação no grupo vinculado.',
    icon: MessageCircle,
    status: 'ACTIVE',
    lastRun: 'Ontem, 16:40',
    nextRun: 'Ao finalizar arte',
    impact: 'Remove etapa manual',
    primaryAction: 'Rodar agora',
  },
  {
    id: 6,
    clientId: 2,
    title: 'Gerar legenda com IA',
    description: 'Cria legenda usando objetivo do post e posicionamento imobiliário.',
    icon: Sparkles,
    status: 'ACTIVE',
    lastRun: 'Ontem, 16:32',
    nextRun: 'Ao criar aprovação',
    impact: 'Padroniza comunicação',
    primaryAction: 'Rodar agora',
  },
  {
    id: 7,
    clientId: 2,
    title: 'Monitorar atrasos',
    description: 'Destaca demandas próximas da data de publicação que ainda estão em produção.',
    icon: CalendarClock,
    status: 'ACTIVE',
    lastRun: 'Hoje, 08:00',
    nextRun: 'Hoje, 16:00',
    impact: 'Evita perda de prazo',
    primaryAction: 'Ver board',
  },
  {
    id: 8,
    clientId: 3,
    title: 'Enviar aprovação no WhatsApp',
    description: 'Bloqueado até vincular um grupo do WhatsApp ao cliente.',
    icon: MessageCircle,
    status: 'MISSING_SETUP',
    lastRun: 'Nunca',
    nextRun: 'Após configuração',
    impact: 'Depende de configuração',
    primaryAction: 'Configurar',
  },
  {
    id: 9,
    clientId: 3,
    title: 'Buscar ideias virais',
    description: 'Bloqueado até ativar a coleta de referências para o nicho.',
    icon: Lightbulb,
    status: 'MISSING_SETUP',
    lastRun: 'Nunca',
    nextRun: 'Após configuração',
    impact: 'Depende de Apify',
    primaryAction: 'Configurar',
  },
  {
    id: 10,
    clientId: 4,
    title: 'Buscar ideias virais',
    description: 'Coleta sinais recentes e transforma em ideias de posts para cafeteria.',
    icon: Lightbulb,
    status: 'ACTIVE',
    lastRun: 'Hoje, 06:00',
    nextRun: 'Amanhã, 06:00',
    impact: 'Gerou 5 ideias novas',
    primaryAction: 'Ver ideias',
  },
  {
    id: 11,
    clientId: 4,
    title: 'Resumo semanal',
    description: 'Compila publicações, aprovações e oportunidades do cliente.',
    icon: FileText,
    status: 'ACTIVE',
    lastRun: 'Segunda, 08:00',
    nextRun: 'Próxima segunda',
    impact: 'Relatório pronto para revisão',
    primaryAction: 'Ver resumo',
  },
])

const activities = ref<ActivityItem[]>([
  {
    id: 1,
    clientId: 1,
    title: 'Falha no envio de aprovação',
    description: 'Legenda pronta, mas mensagem não foi entregue ao grupo.',
    time: 'Hoje, 09:20',
    status: 'ERROR',
  },
  {
    id: 2,
    clientId: 1,
    title: 'Legenda gerada com sucesso',
    description: 'Demanda de procedimento estético recebeu legenda com IA.',
    time: 'Hoje, 09:12',
    status: 'SUCCESS',
  },
  {
    id: 3,
    clientId: 4,
    title: '5 ideias criadas',
    description: 'A coleta de tendências encontrou novos temas para cafeteria.',
    time: 'Hoje, 06:00',
    status: 'SUCCESS',
  },
  {
    id: 4,
    clientId: 2,
    title: 'Board verificado',
    description: 'Nenhuma demanda imobiliária atrasada no momento.',
    time: 'Hoje, 08:00',
    status: 'INFO',
  },
])

const statusLabels: Record<AutomationStatus, string> = {
  ACTIVE: 'Ativa',
  PAUSED: 'Pausada',
  ERROR: 'Erro',
  MISSING_SETUP: 'Configurar',
}

const statusVariants: Record<AutomationStatus, 'success' | 'warning' | 'destructive' | 'purple'> = {
  ACTIVE: 'success',
  PAUSED: 'warning',
  ERROR: 'destructive',
  MISSING_SETUP: 'purple',
}

const healthLabels: Record<ClientHealth, string> = {
  READY: 'Pronto',
  ATTENTION: 'Atenção',
  BLOCKED: 'Bloqueado',
}

const healthClasses: Record<ClientHealth, string> = {
  READY: 'bg-emerald-50 text-emerald-700 border-emerald-100',
  ATTENTION: 'bg-orange-50 text-orange-700 border-orange-100',
  BLOCKED: 'bg-red-50 text-red-700 border-red-100',
}

const statusOptions: Array<AutomationStatus | 'ALL'> = ['ALL', 'ACTIVE', 'ERROR', 'MISSING_SETUP', 'PAUSED']

const selectedClient = computed<AutomationClient>(() => {
  const client = clients.value.find((item) => item.id === selectedClientId.value)
  if (client) return client
  return {
    id: 0,
    name: 'Cliente não encontrado',
    niche: 'Sem nicho',
    health: 'BLOCKED',
    score: 0,
    summary: 'Selecione um cliente válido para visualizar as automações.',
    nextAction: 'Selecionar cliente',
    missing: [],
  }
})

const filteredModules = computed(() => {
  const normalized = searchTerm.value.trim().toLowerCase()

  return automationModules.value.filter((automation) => {
    const matchesClient = automation.clientId === selectedClientId.value
    const matchesStatus = selectedStatus.value === 'ALL' || automation.status === selectedStatus.value
    const matchesSearch =
      !normalized ||
      automation.title.toLowerCase().includes(normalized) ||
      automation.description.toLowerCase().includes(normalized) ||
      automation.impact.toLowerCase().includes(normalized)

    return matchesClient && matchesStatus && matchesSearch
  })
})

const clientActivities = computed(() =>
  activities.value.filter((activity) => activity.clientId === selectedClientId.value),
)

const overviewStats = computed(() => {
  const modules = automationModules.value
  return [
    {
      label: 'Automações ativas',
      value: modules.filter((automation) => automation.status === 'ACTIVE').length,
      icon: Zap,
      tone: 'text-emerald-600 bg-emerald-50',
    },
    {
      label: 'Precisam configurar',
      value: modules.filter((automation) => automation.status === 'MISSING_SETUP').length,
      icon: Settings2,
      tone: 'text-primary bg-purple-50',
    },
    {
      label: 'Falhas abertas',
      value: modules.filter((automation) => automation.status === 'ERROR').length,
      icon: AlertCircle,
      tone: 'text-red-600 bg-red-50',
    },
    {
      label: 'Clientes prontos',
      value: clients.value.filter((client) => client.health === 'READY').length,
      icon: CheckCircle2,
      tone: 'text-sky-600 bg-sky-50',
    },
  ]
})
</script>

<template>
  <AppLayout v-model:search="searchTerm" topbar-placeholder="Buscar automações deste cliente...">
    <div class="flex h-full min-h-0 flex-col gap-3 overflow-hidden">
    <div class="flex shrink-0 flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
      <div class="flex items-start gap-3">
        <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
          <Bot class="h-4 w-4" />
        </div>
        <div>
          <h1 class="text-xl font-bold text-gray-900">Hub de Automação</h1>
          <p class="mt-0.5 text-xs text-gray-500">
            Controle o que roda sozinho para cada cliente e o que ainda bloqueia a operação.
          </p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <Button variant="outline" class="gap-2">
          <RefreshCw class="h-4 w-4" />
          Atualizar
        </Button>
        <Button class="gap-2">
          <Play class="h-4 w-4" />
          Rodar selecionada
        </Button>
      </div>
    </div>

    <div class="grid shrink-0 grid-cols-2 gap-3 lg:grid-cols-4">
      <Card v-for="stat in overviewStats" :key="stat.label" class="p-3">
        <div class="flex items-start justify-between gap-3">
          <div>
            <p class="text-xs font-semibold uppercase text-gray-400">{{ stat.label }}</p>
            <p class="mt-1 text-xl font-bold text-gray-900">{{ stat.value }}</p>
          </div>
          <div :class="['flex h-8 w-8 items-center justify-center rounded-lg', stat.tone]">
            <component :is="stat.icon" class="h-4 w-4" />
          </div>
        </div>
      </Card>
    </div>

    <div class="grid min-h-0 flex-1 grid-cols-1 gap-3 overflow-hidden xl:grid-cols-[260px_minmax(0,1fr)_320px]">
      <Card class="flex min-h-0 flex-col overflow-hidden">
        <div class="shrink-0 border-b border-gray-100 px-4 py-3">
          <h2 class="font-semibold text-gray-900">Clientes</h2>
          <p class="mt-0.5 text-xs text-gray-500">Selecione para ver automações e bloqueios.</p>
        </div>

        <div class="min-h-0 space-y-2 overflow-hidden p-3">
          <button
            v-for="client in clients"
            :key="client.id"
            type="button"
            :class="[
              'w-full rounded-lg border p-2.5 text-left transition-colors',
              selectedClientId === client.id
                ? 'border-primary/30 bg-primary/5'
                : 'border-gray-100 bg-white hover:bg-gray-50',
            ]"
            @click="selectedClientId = client.id"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <p class="truncate text-sm font-semibold text-gray-900">{{ client.name }}</p>
                <p class="mt-0.5 truncate text-xs text-gray-500">{{ client.niche }}</p>
              </div>
              <span :class="['rounded-full border px-2 py-0.5 text-[10px] font-semibold', healthClasses[client.health]]">
                {{ healthLabels[client.health] }}
              </span>
            </div>
            <div class="mt-3 h-1.5 overflow-hidden rounded-full bg-gray-100">
              <div class="h-full rounded-full bg-primary" :style="{ width: `${client.score}%` }" />
            </div>
          </button>
        </div>
      </Card>

      <div class="flex min-h-0 flex-col gap-3 overflow-hidden">
        <Card class="shrink-0 p-4">
          <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
            <div class="max-w-2xl">
              <div class="mb-1.5 flex flex-wrap items-center gap-2">
                <span :class="['rounded-full border px-2.5 py-0.5 text-xs font-semibold', healthClasses[selectedClient.health]]">
                  {{ healthLabels[selectedClient.health] }}
                </span>
                <Badge variant="outline">{{ selectedClient.niche }}</Badge>
              </div>
              <h2 class="text-lg font-bold text-gray-900">{{ selectedClient.name }}</h2>
              <p class="mt-1 line-clamp-2 text-sm leading-5 text-gray-600">{{ selectedClient.summary }}</p>
            </div>

            <div class="min-w-[210px] rounded-lg border border-gray-100 bg-gray-50 p-3">
              <p class="text-xs font-semibold uppercase text-gray-400">Próxima ação</p>
              <p class="mt-1 line-clamp-1 text-sm font-semibold text-gray-900">{{ selectedClient.nextAction }}</p>
              <Button size="sm" class="mt-2 w-full gap-2">
                Resolver
                <ArrowRight class="h-4 w-4" />
              </Button>
            </div>
          </div>

          <div v-if="selectedClient.missing.length > 0" class="mt-3 flex flex-wrap gap-2">
            <span
              v-for="missing in selectedClient.missing"
              :key="missing"
              class="rounded-full border border-red-100 bg-red-50 px-2.5 py-1 text-xs font-semibold text-red-700"
            >
              Falta: {{ missing }}
            </span>
          </div>
        </Card>

        <Card class="flex min-h-0 flex-1 flex-col overflow-hidden">
          <div class="shrink-0 border-b border-gray-100 px-4 py-3">
            <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h2 class="font-semibold text-gray-900">Automações do cliente</h2>
                <p class="mt-0.5 text-xs text-gray-500">Cards acionáveis, com status e impacto operacional.</p>
              </div>

              <div class="flex flex-wrap items-center gap-2">
                <Button
                  v-for="status in statusOptions"
                  :key="status"
                  size="sm"
                  :variant="selectedStatus === status ? 'default' : 'outline'"
                  class="h-7 rounded-full px-3 text-xs"
                  @click="selectedStatus = status"
                >
                  {{ status === 'ALL' ? 'Todas' : statusLabels[status] }}
                </Button>
              </div>
            </div>
          </div>

          <div class="shrink-0 border-b border-gray-100 bg-gray-50/70 px-4 py-2">
            <div class="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-1.5">
              <Search class="h-4 w-4 text-gray-400" />
              <input
                v-model="searchTerm"
                class="w-full bg-transparent text-sm outline-none placeholder:text-gray-400"
                placeholder="Buscar automação, impacto ou descrição..."
              />
            </div>
          </div>

          <div class="grid min-h-0 grid-cols-1 gap-3 overflow-hidden p-3 lg:grid-cols-2">
            <div
              v-for="automation in filteredModules"
              :key="automation.id"
              class="rounded-lg border border-gray-100 bg-white p-3 transition-shadow hover:shadow-sm"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="flex min-w-0 items-start gap-3">
                  <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-gray-50 text-primary">
                    <component :is="automation.icon" class="h-4 w-4" />
                  </div>
                  <div class="min-w-0">
                    <p class="text-sm font-semibold text-gray-900">{{ automation.title }}</p>
                    <p class="mt-1 line-clamp-2 text-xs leading-5 text-gray-500">{{ automation.description }}</p>
                  </div>
                </div>
                <Badge :variant="statusVariants[automation.status]" class="shrink-0 text-[10px]">
                  {{ statusLabels[automation.status] }}
                </Badge>
              </div>

              <div class="mt-3 grid grid-cols-2 gap-3 rounded-lg bg-gray-50 p-2.5">
                <div>
                  <p class="text-[10px] font-semibold uppercase text-gray-400">Última</p>
                  <p class="mt-1 text-xs font-medium text-gray-800">{{ automation.lastRun }}</p>
                </div>
                <div>
                  <p class="text-[10px] font-semibold uppercase text-gray-400">Próxima</p>
                  <p class="mt-1 text-xs font-medium text-gray-800">{{ automation.nextRun }}</p>
                </div>
              </div>

              <div class="mt-3 flex items-center justify-between gap-3">
                <p class="text-xs font-medium text-gray-500">{{ automation.impact }}</p>
                <Button size="sm" :variant="automation.status === 'ERROR' ? 'default' : 'outline'" class="h-8">
                  {{ automation.primaryAction }}
                </Button>
              </div>
            </div>

            <div v-if="filteredModules.length === 0" class="col-span-full px-5 py-10 text-center text-sm text-gray-400">
              Nenhuma automação encontrada para este filtro.
            </div>
          </div>
        </Card>
      </div>

      <div class="grid min-h-0 grid-rows-[1fr_1fr] gap-3 overflow-hidden">
        <Card class="min-h-0 overflow-hidden p-4">
          <div class="mb-3 flex items-center justify-between">
            <div>
              <h2 class="font-semibold text-gray-900">Fluxo sugerido</h2>
              <p class="mt-0.5 text-xs text-gray-500">Ordem natural das automações do cliente.</p>
            </div>
            <Send class="h-5 w-5 text-gray-400" />
          </div>

          <div class="space-y-2">
            <div class="flex gap-3 rounded-lg border border-gray-100 p-2.5">
              <div class="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-purple-50 text-primary">
                <Lightbulb class="h-3.5 w-3.5" />
              </div>
              <div>
                <p class="text-sm font-semibold text-gray-900">Ideia</p>
                <p class="text-xs leading-5 text-gray-500">Apify e IA sugerem temas para novas demandas.</p>
              </div>
            </div>
            <div class="flex gap-3 rounded-lg border border-gray-100 p-2.5">
              <div class="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-purple-50 text-primary">
                <Sparkles class="h-3.5 w-3.5" />
              </div>
              <div>
                <p class="text-sm font-semibold text-gray-900">Legenda</p>
                <p class="text-xs leading-5 text-gray-500">IA cria copy com tom de voz e objetivo da demanda.</p>
              </div>
            </div>
            <div class="flex gap-3 rounded-lg border border-gray-100 p-2.5">
              <div class="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-purple-50 text-primary">
                <MessageCircle class="h-3.5 w-3.5" />
              </div>
              <div>
                <p class="text-sm font-semibold text-gray-900">Aprovação</p>
                <p class="text-xs leading-5 text-gray-500">WhatsApp envia conteúdo para o grupo do cliente.</p>
              </div>
            </div>
            <div class="flex gap-3 rounded-lg border border-gray-100 p-2.5">
              <div class="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-purple-50 text-primary">
                <CalendarClock class="h-3.5 w-3.5" />
              </div>
              <div>
                <p class="text-sm font-semibold text-gray-900">Agenda</p>
                <p class="text-xs leading-5 text-gray-500">Monitor indica atrasos e próximas publicações.</p>
              </div>
            </div>
          </div>
        </Card>

        <Card class="min-h-0 overflow-hidden p-4">
          <div class="mb-3 flex items-center justify-between">
            <div>
              <h2 class="font-semibold text-gray-900">Atividade recente</h2>
              <p class="mt-0.5 text-xs text-gray-500">Eventos do cliente selecionado.</p>
            </div>
            <Clock3 class="h-5 w-5 text-gray-400" />
          </div>

          <div class="space-y-3 overflow-hidden">
            <div v-for="activity in clientActivities" :key="activity.id" class="flex gap-3">
              <span
                :class="[
                  'mt-1 h-2 w-2 rounded-full',
                  activity.status === 'SUCCESS' && 'bg-emerald-500',
                  activity.status === 'ERROR' && 'bg-red-500',
                  activity.status === 'INFO' && 'bg-primary',
                ]"
              />
              <div>
                <p class="text-sm font-medium text-gray-900">{{ activity.title }}</p>
                <p class="mt-0.5 text-xs leading-5 text-gray-500">{{ activity.description }}</p>
                <p class="mt-1 text-xs text-gray-400">{{ activity.time }}</p>
              </div>
            </div>

            <div v-if="clientActivities.length === 0" class="rounded-lg border border-gray-100 p-4 text-sm text-gray-400">
              Nenhuma atividade recente para este cliente.
            </div>
          </div>
        </Card>
      </div>
    </div>
    </div>
  </AppLayout>
</template>
