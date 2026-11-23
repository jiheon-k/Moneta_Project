// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from './App'
import { ApiError, authApi } from './lib/auth'
import { ledgerApi } from './lib/ledger'

vi.mock('./lib/ledger', async importOriginal => {
  const original = await importOriginal<typeof import('./lib/ledger')>()
  return { ...original, ledgerApi: { ...original.ledgerApi, accounts: vi.fn(), categories: vi.fn() } }
})

vi.mock('./lib/auth', async importOriginal => {
  const original = await importOriginal<typeof import('./lib/auth')>()
  return { ...original, authApi: { me: vi.fn(), login: vi.fn(), signup: vi.fn(), logout: vi.fn() } }
})

const currentUser = { id: 1, loginId: 'user_1' }
const me = vi.mocked(authApi.me)
const login = vi.mocked(authApi.login)
const signup = vi.mocked(authApi.signup)
const logout = vi.mocked(authApi.logout)

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(ledgerApi.accounts).mockResolvedValue([])
  vi.mocked(ledgerApi.categories).mockResolvedValue([])
  me.mockRejectedValue(new ApiError(401, '로그인이 필요합니다.'))
  vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => setTimeout(() => callback(0), 0))
})
afterEach(() => { cleanup(); vi.unstubAllGlobals() })

async function loginForm() {
  render(<App />)
  return screen.findByRole('form', { name: '로그인 양식' })
}

async function signupForm() {
  const user = userEvent.setup()
  await loginForm()
  await user.click(screen.getAllByRole('button', { name: '회원가입' })[0])
  return { user, form: screen.getByRole('form', { name: '회원가입 양식' }) }
}

async function fillSignup(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('이름'), '지헌')
  await user.type(screen.getByLabelText('나이'), '25')
  await user.type(screen.getByLabelText('로그인 아이디'), 'user_1')
  await user.type(screen.getByLabelText('비밀번호', { exact: true }), 'a long passphrase')
  await user.type(screen.getByLabelText('비밀번호 확인', { exact: true }), 'a long passphrase')
}

describe('인증 화면', () => {
  it('로그인 성공 후 현재 사용자 화면을 표시한다', async () => {
    const user = userEvent.setup()
    const form = await loginForm()
    login.mockResolvedValue(currentUser)
    await user.type(screen.getByLabelText('로그인 아이디'), 'user_1')
    await user.type(screen.getByLabelText('비밀번호', { exact: true }), 'a long passphrase')
    await user.click(within(form).getByRole('button', { name: '로그인' }))
    await screen.findByRole('heading', { name: /user_1님/ })
    expect(login).toHaveBeenCalledWith({ loginId: 'user_1', password: 'a long passphrase' })
    expect(screen.queryByRole('form', { name: '로그인 양식' })).toBeNull()
    expect(localStorage.length).toBe(0)
  })

  it('가입 성공 후 로그인으로 전환하고 비밀번호는 지운다', async () => {
    const { user, form } = await signupForm()
    signup.mockResolvedValue(undefined)
    await fillSignup(user)
    await user.click(within(form).getByRole('button', { name: 'Moneta 시작하기' }))
    await screen.findByText('가입이 완료되었어요. 이제 로그인해 주세요.')
    expect(signup).toHaveBeenCalledWith({ loginId: 'user_1', password: 'a long passphrase', name: '지헌', age: 25 })
    expect((screen.getByLabelText('로그인 아이디') as HTMLInputElement).value).toBe('user_1')
    expect((screen.getByLabelText('비밀번호', { exact: true }) as HTMLInputElement).value).toBe('')
  })

  it('가입 확인 비밀번호가 다르면 서버로 보내지 않는다', async () => {
    const { user, form } = await signupForm()
    await fillSignup(user)
    await user.clear(screen.getByLabelText('비밀번호 확인', { exact: true }))
    await user.type(screen.getByLabelText('비밀번호 확인', { exact: true }), 'different')
    await user.click(within(form).getByRole('button', { name: 'Moneta 시작하기' }))
    await screen.findByText('비밀번호가 일치하지 않아요.')
    expect(signup).not.toHaveBeenCalled()
    expect(screen.getByLabelText('비밀번호 확인', { exact: true }).getAttribute('aria-invalid')).toBe('true')
  })

  it('중복 아이디를 필드 오류로 표시한다', async () => {
    const { user, form } = await signupForm()
    signup.mockRejectedValue(new ApiError(409, '이미 사용 중인 아이디입니다.'))
    await fillSignup(user)
    await user.click(within(form).getByRole('button', { name: 'Moneta 시작하기' }))
    await waitFor(() => expect(screen.getByLabelText('로그인 아이디').getAttribute('aria-invalid')).toBe('true'))
    expect(screen.getByRole('alert').textContent).toContain('이미 사용 중인 아이디입니다.')
  })

  it('로그인 실패를 표시하고 로그인 화면에 머문다', async () => {
    const user = userEvent.setup()
    const form = await loginForm()
    login.mockRejectedValue(new ApiError(401, '아이디 또는 비밀번호가 올바르지 않습니다.'))
    await user.type(screen.getByLabelText('로그인 아이디'), 'user_1')
    await user.type(screen.getByLabelText('비밀번호', { exact: true }), 'wrong password')
    await user.click(within(form).getByRole('button', { name: '로그인' }))
    expect((await screen.findByRole('alert')).textContent).toContain('아이디 또는 비밀번호가 올바르지 않습니다.')
    expect(screen.queryByRole('button', { name: '로그아웃' })).toBeNull()
  })

  it('새로고침 시 세션을 복원하고 로그아웃할 수 있다', async () => {
    const user = userEvent.setup()
    me.mockResolvedValue(currentUser)
    logout.mockResolvedValue(undefined)
    render(<App />)
    await screen.findByRole('heading', { name: /user_1님/ })
    await user.click(screen.getByRole('button', { name: '로그아웃' }))
    await screen.findByText('안전하게 로그아웃했어요. 다음에 또 만나요.')
    expect(logout).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('form', { name: '로그인 양식' })).toBeTruthy()
  })

  it('로그아웃 실패를 성공으로 표시하지 않는다', async () => {
    const user = userEvent.setup()
    me.mockResolvedValue(currentUser)
    logout.mockRejectedValue(new ApiError(0, '서버에 연결할 수 없어요.'))
    render(<App />)
    await screen.findByRole('heading', { name: /user_1님/ })
    await user.click(screen.getByRole('button', { name: '로그아웃' }))
    await screen.findByRole('alert')
    expect(screen.getByRole('heading', { name: /user_1님/ })).toBeTruthy()
    expect(screen.queryByText('안전하게 로그아웃했어요. 다음에 또 만나요.')).toBeNull()
  })

  it('초기 연결 실패에서 다시 연결할 수 있다', async () => {
    const user = userEvent.setup()
    me.mockRejectedValueOnce(new ApiError(0, '서버에 연결할 수 없어요.'))
    render(<App />)
    await screen.findByRole('alert')
    me.mockResolvedValue(currentUser)
    await user.click(screen.getByRole('button', { name: '다시 연결' }))
    await screen.findByRole('heading', { name: /user_1님/ })
  })

  it('비밀번호 표시 전환이 값을 바꾸지 않는다', async () => {
    const user = userEvent.setup()
    await loginForm()
    const input = screen.getByLabelText('비밀번호', { exact: true }) as HTMLInputElement
    await user.type(input, 'my password')
    expect(input.type).toBe('password')
    await user.click(screen.getByRole('button', { name: '비밀번호 표시' }))
    expect(input.type).toBe('text')
    expect(input.value).toBe('my password')
    await user.click(screen.getByRole('button', { name: '비밀번호 숨기기' }))
    expect(input.type).toBe('password')
  })
})
