<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  BarChart3,
  CalendarDays,
  Download,
  Eye,
  MessageCircle,
  MousePointerClick,
  PlayCircle,
  RefreshCw,
  TrendingUp,
  Users,
  Wallet,
} from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'
import Card from '@/components/ui/Card.vue'
import { clientService, type Client } from '@/services/clientService'
import { metaAdsService, type AdsReportSnapshot } from '@/services/metaAdsService'
import { getErrorMessage } from '@/lib/errors'
import { useFeedback } from '@/lib/feedback'

const clients = ref<Client[]>([])
const selectedClientId = ref('')
const selectedMonth = ref(previousMonth())
const report = ref<AdsReportSnapshot | null>(null)
const loadingClients = ref(true)
const loadingReport = ref(false)
const error = ref('')
const feedback = useFeedback()

const sortedClients = computed(() =>
  [...clients.value].sort((a, b) => a.name.localeCompare(b.name))
)

const selectedPeriod = computed(() => monthRange(selectedMonth.value))

const selectedClientName = computed(() => {
  const client = clients.value.find(item => String(item.id) === selectedClientId.value)
  return client?.name ?? 'Selecione um cliente'
})

const summaryCards = computed(() => {
  if (!report.value) return []
  const summary = report.value.summary
  return [
    {
      label: 'Investimento',
      value: currency(summary.spend),
      icon: Wallet,
      tone: 'text-emerald-600 bg-emerald-50',
    },
    {
      label: 'Alcance',
      value: integer(summary.reach),
      icon: Users,
      tone: 'text-blue-600 bg-blue-50',
    },
    {
      label: 'Impressões',
      value: integer(summary.impressions),
      icon: Eye,
      tone: 'text-violet-600 bg-violet-50',
    },
    {
      label: 'Cliques',
      value: integer(summary.clicks),
      icon: MousePointerClick,
      tone: 'text-amber-600 bg-amber-50',
    },
    {
      label: 'CTR',
      value: percent(summary.ctr),
      icon: TrendingUp,
      tone: 'text-pink-600 bg-pink-50',
    },
    {
      label: 'CPC',
      value: currency(summary.cpc),
      icon: BarChart3,
      tone: 'text-sky-600 bg-sky-50',
    },
    {
      label: 'CPM',
      value: currency(summary.cpm),
      icon: CalendarDays,
      tone: 'text-slate-600 bg-slate-100',
    },
  ]
})

const actionRows = computed(() => {
  if (!report.value) return []
  const actions = report.value.actions
  return [
    {
      label: 'Cliques no link',
      description: 'Pessoas que tocaram no link do anúncio.',
      value: integer(actions.linkClicks),
      icon: MousePointerClick,
    },
    {
      label: 'Engajamentos',
      description: 'Interações gerais com os anúncios.',
      value: integer(actions.postEngagement),
      icon: BarChart3,
    },
    {
      label: 'Reações',
      description: 'Curtidas e reações registradas nos criativos.',
      value: integer(actions.postReactions),
      icon: TrendingUp,
    },
    {
      label: 'Visualizações de vídeo',
      description: 'Volume de views em anúncios com vídeo.',
      value: integer(actions.videoViews),
      icon: PlayCircle,
    },
    {
      label: 'Conversas iniciadas',
      description: 'Conversas abertas a partir dos anúncios.',
      value: integer(actions.conversationsStarted),
      icon: MessageCircle,
    },
    {
      label: 'Conexões por mensagem',
      description: 'Novas conexões atribuídas às campanhas.',
      value: integer(actions.messagingConnections),
      icon: MessageCircle,
    },
  ]
})

function previousMonth() {
  const date = new Date()
  date.setMonth(date.getMonth() - 1)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function monthRange(month: string) {
  const [rawYear, rawMonth] = month.split('-').map(Number)
  const fallback = previousMonth().split('-').map(Number)
  const year = Number.isFinite(rawYear) ? rawYear : fallback[0]
  const monthNumber = Number.isFinite(rawMonth) ? rawMonth : fallback[1]

  if (year == null || monthNumber == null) {
    throw new Error('Mês inválido')
  }

  const start = new Date(year, monthNumber - 1, 1)
  const end = new Date(year, monthNumber, 0)

  return {
    dateStart: formatInputDate(start),
    dateStop: formatInputDate(end),
    label: start.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' }),
  }
}

function formatInputDate(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function currency(value: number) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value ?? 0)
}

function integer(value: number) {
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 }).format(value ?? 0)
}

function percent(value: number) {
  return `${new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value ?? 0)}%`
}

function decimal(value: number) {
  return new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value ?? 0)
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

async function fetchClients() {
  loadingClients.value = true
  error.value = ''

  try {
    const data = await clientService.getAll()
    clients.value = Array.isArray(data) ? data : []
    if (!selectedClientId.value && clients.value[0]?.id != null) {
      selectedClientId.value = String(clients.value[0].id)
    }
  } catch (e: unknown) {
    error.value = `Erro ao carregar clientes: ${getErrorMessage(e)}`
  } finally {
    loadingClients.value = false
  }
}

async function generateReport() {
  if (!selectedClientId.value) {
    feedback.error('Selecione um cliente.')
    return
  }

  loadingReport.value = true
  error.value = ''

  try {
    report.value = await metaAdsService.getReportSnapshot(
      selectedClientId.value,
      selectedPeriod.value.dateStart,
      selectedPeriod.value.dateStop
    )
  } catch (e: unknown) {
    report.value = null
    error.value = `Erro ao gerar relatório: ${getErrorMessage(e)}`
  } finally {
    loadingReport.value = false
  }
}

function downloadHtmlReport() {
  if (!report.value) return

  const html = buildHtmlReport(report.value)
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  const filenameClient = report.value.clientName.toLowerCase().replace(/[^a-z0-9]+/gi, '-')

  link.href = url
  link.download = `relatorio-anuncios-${filenameClient}-${selectedMonth.value}.html`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function buildHtmlReport(snapshot: AdsReportSnapshot) {
  const period = `${date(snapshot.dateStart)} a ${date(snapshot.dateStop)}`
  const monthLabel = reportMonthLabel(snapshot.dateStart)
  const accountName = snapshot.accountName || 'Meta Ads'
  const frequency = snapshot.summary.impressions / Math.max(snapshot.summary.reach, 1)
  const conversations = snapshot.actions.conversationsStarted || snapshot.actions.messagingConnections
  const clickShare = percentWidth(snapshot.actions.linkClicks, snapshot.summary.clicks)
  const engagementShare = percentWidth(snapshot.actions.postEngagement, snapshot.summary.impressions)
  const conversationShare = percentWidth(conversations, snapshot.summary.clicks)
  const generatedAt = new Date().toLocaleDateString('pt-BR', {
    month: 'long',
    year: 'numeric',
  })

  return `<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Relatório de Desempenho - ${escapeHtml(snapshot.clientName)} - ${escapeHtml(monthLabel)}</title>
<link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,300;0,400;0,600;1,300;1,400&family=DM+Sans:wght@300;400;500;600&display=swap" rel="stylesheet">
<style>
  :root {
    --gold: #C9A84C;
    --gold-light: #E8C97A;
    --gold-dim: #8B6E2F;
    --dark: #0D0F14;
    --surface: #13161E;
    --card: #1A1E29;
    --card2: #1F2432;
    --border: rgba(201,168,76,0.18);
    --text: #E8E4DC;
    --muted: #7A7F92;
    --green: #4CAF82;
    --blue: #5B9BD5;
    --warn: #FFD54F;
  }

  * { margin: 0; padding: 0; box-sizing: border-box; }

  body {
    background: var(--dark);
    color: var(--text);
    font-family: 'DM Sans', sans-serif;
    font-weight: 300;
    line-height: 1.6;
  }

  .cover {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    text-align: center;
    position: relative;
    overflow: hidden;
    padding: 60px 40px;
  }

  .cover::before {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(ellipse 80% 60% at 50% 40%, rgba(201,168,76,0.08) 0%, transparent 70%);
  }

  .cover-lines {
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(201,168,76,0.04) 1px, transparent 1px),
      linear-gradient(90deg, rgba(201,168,76,0.04) 1px, transparent 1px);
    background-size: 60px 60px;
  }

  .cover-content { position: relative; z-index: 1; width: 100%; }
  .cover-ornament {
    width: 1px;
    height: 80px;
    background: linear-gradient(to bottom, transparent, var(--gold), transparent);
    margin: 0 auto 40px;
    opacity: 0.6;
  }

  .cover-label {
    font-size: 11px;
    font-weight: 500;
    letter-spacing: 4px;
    color: var(--gold);
    text-transform: uppercase;
    margin-bottom: 24px;
  }

  .cover h1 {
    font-family: 'Cormorant Garamond', serif;
    font-size: clamp(42px, 7vw, 80px);
    font-weight: 300;
    letter-spacing: -1px;
    line-height: 1.1;
    color: var(--text);
    margin-bottom: 10px;
  }

  .cover h1 em { font-style: italic; color: var(--gold-light); }
  .cover-sub {
    font-family: 'Cormorant Garamond', serif;
    font-size: 20px;
    font-weight: 300;
    color: var(--muted);
    margin-bottom: 50px;
    letter-spacing: 1px;
  }

  .cover-divider {
    width: 120px;
    height: 1px;
    background: linear-gradient(to right, transparent, var(--gold), transparent);
    margin: 0 auto 40px;
  }

  .cover-meta {
    display: flex;
    gap: 50px;
    justify-content: center;
    flex-wrap: wrap;
  }

  .cover-meta-item { text-align: center; }
  .cover-meta-item .val {
    font-family: 'Cormorant Garamond', serif;
    font-size: 36px;
    font-weight: 300;
    color: var(--gold-light);
    display: block;
    line-height: 1;
  }

  .cover-meta-item .lbl {
    font-size: 11px;
    letter-spacing: 2px;
    text-transform: uppercase;
    color: var(--muted);
    margin-top: 6px;
  }

  .wrap { max-width: 960px; margin: 0 auto; padding: 0 32px; }
  .section { padding: 72px 0; border-top: 1px solid var(--border); }
  .section-header { display: flex; align-items: baseline; gap: 16px; margin-bottom: 48px; }
  .section-num {
    font-family: 'Cormorant Garamond', serif;
    font-size: 13px;
    color: var(--gold);
    letter-spacing: 2px;
  }

  .section-title {
    font-family: 'Cormorant Garamond', serif;
    font-size: 32px;
    font-weight: 300;
    letter-spacing: -0.5px;
  }

  .section-title em { font-style: italic; color: var(--gold-light); }
  .platform-badge {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 14px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
    letter-spacing: 1px;
    text-transform: uppercase;
    margin-bottom: 32px;
    background: rgba(201,168,76,0.12);
    border: 1px solid rgba(201,168,76,0.3);
    color: var(--gold-light);
  }

  .metric-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 16px;
    margin-bottom: 32px;
  }

  .metric-card {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 24px 22px;
    position: relative;
    overflow: hidden;
  }

  .metric-card::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 2px;
    background: linear-gradient(to right, var(--gold), transparent);
    opacity: 0.5;
  }

  .metric-card.highlight {
    border-color: rgba(201,168,76,0.35);
    background: linear-gradient(135deg, var(--card2), rgba(201,168,76,0.04));
  }

  .metric-card.highlight::before { opacity: 1; }
  .metric-label {
    font-size: 11px;
    letter-spacing: 1.5px;
    text-transform: uppercase;
    color: var(--muted);
    margin-bottom: 12px;
  }

  .metric-value {
    font-family: 'Cormorant Garamond', serif;
    font-size: 38px;
    font-weight: 300;
    line-height: 1;
    color: var(--text);
  }

  .metric-value.gold { color: var(--gold-light); }
  .metric-value.green { color: var(--green); }
  .metric-sub { margin-top: 8px; font-size: 12px; color: var(--muted); }

  .stat-highlight-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1px;
    background: var(--border);
    border: 1px solid var(--border);
    border-radius: 12px;
    overflow: hidden;
    margin-bottom: 32px;
  }

  .stat-hl {
    background: var(--card);
    padding: 28px 24px;
    text-align: center;
  }

  .stat-hl .v {
    font-family: 'Cormorant Garamond', serif;
    font-size: 44px;
    font-weight: 300;
    color: var(--gold-light);
    display: block;
    line-height: 1;
  }

  .stat-hl .l {
    font-size: 11px;
    letter-spacing: 1.5px;
    text-transform: uppercase;
    color: var(--muted);
    margin-top: 8px;
    display: block;
  }

  .stat-hl .g { font-size: 12px; color: var(--green); margin-top: 6px; font-weight: 500; }
  .insight-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
  .insight-card {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 24px;
  }

  .insight-card h4 {
    font-size: 11px;
    letter-spacing: 2px;
    text-transform: uppercase;
    color: var(--gold);
    margin-bottom: 16px;
  }

  .bar-item { margin-bottom: 14px; }
  .bar-top {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    margin-bottom: 6px;
    color: var(--text);
  }

  .bar-top span:last-child { color: var(--gold-light); font-weight: 500; }
  .bar-bg {
    height: 5px;
    background: rgba(255,255,255,0.05);
    border-radius: 3px;
    overflow: hidden;
  }

  .bar-fill {
    height: 100%;
    border-radius: 3px;
    background: linear-gradient(to right, var(--gold-dim), var(--gold));
  }

  .diagnosis {
    background: linear-gradient(135deg, rgba(201,168,76,0.06), rgba(201,168,76,0.02));
    border: 1px solid var(--border);
    border-radius: 16px;
    padding: 48px 40px;
    margin-bottom: 32px;
  }

  .diagnosis h3 {
    font-family: 'Cormorant Garamond', serif;
    font-size: 26px;
    font-weight: 300;
    margin-bottom: 28px;
    color: var(--gold-light);
  }

  .diagnosis p {
    font-size: 15px;
    color: rgba(232,228,220,0.8);
    line-height: 1.9;
    margin-bottom: 16px;
  }

  .diag-points { display: flex; flex-direction: column; gap: 16px; margin-top: 32px; }
  .diag-point { display: flex; gap: 16px; align-items: flex-start; }
  .diag-icon {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    flex-shrink: 0;
    margin-top: 2px;
  }

  .diag-icon.pos { background: rgba(76,175,130,0.15); color: var(--green); border: 1px solid rgba(76,175,130,0.3); }
  .diag-icon.warn { background: rgba(255,193,7,0.12); color: var(--warn); border: 1px solid rgba(255,193,7,0.25); }
  .diag-icon.opp { background: rgba(91,155,213,0.12); color: var(--blue); border: 1px solid rgba(91,155,213,0.25); }
  .diag-text strong { font-weight: 500; color: var(--text); display: block; margin-bottom: 4px; }
  .diag-text span { font-size: 13px; color: var(--muted); line-height: 1.7; }

  .ads-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 12px;
    overflow: hidden;
  }

  .ads-table th {
    text-align: left;
    padding: 10px 16px;
    font-size: 10px;
    letter-spacing: 1.5px;
    text-transform: uppercase;
    color: var(--muted);
    border-bottom: 1px solid var(--border);
  }

  .ads-table td {
    padding: 14px 16px;
    border-bottom: 1px solid rgba(255,255,255,0.04);
    color: var(--text);
  }

  .ads-table tr:last-child td { border-bottom: none; }
  .ads-table td.num {
    font-family: 'Cormorant Garamond', serif;
    font-size: 16px;
    color: var(--gold-light);
  }

  .note-box {
    padding: 18px 22px;
    background: rgba(201,168,76,0.05);
    border: 1px solid var(--border);
    border-radius: 10px;
    font-size: 13px;
    color: var(--muted);
    line-height: 1.8;
  }

  .note-box strong { color: var(--text); font-weight: 500; }
  .footer { text-align: center; padding: 60px 32px; border-top: 1px solid var(--border); }
  .footer-logo {
    font-family: 'Cormorant Garamond', serif;
    font-size: 22px;
    font-weight: 300;
    color: var(--gold);
    letter-spacing: 2px;
    margin-bottom: 8px;
  }

  .footer-sub { font-size: 12px; color: var(--muted); letter-spacing: 1px; }
  @media (max-width: 600px) {
    .wrap { padding: 0 20px; }
    .section { padding: 48px 0; }
    .diagnosis { padding: 32px 24px; }
    .stat-highlight-row,
    .insight-row { grid-template-columns: 1fr; }
  }
  </style>
</head>
<body>
  <div class="cover">
    <div class="cover-lines"></div>
    <div class="cover-content">
      <div class="cover-ornament"></div>
      <div class="cover-label">Relatório de Desempenho Digital</div>
      <h1>${clientTitleHtml(snapshot.clientName)}</h1>
      <div class="cover-sub">${escapeHtml(monthLabel)} - ${escapeHtml(period)}</div>
      <div class="cover-divider"></div>
      <div class="cover-meta">
        <div class="cover-meta-item">
          <span class="val">${compactNumber(snapshot.summary.impressions)}</span>
          <div class="lbl">Impressões</div>
        </div>
        <div class="cover-meta-item">
          <span class="val">${compactNumber(snapshot.summary.reach)}</span>
          <div class="lbl">Pessoas alcançadas</div>
        </div>
        <div class="cover-meta-item">
          <span class="val">${integer(conversations)}</span>
          <div class="lbl">Conversas</div>
        </div>
        <div class="cover-meta-item">
          <span class="val">${currencyCompact(snapshot.summary.spend)}</span>
          <div class="lbl">Investido em anúncios</div>
        </div>
      </div>
    </div>
  </div>

  <div class="wrap">
    <div class="section">
      <div class="section-header">
        <span class="section-num">00</span>
        <h2 class="section-title">Diagnóstico <em>do Mês</em></h2>
      </div>

      <div class="diagnosis">
        <h3>${escapeHtml(monthLabel)} teve ${escapeHtml(compactNumber(snapshot.summary.reach))} pessoas alcançadas com investimento de ${escapeHtml(currency(snapshot.summary.spend))}.</h3>
        <p>Os anúncios de ${escapeHtml(snapshot.clientName)} entregaram <strong>${escapeHtml(integer(snapshot.summary.impressions))} impressões</strong> para <strong>${escapeHtml(integer(snapshot.summary.reach))} pessoas</strong>, com <strong>${escapeHtml(integer(snapshot.summary.clicks))} cliques</strong> no período.</p>
        <p>O custo médio ficou em <strong>${escapeHtml(currency(snapshot.summary.cpc))} por clique</strong> e <strong>${escapeHtml(currency(snapshot.summary.cpm))} por mil impressões</strong>. A taxa de cliques registrada foi de <strong>${escapeHtml(percent(snapshot.summary.ctr))}</strong>.</p>

        <div class="diag-points">
          <div class="diag-point">
            <div class="diag-icon pos">✓</div>
            <div class="diag-text">
              <strong>Distribuição paga ativa no período</strong>
              <span>A conta ${escapeHtml(accountName)} gerou presença de marca com frequência média de ${escapeHtml(decimal(frequency))} impressões por pessoa alcançada.</span>
            </div>
          </div>
          <div class="diag-point">
            <div class="diag-icon pos">✓</div>
            <div class="diag-text">
              <strong>${escapeHtml(integer(snapshot.actions.linkClicks))} cliques no link</strong>
              <span>Esse indicador mostra o volume de pessoas que saíram do anúncio para uma ação mais próxima de contato, visita ou conversão.</span>
            </div>
          </div>
          <div class="diag-point">
            <div class="diag-icon pos">✓</div>
            <div class="diag-text">
              <strong>${escapeHtml(integer(conversations))} conversas atribuídas aos anúncios</strong>
              <span>Conversas são uma métrica importante para operação comercial, principalmente quando o atendimento acontece via WhatsApp ou direct.</span>
            </div>
          </div>
          <div class="diag-point">
            <div class="diag-icon warn">!</div>
            <div class="diag-text">
              <strong>Ponto de atenção: validar qualidade dos contatos</strong>
              <span>O relatório mostra volume e custo. Para avaliar venda real, o próximo passo é cruzar esses dados com atendimentos, orçamentos e fechamentos do cliente.</span>
            </div>
          </div>
          <div class="diag-point">
            <div class="diag-icon opp">→</div>
            <div class="diag-text">
              <strong>Oportunidade: transformar cliques em atendimento mensurável</strong>
              <span>Use chamadas diretas para WhatsApp, ofertas com prazo e criativos focados em dor/benefício para aumentar a intenção de contato.</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <span class="section-num">01</span>
        <h2 class="section-title">Anúncios — <em>Investimento e Resultados</em></h2>
      </div>

      <div class="platform-badge">● Tráfego Pago · ${escapeHtml(accountName)}</div>

      <div class="stat-highlight-row">
        <div class="stat-hl">
          <span class="v">${currency(snapshot.summary.spend)}</span>
          <span class="l">Total investido</span>
        </div>
        <div class="stat-hl">
          <span class="v">${integer(snapshot.summary.reach)}</span>
          <span class="l">Pessoas alcançadas</span>
        </div>
        <div class="stat-hl">
          <span class="v">${currency(snapshot.summary.cpm)}</span>
          <span class="l">CPM médio</span>
          <div class="g">Custo de distribuição</div>
        </div>
      </div>

      <div class="metric-grid">
        <div class="metric-card">
          <div class="metric-label">Impressões</div>
          <div class="metric-value">${integer(snapshot.summary.impressions)}</div>
          <div class="metric-sub">Frequência média: ${decimal(frequency)}</div>
        </div>
        <div class="metric-card highlight">
          <div class="metric-label">Cliques no link</div>
          <div class="metric-value gold">${integer(snapshot.actions.linkClicks)}</div>
          <div class="metric-sub">CPC médio: ${currency(snapshot.summary.cpc)}</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">Cliques totais</div>
          <div class="metric-value">${integer(snapshot.summary.clicks)}</div>
          <div class="metric-sub">CTR: ${percent(snapshot.summary.ctr)}</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">Conversas</div>
          <div class="metric-value green">${integer(conversations)}</div>
          <div class="metric-sub">Conversas atribuídas aos anúncios</div>
        </div>
      </div>

      <div class="note-box">
        Com <strong>${currency(snapshot.summary.spend)} investidos</strong>, os anúncios entregaram <strong>${integer(snapshot.summary.impressions)} impressões</strong> para <strong>${integer(snapshot.summary.reach)} pessoas</strong>, com CPM de <strong>${currency(snapshot.summary.cpm)}</strong>. O relatório indica o desempenho geral do mês e serve como base para avaliar continuidade, ajustes de criativo e foco em conversão.
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <span class="section-num">02</span>
        <h2 class="section-title">Ações — <em>Interesse Comercial</em></h2>
      </div>

      <div class="insight-row">
        <div class="insight-card">
          <h4>Distribuição das ações</h4>
          <div class="bar-item">
            <div class="bar-top"><span>Cliques no link</span><span>${integer(snapshot.actions.linkClicks)}</span></div>
            <div class="bar-bg"><div class="bar-fill" style="width:${clickShare}%"></div></div>
          </div>
          <div class="bar-item">
            <div class="bar-top"><span>Engajamentos</span><span>${integer(snapshot.actions.postEngagement)}</span></div>
            <div class="bar-bg"><div class="bar-fill" style="width:${engagementShare}%"></div></div>
          </div>
          <div class="bar-item">
            <div class="bar-top"><span>Conversas</span><span>${integer(conversations)}</span></div>
            <div class="bar-bg"><div class="bar-fill" style="width:${conversationShare}%"></div></div>
          </div>
        </div>

        <div class="insight-card">
          <h4>Leitura dos resultados</h4>
          <table class="ads-table">
            <tbody>
              <tr><td>Cliques totais</td><td class="num">${integer(snapshot.summary.clicks)}</td></tr>
              <tr><td>Reações</td><td class="num">${integer(snapshot.actions.postReactions)}</td></tr>
              <tr><td>Visualizações de vídeo</td><td class="num">${integer(snapshot.actions.videoViews)}</td></tr>
              <tr><td>Conexões por mensagem</td><td class="num">${integer(snapshot.actions.messagingConnections)}</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <span class="section-num">03</span>
        <h2 class="section-title">Próximos Passos <em>Estratégicos</em></h2>
      </div>

      <div class="diagnosis" style="margin-bottom:0">
        <h3>Direcionamento recomendado para o próximo mês.</h3>
        <div class="diag-points" style="margin-top:0">
          <div class="diag-point">
            <div class="diag-icon opp">1</div>
            <div class="diag-text">
              <strong>Manter acompanhamento mensal de investimento, alcance e cliques</strong>
              <span>Esses indicadores mostram se a verba está sustentando distribuição suficiente e se o público está reagindo aos criativos.</span>
            </div>
          </div>
          <div class="diag-point">
            <div class="diag-icon opp">2</div>
            <div class="diag-text">
              <strong>Fortalecer chamadas para contato</strong>
              <span>Quando o objetivo comercial é atendimento, os anúncios devem conduzir claramente para WhatsApp, direct ou formulário.</span>
            </div>
          </div>
          <div class="diag-point">
            <div class="diag-icon opp">3</div>
            <div class="diag-text">
              <strong>Cruzar dados de mídia com vendas ou atendimentos</strong>
              <span>O próximo salto do relatório é comparar conversas geradas com contatos qualificados, orçamentos e fechamentos reais.</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="footer">
      <div class="cover-divider" style="margin-bottom:28px"></div>
      <div class="footer-logo">North Produções</div>
      <div class="footer-sub">Relatório elaborado em ${escapeHtml(generatedAt)} · Dados: Gerenciador de Anúncios · Meta Ads</div>
    </div>
  </div>
</body>
</html>`
}

function clientTitleHtml(name: string) {
  const parts = name.trim().split(/\s+/)
  if (parts.length < 2) return escapeHtml(name)

  const last = parts[parts.length - 1]
  const first = parts.slice(0, -1).join(' ')
  return `${escapeHtml(first)} <em>${escapeHtml(last)}</em>`
}

function compactNumber(value: number) {
  const safeValue = value ?? 0
  if (safeValue >= 1_000_000) return `${decimal(safeValue / 1_000_000)} mi`
  if (safeValue >= 1_000) return `${decimal(safeValue / 1_000)}k`
  return integer(safeValue)
}

function currencyCompact(value: number) {
  if ((value ?? 0) >= 1_000) return `R$${decimal(value / 1_000)}k`
  return currency(value).replace(',00', '')
}

function percentWidth(value: number, total: number) {
  if (!total || total <= 0) return '0'
  return String(Math.min(100, Math.max(3, (value / total) * 100)))
}

function reportMonthLabel(value: string) {
  const [year, month] = value.split('-').map(Number)
  const date = new Date(year, month - 1, 1)
  return date.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })
}

function date(value: string) {
  const [year, month, day] = value.split('-')
  return `${day}/${month}/${year}`
}

onMounted(fetchClients)
</script>

<template>
  <AppLayout topbar-placeholder="Buscar relatórios, clientes ou períodos...">
    <div class="w-full space-y-6 p-4 sm:p-6">
      <header class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 class="text-3xl font-bold tracking-tight text-gray-900">Relatórios do Cliente</h1>
          <p class="mt-1 text-sm text-gray-500">
            Gere um resumo mensal dos anúncios vinculados ao cliente.
          </p>
        </div>

        <Button
          variant="outline"
          class="gap-2"
          :disabled="loadingReport || !report"
          @click="downloadHtmlReport"
        >
          <Download class="h-4 w-4" />
          Baixar HTML
        </Button>
      </header>

      <Card class="rounded-lg p-4">
        <div class="grid gap-4 md:grid-cols-[minmax(0,1.5fr)_220px_auto] md:items-end">
          <label class="space-y-2">
            <span class="text-xs font-semibold uppercase text-gray-500">Cliente</span>
            <select
              v-model="selectedClientId"
              class="h-10 w-full rounded-md border border-gray-200 bg-white px-3 text-sm outline-none transition focus:border-purple-500 focus:ring-2 focus:ring-purple-100 disabled:bg-gray-50"
              :disabled="loadingClients"
            >
              <option value="" disabled>Selecione um cliente</option>
              <option
                v-for="client in sortedClients"
                :key="client.id"
                :value="String(client.id)"
              >
                {{ client.name }}
              </option>
            </select>
          </label>

          <label class="space-y-2">
            <span class="text-xs font-semibold uppercase text-gray-500">Mês</span>
            <input
              v-model="selectedMonth"
              type="month"
              class="h-10 w-full rounded-md border border-gray-200 bg-white px-3 text-sm outline-none transition focus:border-purple-500 focus:ring-2 focus:ring-purple-100"
            >
          </label>

          <Button class="gap-2" :disabled="loadingClients || loadingReport" @click="generateReport">
            <RefreshCw v-if="loadingReport" class="h-4 w-4 animate-spin" />
            <BarChart3 v-else class="h-4 w-4" />
            {{ loadingReport ? 'Gerando...' : 'Gerar relatório' }}
          </Button>
        </div>
      </Card>

      <div v-if="error" class="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        {{ error }}
      </div>

      <section v-if="report" class="space-y-6">
        <Card class="rounded-lg p-5">
          <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <p class="text-xs font-semibold uppercase text-purple-600">Relatório gerado</p>
              <h2 class="mt-1 text-2xl font-bold text-gray-900">{{ report.clientName }}</h2>
              <p class="mt-1 text-sm text-gray-500">
                {{ selectedPeriod.label }} · {{ report.accountName || 'Meta Ads' }}
              </p>
            </div>
            <div class="rounded-lg border border-gray-200 px-4 py-3 text-sm text-gray-600">
              {{ date(report.dateStart) }} até {{ date(report.dateStop) }}
            </div>
          </div>
        </Card>

        <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <Card
            v-for="item in summaryCards"
            :key="item.label"
            class="rounded-lg p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="text-xs font-semibold uppercase text-gray-500">{{ item.label }}</p>
                <p class="mt-2 text-2xl font-bold text-gray-900">{{ item.value }}</p>
              </div>
              <span :class="['flex h-10 w-10 items-center justify-center rounded-md', item.tone]">
                <component :is="item.icon" class="h-5 w-5" />
              </span>
            </div>
          </Card>
        </div>

        <div class="grid gap-6 xl:grid-cols-[minmax(0,1.1fr)_360px]">
          <Card class="rounded-lg p-5">
            <div class="mb-4">
              <h3 class="text-lg font-semibold text-gray-900">Ações geradas</h3>
              <p class="text-sm text-gray-500">
                Métricas filtradas para leitura operacional do relatório.
              </p>
            </div>

            <div class="divide-y divide-gray-100">
              <div
                v-for="item in actionRows"
                :key="item.label"
                class="flex items-center justify-between gap-4 py-3"
              >
                <div class="flex min-w-0 items-center gap-3">
                  <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-gray-100 text-gray-600">
                    <component :is="item.icon" class="h-4 w-4" />
                  </span>
                  <div class="min-w-0">
                    <p class="font-medium text-gray-900">{{ item.label }}</p>
                    <p class="text-sm text-gray-500">{{ item.description }}</p>
                  </div>
                </div>
                <p class="shrink-0 text-lg font-bold text-gray-900">{{ item.value }}</p>
              </div>
            </div>
          </Card>

          <Card class="rounded-lg p-5">
            <h3 class="text-lg font-semibold text-gray-900">Leitura rápida</h3>
            <dl class="mt-4 space-y-4">
              <div class="flex items-center justify-between gap-4">
                <dt class="text-sm text-gray-500">Custo por clique</dt>
                <dd class="font-semibold text-gray-900">{{ currency(report.summary.cpc) }}</dd>
              </div>
              <div class="flex items-center justify-between gap-4">
                <dt class="text-sm text-gray-500">Custo por mil impressões</dt>
                <dd class="font-semibold text-gray-900">{{ currency(report.summary.cpm) }}</dd>
              </div>
              <div class="flex items-center justify-between gap-4">
                <dt class="text-sm text-gray-500">Taxa de cliques</dt>
                <dd class="font-semibold text-gray-900">{{ percent(report.summary.ctr) }}</dd>
              </div>
              <div class="flex items-center justify-between gap-4">
                <dt class="text-sm text-gray-500">Frequência média</dt>
                <dd class="font-semibold text-gray-900">
                  {{ decimal(report.summary.impressions / Math.max(report.summary.reach, 1)) }}
                </dd>
              </div>
            </dl>
          </Card>
        </div>
      </section>

      <Card v-else class="rounded-lg p-10 text-center">
        <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-md bg-purple-50 text-purple-700">
          <BarChart3 class="h-6 w-6" />
        </div>
        <h2 class="mt-4 text-lg font-semibold text-gray-900">Nenhum relatório gerado</h2>
        <p class="mx-auto mt-2 max-w-md text-sm text-gray-500">
          Escolha um cliente e um mês para consultar os dados da conta de anúncios vinculada.
        </p>
        <p v-if="loadingClients" class="mt-4 text-sm text-gray-400">Carregando clientes...</p>
      </Card>
    </div>
  </AppLayout>
</template>
