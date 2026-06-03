<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { UserPlus, Plus } from 'lucide-vue-next'
import AppLayout from '@/components/layout/AppLayout.vue'
import Button from '@/components/ui/Button.vue'

import StatCards from '@/components/dashboard/StatCards.vue'
import RecentPosts from '@/components/dashboard/RecentPosts.vue'
import WeeklySchedule from '@/components/dashboard/WeeklySchedule.vue'
import ClientHealth from '@/components/dashboard/ClientHealth.vue'

import { clientService, type Client } from '@/services/clientService'
import { postService, type Post } from '@/services/postService'
import { userService } from '@/services/userService'

const router = useRouter()
const today = new Date()
const userName = ref('')

const totalClients = ref(0)
const postsThisMonth = ref(0)
const pendingPosts = ref(0)
const publishedPosts = ref(0)
const allPosts = ref<Post[]>([])
const allClients = ref<Client[]>([])
const recentPosts = ref<Post[]>([])

const selectedWeekStart = ref(
  (() => {
    const d = new Date(today)
    d.setDate(d.getDate() - (d.getDay() === 0 ? 6 : d.getDay() - 1))
    return d
  })(),
)

const selectedWeekDate = ref(new Date(today))

function isSameDay(a: Date, b: Date) {
  return (
    a.getDate() === b.getDate() &&
    a.getMonth() === b.getMonth() &&
    a.getFullYear() === b.getFullYear()
  )
}

function selectWeekDay(date: Date) {
  selectedWeekDate.value = date
}

function prevWeek() {
  selectedWeekStart.value = new Date(selectedWeekStart.value)
  selectedWeekStart.value.setDate(selectedWeekStart.value.getDate() - 7)
}

function nextWeek() {
  selectedWeekStart.value = new Date(selectedWeekStart.value)
  selectedWeekStart.value.setDate(selectedWeekStart.value.getDate() + 7)
}

const selectedDayPosts = computed(() =>
  allPosts.value.filter((p) => {
    if (!p.scheduledAt) return false
    return isSameDay(new Date(p.scheduledAt), selectedWeekDate.value)
  }),
)

async function fetchDashboardData() {
  try {
    const [clientsData, postsData] = await Promise.all([
      clientService.getAll(),
      postService.getAll(),
    ])

    const clients = Array.isArray(clientsData)
      ? clientsData
      : (clientsData as { data: Client[] }).data || []
    const posts = Array.isArray(postsData) ? postsData : (postsData as { data: Post[] }).data || []

    allClients.value = clients
    allPosts.value = posts

    totalClients.value = clients.filter((c: Client) => c.status === 'ACTIVE').length

    const monthPosts = posts.filter((p: Post) => {
      if (!p.scheduledAt) return false
      const d = new Date(p.scheduledAt)
      return d.getMonth() === today.getMonth() && d.getFullYear() === today.getFullYear()
    })

    postsThisMonth.value = monthPosts.length
    pendingPosts.value = posts.filter(
      (p: Post) => p.status !== 'PUBLISHED' && p.status !== 'FINISHED',
    ).length
    publishedPosts.value = posts.filter((p: Post) => p.status === 'PUBLISHED').length

    recentPosts.value = [...posts]
      .sort((a, b) => new Date(b.scheduledAt).getTime() - new Date(a.scheduledAt).getTime())
      .slice(0, 8)
  } catch (error) {
    console.error('Error fetching dashboard data:', error)
  }
}

onMounted(async () => {
  fetchDashboardData()
  try {
    const me = await userService.getMe()
    userName.value = me.name?.split(' ')[0] || 'North'
  } catch {
    userName.value = 'North'
  }
})
</script>

<template>
  <AppLayout>
    <div class="flex items-start justify-between mb-6">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">Olá {{ userName }}.</h1>
        <p class="text-gray-500 mt-1">Aqui está o que está acontecendo na empresa hoje.</p>
      </div>
      <div class="flex items-center gap-3">
        <Button variant="outline" class="gap-2" @click="router.push('/clients')">
          <UserPlus class="w-4 h-4" />
          Novo Cliente
        </Button>
        <Button class="gap-2" @click="router.push('/board')">
          <Plus class="w-4 h-4" />
          Novo Post
        </Button>
      </div>
    </div>

    <div class="grid grid-cols-3 gap-6">
      <div class="col-span-2 space-y-6">
        <StatCards
          :total-clients="totalClients"
          :posts-this-month="postsThisMonth"
          :pending-posts="pendingPosts"
          :published-posts="publishedPosts"
        />

        <RecentPosts :posts="recentPosts" :clients="allClients" />
      </div>

      <div class="space-y-6">
        <WeeklySchedule
          :selected-week-start="selectedWeekStart"
          :selected-week-date="selectedWeekDate"
          :today="today"
          :selected-day-posts="selectedDayPosts"
          @prev-week="prevWeek"
          @next-week="nextWeek"
          @select-day="selectWeekDay"
        />

        <ClientHealth :clients="allClients" />
      </div>
    </div>
  </AppLayout>
</template>
