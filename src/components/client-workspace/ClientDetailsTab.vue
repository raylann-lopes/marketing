<script setup lang="ts">
import { Save } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'

interface ClientSnapshot {
  name: string
  niche: string
  email: string
  voiceTone: string
}

interface ClientWorkspaceDetails {
  mainGoal: string
  targetAudience: string
  preferredChannels: string
  contentPillars: string
  approvalSla: string
  seasonality: string
  reminders: string
}

interface Props {
  client: ClientSnapshot
  detailsDraft: ClientWorkspaceDetails
  isSavingDetails: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'saveDetails'): void
  (e: 'update:detailsDraft', val: ClientWorkspaceDetails): void
}>()

function updateDraft(key: keyof ClientWorkspaceDetails, value: string) {
  emit('update:detailsDraft', { ...props.detailsDraft, [key]: value })
}
</script>

<template>
  <section class="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
    <div class="mb-5 flex items-start justify-between gap-4">
      <div>
        <h2 class="text-xl font-bold text-gray-900">Dados Essenciais do Cliente</h2>
        <p class="text-sm text-gray-500">Somente contexto necessário para planejar posts e melhorar recomendações.</p>
      </div>
      <Button class="gap-2" :disabled="isSavingDetails" @click="$emit('saveDetails')">
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
        <textarea
          :value="detailsDraft.mainGoal"
          @input="updateDraft('mainGoal', ($event.target as HTMLTextAreaElement).value)"
          rows="3"
          class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
          placeholder="Ex: gerar mais leads qualificados com conteúdo semanal."
        />
      </div>

      <div class="space-y-1.5">
        <label class="text-xs font-semibold uppercase text-gray-500">Público-alvo</label>
        <input
          :value="detailsDraft.targetAudience"
          @input="updateDraft('targetAudience', ($event.target as HTMLInputElement).value)"
          type="text"
          class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
          placeholder="Ex: donos de negócio local 30-50 anos"
        />
      </div>

      <div class="space-y-1.5">
        <label class="text-xs font-semibold uppercase text-gray-500">Canais Prioritários</label>
        <input
          :value="detailsDraft.preferredChannels"
          @input="updateDraft('preferredChannels', ($event.target as HTMLInputElement).value)"
          type="text"
          class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
          placeholder="Ex: Instagram, Facebook"
        />
      </div>

      <div class="space-y-1.5 md:col-span-2">
        <label class="text-xs font-semibold uppercase text-gray-500">Pilares de Conteúdo</label>
        <input
          :value="detailsDraft.contentPillars"
          @input="updateDraft('contentPillars', ($event.target as HTMLInputElement).value)"
          type="text"
          class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
          placeholder="Ex: prova social, bastidores, oferta"
        />
      </div>

      <div class="space-y-1.5">
        <label class="text-xs font-semibold uppercase text-gray-500">SLA de Aprovação</label>
        <input
          :value="detailsDraft.approvalSla"
          @input="updateDraft('approvalSla', ($event.target as HTMLInputElement).value)"
          type="text"
          class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
          placeholder="Ex: 24h úteis"
        />
      </div>

      <div class="space-y-1.5">
        <label class="text-xs font-semibold uppercase text-gray-500">Sazonalidade</label>
        <input
          :value="detailsDraft.seasonality"
          @input="updateDraft('seasonality', ($event.target as HTMLInputElement).value)"
          type="text"
          class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
          placeholder="Ex: campanhas fortes em novembro e dezembro"
        />
      </div>

      <div class="space-y-1.5 md:col-span-2">
        <label class="text-xs font-semibold uppercase text-gray-500">Lembretes da Equipe</label>
        <textarea
          :value="detailsDraft.reminders"
          @input="updateDraft('reminders', ($event.target as HTMLTextAreaElement).value)"
          rows="3"
          class="w-full rounded-lg border border-gray-200 bg-gray-50 p-2.5 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
          placeholder="Ex: sempre validar CTA com comercial antes de enviar."
        />
      </div>
    </div>
  </section>
</template>
