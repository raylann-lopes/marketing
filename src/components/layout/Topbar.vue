<script setup lang="ts">
import { Bell, Search } from 'lucide-vue-next'
import Avatar from '@/components/ui/Avatar.vue'

type TopbarUser = {
  name: string
  role: string
}

const searchModel = defineModel<string>('search', { default: '' })

withDefaults(
  defineProps<{
    user?: TopbarUser
    placeholder?: string
  }>(),
  {
    user: () => ({ name: 'Ana Silva', role: 'Diretora Criativa' }),
    placeholder: 'Buscar projetos, clientes ou arquivos...',
  },
)
</script>

<template>
  <header class="h-14 bg-white border-b border-gray-100 flex items-center px-6 gap-4 shrink-0">
    <!-- Search -->
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
      <!-- Notification -->
      <button class="relative p-2 hover:bg-gray-100 rounded-lg transition-colors">
        <Bell class="w-5 h-5 text-gray-500" />
        <span class="absolute top-1.5 right-1.5 w-2 h-2 bg-red-500 rounded-full" />
      </button>

      <!-- User -->
      <div class="flex items-center gap-2">
        <div class="text-right">
          <p class="text-sm font-medium text-gray-800">{{ user.name }}</p>
          <p class="text-xs text-gray-500">{{ user.role }}</p>
        </div>
        <Avatar :name="user.name" size="md" class="bg-purple-200 text-purple-700" />
      </div>
    </div>
  </header>
</template>
