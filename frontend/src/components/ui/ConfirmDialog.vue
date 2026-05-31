<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue'
import Button from '@/components/ui/Button.vue'
import { useFeedback } from '@/lib/feedback'

const feedback = useFeedback()
const dialog = computed(() => feedback.state.confirm)

const confirmVariant = computed(() => (dialog.value.tone === 'danger' ? 'destructive' : 'default'))
const confirmClass = computed(() => (dialog.value.tone === 'warning' ? 'bg-amber-500 hover:bg-amber-600' : ''))

function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape') feedback.resolveConfirm(false)
}

watch(
  () => dialog.value.open,
  (isOpen) => {
    if (isOpen) {
      document.addEventListener('keydown', handleEscape)
      return
    }
    document.removeEventListener('keydown', handleEscape)
  },
)

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleEscape)
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="dialog.open"
      class="fixed inset-0 z-[110] flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
      @click.self="feedback.resolveConfirm(false)"
    >
      <div class="w-full max-w-md rounded-lg border border-gray-100 bg-white p-5 shadow-2xl">
        <h2 class="text-lg font-semibold text-gray-900">{{ dialog.title }}</h2>
        <p class="mt-2 text-sm leading-6 text-gray-600">{{ dialog.message }}</p>
        <div class="mt-5 flex justify-end gap-2">
          <Button variant="outline" @click="feedback.resolveConfirm(false)">
            {{ dialog.cancelText }}
          </Button>
          <Button :variant="confirmVariant" :class="confirmClass" @click="feedback.resolveConfirm(true)">
            {{ dialog.confirmText }}
          </Button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
