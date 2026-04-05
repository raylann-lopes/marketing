<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, EyeOff, Shield } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import Input from '@/components/ui/Input.vue'
import { setCurrentUserId } from '@/lib/api'

const router = useRouter()
const email = ref('')
const password = ref('')
const showPassword = ref(false)
const keepConnected = ref(false)
const loading = ref(false)
const error = ref('')

const baseUrl = import.meta.env.VITE_API_BASE_URL
const storage = () => (keepConnected.value ? localStorage : sessionStorage)

async function handleLogin() {
  if (!email.value || !password.value) {
    error.value = 'Preencha e-mail e senha.'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await fetch(`${baseUrl}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email.value, password: password.value }),
    })

    if (!res.ok) throw new Error('Credenciais inválidas.')
    const data = await res.json()
    const token = data.accessToken || data.token
    if (!token) throw new Error('Token não recebido do servidor.')
    storage().setItem('token', token)
    if (data.role) storage().setItem('role', data.role)
    if (data.userId) {
      storage().setItem('userId', String(data.userId))
      setCurrentUserId(data.userId)
    }
    router.push('/dashboard')
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Erro ao realizar login.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div
    class="min-h-screen bg-gradient-to-br from-purple-50 via-white to-indigo-50 flex flex-col items-center justify-center px-4"
  >
    <!-- Card -->
    <div class="w-full max-w-sm">
      <!-- Header -->
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-[#6B21A8]">North Produções</h1>
        <p class="text-xs tracking-[0.2em] text-gray-400 mt-1 uppercase">Digital Ateliê</p>
        <p class="text-sm text-gray-500 mt-3">Entre na sua conta para gerenciar seus projetos.</p>
      </div>

      <!-- Form -->
      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 space-y-5">
        <div v-if="error" class="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
          {{ error }}
        </div>

        <!-- Email -->
        <div class="space-y-1.5">
          <label class="text-xs font-semibold text-gray-500 tracking-wide uppercase">E-mail</label>
          <Input
            v-model="email"
            type="email"
            placeholder="seu@email.com"
            class="bg-gray-50 border-gray-200"
            @keyup.enter="handleLogin"
          />
        </div>

        <!-- Password -->
        <div class="space-y-1.5">
          <div class="flex items-center justify-between">
            <label class="text-xs font-semibold text-gray-500 tracking-wide uppercase">Senha</label>
            <a href="#" class="text-xs text-[#7C3AED] hover:underline">Esqueceu sua senha?</a>
          </div>
          <div class="relative">
            <Input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="••••••••"
              class="bg-gray-50 border-gray-200 pr-10"
              @keyup.enter="handleLogin"
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              @click="showPassword = !showPassword"
            >
              <EyeOff v-if="showPassword" class="w-4 h-4" />
              <Eye v-else class="w-4 h-4" />
            </button>
          </div>
        </div>

        <!-- Keep connected -->
        <label class="flex items-center gap-2 cursor-pointer">
          <input
            v-model="keepConnected"
            type="checkbox"
            class="w-4 h-4 rounded border-gray-300 text-purple-600 accent-purple-700"
          />
          <span class="text-sm text-gray-600">Manter conectado</span>
        </label>

        <!-- Submit -->
        <Button class="w-full h-11 text-base" :disabled="loading" @click="handleLogin">
          {{ loading ? 'Entrando...' : 'Entrar no Ateliê' }}
        </Button>

        <div class="border-t border-gray-100 pt-4 text-center">
          <p class="text-xs text-gray-400">
            Acesso restrito a colaboradores e parceiros North Produções.
          </p>
          <div class="flex items-center justify-center gap-4 mt-2">
            <a href="#" class="text-xs text-gray-400 hover:text-gray-600 uppercase tracking-wide"
              >Suporte</a
            >
            <a href="#" class="text-xs text-gray-400 hover:text-gray-600 uppercase tracking-wide"
              >Segurança</a
            >
          </div>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="mt-8 flex flex-col items-center gap-2">
      <div class="flex items-center gap-2 border border-gray-200 rounded-full px-4 py-1.5 bg-white">
        <Shield class="w-3.5 h-3.5 text-[#7C3AED]" />
        <span class="text-xs text-gray-500 uppercase tracking-widest">Ambiente Seguro</span>
      </div>
      <p class="text-xs text-gray-400">© 2024 North Produções. Todos os direitos reservados.</p>
    </div>
  </div>
</template>
