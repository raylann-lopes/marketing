<script setup lang="ts">
import { CheckCircle2, Info, TriangleAlert, X, XCircle } from 'lucide-vue-next'
import type { Component } from 'vue'
import { useFeedback, type FeedbackVariant } from '@/lib/feedback'

const feedback = useFeedback()

const icons: Record<FeedbackVariant, Component> = {
  success: CheckCircle2,
  error: XCircle,
  warning: TriangleAlert,
  info: Info,
}

const classes: Record<FeedbackVariant, string> = {
  success: 'border-green-200 bg-green-50 text-green-900',
  error: 'border-red-200 bg-red-50 text-red-900',
  warning: 'border-amber-200 bg-amber-50 text-amber-900',
  info: 'border-blue-200 bg-blue-50 text-blue-900',
}

const iconClasses: Record<FeedbackVariant, string> = {
  success: 'text-green-600',
  error: 'text-red-600',
  warning: 'text-amber-600',
  info: 'text-blue-600',
}
</script>

<template>
  <Teleport to="body">
    <div class="pointer-events-none fixed right-4 top-4 z-[100] flex w-[min(28rem,calc(100vw-2rem))] flex-col gap-2">
      <TransitionGroup name="toast">
        <div
          v-for="toast in feedback.state.toasts"
          :key="toast.id"
          :class="[
            'pointer-events-auto rounded-lg border px-3 py-2.5 shadow-lg',
            classes[toast.variant],
          ]"
          role="status"
          aria-live="polite"
        >
          <div class="flex items-start gap-2.5">
            <component :is="icons[toast.variant]" :class="['mt-0.5 h-4 w-4 shrink-0', iconClasses[toast.variant]]" />
            <div class="min-w-0 flex-1">
              <p v-if="toast.title" class="text-sm font-semibold leading-5">{{ toast.title }}</p>
              <p class="text-sm leading-5">{{ toast.message }}</p>
            </div>
            <button
              type="button"
              class="rounded p-0.5 opacity-70 transition hover:bg-black/5 hover:opacity-100"
              aria-label="Fechar alerta"
              @click="feedback.dismiss(toast.id)"
            >
              <X class="h-4 w-4" />
            </button>
          </div>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.18s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.98);
}
</style>
