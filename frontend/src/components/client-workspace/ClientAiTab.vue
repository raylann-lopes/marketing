<script setup lang="ts">
interface RecommendationCard {
  title: string
  priority: 'ALTA' | 'MEDIA' | 'BAIXA'
  details: string
}

interface Props {
  aiFieldChecks: { filled: number; total: number; percent: number }
  aiMissingInputs: string[]
  aiPromptContext: string
  aiRecommendations: RecommendationCard[]
}

defineProps<Props>()
</script>

<template>
  <section class="grid gap-6 xl:grid-cols-[380px_1fr]">
    <div class="space-y-4 rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
      <div>
        <h2 class="text-lg font-bold text-gray-900">Qualidade do Contexto</h2>
        <p class="text-xs text-gray-500">Recomendações ficam melhores quando os dados essenciais estão completos.</p>
      </div>

      <div>
        <div class="mb-1 flex items-center justify-between text-xs font-semibold text-gray-500">
          <span>Campos preenchidos</span>
          <span>{{ aiFieldChecks.filled }}/{{ aiFieldChecks.total }}</span>
        </div>
        <div class="h-2 rounded-full bg-gray-100">
          <div class="h-2 rounded-full bg-primary transition-all" :style="{ width: `${aiFieldChecks.percent}%` }" />
        </div>
        <p class="mt-1 text-xs text-gray-500">{{ aiFieldChecks.percent }}% do briefing principal</p>
      </div>

      <div class="rounded-xl border border-gray-200 bg-gray-50 p-3">
        <p class="text-xs font-semibold uppercase text-gray-500">Pendências</p>
        <ul v-if="aiMissingInputs.length > 0" class="mt-2 space-y-1 text-xs text-gray-700">
          <li v-for="missing in aiMissingInputs" :key="missing">• {{ missing }}</li>
        </ul>
        <p v-else class="mt-2 text-xs text-emerald-600">Contexto essencial preenchido.</p>
      </div>

      <div class="rounded-xl border border-gray-200 bg-gray-50 p-3">
        <p class="text-xs font-semibold uppercase text-gray-500">Resumo para Prompt</p>
        <pre class="mt-2 whitespace-pre-wrap text-xs text-gray-700">{{ aiPromptContext }}</pre>
      </div>
    </div>

    <div class="space-y-4 rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
      <div>
        <h2 class="text-lg font-bold text-gray-900">Recomendações de IA por Nicho</h2>
        <p class="text-xs text-gray-500">Sugestões simples com base no nicho e no calendário já planejado.</p>
      </div>

      <div class="grid gap-3">
        <article
          v-for="(rec, index) in aiRecommendations"
          :key="`${rec.title}-${index}`"
          class="rounded-xl border border-gray-100 bg-gray-50 p-4"
        >
          <div class="mb-2 flex items-center justify-between gap-2">
            <p class="text-sm font-semibold text-gray-900">{{ rec.title }}</p>
            <span
              :class="[
                'rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase',
                rec.priority === 'ALTA'
                  ? 'border border-red-200 bg-red-50 text-red-700'
                  : rec.priority === 'MEDIA'
                    ? 'border border-amber-200 bg-amber-50 text-amber-700'
                    : 'border border-emerald-200 bg-emerald-50 text-emerald-700'
              ]"
            >
              {{ rec.priority }}
            </span>
          </div>
          <p class="text-xs leading-relaxed text-gray-600">{{ rec.details }}</p>
        </article>
      </div>
    </div>
  </section>
</template>
