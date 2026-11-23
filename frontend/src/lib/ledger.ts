import { mutate, request } from './auth'

export type AccountType = 'LIVING' | 'SAVING' | 'INVESTMENT'
export type AccountInput = { account_name: string; account_type: AccountType; initialBalance: number }
export type Account = { id: number; name: string; type: AccountType; initialBalance: number; balance: number; createdAt: string }
export type AccountSummary = Pick<Account, 'id' | 'name' | 'type'>
export type ExpenseInput = { accountId: number; amount: number; transactionDate: string; memo: string; categoryId: number | null }
export type Category = { id: number; name: string; common: boolean }
export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER'
export type Transaction = { id: number; amount: number; memo: string | null; transactionDate: string;
  type: TransactionType; accountId: number; transferAccountId: number | null; categoryId: number | null }
export type TransactionList = { id: number; name: string; tList: Transaction[] }

export const ledgerApi = {
  accounts: (signal?: AbortSignal) => request<AccountSummary[]>('/accounts', { signal }),
  createExpense: (input: ExpenseInput) => mutate<number>('/expenses', 'POST', input),
  account: (id: number) => request<Account>(`/accounts/${id}`),
  createAccount: (input: AccountInput) => mutate<number>('/accounts', 'POST', input),
  updateAccount: (id: number, input: AccountInput) => mutate<void>(`/accounts/${id}`, 'PATCH', input),
  deleteAccount: (id: number) => mutate<void>(`/accounts/${id}`, 'DELETE'),
  transactions: (id: number, start: string, end: string, type: string) => {
    const query = new URLSearchParams()
    if (start) query.set('start', start)
    if (end) query.set('end', end)
    if (type) query.set('type', type)
    return request<TransactionList>(`/accounts/${id}/transactions?${query}`)
  },
  categories: (signal?: AbortSignal) => request<Category[]>('/categories', { signal }),
  createCategory: (name: string) => mutate<void>('/categories', 'POST', { name }),
  updateCategory: (id: number, name: string) => mutate<void>(`/categories/${id}`, 'PATCH', { name }),
  deleteCategory: (id: number) => mutate<void>(`/categories/${id}`, 'DELETE'),
}
