<script setup lang="ts">
import { Link2, RefreshCw, Instagram, MessageCircle } from 'lucide-vue-next'
import Card from '@/components/ui/Card.vue'
import Button from '@/components/ui/Button.vue'
import { type Client } from '@/services/clientService'
import { type MetaInstagramAccount, type AccountConfig } from '@/services/accountConfigService'
import { type EvolutionGroup } from '@/services/evolutionGroupService'

interface Props {
  clients: Client[]
  metaAccounts: MetaInstagramAccount[]
  evolutionGroups: EvolutionGroup[]
  selectedClientId: string
  selectedMetaAccountKey: string
  selectedEvolutionGroupId: string
  loadingMetaAccounts: boolean
  loadingEvolutionGroups: boolean
  savingIntegrations: boolean
  selectedClientConfig: AccountConfig | null
  selectedClientConnectionSummary: string
  selectedMetaAccount: MetaInstagramAccount | null
  selectedEvolutionGroup: EvolutionGroup | null
}

defineProps<Props>()
const emit = defineEmits<{
  (e: 'fetchMetaAccounts'): void
  (e: 'fetchEvolutionGroups'): void
  (e: 'linkIntegrations'): void
  (e: 'update:selectedClientId', val: string): void
  (e: 'update:selectedMetaAccountKey', val: string): void
  (e: 'update:selectedEvolutionGroupId', val: string): void
}>()

function metaAccountKey(account: MetaInstagramAccount) {
  return `${account.pageId}:${account.igUserId}`
}

function metaAccountTitle(account: MetaInstagramAccount) {
  if (account.igUsername) return `@${account.igUsername}`
  return account.igName || 'Conta Instagram'
}

function metaAccountOptionLabel(account: MetaInstagramAccount) {
  const title = metaAccountTitle(account)
  const status = account.alreadyLinked && account.linkedClientName ? ` - vinculado a ${account.linkedClientName}` : ''
  return `${title} - ${account.pageName}${status}`
}

function isGroupLinkedToAnotherClient(group: EvolutionGroup, clientId: string) {
  if (!group.alreadyLinked || !group.linkedClientId) return false
  return String(group.linkedClientId) !== String(clientId)
}

function evolutionGroupOptionLabel(group: EvolutionGroup) {
  const participants = typeof group.participantsCount === 'number' ? ` - ${group.participantsCount} participantes` : ''
  const status = group.alreadyLinked && group.linkedClientName ? ` - vinculado a ${group.linkedClientName}` : ''
  return `${group.groupName}${participants}${status}`
}
</script>

<template>
  <Card class="w-full xl:flex-1 min-w-0 p-6">
    <div class="flex flex-col gap-3 mb-6 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <h2 class="text-xl font-semibold text-gray-900 flex items-center gap-2">
          <Link2 class="w-5 h-5 text-gray-500" />
          Integrações do cliente
        </h2>
        <p class="text-sm text-gray-500 mt-1">Configure Instagram e grupo de envio no mesmo cliente.</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <Button variant="outline" size="sm" :disabled="loadingMetaAccounts" @click="$emit('fetchMetaAccounts')">
          <RefreshCw :class="['w-4 h-4', loadingMetaAccounts ? 'animate-spin' : '']" />
          {{ loadingMetaAccounts ? 'Buscando...' : 'Buscar Meta' }}
        </Button>
        <Button variant="outline" size="sm" :disabled="loadingEvolutionGroups" @click="$emit('fetchEvolutionGroups')">
          <RefreshCw :class="['w-4 h-4', loadingEvolutionGroups ? 'animate-spin' : '']" />
          {{ loadingEvolutionGroups ? 'Buscando...' : 'Buscar grupos' }}
        </Button>
      </div>
    </div>

    <div class="space-y-4">
      <div class="space-y-1.5">
        <label class="text-xs font-semibold text-gray-500 uppercase">Cliente</label>
        <select
          :value="selectedClientId"
          @change="$emit('update:selectedClientId', ($event.target as HTMLSelectElement).value)"
          required
          class="w-full p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary appearance-none cursor-pointer"
        >
          <option value="">Selecione o cliente</option>
          <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>

      <div class="pt-2 space-y-4">
        <div class="flex items-center gap-2">
          <Instagram class="w-4 h-4 text-pink-500" />
          <h3 class="text-sm font-semibold text-gray-800">Instagram</h3>
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Conta encontrada na Meta</label>
          <select
            :value="selectedMetaAccountKey"
            @change="$emit('update:selectedMetaAccountKey', ($event.target as HTMLSelectElement).value)"
            required
            class="w-full p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary appearance-none cursor-pointer disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="loadingMetaAccounts"
          >
            <option value="">Selecione uma conta da Meta</option>
            <option
              v-for="account in metaAccounts"
              :key="metaAccountKey(account)"
              :value="metaAccountKey(account)"
              :disabled="account.alreadyLinked"
            >
              {{ metaAccountOptionLabel(account) }}
            </option>
          </select>
          <p v-if="metaAccounts.length === 0" class="text-xs text-gray-400">
            Clique em buscar para carregar as contas disponíveis.
          </p>
        </div>

        <div v-if="selectedMetaAccount" class="rounded-lg border border-pink-100 bg-pink-50 px-3 py-2 text-sm text-pink-800">
          {{ metaAccountTitle(selectedMetaAccount) }} será vinculada usando a página {{ selectedMetaAccount.pageName }}.
        </div>
      </div>

      <div class="pt-5 space-y-4 border-t border-gray-100">
        <div class="flex items-center gap-2">
          <MessageCircle class="w-4 h-4 text-emerald-500" />
          <h3 class="text-sm font-semibold text-gray-800">WhatsApp</h3>
        </div>

        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 uppercase">Grupo do WhatsApp</label>
          <select
            :value="selectedEvolutionGroupId"
            @change="$emit('update:selectedEvolutionGroupId', ($event.target as HTMLSelectElement).value)"
            required
            class="w-full p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary appearance-none cursor-pointer disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="loadingEvolutionGroups"
          >
            <option value="">Selecione um grupo</option>
            <option
              v-for="group in evolutionGroups"
              :key="group.groupId"
              :value="group.groupId"
              :disabled="isGroupLinkedToAnotherClient(group, selectedClientId)"
            >
              {{ evolutionGroupOptionLabel(group) }}
            </option>
          </select>
          <p v-if="evolutionGroups.length === 0" class="text-xs text-gray-400">
            Clique em buscar para carregar os grupos disponíveis.
          </p>
        </div>
      </div>

      <p v-if="selectedClientId" class="text-xs text-gray-500">
        <span class="font-semibold text-gray-600">Conexões já realizadas:</span>
        {{ selectedClientConnectionSummary }}
      </p>

      <Button
        class="w-full"
        :disabled="savingIntegrations || loadingMetaAccounts || loadingEvolutionGroups || !selectedClientId || !selectedMetaAccount || !selectedEvolutionGroup || !!selectedClientConfig || selectedMetaAccount?.alreadyLinked || (selectedEvolutionGroup ? isGroupLinkedToAnotherClient(selectedEvolutionGroup, selectedClientId) : false)"
        @click="$emit('linkIntegrations')"
      >
        <Link2 class="w-4 h-4" />
        {{ savingIntegrations ? 'Vinculando...' : 'Vincular Instagram e WhatsApp' }}
      </Button>
    </div>
  </Card>
</template>
