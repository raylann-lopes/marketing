<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { CheckCircle, Shield, X, User, Instagram } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Card from '@/components/ui/Card.vue'
import Input from '@/components/ui/Input.vue'
import Button from '@/components/ui/Button.vue'
import { userService } from '@/services/userService'
import { setCurrentUserId } from '@/lib/api'
import { clientService, type Client } from '@/services/clientService'
import { accountConfigService, type AccountConfig } from '@/services/accountConfigService'

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

// Instagram Account Config
const clients = ref<Client[]>([])
const selectedClientId = ref('')
const igUserId = ref('')
const instagramAccountId = ref('')
const accessToken = ref('')
const savingAccount = ref(false)
const clientConfigs = ref<Map<number, AccountConfig>>(new Map())
const selectedClientConfig = computed(() => {
  if (!selectedClientId.value) return null
  return clientConfigs.value.get(Number(selectedClientId.value)) ?? null
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

async function handleSaveAccount() {
  accountSuccess.value = ''
  error.value = ''

  if (!selectedClientId.value) {
    error.value = 'Selecione um cliente.'
    return
  }
  if (!igUserId.value.trim()) {
    error.value = 'Informe o IG User ID.'
    return
  }
  if (!/^\d+$/.test(igUserId.value.trim())) {
    error.value = 'O IG User ID deve conter apenas dígitos.'
    return
  }
  if (selectedClientConfig.value) {
    error.value = 'Este cliente já possui configuração cadastrada. O backend atual não permite edição por esta tela.'
    return
  }

  savingAccount.value = true
  try {
    const config = await accountConfigService.configure({
      clientId: Number(selectedClientId.value),
      igUserId: igUserId.value.trim(),
      instagramAccountId: instagramAccountId.value.trim() || undefined,
      accessToken: accessToken.value.trim() || undefined,
    })
    clientConfigs.value.set(config.clientId, config)
    accountSuccess.value = `Conta vinculada por ${config.configuredBy} em ${new Date(config.configuredAt).toLocaleDateString('pt-BR')}`
    selectedClientId.value = ''
    igUserId.value = ''
    instagramAccountId.value = ''
    accessToken.value = ''
    setTimeout(() => { accountSuccess.value = '' }, 5000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao configurar conta do Instagram'
  } finally {
    savingAccount.value = false
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
    await fetchAccountConfigs()
  }
})

watch(selectedClientId, (clientId) => {
  accountSuccess.value = ''
  error.value = ''

  if (!clientId) {
    igUserId.value = ''
    instagramAccountId.value = ''
    accessToken.value = ''
    return
  }

  const existingConfig = clientConfigs.value.get(Number(clientId))
  igUserId.value = existingConfig?.igUserId ?? ''
  instagramAccountId.value = existingConfig?.instagramAccountId ?? ''
  accessToken.value = ''
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
      <div class="flex gap-6 items-start">
      <div class="flex-1 min-w-0 space-y-6">
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

      <!-- Right: Instagram Config (Admin) -->
      <Card v-if="isAdmin" class="flex-1 min-w-0 p-6">
        <h2 class="text-xl font-semibold mb-2 text-gray-900 flex items-center gap-2">
          <Instagram class="w-5 h-5 text-pink-500" />
          Integração Instagram
        </h2>
        <p class="text-sm text-gray-500 mb-6">Vincule o IG User ID e, se necessário, o Instagram Account ID e access token do cliente.</p>

        <div class="space-y-4">
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Cliente</label>
            <select
              v-model="selectedClientId"
              class="w-full p-2.5 pr-10 rounded-lg border bg-gray-50 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary appearance-none cursor-pointer"
            >
              <option value="">Selecione o cliente</option>
              <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">IG User ID</label>
            <Input v-model="igUserId" placeholder="Ex: 17841400000000000" />
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Instagram Account ID</label>
            <Input v-model="instagramAccountId" placeholder="Opcional" />
          </div>

          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gray-500 uppercase">Access Token</label>
            <textarea
              v-model="accessToken"
              rows="4"
              placeholder="Opcional"
              class="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            />
          </div>

          <div v-if="selectedClientConfig" class="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
            Este cliente já possui configuração cadastrada. IG User ID:
            <code class="font-mono">{{ selectedClientConfig.igUserId }}</code>.
          </div>

          <Button :disabled="savingAccount || !!selectedClientConfig" @click="handleSaveAccount">
            {{ savingAccount ? 'Salvando...' : 'Vincular Conta do Instagram' }}
          </Button>

          <!-- Configured clients list -->
          <div v-if="clientConfigs.size > 0" class="mt-4 pt-4 border-t border-gray-100">
            <p class="text-xs font-semibold text-gray-400 uppercase mb-2">Clientes configurados</p>
            <div class="space-y-2">
              <div
                v-for="client in clients.filter(c => clientConfigs.has(Number(c.id)))"
                :key="client.id"
                class="flex items-center justify-between bg-gray-50 rounded-lg px-3 py-2.5"
              >
                <div class="flex items-center gap-2.5 min-w-0 flex-1">
                  <div class="w-7 h-7 rounded-full bg-pink-100 flex items-center justify-center shrink-0">
                    <Instagram class="w-3.5 h-3.5 text-pink-600" />
                  </div>
                  <div class="min-w-0">
                    <p class="text-sm font-medium text-gray-800 truncate">{{ client.name }}</p>
                    <p class="text-[10px] text-gray-400 truncate">{{ clientConfigs.get(Number(client.id))?.configuredBy }}</p>
                  </div>
                </div>
                <code class="text-[10px] font-mono bg-white px-2 py-1 rounded border border-gray-200 shrink-0">
                  {{ clientConfigs.get(Number(client.id))?.instagramAccountId || clientConfigs.get(Number(client.id))?.igUserId }}
                </code>
              </div>
            </div>
          </div>
          <div v-else class="text-center py-6 text-sm text-gray-400 bg-gray-50 rounded-lg mt-4">
            Nenhum cliente com conta do Instagram vinculada.
          </div>
        </div>
      </Card>
    </div>
    </div>
  </AppLayout>
</template>
