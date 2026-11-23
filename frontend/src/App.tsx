import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Dashboard } from './components/Dashboard'
import { BrandPanel } from './components/BrandPanel'
import { Icon } from './components/Icon'
import { ApiError, authApi } from './lib/auth'
import type { AuthUser } from './lib/auth'
import { validate } from './lib/validation'
import type { AuthDraft, AuthMode, FormErrors } from './lib/validation'

type Notice = { kind: 'error' | 'success' | 'info'; text: string }
type Session = { status: 'checking' | 'anonymous' } | { status: 'authenticated'; user: AuthUser }
const emptyDraft: AuthDraft = { loginId: '', password: '', confirmation: '', name: '', age: '' }

function Field({ name, label, value, onChange, error, hint, type = 'text', autoComplete, placeholder,
  maxLength, reveal, onReveal, disabled }: {
  name: keyof AuthDraft; label: string; value: string; onChange: (value: string) => void;
  error?: string; hint?: string; type?: 'text' | 'password' | 'number'; autoComplete?: string;
  placeholder?: string; maxLength?: number; reveal?: boolean; onReveal?: () => void; disabled: boolean;
}) {
  const id = `auth-${name}`
  return <div className={`field ${error ? 'field-invalid' : ''}`}>
    <label htmlFor={id}>{label}</label>
    <div className="input-wrap">
      <input id={id} name={name} type={type === 'password' && reveal ? 'text' : type}
        value={value} onChange={event => onChange(event.target.value)} autoComplete={autoComplete}
        placeholder={placeholder} maxLength={maxLength} disabled={disabled}
        inputMode={type === 'number' ? 'numeric' : undefined}
        min={type === 'number' ? 1 : undefined} max={type === 'number' ? 150 : undefined}
        step={type === 'number' ? 1 : undefined}
        aria-invalid={Boolean(error)} aria-describedby={error || hint ? `${id}-description` : undefined}
        className={onReveal ? 'has-reveal' : undefined} />
      {onReveal && <button type="button" className="reveal-button" onClick={onReveal}
        aria-label={`${label} ${reveal ? '숨기기' : '표시'}`} aria-pressed={reveal} disabled={disabled}>
        <Icon name={reveal ? 'eye-off' : 'eye'} />
      </button>}
    </div>
    {(error || hint) && <p id={`${id}-description`} className={error ? 'field-error' : 'field-hint'}>{error || hint}</p>}
  </div>
}

function NoticeBox({ notice, retry }: { notice: Notice; retry?: () => void }) {
  return <div className={`notice notice-${notice.kind}`} role={notice.kind === 'error' ? 'alert' : 'status'}>
    <Icon name={notice.kind === 'success' ? 'check' : notice.kind === 'error' ? 'alert' : 'lock'} />
    <span>{notice.text}</span>
    {retry && <button type="button" onClick={retry} className="notice-retry">다시 연결</button>}
  </div>
}

export default function App() {
  const [mode, setMode] = useState<AuthMode>('login')
  const [draft, setDraft] = useState<AuthDraft>({ ...emptyDraft })
  const [errors, setErrors] = useState<FormErrors>({})
  const [notice, setNotice] = useState<Notice | null>(null)
  const [session, setSession] = useState<Session>({ status: 'checking' })
  const [busy, setBusy] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmation, setShowConfirmation] = useState(false)
  const [connectionIssue, setConnectionIssue] = useState(false)
  const [checkVersion, setCheckVersion] = useState(0)
  const formRef = useRef<HTMLFormElement>(null)
  const headingRef = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    const controller = new AbortController()
    authApi.me(controller.signal).then(user => {
      setSession({ status: 'authenticated', user })
      setConnectionIssue(false)
    }).catch(error => {
      if (controller.signal.aborted) return
      setSession({ status: 'anonymous' })
      if (error instanceof ApiError && error.status === 401) {
        setConnectionIssue(false)
      } else {
        setConnectionIssue(true)
        setNotice({ kind: 'error', text: error instanceof Error ? error.message : '연결을 확인해 주세요.' })
      }
    })
    return () => controller.abort()
  }, [checkVersion])

  function switchMode(next: AuthMode) {
    if (busy || next === mode) return
    setMode(next)
    setDraft(previous => ({ ...emptyDraft, loginId: previous.loginId }))
    setErrors({})
    setShowPassword(false)
    setShowConfirmation(false)
    if (!connectionIssue) setNotice(null)
  }

  function update(name: keyof AuthDraft, value: string) {
    setDraft(previous => ({ ...previous, [name]: value }))
    setErrors(previous => ({ ...previous, [name]: undefined }))
    if (notice && !connectionIssue) setNotice(null)
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy || session.status === 'checking') return
    const nextErrors = validate(draft, mode)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) {
      const firstInvalid = Object.keys(nextErrors)[0]
      requestAnimationFrame(() => (formRef.current?.elements.namedItem(firstInvalid) as HTMLElement | null)?.focus())
      return
    }
    setBusy(true)
    setNotice(null)
    try {
      if (mode === 'signup') {
        await authApi.signup({ loginId: draft.loginId, password: draft.password, name: draft.name, age: Number(draft.age) })
        setMode('login')
        setDraft({ ...emptyDraft, loginId: draft.loginId })
        setShowPassword(false)
        setShowConfirmation(false)
        setNotice({ kind: 'success', text: '가입이 완료되었어요. 이제 로그인해 주세요.' })
        requestAnimationFrame(() => document.getElementById('auth-password')?.focus())
      } else {
        const user = await authApi.login({ loginId: draft.loginId, password: draft.password })
        setSession({ status: 'authenticated', user })
        setDraft({ ...emptyDraft })
        setNotice(null)
        requestAnimationFrame(() => headingRef.current?.focus())
      }
      setConnectionIssue(false)
    } catch (error) {
      const apiError = error instanceof ApiError ? error : new ApiError(0, '요청을 처리하지 못했어요. 다시 시도해 주세요.')
      setConnectionIssue(apiError.status === 0 || apiError.status >= 500)
      setNotice({ kind: 'error', text: apiError.message })
      if (apiError.status === 409) {
        setErrors({ loginId: '이미 사용 중인 아이디입니다.' })
        document.getElementById('auth-loginId')?.focus()
      }
    } finally { setBusy(false) }
  }

  async function logout() {
    if (busy) return
    setBusy(true)
    setNotice(null)
    try {
      await authApi.logout()
      setSession({ status: 'anonymous' })
      setMode('login')
      setDraft({ ...emptyDraft })
      setErrors({})
      setConnectionIssue(false)
      setNotice({ kind: 'success', text: '안전하게 로그아웃했어요. 다음에 또 만나요.' })
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setSession({ status: 'anonymous' })
        setNotice({ kind: 'info', text: '로그인이 만료되었어요. 다시 로그인해 주세요.' })
      } else {
        setNotice({ kind: 'error', text: error instanceof Error ? error.message : '로그아웃하지 못했어요. 다시 시도해 주세요.' })
      }
    } finally { setBusy(false) }
  }

  function recheck() {
    if (busy) return
    setNotice(null)
    setSession({ status: 'checking' })
    setCheckVersion(previous => previous + 1)
  }

  const expireSession = useCallback(() => {
    setSession({ status: 'anonymous' })
    setMode('login')
    setDraft({ ...emptyDraft })
    setNotice({ kind: 'info', text: '로그인이 만료되었어요. 다시 로그인해 주세요.' })
  }, [])

  if (session.status === 'authenticated') {
    return <Dashboard key={session.user.id} user={session.user} onLogout={logout} logoutBusy={busy}
      logoutNotice={notice?.text} onExpired={expireSession} />
  }

  const checking = session.status === 'checking'
  const disabled = busy || checking
  const isSignup = mode === 'signup'
  const currentYear = new Date().getFullYear()

  return <main className="app-shell">
    <BrandPanel />
    <section className="account-panel" aria-label="Moneta 계정">
      <div className="account-top"><span className="top-caption">A LITTLE MORE PEACE OF MIND</span><span className="account-tag"><span />나를 위한 가계부</span></div>
      <div className={`account-content ${isSignup && session.status === 'anonymous' ? 'signup-content' : ''}`}>
        {import.meta.env.MODE === 'demo' && <p className="notice notice-info">실험용 가계부 · 아이디: demo_user / 비밀번호: moneta-demo-1234</p>}
        {checking ? <div className="checking-state" role="status"><span className="spinner" /><h2>반가워요, 잠시만 기다려 주세요.</h2><p>로그인 상태를 확인하고 있어요.</p></div>
          : <>
              <div className="auth-switch" role="group" aria-label="로그인 또는 회원가입 선택"><button type="button" aria-pressed={!isSignup} className={!isSignup ? 'active' : ''} disabled={disabled} onClick={() => switchMode('login')}>로그인</button><button type="button" aria-pressed={isSignup} className={isSignup ? 'active' : ''} disabled={disabled} onClick={() => switchMode('signup')}>회원가입</button></div>
              <header className="form-heading"><p className="eyebrow">{isSignup ? 'A FRESH START' : 'GOOD TO SEE YOU AGAIN'}</p><h2>{isSignup ? '나의 첫 기록을 시작해요.' : '다시 만나 반가워요.'}</h2><p className="section-description">{isSignup ? 'Moneta와 함께 나다운 돈 관리를 시작하세요.' : '당신의 일상에 작은 여유를 더해 보세요.'}</p></header>
              {notice && <NoticeBox notice={notice} retry={connectionIssue ? recheck : undefined} />}
              <form ref={formRef} onSubmit={submit} noValidate aria-label={isSignup ? '회원가입 양식' : '로그인 양식'} aria-busy={busy}>
                {isSignup && <div className="field-row"><Field name="name" label="이름" value={draft.name} onChange={v => update('name', v)} error={errors.name} autoComplete="name" placeholder="이름" maxLength={50} disabled={disabled} /><Field name="age" label="나이" type="number" value={draft.age} onChange={v => update('age', v)} error={errors.age} placeholder="만 나이" disabled={disabled} /></div>}
                <Field name="loginId" label="로그인 아이디" value={draft.loginId} onChange={v => update('loginId', v)} error={errors.loginId} hint={isSignup ? '영문·숫자·밑줄로 4~20자' : undefined} autoComplete="username" placeholder={isSignup ? '사용할 아이디를 입력해 주세요' : '아이디를 입력해 주세요'} maxLength={isSignup ? 20 : 50} disabled={disabled} />
                <Field name="password" label="비밀번호" type="password" value={draft.password} onChange={v => update('password', v)} error={errors.password} hint={isSignup ? '15~64자 · 띄어쓰기와 특수문자도 사용할 수 있어요' : undefined} autoComplete={isSignup ? 'new-password' : 'current-password'} placeholder={isSignup ? '길고 기억하기 쉬운 비밀번호' : '비밀번호를 입력해 주세요'} maxLength={isSignup ? 64 : 1024} reveal={showPassword} onReveal={() => setShowPassword(v => !v)} disabled={disabled} />
                {isSignup && <Field name="confirmation" label="비밀번호 확인" type="password" value={draft.confirmation} onChange={v => update('confirmation', v)} error={errors.confirmation} autoComplete="new-password" placeholder="비밀번호를 한 번 더 입력해 주세요" maxLength={64} reveal={showConfirmation} onReveal={() => setShowConfirmation(v => !v)} disabled={disabled} />}
                <button className="primary-button" type="submit" disabled={disabled}><span>{busy ? (isSignup ? '가입하는 중이에요' : '로그인하는 중이에요') : (isSignup ? 'Moneta 시작하기' : '로그인')}</span>{busy ? <span className="spinner small" /> : <Icon name="arrow" />}</button>
              </form>
              <p className="switch-prompt">{isSignup ? '이미 Moneta와 함께하고 있나요?' : 'Moneta가 처음이신가요?'}<button type="button" disabled={disabled} onClick={() => switchMode(isSignup ? 'login' : 'signup')}>{isSignup ? '로그인' : '회원가입'}<span aria-hidden="true">↗</span></button></p>
              <div className="account-reassurance"><Icon name="lock" /><span>나만의 기록을 위한, 나만의 공간.</span></div>
            </>}
      </div>
      <footer className="account-footer"><span>작은 기록이 만드는 더 나은 일상</span><span>© {currentYear} Moneta</span></footer>
    </section>
  </main>
}
