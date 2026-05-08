<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { CheckCircle, Shield, X, User, Instagram, RefreshCw, Link2, MessageCircle } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Card from '@/components/ui/Card.vue'
import Input from '@/components/ui/Input.vue'
import Button from '@/components/ui/Button.vue'
import { userService } from '@/services/userService'
import { setCurrentUserId } from '@/lib/api'
import { clientService, type Client } from '@/services/clientService'
import { accountConfigService, type AccountConfig, type MetaInstagramAccount } from '@/services/accountConfigService'
import { evolutionGroupService, type EvolutionGroup } from '@/services/evolutionGroupService'

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

// Client integrations
const clients = ref<Client[]>([])
const selectedClientId = ref('')
const clientConfigs = ref<Map<number, AccountConfig>>(new Map())
const metaAccounts = ref<MetaInstagramAccount[]>([])
const selectedMetaAccountKey = ref('')
const loadingMetaAccounts = ref(false)
const evolutionGroups = ref<EvolutionGroup[]>([])
const selectedEvolutionGroupId = ref('')
const loadingEvolutionGroups = ref(false)
const savingIntegrations = ref(false)
const selectedClientConfig = computed(() => {
  if (!selectedClientId.value) return null
  return clientConfigs.value.get(Number(selectedClientId.value)) ?? null
})
const selectedMetaAccount = computed(() => {
  if (!selectedMetaAccountKey.value) return null
  return metaAccounts.value.find(account => metaAccountKey(account) === selectedMetaAccountKey.value) ?? null
})
const selectedGroupClient = computed(() => {
  if (!selectedClientId.value) return null
  return clients.value.find(client => String(client.id) === String(selectedClientId.value)) ?? null
})
const selectedEvolutionGroup = computed(() => {
  if (!selectedEvolutionGroupId.value) return null
  return evolutionGroups.value.find(group => group.groupId === selectedEvolutionGroupId.value) ?? null
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

  return connections.length > 0 ? connections.join(' | ') : 'Nenhuma conexão cadastrada para este cliente.'
})

async function fetchClients() {
  try {
    const data = await clientService.getAll()
    clients.value = Array.isArray(data) ? data : (((data as { data: Client[] }).data) || [])
  } catch {
    // Silently handled — user sees empty list
  }
}

async function fetchAccountConfigs() {
  for (const client of clients.value) {
    try {
      const config = await accountConfigService.getByClientId(Number(client.id))
      clientConfigs.value.set(Number(client.id), config)
    } catch {
      // No config for this client yet
    }
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

async function handleLinkClientIntegrations() {
  accountSuccess.value = ''
  error.value = ''

  if (!selectedClientId.value) {
    error.value = 'Selecione um cliente.'
    return
  }
  if (!selectedMetaAccount.value) {
    error.value = 'Selecione uma conta retornada pela Meta.'
    return
  }
  if (selectedClientConfig.value) {
    error.value = 'Este cliente já possui configuração cadastrada. O backend atual não permite edição por esta tela.'
    return
  }
  if (selectedMetaAccount.value.alreadyLinked) {
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

  const accountToLink = selectedMetaAccount.value
  const groupToLink = selectedEvolutionGroup.value
  const clientId = Number(selectedClientId.value)
  const selectedClient = clients.value.find(client => String(client.id) === selectedClientId.value)

  savingIntegrations.value = true
  try {
    const config = await accountConfigService.linkMetaInstagramAccount({
      clientId,
      pageId: accountToLink.pageId,
      igUserId: accountToLink.igUserId,
    })
    const linkedGroup = await evolutionGroupService.linkGroupToClient({
      clientId,
      groupId: groupToLink.groupId,
    })

    clientConfigs.value.set(config.clientId, config)
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

    accountSuccess.value = `Instagram e WhatsApp vinculados ao cliente ${selectedClient?.name ?? clientId}.`
    selectedMetaAccountKey.value = ''
    selectedEvolutionGroupId.value = ''
    setTimeout(() => { accountSuccess.value = '' }, 5000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao vincular integrações do cliente'
  } finally {
    savingIntegrations.value = false
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

function isGroupLinkedToAnotherClient(group: EvolutionGroup) {
  if (!group.alreadyLinked || !group.linkedClientId) return false
  return String(group.linkedClientId) !== String(selectedClientId.value)
}

function evolutionGroupOptionLabel(group: EvolutionGroup) {
  const participants = typeof group.participantsCount === 'number' ? ` - ${group.participantsCount} participantes` : ''
  const status = group.alreadyLinked && group.linkedClientName ? ` - vinculado a ${group.linkedClientName}` : ''
  return `${group.groupName}${participants}${status}`
}

onMounted(async () => {
  await fetchProfile()
  if (isAdmin) {
    await fetchClients()
    await fetchAccountConfigs()
  }
})

watch(selectedClientId, (clientId) => {
  accountSuccess.value = ''
  error.value = ''
  selectedMetaAccountKey.value = ''
  selectedEvolutionGroupId.value = ''

  if (!clientId) return
})

watch(selectedMetaAccountKey, () => {
  accountSuccess.value = ''
  error.value = ''

  if (!selectedMetaAccount.value) {
    return
  }
})

watch(selectedEvolutionGroupId, () => {
  accountSuccess.value = ''
  error.value = ''
})

const roleLabel: Record<string, string> = {
  ADMIN: 'Administrador',
  USER: 'Colaborador'
}
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
        <!-- Perfil -->
        <Card class="p-6">
          <h2 class="text-xl font-semibold mb-4 text-gray-900 flex items-center gap-2">
            <User class="w-5 h-5 text-gray-400" />
            Meu Perfil
          </h2>

          <div class="space-y-4">
            <div class="bg-gray-50 rounded-lg p-4 space-y-3">
              <p class="text-xs font-semibold text-gray-400 uppercase">Informações da Conta</p>
              <div class="flex items-center justify-between text-sm">
                <span class="text-gray-500">Função</span>
                <span class="font-semibold text-gray-800">{{ roleLabel[profile.role] || profile.role }}</span>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-1.5">
                <label class="text-xs font-semibold text-gray-500 uppercase">Nome</label>
                <Input v-model="profile.name" placeholder="Seu nome" />
              </div>
              <div class="space-y-1.5">
                <label class="text-xs font-semibold text-gray-500 uppercase">E-mail</label>
                <Input v-model="profile.email" type="email" placeholder="seu@email.com" />
              </div>
            </div>

            <Button class="w-full" :disabled="savingProfile" @click="handleSaveProfile">
              {{ savingProfile ? 'Salvando...' : 'Salvar Alterações' }}
            </Button>
          </div>
        </Card>

        <!-- Senha -->
        <Card class="p-6">
          <h2 class="text-xl font-semibold mb-4 text-gray-900 flex items-center gap-2">
            <Shield class="w-5 h-5 text-gray-400" />
            Segurança
          </h2>
          <div class="space-y-4">
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-gray-500 uppercase">Senha Atual</label>
              <Input v-model="password.current" type="password" placeholder="••••••••" />
            </div>
            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-1.5">
                <label class="text-xs font-semibold text-gray-500 uppercase">Nova Senha</label>
                <Input v-model="password.new" type="password" placeholder="Mínimo 6 caracteres" />
              </div>
              <div class="space-y-1.5">
                <label class="text-xs font-semibold text-gray-500 uppercase">Confirmar Senha</label>
                <Input v-model="password.confirm" type="password" placeholder="Repita a senha" />
              </div>
            </div>
            <Button variant="outline" class="w-full" :disabled="savingPassword" @click="handleChangePassword">
              {{ savingPassword ? 'Salvando...' : 'Atualizar Senha' }}
            </Button>
          </div>
        </Card>

        <!-- Sessão -->
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

      <Card v-if="isAdmin" class="w-full xl:flex-1 min-w-0 p-6">
        <div class="flex flex-col gap-3 mb-6 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h2 class="text-xl font-semibold text-gray-900 flex items-center gap-2">
              <Link2 class="w-5 h-5 text-gray-500" />
              Integrações do cliente
            </h2>
            <p class="text-sm text-gray-500 mt-1">Configure Instagram e grupo de envio no mesmo cliente.</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <Button variant="outline" size="sm" :disabled="loadingMetaAccounts" @click="fetchMetaAccounts">
              <RefreshCw :class="['w-4 h-4', loadingMetaAccounts ? 'animate-spin' : '']" />
              {{ loadingMetaAccounts ? 'Buscando...' : 'Buscar Meta' }}
            </Button>
            <Button variant="outline" size="sm" :disabled="loadingEvolutionGroups" @click="fetchEvolutionGroups">
              <RefreshCw :class="['w-4 h-4', loadingEvolutionGroups ? 'animate-spin' : '']" />
              {{ loadingEvolutionGroups ? 'Buscando...' : 'Buscar grupos' }}
            </Button>
          </div>
        </div>

        <div class="space-y-4">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Cliente</label>
            <select
              v-model="selectedClientId"
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
              v-model="selectedMetaAccountKey"
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
              v-model="selectedEvolutionGroupId"
              required
              class="w-full p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary appearance-none cursor-pointer disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="loadingEvolutionGroups"
            >
              <option value="">Selecione um grupo</option>
              <option
                v-for="group in evolutionGroups"
                :key="group.groupId"
                :value="group.groupId"
                :disabled="isGroupLinkedToAnotherClient(group)"
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
            :disabled="savingIntegrations || loadingMetaAccounts || loadingEvolutionGroups || !selectedClientId || !selectedMetaAccount || !selectedEvolutionGroup || !!selectedClientConfig || selectedMetaAccount?.alreadyLinked || isGroupLinkedToAnotherClient(selectedEvolutionGroup)"
            @click="handleLinkClientIntegrations"
          >
            <Link2 class="w-4 h-4" />
            {{ savingIntegrations ? 'Vinculando...' : 'Vincular Instagram e WhatsApp' }}
          </Button>
        </div>
      </Card>
    </div>
    </div>
  </AppLayout>
</template>
