import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ledgerApi } from './ledger'

const fetchMock = vi.fn<typeof fetch>()
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status })
const csrf = { headerName: 'X-CSRF-TOKEN', token: 'ledger-token' }
beforeEach(() => { fetchMock.mockReset(); vi.stubGlobal('fetch', fetchMock) })
afterEach(() => vi.unstubAllGlobals())

describe('가계부 API', () => {
  it('계좌 생성의 실제 필드명과 숫자 ID 응답을 사용한다', async () => {
    fetchMock.mockResolvedValueOnce(json(csrf)).mockResolvedValueOnce(json(12))
    const input = { account_name: '생활비', account_type: 'LIVING' as const, initialBalance: 1000 }
    await expect(ledgerApi.createAccount(input)).resolves.toBe(12)
    expect(fetchMock.mock.calls[1]).toEqual(['/api/accounts', expect.objectContaining({ method: 'POST', body: JSON.stringify(input) })])
  })

  it.each(['updateCategory', 'deleteCategory'] as const)('%s는 CSRF와 함께 전송하고 빈 200 응답을 처리한다', async method => {
    fetchMock.mockResolvedValueOnce(json(csrf)).mockResolvedValueOnce(new Response(null, { status: 200 }))
    await expect(method === 'updateCategory' ? ledgerApi.updateCategory(5, '취미') : ledgerApi.deleteCategory(5)).resolves.toBeUndefined()
    expect(fetchMock.mock.calls[1]).toEqual(['/api/categories/5', expect.objectContaining({
      method: method === 'updateCategory' ? 'PATCH' : 'DELETE',
      headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'ledger-token' }),
    })])
  })

  it('수정 불가능한 카테고리의 404를 성공으로 처리하지 않는다', async () => {
    fetchMock.mockResolvedValueOnce(json(csrf)).mockResolvedValueOnce(json({ message: '수정할 카테고리가 없습니다.' }, 404))
    await expect(ledgerApi.updateCategory(1, '취미')).rejects.toMatchObject({ status: 404 })
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('거래 조회 필터와 실제 DTO의 tList 응답을 사용한다', async () => {
    fetchMock.mockResolvedValueOnce(json({ id: 3, name: '생활비', tList: [] }))
    expect((await ledgerApi.transactions(3, '2026-10-01', '2026-11-01', 'EXPENSE')).tList).toEqual([])
    expect(fetchMock.mock.calls[0][0]).toBe('/api/accounts/3/transactions?start=2026-10-01&end=2026-11-01&type=EXPENSE')
  })
})
