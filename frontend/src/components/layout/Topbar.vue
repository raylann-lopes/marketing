<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, ChevronDown, LogOut, Search, Settings } from 'lucide-vue-next'
import Avatar from '@/components/ui/Avatar.vue'
import Button from '@/components/ui/Button.vue'
import { setCurrentUserId } from '@/lib/api'
import { useFeedback } from '@/lib/feedback'
import { userService } from '@/services/userService'

type TopbarUser = {
  name: string
  role: string
  email?: string
}

const searchModel = defineModel<string>('search', { default: '' })

const props = withDefaults(
  defineProps<{
    user?: TopbarUser
    placeholder?: string
  }>(),
  {
    user: () => ({ name: 'Equipe North', role: localStorage.getItem('role') || sessionStorage.getItem('role') || 'USER' }),
    placeholder: 'Buscar projetos, clientes ou arquivos...',
  },
)

const router = useRouter()
const feedback = useFeedback()
const profile = ref<TopbarUser>(props.user)
const isProfileOpen = ref(false)
const isNotificationsOpen = ref(false)
const profileRef = ref<HTMLElement | null>(null)
const notificationsRef = ref<HTMLElement | null>(null)

const roleLabels: Record<string, string> = {
  ADMIN: 'Administrador',
  USER: 'Colaborador',
}

const roleLabel = computed(() => roleLabels[profile.value.role] || profile.value.role)
const recentNotifications = computed(() => feedback.state.notifications.slice(0, 8))

function closeMenus() {
  isProfileOpen.value = false
  isNotificationsOpen.value = false
}

function toggleNotifications() {
  isNotificationsOpen.value = !isNotificationsOpen.value
  isProfileOpen.value = false
  if (isNotificationsOpen.value) feedback.markNotificationsRead()
}

function toggleProfile() {
  isProfileOpen.value = !isProfileOpen.value
  isNotificationsOpen.value = false
}

function handleOutsideClick(event: MouseEvent) {
  const target = event.target as Node
  if (!(profileRef.value?.contains(target) ?? false)) isProfileOpen.value = false
  if (!(notificationsRef.value?.contains(target) ?? false)) isNotificationsOpen.value = false
}

async function fetchProfile() {
  try {
    const data = await userService.getMe()
    profile.value = {
      name: data.name || profile.value.name,
      role: data.role || profile.value.role,
      email: data.email,
    }
    setCurrentUserId(data.id)
  } catch (error) {
    console.warn('Não foi possível carregar o perfil da topbar:', error)
  }
}

async function goToSettings() {
  closeMenus()
  await router.push('/settings')
}

async function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('userId')
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('userId')
  setCurrentUserId(null)
  closeMenus()
  await router.push('/login')
}

watch(
  () => props.user,
  (user) => {
    if (user) profile.value = user
  },
)

onMounted(() => {
  fetchProfile()
  document.addEventListener('click', handleOutsideClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleOutsideClick)
})
</script>

<template>
  <header class="h-14 bg-white border-b border-gray-100 flex items-center px-6 gap-4 shrink-0">
    <div class="flex-1 max-w-xl relative">
      <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
      <input
        v-model="searchModel"
        type="text"
        :placeholder="placeholder"
        class="w-full pl-9 pr-4 h-9 rounded-lg border border-gray-200 bg-gray-50 text-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-purple-300"
      />
    </div>

    <div class="flex items-center gap-3 ml-auto">
      <div ref="notificationsRef" class="relative hidden md:block">
        <Button
          variant="ghost"
          size="icon"
          type="button"
          class="relative"
          aria-label="Abrir notificações"
          :aria-expanded="isNotificationsOpen"
          @click.stop="toggleNotifications"
        >
          <Bell class="w-5 h-5 text-gray-500" />
          <span
            v-if="feedback.unreadCount.value > 0"
            class="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white"
          >
            {{ feedback.unreadCount.value > 9 ? '9+' : feedback.unreadCount.value }}
          </span>
        </Button>

        <div
          v-if="isNotificationsOpen"
          class="absolute right-0 top-12 z-50 w-80 rounded-lg border border-gray-200 bg-white p-2 shadow-xl"
        >
          <div class="border-b border-gray-100 px-2 pb-2 pt-1">
            <p class="text-sm font-semibold text-gray-800">Notificações</p>
            <p class="text-xs text-gray-500">Eventos recentes do sistema</p>
          </div>
          <div v-if="recentNotifications.length === 0" class="px-2 py-6 text-center text-sm text-gray-500">
            Nenhuma notificação por enquanto.
          </div>
          <div v-else class="max-h-80 overflow-y-auto py-2">
            <div
              v-for="item in recentNotifications"
              :key="item.id"
              class="rounded-md px-2 py-2 text-sm hover:bg-gray-50"
            >
              <p class="font-medium text-gray-800">{{ item.title || 'Atualização' }}</p>
              <p class="mt-0.5 text-xs text-gray-600">{{ item.message }}</p>
            </div>
          </div>
        </div>
      </div>

      <div ref="profileRef" class="relative">
        <button
          type="button"
          class="flex items-center gap-2 rounded-lg px-2 py-1 transition-colors hover:bg-gray-100"
          aria-label="Abrir menu do perfil"
          :aria-expanded="isProfileOpen"
          @click.stop="toggleProfile"
        >
          <div class="hidden text-right sm:block">
            <p class="text-sm font-medium text-gray-800">{{ profile.name }}</p>
            <p class="text-xs text-gray-500">{{ roleLabel }}</p>
          </div>
          <Avatar :name="profile.name" size="md" class="bg-purple-200 text-purple-700" />
          <ChevronDown class="h-4 w-4 text-gray-400" />
        </button>

        <div
          v-if="isProfileOpen"
          class="absolute right-0 top-12 z-50 w-64 rounded-lg border border-gray-200 bg-white p-2 shadow-xl"
        >
          <div class="border-b border-gray-100 px-2 pb-2 pt-1">
            <p class="text-sm font-semibold text-gray-800">{{ profile.name }}</p>
            <p class="text-xs text-gray-500">{{ profile.email || roleLabel }}</p>
          </div>
          <button
            type="button"
            class="mt-1 flex w-full items-center gap-2 rounded-md px-2 py-2 text-sm text-gray-700 hover:bg-gray-50"
            @click="goToSettings"
          >
            <Settings class="h-4 w-4" />
            Configurações
          </button>
          <button
            type="button"
            class="flex w-full items-center gap-2 rounded-md px-2 py-2 text-sm text-red-600 hover:bg-red-50"
            @click="logout"
          >
            <LogOut class="h-4 w-4" />
            Sair
          </button>
        </div>
      </div>
    </div>
  </header>
</template>
