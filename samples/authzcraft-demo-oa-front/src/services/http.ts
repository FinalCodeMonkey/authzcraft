export type ApiResponse<T> = {
  code: string
  message: string
  data: T
}

export function authHeaders(selectedPersonaKey: string): Record<string, string> {
  return {
    'Content-Type': 'application/json',
    'X-AuthzCraft-Requester-Kind': 'USER',
    'X-AuthzCraft-Requester-Key': selectedPersonaKey,
  }
}

export async function postJsonWithAuth<T>(url: string, body: unknown, selectedPersonaKey: string): Promise<T> {
  const response = await fetch(url, {
    method: 'POST',
    headers: authHeaders(selectedPersonaKey),
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new Error(`${url} returned HTTP ${response.status}`)
  }
  return await response.json() as T
}

export async function postJson<T>(url: string, body: unknown): Promise<T> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new Error(`${url} returned HTTP ${response.status}`)
  }
  return await response.json() as T
}
