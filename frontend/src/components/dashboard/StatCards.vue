<script setup lang="ts">
import { Users, FileText, Clock, CheckCircle2 } from 'lucide-vue-next'
import Card from '@/components/ui/Card.vue'

interface Props {
  totalClients: number
  postsThisMonth: number
  pendingPosts: number
  publishedPosts: number
}

defineProps<Props>()
</script>

<template>
  <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
    <Card class="p-5 transition-all hover:shadow-md cursor-default">
      <p class="text-xs font-semibold uppercase tracking-wide text-gray-400">Clientes Ativos</p>
      <div class="flex items-baseline gap-2 mt-2">
        <span class="text-3xl font-bold text-gray-900">{{ totalClients }}</span>
        <span class="text-sm font-medium text-green-600"><Users class="w-4 h-4 inline" /></span>
      </div>
    </Card>
    <Card class="p-5 transition-all hover:shadow-md cursor-default">
      <p class="text-xs font-semibold uppercase tracking-wide text-gray-400">Posts Este Mês</p>
      <div class="flex items-baseline gap-2 mt-2">
        <span class="text-3xl font-bold text-gray-900">{{ postsThisMonth }}</span>
        <span class="text-sm font-medium text-gray-400"><FileText class="w-4 h-4 inline" /></span>
      </div>
    </Card>
    <Card class="p-5 transition-all hover:shadow-md cursor-default border-primary ring-1 ring-primary/20">
      <p class="text-xs font-semibold uppercase tracking-wide text-gray-400">Em Andamento</p>
      <div class="flex items-baseline gap-2 mt-2">
        <span class="text-3xl font-bold text-primary">{{ pendingPosts }}</span>
        <span class="text-sm font-medium text-purple-600"><Clock class="w-4 h-4 inline" /></span>
      </div>
    </Card>
    <Card class="p-5 transition-all hover:shadow-md cursor-default">
      <p class="text-xs font-semibold uppercase tracking-wide text-gray-400">Publicados</p>
      <div class="flex items-baseline gap-2 mt-2">
        <span class="text-3xl font-bold text-gray-900">{{ publishedPosts }}</span>
        <span class="text-sm font-medium text-green-600"><CheckCircle2 class="w-4 h-4 inline" /></span>
      </div>
    </Card>
  </div>
</template>
