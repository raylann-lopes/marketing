import { onMounted, onUnmounted, ref } from 'vue'

// Abaixo do breakpoint md (768px) do Tailwind, mesmo usado no resto do projeto
const MOBILE_QUERY = '(max-width: 767px)'

export function useIsMobile() {
  const isMobile = ref(
    typeof window !== 'undefined' ? window.matchMedia(MOBILE_QUERY).matches : false,
  )

  let mql: MediaQueryList | null = null
  function handleChange(e: MediaQueryListEvent) {
    isMobile.value = e.matches
  }

  onMounted(() => {
    mql = window.matchMedia(MOBILE_QUERY)
    isMobile.value = mql.matches
    mql.addEventListener('change', handleChange)
  })

  onUnmounted(() => {
    mql?.removeEventListener('change', handleChange)
  })

  return { isMobile }
}
