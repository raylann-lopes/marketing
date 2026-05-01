<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Hammer } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'

const route = useRoute()

const title = computed(() => String(route.meta.title || 'Página em construção'))
const description = computed(() => String(route.meta.description || 'Estamos preparando esta área para as próximas entregas.'))
</script>

<template>
  <AppLayout topbar-placeholder="Navegue pelas áreas...">
    <div class="mx-auto flex min-h-[68vh] w-full max-w-3xl items-center justify-center p-4">
      <section class="w-full rounded-2xl border border-amber-200 bg-amber-50 p-8 text-center shadow-sm">
        <div class="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 text-amber-700">
          <Hammer class="h-6 w-6" />
        </div>
        <h1 class="text-2xl font-bold text-amber-900">{{ title }}</h1>
        <p class="mt-2 text-sm text-amber-800">{{ description }}</p>
        <p class="mt-4 text-xs font-semibold uppercase tracking-wide text-amber-700">Em construção</p>
      </section>
    </div>
  </AppLayout>
</template>
