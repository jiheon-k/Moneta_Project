export type AuthMode = 'login' | 'signup'
export type AuthDraft = { loginId: string; password: string; confirmation: string; name: string; age: string }
export type FormErrors = Partial<Record<keyof AuthDraft, string>>

export function validate(draft: AuthDraft, mode: AuthMode): FormErrors {
  const errors: FormErrors = {}
  if (!draft.loginId.trim()) errors.loginId = '로그인 아이디를 입력해 주세요.'
  if (!draft.password.trim()) errors.password = '비밀번호를 입력해 주세요.'
  if (mode === 'login') return errors

  if (!/^[A-Za-z0-9_]{4,20}$/.test(draft.loginId)) {
    errors.loginId = '영문·숫자·밑줄로 4~20자를 입력해 주세요.'
  }
  if (draft.password.length < 15 || draft.password.length > 64) {
    errors.password = '비밀번호는 15~64자로 입력해 주세요.'
  } else if (new TextEncoder().encode(draft.password).length > 72) {
    errors.password = '비밀번호가 너무 길어요. 한글·이모지를 포함하면 허용 길이가 줄어듭니다.'
  }
  if (!draft.confirmation) errors.confirmation = '비밀번호를 한 번 더 입력해 주세요.'
  else if (draft.password !== draft.confirmation) errors.confirmation = '비밀번호가 일치하지 않아요.'

  if (!draft.name || draft.name.length > 50
    || !/^[\p{L}\p{M}]+(?:[ '·-][\p{L}\p{M}]+)*$/u.test(draft.name)) {
    errors.name = '문자와 이름 구분 기호로 1~50자를 입력해 주세요.'
  }
  const age = Number(draft.age)
  if (!draft.age.trim() || !Number.isInteger(age) || age < 1 || age > 150) {
    errors.age = '나이는 1~150의 정수로 입력해 주세요.'
  }
  return errors
}
