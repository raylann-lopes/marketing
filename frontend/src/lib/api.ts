export const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

let currentUserId: number | null = null

export function getCurrentUserId(): number | null {
  return currentUserId
}

export function setCurrentUserId(id: number | null) {
  currentUserId = id
}

function clearSessionAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  setCurrentUserId(null)
  window.location.href = '/login'
}

function isAuthenticationFailure(errorData: { error?: string; message?: string }) {
  const error = errorData.error ?? ''
  const message = errorData.message ?? ''

  return (
    error === 'Token invalido ou expirado' ||
    message.includes('Sessão inválida') ||
    message.includes('token') ||
    message.includes('Token')
  )
}

export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...options.headers,
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))

    if (response.status === 401) {
      if (isAuthenticationFailure(errorData)) {
        clearSessionAndRedirect()
      }
      throw new Error(errorData.message || errorData.error || 'Não foi possível concluir a ação.')
    }
    if (response.status === 403) {
      throw new Error('Acesso negado. Esta ação requer permissão de administrador.')
    }
    throw new Error(errorData.message || `Erro ${response.status}: Ação não permitida ou dados inválidos`)
  }

  const contentType = response.headers.get('content-type')
  if (response.status === 204 || !contentType || !contentType.includes('application/json')) {
    return undefined as unknown as T
  }

  return response.json()
}
