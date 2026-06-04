<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { UserPlus, X, CheckCircle, Eye, EyeOff, UserX, UserCheck, Pencil } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Card from '@/components/ui/Card.vue'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import { userService, type UserProfile, type CreateUserPayload } from '@/services/userService'

const users = ref<UserProfile[]>([])
const loading = ref(true)
const error = ref('')
const success = ref('')

const showForm = ref(false)
const saving = ref(false)
const showPassword = ref(false)

const form = ref<CreateUserPayload>({ name: '', email: '', password: '' })
const formError = ref('')

const updatingRoleId = ref<number | null>(null)
const currentUserId = Number(localStorage.getItem('userId') || sessionStorage.getItem('userId') || 0)

const editingUser = ref<UserProfile | null>(null)
const editForm = ref({ name: '', email: '' })
const editError = ref('')
const editSaving = ref(false)

function openEdit(user: UserProfile) {
  editingUser.value = user
  editForm.value = { name: user.name, email: user.email }
  editError.value = ''
}

function closeEdit() {
  editingUser.value = null
  editForm.value = { name: '', email: '' }
  editError.value = ''
}

async function handleEdit() {
  if (!editingUser.value) return
  editError.value = ''
  if (!editForm.value.name.trim() || !editForm.value.email.trim()) {
    editError.value = 'Preencha todos os campos.'
    return
  }
  editSaving.value = true
  try {
    const updated = await userService.updateById(editingUser.value.id, editForm.value)
    const idx = users.value.findIndex(u => u.id === editingUser.value!.id)
    if (idx !== -1) users.value[idx] = updated
    success.value = `Usuário "${updated.name}" atualizado.`
    setTimeout(() => { success.value = '' }, 4000)
    closeEdit()
  } catch (e) {
    editError.value = e instanceof Error ? e.message : 'Erro ao atualizar usuário'
  } finally {
    editSaving.value = false
  }
}

async function fetchUsers() {
  loading.value = true
  error.value = ''
  try {
    users.value = await userService.listAll()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao carregar usuários'
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  formError.value = ''
  if (!form.value.name.trim() || !form.value.email.trim() || !form.value.password.trim()) {
    formError.value = 'Preencha todos os campos.'
    return
  }
  if (form.value.password.length < 6) {
    formError.value = 'A senha deve ter pelo menos 6 caracteres.'
    return
  }

  saving.value = true
  try {
    const created = await userService.create(form.value)
    users.value.push(created)
    form.value = { name: '', email: '', password: '' }
    showForm.value = false
    success.value = `Usuário "${created.name}" criado com sucesso!`
    setTimeout(() => { success.value = '' }, 4000)
  } catch (e) {
    formError.value = e instanceof Error ? e.message : 'Erro ao criar usuário'
  } finally {
    saving.value = false
  }
}

async function handleRoleChange(user: UserProfile, newRole: 'ADMIN' | 'USER') {
  if (user.role === newRole) return
  updatingRoleId.value = user.id
  error.value = ''
  try {
    const updated = await userService.updateRole(user.id, newRole)
    const idx = users.value.findIndex(u => u.id === user.id)
    if (idx !== -1) users.value[idx] = updated
    success.value = `Perfil de "${updated.name}" alterado para ${newRole === 'ADMIN' ? 'Admin' : 'Usuário'}.`
    setTimeout(() => { success.value = '' }, 4000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao alterar perfil'
  } finally {
    updatingRoleId.value = null
  }
}

async function handleActivate(user: UserProfile) {
  error.value = ''
  try {
    const updated = await userService.activateById(user.id)
    const idx = users.value.findIndex(u => u.id === user.id)
    if (idx !== -1) users.value[idx] = updated
    success.value = `Usuário "${updated.name}" reativado.`
    setTimeout(() => { success.value = '' }, 4000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao reativar usuário'
  }
}

async function handleDeactivate(user: UserProfile) {
  if (!confirm(`Desativar o usuário "${user.name}"? Ele perderá o acesso ao sistema.`)) return
  error.value = ''
  try {
    await userService.deleteById(user.id)
    const idx = users.value.findIndex(u => u.id === user.id)
    if (idx !== -1) users.value[idx] = { ...users.value[idx], active: false }
    success.value = `Usuário "${user.name}" desativado.`
    setTimeout(() => { success.value = '' }, 4000)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Erro ao desativar usuário'
  }
}

function cancelForm() {
  showForm.value = false
  form.value = { name: '', email: '', password: '' }
  formError.value = ''
  showPassword.value = false
}

onMounted(fetchUsers)
</script>

<template>
  <AppLayout>
    <div class="mb-6 flex items-start justify-between">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Usuários</h1>
        <p class="text-gray-500 mt-1">Gerencie quem tem acesso ao sistema.</p>
      </div>
      <Button @click="showForm = true" :disabled="showForm">
        <UserPlus class="w-4 h-4" />
        Novo Usuário
      </Button>
    </div>

    <!-- Feedback -->
    <div v-if="error" class="flex items-center gap-2 bg-red-50 text-red-600 rounded-xl px-4 py-3 border border-red-100 mb-4">
      <X class="w-4 h-4 shrink-0" />
      <span class="text-sm">{{ error }}</span>
    </div>
    <div v-if="success" class="flex items-center gap-2 bg-green-50 text-green-700 rounded-xl px-4 py-3 border border-green-100 mb-4">
      <CheckCircle class="w-4 h-4 shrink-0" />
      <span class="text-sm">{{ success }}</span>
    </div>

    <!-- Formulário de criação -->
    <Card v-if="showForm" class="p-6 mb-6">
      <h2 class="text-lg font-semibold text-gray-900 mb-4">Cadastrar Novo Usuário</h2>

      <div v-if="formError" class="flex items-center gap-2 bg-red-50 text-red-600 rounded-xl px-4 py-3 border border-red-100 mb-4">
        <X class="w-4 h-4 shrink-0" />
        <span class="text-sm">{{ formError }}</span>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div class="flex flex-col gap-1">
          <label class="text-sm font-medium text-gray-700">Nome</label>
          <Input v-model="form.name" placeholder="Nome completo" />
        </div>
        <div class="flex flex-col gap-1">
          <label class="text-sm font-medium text-gray-700">E-mail</label>
          <Input v-model="form.email" type="email" placeholder="email@exemplo.com" />
        </div>
        <div class="flex flex-col gap-1">
          <label class="text-sm font-medium text-gray-700">Senha</label>
          <div class="relative">
            <Input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Mínimo 6 caracteres"
              class="pr-10"
            />
            <button
              type="button"
              class="absolute inset-y-0 right-2 flex items-center text-gray-400 hover:text-gray-600"
              @click="showPassword = !showPassword"
            >
              <EyeOff v-if="showPassword" class="w-4 h-4" />
              <Eye v-else class="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      <div class="flex gap-3 mt-4">
        <Button @click="handleCreate" :disabled="saving">
          {{ saving ? 'Salvando...' : 'Criar Usuário' }}
        </Button>
        <Button variant="outline" @click="cancelForm" :disabled="saving">
          Cancelar
        </Button>
      </div>
    </Card>

    <!-- Lista de usuários -->
    <Card class="overflow-hidden">
      <div v-if="loading" class="flex items-center justify-center h-48">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>

      <div v-else-if="users.length === 0" class="flex flex-col items-center justify-center h-48 text-gray-400">
        <UserPlus class="w-10 h-10 mb-2 opacity-30" />
        <p class="text-sm">Nenhum usuário cadastrado.</p>
      </div>

      <table v-else class="w-full text-sm">
        <thead class="bg-gray-50 border-b border-gray-100">
          <tr>
            <th class="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">Nome</th>
            <th class="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">E-mail</th>
            <th class="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">Perfil</th>
            <th class="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wide">Status</th>
            <th class="px-6 py-3"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-50">
          <tr
            v-for="user in users"
            :key="user.id"
            :class="['hover:bg-gray-50/60 transition-colors', !user.active && 'opacity-50']"
          >
            <td class="px-6 py-4 font-medium text-gray-900">{{ user.name }}</td>
            <td class="px-6 py-4 text-gray-500">{{ user.email }}</td>
            <td class="px-6 py-4">
              <button
                type="button"
                :disabled="updatingRoleId === user.id || user.id === currentUserId || !user.active"
                :title="!user.active ? 'Usuário inativo' : user.id === currentUserId ? 'Não é possível alterar o próprio perfil' : user.role === 'ADMIN' ? 'Clique para tornar Usuário' : 'Clique para tornar Admin'"
                :class="[
                  'inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold transition-all select-none',
                  'disabled:pointer-events-none',
                  updatingRoleId === user.id
                    ? 'bg-gray-100 text-gray-400'
                    : user.id === currentUserId || !user.active
                      ? user.role === 'ADMIN'
                        ? 'bg-primary/10 text-primary ring-1 ring-primary/20 opacity-50 cursor-not-allowed'
                        : 'bg-gray-100 text-gray-600 ring-1 ring-gray-200 opacity-50 cursor-not-allowed'
                      : user.role === 'ADMIN'
                        ? 'bg-primary/10 text-primary hover:bg-primary/20 ring-1 ring-primary/20'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200 ring-1 ring-gray-200'
                ]"
                @click="handleRoleChange(user, user.role === 'ADMIN' ? 'USER' : 'ADMIN')"
              >
                <span
                  v-if="updatingRoleId === user.id"
                  class="w-3 h-3 rounded-full border-2 border-gray-300 border-t-gray-500 animate-spin"
                />
                <span v-else>{{ user.role === 'ADMIN' ? '★' : '○' }}</span>
                {{ updatingRoleId === user.id ? '...' : user.role === 'ADMIN' ? 'Admin' : 'Usuário' }}
              </button>
            </td>
            <td class="px-6 py-4">
              <span
                :class="[
                  'inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium',
                  user.active
                    ? 'bg-green-50 text-green-700 ring-1 ring-green-200'
                    : 'bg-gray-100 text-gray-500 ring-1 ring-gray-200'
                ]"
              >
                {{ user.active ? 'Ativo' : 'Inativo' }}
              </span>
            </td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-1">
                <button
                  v-if="!user.active"
                  type="button"
                  class="p-2 rounded-lg text-gray-400 hover:text-green-600 hover:bg-green-50 transition-colors"
                  title="Reativar usuário"
                  @click="handleActivate(user)"
                >
                  <UserCheck class="w-4 h-4" />
                </button>
                <button
                  v-if="user.active"
                  type="button"
                  class="p-2 rounded-lg text-gray-400 hover:text-blue-500 hover:bg-blue-50 transition-colors"
                  title="Editar usuário"
                  @click="openEdit(user)"
                >
                  <Pencil class="w-4 h-4" />
                </button>
                <button
                  v-if="user.active && user.id !== currentUserId"
                  type="button"
                  class="p-2 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition-colors"
                  title="Desativar usuário"
                  @click="handleDeactivate(user)"
                >
                  <UserX class="w-4 h-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </Card>
    <!-- Modal de edição -->
    <div
      v-if="editingUser"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm"
      @click.self="closeEdit"
    >
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">Editar Usuário</h2>
          <button type="button" class="text-gray-400 hover:text-gray-600" @click="closeEdit">
            <X class="w-5 h-5" />
          </button>
        </div>

        <div v-if="editError" class="flex items-center gap-2 bg-red-50 text-red-600 rounded-xl px-4 py-3 border border-red-100 mb-4">
          <X class="w-4 h-4 shrink-0" />
          <span class="text-sm">{{ editError }}</span>
        </div>

        <div class="flex flex-col gap-4">
          <div class="flex flex-col gap-1">
            <label class="text-sm font-medium text-gray-700">Nome</label>
            <Input v-model="editForm.name" placeholder="Nome completo" />
          </div>
          <div class="flex flex-col gap-1">
            <label class="text-sm font-medium text-gray-700">E-mail</label>
            <Input v-model="editForm.email" type="email" placeholder="email@exemplo.com" />
          </div>
        </div>

        <div class="flex gap-3 mt-6">
          <Button @click="handleEdit" :disabled="editSaving">
            {{ editSaving ? 'Salvando...' : 'Salvar' }}
          </Button>
          <Button variant="outline" @click="closeEdit" :disabled="editSaving">
            Cancelar
          </Button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
