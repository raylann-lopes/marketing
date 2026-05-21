<script setup lang="ts">
import { TrendingUp, TrendingDown, DollarSign } from 'lucide-vue-next'
import Card from '@/components/ui/Card.vue'

interface Props {
  incomeTotal: string
  expenseTotal: string
  netProfit: string
}

defineProps<Props>()

const statsConfig = [
  { key: 'incomeTotal', label: 'Receita do Mês', sub: 'Total bruto', icon: TrendingUp, color: 'text-green-600', bg: 'bg-green-50' },
  { key: 'expenseTotal', label: 'Despesas', sub: 'Total de saídas', icon: TrendingDown, color: 'text-red-500', bg: 'bg-red-50' },
  { key: 'netProfit', label: 'Lucro Líquido', sub: 'Margem real', icon: DollarSign, color: 'text-purple-600', bg: 'bg-purple-50' },
]
</script>

<template>
  <div class="grid grid-cols-3 gap-4 mb-6">
    <Card v-for="stat in statsConfig" :key="stat.label" class="p-5 flex items-center gap-4">
      <div :class="['w-12 h-12 rounded-xl flex items-center justify-center', stat.bg]">
        <component :is="stat.icon" :class="['w-6 h-6', stat.color]" />
      </div>
      <div>
        <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">{{ stat.label }}</p>
        <p class="text-2xl font-bold text-gray-900 mt-0.5">
          <template v-if="stat.key === 'incomeTotal'">{{ incomeTotal }}</template>
          <template v-else-if="stat.key === 'expenseTotal'">{{ expenseTotal }}</template>
          <template v-else>{{ netProfit }}</template>
        </p>
        <p class="text-xs text-gray-400 mt-0.5">{{ stat.sub }}</p>
      </div>
    </Card>
  </div>
</template>
