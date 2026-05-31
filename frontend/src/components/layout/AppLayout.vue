<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Topbar from './Topbar.vue'

defineProps<{
  topbarPlaceholder?: string
}>()

const search = defineModel<string>('search', { default: '' })
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-gray-50">
    <Sidebar />
    <div class="flex flex-col flex-1 overflow-hidden">
      <Topbar v-model:search="search" :placeholder="topbarPlaceholder" />
      <main class="flex-1 overflow-y-auto p-6">
        <slot />
      </main>
    </div>
  </div>
</template>
