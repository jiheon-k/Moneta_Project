import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../lib/auth'
import type { AuthUser } from '../lib/auth'
import { ledgerApi } from '../lib/ledger'
import type { Account, AccountSummary, AccountType, Category, TransactionList } from '../lib/ledger'

const accountTypes: Record<AccountType, string> = { LIVING: '생활비', SAVING: '저축', INVESTMENT: '투자' }
const transactionTypes = { INCOME: '수입', EXPENSE: '지출', TRANSFER: '이체' }
const won = (value: number) => `${value.toLocaleString('ko-KR')}원`
type Message = { error: boolean; text: string }

export function Dashboard({ user, onLogout, logoutBusy, logoutNotice, onExpired }: {
  user: AuthUser; onLogout: () => void; logoutBusy: boolean; logoutNotice?: string; onExpired: () => void;
}) {
  const [tab, setTab] = useState<'accounts' | 'categories'>('accounts')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<Message | null>(null)
  const [account, setAccount] = useState<Account | null>(null)
  const [accountId, setAccountId] = useState('')
  const [accounts, setAccounts] = useState<AccountSummary[]>([])
  const [accountsLoading, setAccountsLoading] = useState(true)
  const [accountsError, setAccountsError] = useState('')
  const [accountsVersion, setAccountsVersion] = useState(0)
  const [expenseAmount, setExpenseAmount] = useState('')
  const [expenseDate, setExpenseDate] = useState(() => {
    const today = new Date()
    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  })
  const [expenseMemo, setExpenseMemo] = useState('')
  const [expenseCategory, setExpenseCategory] = useState('')
  const [name, setName] = useState('')
  const [type, setType] = useState<AccountType>('LIVING')
  const [initialBalance, setInitialBalance] = useState('0')
  const [editingAccount, setEditingAccount] = useState(false)
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryLoading, setCategoryLoading] = useState(true)
  const [categoryError, setCategoryError] = useState('')
  const [categoryVersion, setCategoryVersion] = useState(0)
  const [categoryName, setCategoryName] = useState('')
  const [editingCategory, setEditingCategory] = useState<number | null>(null)
  const categoryNameRef = useRef<HTMLInputElement>(null)
  const [transactions, setTransactions] = useState<TransactionList | null>(null)
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [transactionType, setTransactionType] = useState('')
  const [deleting, setDeleting] = useState<{ kind: 'account' | 'category'; id: number; name: string } | null>(null)
  const disabled = busy || logoutBusy

  useEffect(() => {
    const controller = new AbortController()
    ledgerApi.categories(controller.signal).then(setCategories).catch(error => {
      if (!controller.signal.aborted) {
        if (error instanceof ApiError && error.status === 401) onExpired()
        else setCategoryError(error instanceof Error ? error.message : '카테고리를 불러오지 못했어요.')
      }
    }).finally(() => { if (!controller.signal.aborted) setCategoryLoading(false) })
    return () => controller.abort()
  }, [categoryVersion, onExpired])

  useEffect(() => {
    const controller = new AbortController()
    ledgerApi.accounts(controller.signal).then(setAccounts).catch(error => {
      if (!controller.signal.aborted) {
        if (error instanceof ApiError && error.status === 401) onExpired()
        else setAccountsError(error instanceof Error ? error.message : '계좌를 불러오지 못했어요.')
      }
    }).finally(() => { if (!controller.signal.aborted) setAccountsLoading(false) })
    return () => controller.abort()
  }, [accountsVersion, onExpired])

  function reloadAccounts() {
    setAccountsLoading(true)
    setAccountsError('')
    setAccountsVersion(value => value + 1)
  }

  function saveExpense(event: FormEvent) {
    event.preventDefault()
    if (!account) return
    if (!/^\d+$/.test(expenseAmount) || Number(expenseAmount) < 1 || Number(expenseAmount) > 1_000_000_000_000) {
      setMessage({ error: true, text: '지출 금액은 1원 이상 1조 원 이하의 정수로 입력해 주세요.' }); return
    }
    void run(async () => {
      await ledgerApi.createExpense({ accountId: account.id, amount: Number(expenseAmount),
        transactionDate: expenseDate, memo: expenseMemo.trim(), categoryId: expenseCategory ? Number(expenseCategory) : null })
      setExpenseAmount(''); setExpenseMemo('')
      // 방금 등록한 날짜를 포함해 조회하므로 과거 지출도 바로 확인할 수 있다.
      const nextDay = new Date(`${expenseDate}T12:00:00`)
      nextDay.setDate(nextDay.getDate() + 1)
      const endDate = `${nextDay.getFullYear()}-${String(nextDay.getMonth() + 1).padStart(2, '0')}-${String(nextDay.getDate()).padStart(2, '0')}`
      setStart(expenseDate); setEnd(endDate); setTransactionType(''); setTransactions(null)
      setMessage({ error: false, text: '지출을 등록했어요.' })
      try {
        const [detail, history] = await Promise.all([
          ledgerApi.account(account.id), ledgerApi.transactions(account.id, expenseDate, endDate, ''),
        ])
        setAccount(detail); setTransactions(history)
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) throw error
        setAccount(null)
        setMessage({ error: true, text: '지출은 저장됐지만 조회에 실패했어요. 계좌를 다시 조회해 주세요. 같은 지출을 다시 등록하지 마세요.' })
      }
    })
  }

  function reloadCategories() {
    setCategoryLoading(true)
    setCategoryError('')
    setCategoryVersion(value => value + 1)
  }

  function openCategoryForm() {
    setTab('categories')
    setEditingCategory(null)
    setCategoryName('')
    setMessage(null)
    setDeleting(null)
    requestAnimationFrame(() => {
      categoryNameRef.current?.scrollIntoView({ block: 'center' })
      categoryNameRef.current?.focus({ preventScroll: true })
    })
  }

  async function run(action: () => Promise<void>) {
    if (disabled) return
    setBusy(true)
    setMessage(null)
    try { await action() } catch (error) {
      if (error instanceof ApiError && error.status === 401) onExpired()
      else setMessage({ error: true, text: error instanceof Error ? error.message : '요청을 처리하지 못했어요.' })
    } finally { setBusy(false) }
  }

  function resetAccountForm() {
    setEditingAccount(false); setName(''); setType('LIVING'); setInitialBalance('0')
  }

  function lookup(event: FormEvent) {
    event.preventDefault()
    const id = Number(accountId)
    if (!Number.isSafeInteger(id) || id < 1) {
      setMessage({ error: true, text: '계좌 ID는 1 이상의 정수로 입력해 주세요.' }); return
    }
    void run(async () => {
      setAccount(null); setTransactions(null); resetAccountForm(); setDeleting(null)
      setAccount(await ledgerApi.account(id))
      setTransactions(await ledgerApi.transactions(id, start, end, transactionType))
    })
  }

  function saveAccount(event: FormEvent) {
    event.preventDefault()
    if (!name.trim() || name.length > 100 || !/^-?\d+$/.test(initialBalance) || !Number.isSafeInteger(Number(initialBalance))) {
      setMessage({ error: true, text: '계좌 이름은 1~100자, 초기 잔액은 정확히 표현 가능한 원 단위 정수로 입력해 주세요.' }); return
    }
    void run(async () => {
      const input = { account_name: name.trim(), account_type: type, initialBalance: Number(initialBalance) }
      let id: number
      if (editingAccount && account) {
        id = account.id
        await ledgerApi.updateAccount(id, input)
      } else {
        id = await ledgerApi.createAccount(input)
      }
      reloadAccounts()
      setAccountId(String(id)); setAccount(null); setTransactions(null); resetAccountForm()
      setMessage({ error: false, text: `계좌를 저장했어요. 계좌 ID: ${id}` })
      try { setAccount(await ledgerApi.account(id)) } catch (error) {
        if (error instanceof ApiError && error.status === 401) throw error
        setMessage({ error: true, text: `계좌 ID ${id} 저장은 완료했지만 상세 조회에 실패했어요. ID로 다시 조회해 주세요.` })
      }
    })
  }

  function saveCategory(event: FormEvent) {
    event.preventDefault()
    if (!categoryName.trim() || categoryName.length > 100) {
      setMessage({ error: true, text: '카테고리 이름은 1~100자로 입력해 주세요.' }); return
    }
    void run(async () => {
      if (editingCategory === null) await ledgerApi.createCategory(categoryName.trim())
      else await ledgerApi.updateCategory(editingCategory, categoryName.trim())
      setCategoryName(''); setEditingCategory(null)
      setMessage({ error: false, text: '카테고리를 저장했어요.' })
      reloadCategories()
    })
  }

  function loadTransactions(event: FormEvent) {
    event.preventDefault()
    if (!account) return
    if (start && end && start >= end) {
      setMessage({ error: true, text: '시작일은 종료일보다 이전이어야 해요.' }); return
    }
    void run(async () => {
      setTransactions(null)
      setTransactions(await ledgerApi.transactions(account.id, start, end, transactionType))
    })
  }

  function confirmDelete() {
    if (!deleting) return
    void run(async () => {
      if (deleting.kind === 'account') {
        await ledgerApi.deleteAccount(deleting.id)
        setAccount(null); setTransactions(null); setAccountId(''); resetAccountForm(); reloadAccounts()
      } else {
        await ledgerApi.deleteCategory(deleting.id)
        setEditingCategory(null); setCategoryName(''); setExpenseCategory(''); reloadCategories()
      }
      setDeleting(null)
      setMessage({ error: false, text: '삭제했어요.' })
    })
  }

  return <main className="dashboard">
    <header className="dashboard-header"><a className="dashboard-brand" href="/">moneta<span>나의 가계부</span></a><div className="dashboard-user"><span>{user.loginId}</span><button disabled={disabled} onClick={onLogout}>로그아웃</button></div></header>
    <div className="dashboard-body">
      {import.meta.env.MODE === 'demo' && <p className="notice notice-info">실험용 가계부입니다. 입력한 기록은 이 컴퓨터의 별도 DB에 저장돼요.</p>}
      <div className="dashboard-intro-row">
        <header className="dashboard-intro"><p className="eyebrow">MY MONEY, MY PACE</p><h2>{user.loginId}님, 오늘도 차근차근.</h2><p>계좌를 선택하고 지출을 기록해 보세요.</p></header>
        <button type="button" className="category-shortcut" aria-label="새 카테고리 추가" disabled={disabled} onClick={openCategoryForm}>＋ 카테고리 추가</button>
      </div>
      <nav className="dashboard-tabs" aria-label="가계부 메뉴">
        <button aria-pressed={tab === 'accounts'} disabled={disabled} onClick={() => { setTab('accounts'); setMessage(null); setDeleting(null) }}>계좌 관리</button>
        <button aria-pressed={tab === 'categories'} disabled={disabled} onClick={() => { setTab('categories'); setMessage(null); setDeleting(null) }}>카테고리 관리</button>
      </nav>
      {logoutNotice && <p className="notice notice-error" role="alert">{logoutNotice}</p>}
      {message && <p className={`notice notice-${message.error ? 'error' : 'success'}`} role={message.error ? 'alert' : 'status'}>{message.text}</p>}
      {deleting && <section className="delete-confirm" aria-label="삭제 확인"><p><strong>{deleting.name}</strong>을(를) 삭제할까요? 삭제 후 되돌릴 수 없어요.</p><p>거래에 사용 중인 항목은 삭제할 수 없어요.</p><div className="row-actions"><button className="danger" disabled={disabled} onClick={confirmDelete}>삭제 확인</button><button disabled={disabled} onClick={() => setDeleting(null)}>취소</button></div></section>}
      {tab === 'accounts' ? <>
        <div className="dashboard-grid">
          <section className="ledger-panel"><div className="panel-heading"><span className="panel-number">01</span><h3>{editingAccount ? '계좌 수정' : '새 계좌 만들기'}</h3></div>
            <form onSubmit={saveAccount} aria-label="계좌 양식">
              <label htmlFor="account-name">계좌 이름</label><input id="account-name" value={name} maxLength={100} required disabled={disabled} onChange={e => setName(e.target.value)} placeholder="예: 생활비 통장" />
              <label htmlFor="account-type">계좌 유형</label><select id="account-type" value={type} disabled={disabled} onChange={e => setType(e.target.value as AccountType)}>{Object.entries(accountTypes).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
              <label htmlFor="initial-balance">초기 잔액 (원)</label><input id="initial-balance" type="text" inputMode="numeric" value={initialBalance} required disabled={disabled} onChange={e => setInitialBalance(e.target.value)} /><p className="ledger-hint">기록을 시작하는 시점의 잔액이에요. 음수도 가능하며, 거래 등록 후에는 변경할 수 없어요.</p>
              <button className="primary-button" disabled={disabled}>{busy ? '처리 중…' : editingAccount ? '변경 저장' : '계좌 만들기'}</button>
              {editingAccount && <button type="button" className="text-button" disabled={disabled} onClick={resetAccountForm}>수정 취소</button>}
            </form>
          </section>
          <section className="ledger-panel"><div className="panel-heading"><span className="panel-number">02</span><h3>계좌 찾아보기</h3></div>
            <button type="button" disabled={disabled || accountsLoading} onClick={reloadAccounts}>계좌 목록 새로고침</button>
            {accountsLoading ? <p role="status">계좌를 불러오는 중…</p> : accountsError ? <p className="notice notice-error" role="alert">{accountsError}</p> : accounts.length === 0 ? <p className="ledger-hint">먼저 새 계좌를 만들어 주세요.</p> : null}
            <form className="lookup-form" onSubmit={lookup} aria-label="계좌 조회"><label htmlFor="account-id">내 계좌</label><div className="inline-form"><select id="account-id" required value={accountId} disabled={disabled || accountsLoading} onChange={e => { setAccountId(e.target.value); setAccount(null); setTransactions(null); resetAccountForm(); setDeleting(null) }}><option value="">계좌를 선택해 주세요</option>{accounts.map(item => <option key={item.id} value={item.id}>{item.name} · {accountTypes[item.type]}</option>)}</select><button disabled={disabled || !accountId}>조회</button></div></form>
            {account ? <article className="balance-card"><div><span>{accountTypes[account.type]} · ID {account.id}</span><h4>{account.name}</h4></div><p className="balance-label">현재 잔액</p><strong className="balance-value">{won(account.balance)}</strong><p>초기 잔액 {won(account.initialBalance)}</p><div className="row-actions"><button disabled={disabled} onClick={() => { setEditingAccount(true); setName(account.name); setType(account.type); setInitialBalance(String(account.initialBalance)); setDeleting(null) }}>수정</button><button className="danger" disabled={disabled} onClick={() => setDeleting({ kind: 'account', id: account.id, name: account.name })}>삭제</button></div></article> : <div className="ledger-empty"><span aria-hidden="true">↗</span><p>조회한 계좌가 여기에 표시돼요.</p></div>}
          </section>
        </div>
        {account && <section className="ledger-panel transaction-panel"><div className="panel-heading"><span className="panel-number">03</span><h3>{account.name}에 지출 등록</h3></div>
          <form onSubmit={saveExpense} aria-label="지출 양식">
            <label htmlFor="expense-amount">지출 금액 (원)</label><input id="expense-amount" inputMode="numeric" required value={expenseAmount} disabled={disabled} onChange={e => setExpenseAmount(e.target.value)} placeholder="예: 8000" />
            <label htmlFor="expense-date">지출 날짜</label><input id="expense-date" type="date" required value={expenseDate} disabled={disabled} onChange={e => setExpenseDate(e.target.value)} /><p className="ledger-hint">오늘 또는 이전 날짜를 입력해 주세요.</p>
            <label htmlFor="expense-category">카테고리</label><select id="expense-category" value={expenseCategory} disabled={disabled || categoryLoading} onChange={e => setExpenseCategory(e.target.value)}><option value="">미지정</option>{categories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select>
            <label htmlFor="expense-memo">내용 (선택)</label><input id="expense-memo" maxLength={255} value={expenseMemo} disabled={disabled} onChange={e => setExpenseMemo(e.target.value)} placeholder="예: 점심 식사" />
            <button className="primary-button" disabled={disabled}>{busy ? '처리 중…' : '지출 등록'}</button>
          </form>
        </section>}
        {account && <section className="ledger-panel transaction-panel"><div className="panel-heading"><span className="panel-number">04</span><h3>{account.name}의 거래 내역</h3></div>
          <form className="transaction-filter" onSubmit={loadTransactions} aria-label="거래 조회"><div><label htmlFor="period-start">시작일</label><input id="period-start" type="date" value={start} disabled={disabled} onChange={e => setStart(e.target.value)} /></div><div><label htmlFor="period-end">종료일 (미포함)</label><input id="period-end" type="date" value={end} disabled={disabled} onChange={e => setEnd(e.target.value)} /></div><div><label htmlFor="transaction-type">거래 유형</label><select id="transaction-type" value={transactionType} disabled={disabled} onChange={e => setTransactionType(e.target.value)}><option value="">전체</option>{Object.entries(transactionTypes).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div><button disabled={disabled}>거래 조회</button></form>
          <p className="ledger-hint">기간 생략 시 최근 한 달을 조회해요. 종료일의 거래는 포함하지 않습니다.</p>
          {transactions ? transactions.tList.length ? <div className="table-scroll"><table><caption className="sr-only">조회된 거래 내역</caption><thead><tr><th>날짜</th><th>유형</th><th>내용</th><th>카테고리</th><th>금액</th></tr></thead><tbody>{transactions.tList.map(item => <tr key={item.id}><td>{item.transactionDate}</td><td>{transactionTypes[item.type]}{item.type === 'TRANSFER' ? item.accountId === account.id ? ' · 출금' : ' · 입금' : ''}</td><td>{item.memo || '—'}</td><td>{categories.find(c => c.id === item.categoryId)?.name || (item.categoryId ? `ID ${item.categoryId}` : '미지정')}</td><td>{won(item.amount)}</td></tr>)}</tbody></table></div> : <div className="ledger-empty">해당 기간의 거래가 없어요.</div> : <div className="ledger-empty">기간과 유형을 선택한 뒤 조회해 주세요.</div>}
        </section>}
      </> : <div className="dashboard-grid">
        <section className="ledger-panel"><div className="panel-heading"><span className="panel-number">01</span><h3>{editingCategory === null ? '내 카테고리 추가' : '카테고리 이름 수정'}</h3></div><form onSubmit={saveCategory} aria-label="카테고리 양식"><label htmlFor="category-name">카테고리 이름</label><input ref={categoryNameRef} id="category-name" required maxLength={100} value={categoryName} disabled={disabled} onChange={e => setCategoryName(e.target.value)} placeholder="예: 문화생활" /><button className="primary-button" disabled={disabled}>{busy ? '처리 중…' : editingCategory === null ? '카테고리 추가' : '이름 저장'}</button>{editingCategory !== null && <button type="button" className="text-button" disabled={disabled} onClick={() => { setEditingCategory(null); setCategoryName('') }}>수정 취소</button>}</form><p className="ledger-hint">직접 추가한 카테고리만 수정·삭제할 수 있어요. 공통 카테고리는 변경할 수 없어요.</p></section>
        <section className="ledger-panel"><div className="panel-heading"><span className="panel-number">02</span><h3>사용 가능한 카테고리</h3><button disabled={disabled || categoryLoading} onClick={reloadCategories}>새로고침</button></div>
          {categoryLoading ? <p role="status">카테고리를 불러오는 중…</p> : categoryError ? <p className="notice notice-error" role="alert">{categoryError}</p> : categories.length ? <ul className="category-list">{categories.map(category => <li key={category.id}><div><strong>{category.name}</strong><span>{category.common ? '공통 카테고리' : `ID ${category.id}`}</span></div><div className="row-actions"><button aria-label={`${category.name} 수정`} disabled={disabled || category.common} onClick={() => { setEditingCategory(category.id); setCategoryName(category.name); setDeleting(null) }}>수정</button><button className="danger" aria-label={`${category.name} 삭제`} disabled={disabled || category.common} onClick={() => setDeleting({ kind: 'category', id: category.id, name: category.name })}>삭제</button></div></li>)}</ul> : <div className="ledger-empty">아직 카테고리가 없어요. 첫 카테고리를 추가해 보세요.</div>}
        </section>
      </div>}
      <footer className="dashboard-footer">작은 기록이 만드는 더 나은 일상 · Moneta</footer>
    </div>
  </main>
}
