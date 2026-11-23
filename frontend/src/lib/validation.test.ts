import { describe, expect, it } from 'vitest'
import { validate } from './validation'
import type { AuthDraft } from './validation'

const valid: AuthDraft = { loginId: 'user_1', password: 'a long passphrase', confirmation: 'a long passphrase', name: '지헌', age: '25' }

describe('회원가입 입력 검증', () => {
  it('정상 입력을 허용한다', () => expect(validate(valid, 'signup')).toEqual({}))
  it.each(['abc', 'user@name', '한글아이디', 'a'.repeat(21)])('잘못된 아이디 %s를 거부한다', loginId => {
    expect(validate({ ...valid, loginId }, 'signup').loginId).toBeTruthy()
  })
  it.each(['a'.repeat(14), 'a'.repeat(65), ' '.repeat(15), '가'.repeat(25)])('길이·공백·BCrypt 입력 제한을 검사한다', password => {
    expect(validate({ ...valid, password }, 'signup').password).toBeTruthy()
  })
  it('비밀번호를 임의로 trim하거나 문자 조합을 강제하지 않는다', () => {
    const password = ' a long passphrase '
    expect(validate({ ...valid, password, confirmation: password }, 'signup')).toEqual({})
  })
  it('확인 비밀번호가 다르면 가입을 막는다', () => {
    expect(validate({ ...valid, confirmation: 'different' }, 'signup').confirmation).toBeTruthy()
  })
  it.each(['0', '151', '25.5', '', 'not a number'])('나이 범위를 검사한다', age => {
    expect(validate({ ...valid, age }, 'signup').age).toBeTruthy()
  })
  it.each(['김', '홍 길동', "O'Connor", 'Mary-Jane', '张伟'])('이름 %s를 허용한다', name => {
    expect(validate({ ...valid, name }, 'signup').name).toBeUndefined()
  })
  it('기존 계정 로그인을 가입 규칙으로 막지 않는다', () => {
    expect(validate({ ...valid, loginId: 'old', password: 'short', confirmation: '', age: '' }, 'login')).toEqual({})
  })
  it('로그인 필수값은 검사한다', () => {
    expect(validate({ ...valid, loginId: '', password: '   ' }, 'login')).toHaveProperty('loginId')
    expect(validate({ ...valid, loginId: '', password: '   ' }, 'login')).toHaveProperty('password')
  })
})
