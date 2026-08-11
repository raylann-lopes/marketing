<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { CheckCircle, X } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Card from '@/components/ui/Card.vue'
import Button from '@/components/ui/Button.vue'
import { userService } from '@/services/userService'
import { setCurrentUserId } from '@/lib/api'
import { clientService, type Client } from '@/services/clientService'
import { accountConfigService, type AccountConfig, type MetaInstagramAccount } from '@/services/accountConfigService'
import { evolutionGroupService, type EvolutionGroup } from '@/services/evolutionGroupService'
import { metaAdsService, type MetaAdsAccount } from '@/services/metaAdsService'

import ProfileForm from '@/components/settings/ProfileForm.vue'
import PasswordForm from '@/components/settings/PasswordForm.vue'
import ClientIntegrations from '@/components/settings/ClientIntegrations.vue'

const loading = ref(true)
const savingProfile = ref(false)
const savingPassword = ref(false)
const profileSuccess = ref('')
const passwordSuccess = ref('')
const accountSuccess = ref('')
const error = ref('')
const isAdmin = (localStorage.getItem('role') || sessionStorage.getItem('role')) === 'ADMIN'

const profile = ref({
  name: '',
  email: '',
  role: ''
})

const password = ref({
  current: '',
  new: '',
  confirm: ''
})

const clients = ref<Client[]>([])
const selectedClientId = ref('')
const clientConfigs = ref<Map<number, AccountConfig>>(new Map())
const metaAccounts = ref<MetaInstagramAccount[]>([])
const selectedMetaAccountKey = ref('')
const loadingMetaAccounts = ref(false)
const evolutionGroups = ref<EvolutionGroup[]>([])
const selectedEvolutionGroupId = ref('')
const loadingEvolutionGroups = ref(false)
const metaAdsAccounts = ref<MetaAdsAccount[]>([])
const selectedMetaAdsAccountId = ref('')
const loadingMetaAdsAccounts = ref(false)
const savingMetaAdsAccount = ref(false)
const savingIntegrations = ref(false)

const selectedClientConfig = computed(() => {
  if (!selectedClientId.value) return null
  return clientConfigs.value.get(Number(selectedClientId.value)) ?? null
})

function metaAccountKey(account: MetaInstagramAccount) {
  return `${account.pageId}:${account.igUserId}`
}

const selectedMetaAccount = computed(() => {
  if (!selectedMetaAccountKey.value) return null
  return metaAccounts.value.find(account => metaAccountKey(account) === selectedMetaAccountKey.value) ?? null
})

const selectedGroupClient = computed(() => {
  if (!selectedClientId.value) return null
  return clients.value.find(client => String(client.id) === String(selectedClientId.value)) ?? null
})

const selectedClientHasWhatsapp = computed(() => Boolean(selectedGroupClient.value?.whatsappGroupId))

const selectedEvolutionGroup = computed(() => {
  if (!selectedEvolutionGroupId.value) return null
  return evolutionGroups.value.find(group => group.groupId === selectedEvolutionGroupId.value) ?? null
})

const selectedMetaAdsAccount = computed(() => {
  if (!selectedMetaAdsAccountId.value) return null
  return metaAdsAccounts.value.find(account => account.accountId === selectedMetaAdsAccountId.value) ?? null
})

const selectedClientConnectionSummary = computed(() => {
  if (!selectedClientId.value) return ''

  const connections: string[] = []
  const whatsappGroupName = selectedGroupClient.value?.whatsappGroupName

  if (selectedClientConfig.value) {
    connections.push('Instagram configurado')
  }
  if (whatsappGroupName) {
    connections.push(`WhatsApp: ${whatsappGroupName}`)
  }
  if (selectedClientConfig.value?.metaAdAccountId) {
    connections.push('Meta Ads configurado')
  }

  return connections.length > 0 ? connections.join(' | ') : 'Nenhuma conexão cadastrada para este cliente.'
})

function isGroupLinkedToAnotherClient(group: EvolutionGroup) {
  if (!group.alreadyLinked || !group.linkedClientId) return false
  return String(group.linkedClientId) !== String(selectedClientId.value)
}

async function fetchClients() {
  try {
    const data = await clientService.getAll()
    clients.value = Array.isArray(data) ? data : (((data as { data: Client[] }).data) || [])
  } catch {
  }
}

async function fetchAccountConfig(clientId: number) {
  if (clientConfigs.value.has(clientId)) return

  try {
    const config = await accountConfigService.getByClientId(clientId)
    clientConfigs.value.set(clientId, config)
  } catch {
  }
}

async function fetchMetaAccounts() {
  accountSuccess.value = ''
  error.value = ''
  loadingMetaAccounts.value = true

  try {
    metaAccounts.value = await accountConfigService.getMetaInstagramAccounts()
    if (metaAccounts.value.length === 0) {
      accountSuccess.value = 'Nenhuma conta Instagram profissional foi retornada pela Meta.'
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao buscar contas da Meta'
  } finally {
    loadingMetaAccounts.value = false
  }
}

async function fetchEvolutionGroups() {
  accountSuccess.value = ''
  error.value = ''
  loadingEvolutionGroups.value = true

  try {
    evolutionGroups.value = await evolutionGroupService.getGroups()
    if (evolutionGroups.value.length === 0) {
      accountSuccess.value = 'Nenhum grupo do WhatsApp foi retornado pela Evolution.'
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao buscar grupos da Evolution'
  } finally {
    loadingEvolutionGroups.value = false
  }
}

async function fetchMetaAdsAccounts() {
  accountSuccess.value = ''
  error.value = ''
  loadingMetaAdsAccounts.value = true

  try {
    metaAdsAccounts.value = await metaAdsService.getAdAccounts()
    if (metaAdsAccounts.value.length === 0) {
      accountSuccess.value = 'Nenhuma conta de anúncios foi retornada pela Meta.'
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao buscar contas de anúncios da Meta'
  } finally {
    loadingMetaAdsAccounts.value = false
  }
}

async function handleLinkClientIntegrations() {
  accountSuccess.value = ''
  error.value = ''

  if (!selectedClientId.value) {
    error.value = 'Selecione um cliente.'
    return
  }
  if (!selectedClientConfig.value && !selectedMetaAccount.value) {
    error.value = 'Selecione uma conta retornada pela Meta.'
    return
  }
  if (selectedClientHasWhatsapp.value) {
    error.value = 'Este cliente já possui WhatsApp vinculado.'
    return
  }
  if (!selectedClientConfig.value && selectedMetaAccount.value?.alreadyLinked) {
    error.value = 'Esta conta Instagram já está vinculada a outro cliente.'
    return
  }
  if (!selectedEvolutionGroup.value) {
    error.value = 'Selecione o grupo do WhatsApp.'
    return
  }
  if (isGroupLinkedToAnotherClient(selectedEvolutionGroup.value)) {
    error.value = 'Este grupo já está vinculado a outro cliente.'
    return
  }

  const groupToLink = selectedEvolutionGroup.value
  const accountToLink = selectedMetaAccount.value
  const clientId = Number(selectedClientId.value)
  const selectedClient = clients.value.find(client => String(client.id) === selectedClientId.value)
  const hadInstagramLinked = Boolean(selectedClientConfig.value)

  savingIntegrations.value = true
  try {
    if (!selectedClientConfig.value && accountToLink) {
      const config = await accountConfigService.linkMetaInstagramAccount({
        clientId,
        pageId: accountToLink.pageId,
        igUserId: accountToLink.igUserId,
      })
      clientConfigs.value = new Map(clientConfigs.value).set(config.clientId, config)

      metaAccounts.value = metaAccounts.value.map(account => {
        if (account.pageId !== accountToLink.pageId || account.igUserId !== accountToLink.igUserId) {
          return account
        }
        return {
          ...account,
          alreadyLinked: true,
          linkedClientId: config.clientId,
          linkedClientName: selectedClient?.name ?? account.linkedClientName,
        }
      })
    }

    const linkedGroup = await evolutionGroupService.linkGroupToClient({
      clientId,
      groupId: groupToLink.groupId,
    })

    clients.value = clients.value.map(client => {
      if (String(client.id) !== String(clientId)) return client
      return {
        ...client,
        whatsappGroupId: linkedGroup.groupId,
        whatsappGroupName: linkedGroup.groupName,
      }
    })
    evolutionGroups.value = evolutionGroups.value.map(group => {
      if (group.groupId === linkedGroup.groupId) {
        return linkedGroup
      }
      if (String(group.linkedClientId) === String(clientId)) {
        return {
          ...group,
          alreadyLinked: false,
          linkedClientId: null,
          linkedClientName: null,
        }
      }
      return group
    })

    accountSuccess.value = hadInstagramLinked
      ? `WhatsApp vinculado ao cliente ${selectedClient?.name ?? clientId}.`
      : `Instagram e WhatsApp vinculados ao cliente ${selectedClient?.name ?? clientId}.`
    selectedMetaAccountKey.value = ''
    selectedEvolutionGroupId.value = ''
    setTimeout(() => { accountSuccess.value = '' }, 5000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao vincular integrações do cliente'
  } finally {
    savingIntegrations.value = false
  }
}

async function handleLinkMetaAdsAccount() {
  accountSuccess.value = ''
  error.value = ''

  if (!selectedClientId.value) {
    error.value = 'Selecione um cliente.'
    return
  }
  if (!selectedClientConfig.value) {
    error.value = 'Configure o Instagram do cliente antes de vincular Meta Ads.'
    return
  }
  if (selectedClientConfig.value.metaAdAccountId) {
    error.value = 'Este cliente já possui Meta Ads vinculado.'
    return
  }
  if (!selectedMetaAdsAccount.value) {
    error.value = 'Selecione uma conta de anúncios.'
    return
  }

  const clientId = Number(selectedClientId.value)
  const selectedClient = clients.value.find(client => String(client.id) === selectedClientId.value)

  savingMetaAdsAccount.value = true
  try {
    const config = await accountConfigService.linkMetaAdsAccount(clientId, {
      metaAdAccountId: selectedMetaAdsAccount.value.accountId,
    })
    clientConfigs.value = new Map(clientConfigs.value).set(config.clientId, config)
    selectedMetaAdsAccountId.value = ''
    accountSuccess.value = `Meta Ads vinculado ao cliente ${selectedClient?.name ?? clientId}.`
    setTimeout(() => { accountSuccess.value = '' }, 5000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao vincular Meta Ads'
  } finally {
    savingMetaAdsAccount.value = false
  }
}

async function fetchProfile() {
  loading.value = true
  try {
    const data = await userService.getMe()
    profile.value = { name: data.name, email: data.email, role: data.role }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao carregar perfil'
  } finally {
    loading.value = false
  }
}

async function handleSaveProfile() {
  profileSuccess.value = ''
  error.value = ''
  savingProfile.value = true
  try {
    const updated = await userService.updateProfile({
      name: profile.value.name,
      email: profile.value.email
    })
    profile.value.role = updated.role
    profileSuccess.value = 'Perfil atualizado com sucesso!'
    setTimeout(() => { profileSuccess.value = '' }, 3000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao salvar perfil'
  } finally {
    savingProfile.value = false
  }
}

async function handleChangePassword() {
  passwordSuccess.value = ''
  error.value = ''

  if (password.value.new !== password.value.confirm) {
    error.value = 'As senhas não coincidem.'
    return
  }
  if (password.value.new.length < 6) {
    error.value = 'A nova senha deve ter pelo menos 6 caracteres.'
    return
  }

  savingPassword.value = true
  try {
    await userService.changePassword({
      currentPassword: password.value.current,
      newPassword: password.value.new
    })
    passwordSuccess.value = 'Senha alterada com sucesso!'
    password.value = { current: '', new: '', confirm: '' }
    setTimeout(() => { passwordSuccess.value = '' }, 3000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao alterar senha'
  } finally {
    savingPassword.value = false
  }
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('userId')
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('userId')
  setCurrentUserId(null)
  window.location.href = '/login'
}

onMounted(async () => {
  await fetchProfile()
  if (isAdmin) {
    await fetchClients()
  }
})

watch(selectedClientId, (clientId) => {
  accountSuccess.value = ''
  error.value = ''
  selectedMetaAccountKey.value = ''
  selectedEvolutionGroupId.value = ''
  selectedMetaAdsAccountId.value = ''

  if (clientId) {
    fetchAccountConfig(Number(clientId))
  }
})

watch(selectedMetaAccountKey, () => {
  accountSuccess.value = ''
  error.value = ''
})

watch(selectedEvolutionGroupId, () => {
  accountSuccess.value = ''
  error.value = ''
})

watch(selectedMetaAdsAccountId, () => {
  accountSuccess.value = ''
  error.value = ''
})
</script>

<template>
  <AppLayout>
    <div class="mb-6">
      <h1 class="text-3xl font-bold text-gray-900">Configurações</h1>
      <p class="text-gray-500 mt-1">Gerencie as preferências da sua conta e do ateliê.</p>
    </div>

    <div v-if="loading" class="flex items-center justify-center h-64">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
    </div>

    <div v-else class="space-y-6">
      <!-- Mensagens -->
      <div v-if="error" class="flex items-center gap-2 bg-red-50 text-red-600 rounded-xl px-4 py-3 border border-red-100">
        <X class="w-4 h-4 shrink-0" />
        <span class="text-sm">{{ error }}</span>
      </div>
      <div v-if="profileSuccess" class="flex items-center gap-2 bg-green-50 text-green-700 rounded-xl px-4 py-3 border border-green-100">
        <CheckCircle class="w-4 h-4 shrink-0" />
        <span class="text-sm">{{ profileSuccess }}</span>
      </div>
      <div v-if="passwordSuccess" class="flex items-center gap-2 bg-green-50 text-green-700 rounded-xl px-4 py-3 border border-green-100">
        <CheckCircle class="w-4 h-4 shrink-0" />
        <span class="text-sm">{{ passwordSuccess }}</span>
      </div>
      <div v-if="accountSuccess" class="flex items-center gap-2 bg-green-50 text-green-700 rounded-xl px-4 py-3 border border-green-100">
        <CheckCircle class="w-4 h-4 shrink-0" />
        <span class="text-sm">{{ accountSuccess }}</span>
      </div>

      <!-- Colunas: esquerda + right sidebar -->
      <div class="flex flex-col xl:flex-row gap-6 items-start">
        <div class="w-full xl:flex-1 min-w-0 space-y-6">
          <ProfileForm
            v-model:profile="profile"
            :saving-profile="savingProfile"
            @save="handleSaveProfile"
          />

          <PasswordForm
            v-model:password-form="password"
            :saving-password="savingPassword"
            @save="handleChangePassword"
          />

          <Card class="p-6">
            <h2 class="text-xl font-semibold mb-4 text-gray-900">Sessão</h2>
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm font-medium text-gray-700">Encerrar Sessão</p>
                <p class="text-xs text-gray-400 mt-0.5">Faz logout e limpa todos os dados locais.</p>
              </div>
              <Button variant="outline" class="border-red-200 text-red-500 hover:bg-red-50" @click="handleLogout">
                Sair
              </Button>
            </div>
          </Card>
        </div>

        <ClientIntegrations
          v-if="isAdmin"
          :clients="clients"
          :meta-accounts="metaAccounts"
          :evolution-groups="evolutionGroups"
          :meta-ads-accounts="metaAdsAccounts"
          v-model:selected-client-id="selectedClientId"
          v-model:selected-meta-account-key="selectedMetaAccountKey"
          v-model:selected-evolution-group-id="selectedEvolutionGroupId"
          v-model:selected-meta-ads-account-id="selectedMetaAdsAccountId"
          :loading-meta-accounts="loadingMetaAccounts"
          :loading-evolution-groups="loadingEvolutionGroups"
          :loading-meta-ads-accounts="loadingMetaAdsAccounts"
          :saving-integrations="savingIntegrations"
          :saving-meta-ads-account="savingMetaAdsAccount"
          :selected-client-config="selectedClientConfig"
          :selected-client-has-whatsapp="selectedClientHasWhatsapp"
          :selected-client-connection-summary="selectedClientConnectionSummary"
          :selected-meta-account="selectedMetaAccount"
          :selected-evolution-group="selectedEvolutionGroup"
          :selected-meta-ads-account="selectedMetaAdsAccount"
          @fetch-meta-accounts="fetchMetaAccounts"
          @fetch-evolution-groups="fetchEvolutionGroups"
          @fetch-meta-ads-accounts="fetchMetaAdsAccounts"
          @link-integrations="handleLinkClientIntegrations"
          @link-meta-ads-account="handleLinkMetaAdsAccount"
        />
      </div>
    </div>
  </AppLayout>
</template>
