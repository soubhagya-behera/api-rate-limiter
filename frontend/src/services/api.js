const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function fetchBackend(path) {
  try {
    return await fetch(`${API_BASE_URL}${path}`)
  } catch {
    throw new Error('Unable to connect to backend.')
  }
}

export async function getRateLimitStatus() {
  const response = await fetchBackend('/api/rate-limit/status')
  if (!response.ok) {
    throw new Error('Failed to fetch rate-limit status.')
  }
  try {
    return await response.json()
  } catch {
    throw new Error('Failed to fetch rate-limit status.')
  }
}

export async function sendTestRequest() {
  const response = await fetchBackend('/api/test')
  let body
  try {
    body = await response.json()
  } catch {
    body = null
  }
  return { status: response.status, body }
}
