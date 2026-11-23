// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Dashboard } from './Dashboard'
import { ledgerApi } from '../lib/ledger'
import { ApiError } from '../lib/auth'

vi.mock('../lib/ledger', () => ({ ledgerApi: {
  accounts: vi.fn(), createExpense: vi.fn(), account: vi.fn(), createAccount: vi.fn(), updateAccount: vi.fn(), deleteAccount: vi.fn(),
  transactions: vi.fn(), categories: vi.fn(), createCategory: vi.fn(), updateCategory: vi.fn(), deleteCategory: vi.fn(),
} }))
const api = vi.mocked(ledgerApi)
const account = { id: 7, name: '생활비', type: 'LIVING' as const, initialBalance: 1000, balance: 900, createdAt: '2026-10-26T10:00:00' }
const onExpired = vi.fn()
function show() {
  render(<Dashboard user={{ id: 1, loginId: 'user_1' }} onLogout={vi.fn()} logoutBusy={false} onExpired={onExpired} />)
  return userEvent.setup()
}
beforeEach(() => {
  vi.resetAllMocks()
  api.accounts.mockResolvedValue([account])
  api.transactions.mockResolvedValue({ id: 7, name: '생활비', tList: [] })
  api.categories.mockResolvedValue([{ id: 3, name: '식비', common: false }])
})
afterEach(cleanup)

describe('가계부 관리 화면', () => {
  it('계좌를 생성하고 받은 ID와 잔액을 표시한다', async () => {
    api.createAccount.mockResolvedValue(7); api.account.mockResolvedValue(account)
    const user = show()
    await user.type(screen.getByLabelText('계좌 이름'), '생활비')
    await user.clear(screen.getByLabelText('초기 잔액 (원)'))
    await user.type(screen.getByLabelText('초기 잔액 (원)'), '1000')
    await user.click(screen.getByRole('button', { name: '계좌 만들기' }))
    await screen.findByText('900원')
    expect(api.createAccount).toHaveBeenCalledWith({ account_name: '생활비', account_type: 'LIVING', initialBalance: 1000 })
    expect((screen.getByLabelText('내 계좌') as HTMLSelectElement).value).toBe('7')
  })

  it('없는 계좌 조회의 404를 표시한다', async () => {
    api.account.mockRejectedValue(new ApiError(404, '해당 계좌가 존재하지 않습니다.'))
    const user = show()
    await screen.findByRole('option', { name: '생활비 · 생활비' })
    await user.selectOptions(screen.getByLabelText('내 계좌'), '7')
    await user.click(screen.getByRole('button', { name: '조회' }))
    expect((await screen.findByRole('alert')).textContent).toContain('해당 계좌가 존재하지 않습니다.')
    expect(screen.queryByText('현재 잔액')).toBeNull()
  })

  it('잘못된 초기 잔액은 전송하지 않는다', async () => {
    const user = show()
    await user.type(screen.getByLabelText('계좌 이름'), '생활비')
    await user.clear(screen.getByLabelText('초기 잔액 (원)'))
    await user.type(screen.getByLabelText('초기 잔액 (원)'), '1.5')
    await user.click(screen.getByRole('button', { name: '계좌 만들기' }))
    await screen.findByRole('alert')
    expect(api.createAccount).not.toHaveBeenCalled()
  })

  it('카테고리 추가 후 목록을 다시 조회한다', async () => {
    api.createCategory.mockResolvedValue(undefined)
    const user = show()
    await user.click(screen.getByRole('button', { name: '카테고리 관리' }))
    await user.type(screen.getByLabelText('카테고리 이름'), '게임')
    api.categories.mockResolvedValue([{ id: 3, name: '식비', common: false }, { id: 5, name: '게임', common: false }])
    await user.click(screen.getByRole('button', { name: '카테고리 추가' }))
    await screen.findByText('게임')
    expect(api.createCategory).toHaveBeenCalledWith('게임')
  })

  it('카테고리 삭제는 확인 후 전송하며 실패하면 항목을 유지한다', async () => {
    api.deleteCategory.mockRejectedValue(new ApiError(404, '삭제할 카테고리가 없습니다.'))
    const user = show()
    await user.click(screen.getByRole('button', { name: '카테고리 관리' }))
    await user.click(await screen.findByRole('button', { name: '식비 삭제' }))
    expect(api.deleteCategory).not.toHaveBeenCalled()
    await user.click(within(screen.getByRole('region', { name: '삭제 확인' })).getByRole('button', { name: '삭제 확인' }))
    await screen.findByRole('alert')
    expect(api.deleteCategory).toHaveBeenCalledWith(3)
    expect(screen.getByRole('button', { name: '식비 삭제' })).toBeTruthy()
  })

  it('계좌 수정 후 변경된 상세를 다시 조회한다', async () => {
    api.account.mockResolvedValueOnce(account).mockResolvedValueOnce({ ...account, name: '저축 통장', type: 'SAVING' })
    api.updateAccount.mockResolvedValue(undefined)
    const user = show()
    await screen.findByRole('option', { name: '생활비 · 생활비' })
    await user.selectOptions(screen.getByLabelText('내 계좌'), '7')
    await user.click(screen.getByRole('button', { name: '조회' }))
    await user.click(await screen.findByRole('button', { name: '수정' }))
    await user.clear(screen.getByLabelText('계좌 이름'))
    await user.type(screen.getByLabelText('계좌 이름'), '저축 통장')
    await user.selectOptions(screen.getByLabelText('계좌 유형'), 'SAVING')
    await user.click(screen.getByRole('button', { name: '변경 저장' }))
    await screen.findByRole('heading', { name: '저축 통장' })
    expect(api.updateAccount).toHaveBeenCalledWith(7, { account_name: '저축 통장', account_type: 'SAVING', initialBalance: 1000 })
  })

  it('거래 필터를 전달하고 수입 내역을 표시한다', async () => {
    api.account.mockResolvedValue(account)
    api.transactions.mockResolvedValue({ id: 7, name: '생활비', tList: [{ id: 1, amount: 2000, memo: '용돈',
      transactionDate: '2026-10-26', type: 'INCOME', accountId: 7, transferAccountId: null, categoryId: null }] })
    const user = show()
    await screen.findByRole('option', { name: '생활비 · 생활비' })
    await user.selectOptions(screen.getByLabelText('내 계좌'), '7')
    await user.click(screen.getByRole('button', { name: '조회' }))
    await screen.findByText('현재 잔액')
    await user.selectOptions(screen.getByLabelText('거래 유형'), 'INCOME')
    await user.click(screen.getByRole('button', { name: '거래 조회' }))
    await screen.findByText('용돈')
    expect(api.transactions).toHaveBeenCalledWith(7, '', '', 'INCOME')
    expect(screen.getByText('2,000원')).toBeTruthy()
  })

  it('카테고리 이름을 수정하고 목록에 반영한다', async () => {
    api.updateCategory.mockResolvedValue(undefined)
    const user = show()
    await user.click(screen.getByRole('button', { name: '카테고리 관리' }))
    await user.click(await screen.findByRole('button', { name: '식비 수정' }))
    await user.clear(screen.getByLabelText('카테고리 이름'))
    await user.type(screen.getByLabelText('카테고리 이름'), '외식')
    api.categories.mockResolvedValue([{ id: 3, name: '외식', common: false }])
    await user.click(screen.getByRole('button', { name: '이름 저장' }))
    await screen.findByRole('button', { name: '외식 수정' })
    expect(api.updateCategory).toHaveBeenCalledWith(3, '외식')
  })

  it('세션 만료를 상위 화면에 전달한다', async () => {
    api.categories.mockRejectedValue(new ApiError(401, '로그인이 필요합니다.'))
    show()
    await vi.waitFor(() => expect(onExpired).toHaveBeenCalledTimes(1))
  })
  it('지출 저장 후 해당 날짜의 거래와 줄어든 잔액을 표시한다', async () => {
    api.account.mockResolvedValueOnce(account).mockResolvedValueOnce({ ...account, balance: 600 })
    api.createExpense.mockResolvedValue(8)
    const user = show()
    await screen.findByRole('option', { name: '생활비 · 생활비' })
    await user.selectOptions(screen.getByLabelText('내 계좌'), '7')
    await user.click(screen.getByRole('button', { name: '조회' }))
    await user.type(await screen.findByLabelText('지출 금액 (원)'), '300')
    await user.clear(screen.getByLabelText('지출 날짜'))
    await user.type(screen.getByLabelText('지출 날짜'), '2026-10-01')
    await user.type(screen.getByLabelText('내용 (선택)'), '간식')
    api.transactions.mockResolvedValue({ id: 7, name: '생활비', tList: [{ id: 8, amount: 300,
      memo: '간식', transactionDate: '2026-10-01', type: 'EXPENSE', accountId: 7, transferAccountId: null, categoryId: null }] })
    await user.click(screen.getByRole('button', { name: '지출 등록' }))
    await screen.findByText('600원')
    expect(screen.getByText('간식')).toBeTruthy()
    expect(api.createExpense).toHaveBeenCalledWith({ accountId: 7, amount: 300, transactionDate: '2026-10-01', memo: '간식', categoryId: null })
    expect(api.transactions).toHaveBeenLastCalledWith(7, '2026-10-01', '2026-10-02', '')
  })

  it('공통 카테고리는 수정과 삭제를 막는다', async () => {
    api.categories.mockResolvedValue([{ id: 3, name: '식비', common: true }])
    const user = show()
    await user.click(screen.getByRole('button', { name: '카테고리 관리' }))
    expect((await screen.findByRole('button', { name: '식비 수정' }) as HTMLButtonElement).disabled).toBe(true)
    expect((screen.getByRole('button', { name: '식비 삭제' }) as HTMLButtonElement).disabled).toBe(true)
  })

})
