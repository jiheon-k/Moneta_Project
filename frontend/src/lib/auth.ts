export type AuthUser = { id: number; loginId: string }
export type LoginInput = { loginId: string; password: string }
export type SignupInput = LoginInput & { name: string; age: number }
type CsrfToken = { headerName: string; token: string }

export class ApiError extends Error {
  readonly status: number
  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export async function request<T>(path: string, options: RequestInit = {}, allowEmpty = false): Promise<T> {
  const timeout = AbortSignal.timeout(10_000)
  const signal = options.signal ? AbortSignal.any([options.signal, timeout]) : timeout
  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      ...options,
      credentials: 'same-origin',
      cache: 'no-store',
      signal,
      headers: { Accept: 'application/json', ...options.headers },
    })
  } catch (error) {
    if (options.signal?.aborted) throw error
    throw new ApiError(0, timeout.aborted
      ? '응답이 늦어지고 있어요. 잠시 후 다시 시도해 주세요.'
      : '서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.')
  }

  if (response.status === 204) return undefined as T
  const text = await response.text()
  if (response.ok && response.status === 201 && !text) return undefined as T
  let body: unknown
  try { body = text ? JSON.parse(text) : undefined } catch { body = undefined }

  if (!response.ok) {
    const serverMessage = body && typeof body === 'object' && 'message' in body
      && typeof body.message === 'string' ? body.message : undefined
    if ([502, 503, 504].includes(response.status) && !serverMessage) {
      throw new ApiError(response.status, '인증 서버에 연결할 수 없어요. 백엔드가 실행 중인지 확인해 주세요.')
    }
    const fallback = response.status === 401 ? '로그인이 필요합니다.'
      : response.status === 403 ? '요청을 확인할 수 없어요. 다시 시도해 주세요.'
        : '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.'
    throw new ApiError(response.status, response.status >= 500 ? fallback : serverMessage || fallback)
  }
  if (allowEmpty && !text) return undefined as T
  if (body === undefined) throw new ApiError(0, '서버 응답을 확인할 수 없어요. 잠시 후 다시 시도해 주세요.')
  return body as T
}

export async function mutate<T>(path: string, method: 'POST' | 'PATCH' | 'DELETE', body?: unknown): Promise<T> {
  // 매 변경 요청마다 현재 세션의 토큰을 읽는다. 로그인/로그아웃 후의 토큰 갱신도 반영된다.
  const send = async () => {
    const csrf = await request<CsrfToken>('/auth/csrf')
    if (typeof csrf.headerName !== 'string' || typeof csrf.token !== 'string') {
      throw new ApiError(0, '안전한 연결을 준비하지 못했어요. 다시 시도해 주세요.')
    }
    return request<T>(path, {
      method,
      headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
      body: body === undefined ? undefined : JSON.stringify(body),
    }, true)
  }
  try { return await send() } catch (error) {
    // 다른 탭의 로그인/로그아웃으로 토큰이 갱신된 경우 한 번만 새 토큰으로 재시도한다.
    if (error instanceof ApiError && error.status === 403) return send()
    throw error
  }
}

const post = <T>(path: string, body?: unknown) => mutate<T>(path, 'POST', body)

function asUser(value: AuthUser): AuthUser {
  if (!value || !Number.isSafeInteger(value.id) || value.id < 1
    || typeof value.loginId !== 'string' || !value.loginId) {
    throw new ApiError(0, '사용자 정보를 확인할 수 없어요. 다시 로그인해 주세요.')
  }
  return value
}

export const authApi = {
  me: (signal?: AbortSignal) => request<AuthUser>('/auth/me', { signal }).then(asUser),
  signup: (input: SignupInput) => post<void>('/auth/signup', input),
  login: (input: LoginInput) => post<AuthUser>('/auth/login', input).then(asUser),
  logout: () => post<void>('/auth/logout'),
}
