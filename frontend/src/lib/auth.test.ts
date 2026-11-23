import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from './auth'

const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
  status, headers: { 'Content-Type': 'application/json' },
})
const csrf = { headerName: 'X-CSRF-TOKEN', token: 'test-csrf' }
const user = { id: 1, loginId: 'user_1' }
const fetchMock = vi.fn<typeof fetch>()

beforeEach(() => { fetchMock.mockReset(); vi.stubGlobal('fetch', fetchMock) })
afterEach(() => vi.unstubAllGlobals())

describe('세션 인증 API', () => {
  it('같은 출처의 쿠키를 보내 현재 사용자를 조회한다', async () => {
    fetchMock.mockResolvedValueOnce(json(user))
    expect(await authApi.me()).toEqual(user)
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/me', expect.objectContaining({ credentials: 'same-origin', cache: 'no-store' }))
  })

  it('로그인 전에 CSRF를 받고 서버가 알려준 헤더와 JSON을 보낸다', async () => {
    fetchMock.mockResolvedValueOnce(json(csrf)).mockResolvedValueOnce(json(user))
    await authApi.login({ loginId: 'user_1', password: 'a long passphrase' })
    expect(fetchMock.mock.calls.map(call => call[0])).toEqual(['/api/auth/csrf', '/api/auth/login'])
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: 'POST', credentials: 'same-origin',
      headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'test-csrf', 'Content-Type': 'application/json' }),
      body: JSON.stringify({ loginId: 'user_1', password: 'a long passphrase' }),
    }))
  })

  it('본문 없는 회원가입 201을 정상 처리한다', async () => {
    fetchMock.mockResolvedValueOnce(json(csrf)).mockResolvedValueOnce(new Response(null, { status: 201 }))
    await expect(authApi.signup({ loginId: 'user_1', password: 'a long passphrase', name: '지헌', age: 25 })).resolves.toBeUndefined()
  })

  it('로그인 후 로그아웃에도 새 CSRF 토큰을 받는다', async () => {
    fetchMock.mockResolvedValueOnce(json(csrf)).mockResolvedValueOnce(json(user))
      .mockResolvedValueOnce(json({ ...csrf, token: 'fresh-token' }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    await authApi.login({ loginId: 'user_1', password: 'a long passphrase' })
    await authApi.logout()
    expect(fetchMock.mock.calls[3][1]?.headers).toEqual(expect.objectContaining({ 'X-CSRF-TOKEN': 'fresh-token' }))
  })

  it('만료된 CSRF는 새 토큰으로 한 번만 재시도한다', async () => {
    fetchMock.mockResolvedValueOnce(json(csrf)).mockResolvedValueOnce(json({ message: 'forbidden' }, 403))
      .mockResolvedValueOnce(json({ ...csrf, token: 'renewed' }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    await expect(authApi.logout()).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenCalledTimes(4)
    expect(fetchMock.mock.calls[3][1]?.headers).toEqual(expect.objectContaining({ 'X-CSRF-TOKEN': 'renewed' }))
  })

  it('403이 계속돼도 무한 재시도하지 않는다', async () => {
    fetchMock.mockResolvedValueOnce(json(csrf)).mockResolvedValueOnce(json({}, 403))
      .mockResolvedValueOnce(json(csrf)).mockResolvedValueOnce(json({}, 403))
    await expect(authApi.logout()).rejects.toMatchObject({ status: 403 })
    expect(fetchMock).toHaveBeenCalledTimes(4)
  })

  it.each([400, 401, 409])('%s 오류의 변경 요청을 반복하지 않는다', async status => {
    fetchMock.mockResolvedValueOnce(json(csrf)).mockResolvedValueOnce(json({ message: '입력값을 확인해 주세요.' }, status))
    await expect(authApi.login({ loginId: 'user_1', password: 'wrong' }))
      .rejects.toMatchObject({ status, message: '입력값을 확인해 주세요.' })
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('연결 실패를 이해할 수 있는 메시지로 처리한다', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))
    await expect(authApi.me()).rejects.toMatchObject({ status: 0, message: expect.stringContaining('서버에 연결할 수 없어요') })
  })

  it('서버의 내부 오류 메시지를 화면에 노출하지 않는다', async () => {
    fetchMock.mockResolvedValueOnce(json({ message: 'private database internals' }, 500))
    await expect(authApi.me()).rejects.toMatchObject({ status: 500, message: expect.not.stringContaining('database') })
  })

  it('Vite 프록시의 HTML 오류도 처리한다', async () => {
    fetchMock.mockResolvedValueOnce(new Response('<html>bad gateway</html>', { status: 502 }))
    await expect(authApi.me()).rejects.toMatchObject({ status: 502, message: expect.stringContaining('인증 서버에 연결할 수 없어요') })
  })

  it('잘못된 인증 응답을 로그인 성공으로 표시하지 않는다', async () => {
    fetchMock.mockResolvedValueOnce(json({ message: 'not a user' }))
    await expect(authApi.me()).rejects.toMatchObject({ status: 0 })
  })
})
