# Moneta 인증 기능 설명

구현 범위는 회원가입, 세션 로그인, 로그아웃, 현재 사용자 인증이다.
계좌·카테고리의 기능이나 소유자 검사 코드는 변경하지 않았다.

## 1. 사용할 API

| 요청 | 용도 | 정상 응답 |
|---|---|---|
| GET /auth/csrf | 변경 요청에 사용할 CSRF 토큰과 세션 쿠키 준비 | 200, headerName·token |
| POST /auth/signup | 회원가입. 가입 후 자동 로그인은 하지 않음 | 201, 본문 없음 |
| POST /auth/login | 아이디·비밀번호 검증 후 세션 로그인 | 200, id·loginId |
| GET /auth/me | 현재 세션에 로그인한 사용자 확인 | 200, id·loginId |
| POST /auth/logout | 세션 폐기, 세션 쿠키 삭제 | 204, 본문 없음 |

회원가입·로그인·로그아웃 POST에는 같은 세션의 CSRF 토큰이 필요하다.
로그아웃 처리는 Controller 대신 SecurityConfig에 등록한 Spring Security 필터가 수행한다.
로그인하지 않은 사용자의 /auth/me 요청은 401이다.

## 2. 파일을 읽는 순서

1. `auth/dto/SignupRequest.java`, `LoginRequest.java`: 클라이언트가 보내는 입력값.
2. `auth/controller/SignupController.java`: 회원가입 요청을 Service에 전달.
3. `auth/service/SignupService.java`: 중복 검사, 해시 처리, User 저장.
4. `auth/security/LoginUserDetailsService.java`: 로그인 ID로 DB 사용자를 조회.
5. `auth/security/LoginUser.java`: Spring Security가 사용하는 사용자 정보.
6. `config/SecurityConfig.java`: 인증 처리기, 세션, CSRF, 접근 규칙, 로그아웃 연결.
7. `auth/service/LoginService.java`: Spring Security에 아이디·비밀번호 검증 요청.
8. `auth/controller/LoginController.java`: 검증 결과를 세션에 저장.
9. `auth/controller/CurrentUserController.java`: 세션에서 확인한 사용자 반환.

기존 `auth.serivce` 패키지의 오타를 `auth.service`로 수정했다.

## 3. 회원가입은 어떻게 저장되는가

```text
POST /auth/signup
  → CSRF 검사
  → @Valid로 SignupRequest 검증
  → SignupService.signUp()
  → existsByLoginId()로 중복 확인
  → PasswordEncoder.encode()로 비밀번호 해시 처리
  → User에 loginId·passwordHash·name·age 설정
  → UserRepository.saveAndFlush()
  → DB 저장 성공 후 201
```

`saveAndFlush()`는 저장한 SQL을 DB에 반영하도록 요청한다.
현재 저장은 Repository의 트랜잭션에서 처리한다.
두 가입 요청이 동시에 중복 검사를 통과해도 DB의 UNIQUE 제약이 중복 저장을 막는다.
저장 오류가 발생하면 로그인 ID가 실제로 중복됐는지 다시 확인해서 409로 응답한다.
다른 DB 제약 오류까지 무조건 아이디 중복으로 바꾸지는 않는다.

비밀번호 원본은 요청 처리 중 해시 계산에 사용하며 DB나 응답에 저장하지 않는다.
User의 해시 getter에도 JsonIgnore를 적용했다.
비밀번호는 자르거나 trim하지 않는다.

현재 가입 규칙:

- 로그인 ID: 4~20자, 영문·숫자·밑줄.
- 비밀번호: 15~64자이며 UTF-8 기준 72바이트 이하. BCrypt의 입력 한계 때문이다.
- 이름: 1~50자, 문자와 문자 사이의 공백·작은따옴표·가운뎃점·하이픈.
- 나이: 필수, 1~150.

## 4. 로그인에서는 누가 비밀번호를 비교하는가

```text
LoginController
  → LoginService.login()
  → AuthenticationManager.authenticate()
  → DaoAuthenticationProvider
      → LoginUserDetailsService가 DB에서 사용자 조회
      → PasswordEncoder가 입력 비밀번호와 저장된 해시 비교
  → Authentication 반환
```

`AuthenticationManager`는 인증 요청을 받는 객체다.
`DaoAuthenticationProvider`는 DB에서 읽은 사용자 정보와 PasswordEncoder를 사용해 검증한다.
따라서 LoginService에서 직접 비밀번호 문자열을 비교하지 않는다.

사용자가 없거나 비밀번호가 틀리면 같은 401 메시지를 반환한다.
현재 BCrypt 입력 한계를 넘는 로그인 비밀번호도 500 대신 401로 처리한다.
로그인은 가입 시의 최소 길이를 다시 강제하지 않는다. 가입 정책이 바뀌어도 기존 계정을 검증할 수 있게 하기 위해서다.

## 5. 검증 성공과 로그인 유지는 다르다

`authenticate()` 성공만으로 다음 요청이 로그인 상태가 되지는 않는다.
LoginController에서 다음 작업을 연결했다.

```text
인증 성공
  → SessionAuthenticationStrategy 실행
      → 기존 세션 ID 교체
      → 기존 CSRF 토큰 제거
  → 새 SecurityContext에 Authentication 저장
  → SecurityContextRepository.saveContext()
  → HTTP 세션에 인증 정보 저장
  → 사용자 ID와 로그인 ID 응답
```

- `Authentication`: 인증 결과. 로그인 사용자와 인증 여부를 담는다.
- `SecurityContext`: Authentication을 담는 보관함.
- `SecurityContextHolder`: 현재 요청에서 SecurityContext를 사용할 수 있게 하는 통로.
- `SecurityContextRepository`: 현재 인증 결과를 세션에 저장하고 다음 요청에서 읽는다.

세션 ID를 교체하는 이유는 로그인 전에 사용하던 세션 ID를 로그인 후에도 계속 사용하는 것을 막기 위해서다.
로그인 이후 POST 요청 전에는 /auth/csrf를 다시 호출해야 한다.

## 6. 다음 요청에서 사용자 ID를 얻는 방법

브라우저는 이후 요청에 JSESSIONID 쿠키를 보낸다.
서버는 해당 세션의 SecurityContext를 읽어서 로그인 상태를 복원한다.
쿠키에 사용자 ID나 비밀번호를 직접 담는 방식이 아니다.

```java
@GetMapping("/me")
public AuthUserResponse me(@AuthenticationPrincipal LoginUser user) {
    return AuthUserResponse.from(user);
}
```

`@AuthenticationPrincipal`은 현재 인증 결과 안의 사용자 객체를 받는다.
`user.getId()`로 DB 사용자 ID를 확인할 수 있다.
클라이언트가 JSON이나 URL로 주장하는 사용자 ID를 사용하지 않는다.

LoginUser에는 JPA Entity 대신 인증에 필요한 값만 담는다.
인증이 끝나면 CredentialsContainer.eraseCredentials()로 비밀번호 해시를 지운다.
응답은 AuthUserResponse로 제한해서 id와 loginId만 반환한다.

현재 계좌·카테고리 코드는 수정하지 않았다.
SecurityConfig의 authenticated()는 로그인 여부를 검사하며 개별 계좌의 소유자까지 검사하지 않는다.

## 7. 세션 쿠키와 CSRF 토큰의 차이

| 구분 | 세션 쿠키 | CSRF 토큰 |
|---|---|---|
| 목적 | 서버가 로그인 상태를 찾는 데 사용 | 의도하지 않은 사이트에서 보낸 변경 요청을 방어 |
| 전달 | Cookie 헤더 | /auth/csrf 응답의 headerName에 지정된 헤더 |
| 서버 보관 | 세션에 인증 정보 저장 | 해당 세션에 기대 토큰 저장 |

CSRF 토큰만으로는 로그인할 수 없다. 비밀번호 검증은 별도로 필요하다.
permitAll()은 로그인하지 않아도 접근할 수 있다는 뜻이며 CSRF 검사를 생략하지 않는다.

설정한 세션 동작:

- 비활동 30분 후 만료.
- HttpOnly 쿠키: 브라우저 JavaScript가 세션 쿠키를 직접 읽지 못하도록 설정.
- SameSite=Lax.
- HTTPS 배포 시 환경변수 SESSION_COOKIE_SECURE=true 사용.
- 세션은 기본적으로 서버 메모리에 저장하므로 서버 재시작 시 로그인도 해제된다.
- 현재 브라우저와 서버가 같은 출처에서 통신하는 구성을 기준으로 한다.

## 8. 오류 응답

```json
{"code":"UNAUTHORIZED","message":"로그인이 필요합니다."}
```

| 상황 | 상태 |
|---|---|
| 가입·로그인 입력값 누락 또는 형식 오류 | 400 |
| 미로그인 사용자 접근 | 401 |
| 아이디 또는 비밀번호 불일치 | 401 |
| CSRF 토큰 누락·불일치 | 403 |
| 중복 로그인 ID 가입 | 409 |

Controller 내부 오류는 기존 GlobalExceptionHandler가 처리한다.
보안 필터의 401·403은 Controller에 도착하기 전에 발생하므로 SecurityErrorHandler가 같은 code/message 형식으로 응답한다.

## 9. curl로 실행하는 순서

서버를 켠 뒤 아래 명령을 같은 터미널에서 순서대로 실행한다.
테스트용 ID가 이미 있으면 다른 ID로 바꾸거나 가입을 생략한다.

```bash
auth_tmp=$(mktemp -d)

# 1. 세션 쿠키와 CSRF 토큰 준비
curl -sS -c "$auth_tmp/cookies" \
  http://localhost:8080/auth/csrf -o "$auth_tmp/csrf.json"
csrf_token=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["token"])' "$auth_tmp/csrf.json")

# 2. 회원가입 → 201
curl -i -b "$auth_tmp/cookies" -c "$auth_tmp/cookies" \
  -H 'Content-Type: application/json' -H "X-CSRF-TOKEN: $csrf_token" \
  -d '{"loginId":"auth_test","password":"a long test passphrase","name":"지헌","age":25}' \
  http://localhost:8080/auth/signup

# 3. 로그인 → 200. 새 세션 ID 쿠키를 반드시 저장한다.
curl -i -b "$auth_tmp/cookies" -c "$auth_tmp/cookies" \
  -H 'Content-Type: application/json' -H "X-CSRF-TOKEN: $csrf_token" \
  -d '{"loginId":"auth_test","password":"a long test passphrase"}' \
  http://localhost:8080/auth/login

# 4. 로그인 상태 확인 → 200
curl -i -b "$auth_tmp/cookies" http://localhost:8080/auth/me

# 5. 로그인으로 기존 토큰이 폐기됐으므로 다시 조회
curl -sS -b "$auth_tmp/cookies" -c "$auth_tmp/cookies" \
  http://localhost:8080/auth/csrf -o "$auth_tmp/csrf.json"
csrf_token=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["token"])' "$auth_tmp/csrf.json")

# 6. 로그아웃 → 204
curl -i -X POST -b "$auth_tmp/cookies" -c "$auth_tmp/cookies" \
  -H "X-CSRF-TOKEN: $csrf_token" http://localhost:8080/auth/logout

# 7. 로그인 해제 확인 → 401
curl -i -b "$auth_tmp/cookies" http://localhost:8080/auth/me

rm -r "$auth_tmp"
unset auth_tmp csrf_token
```

## 10. 현재 검증 결과와 전체 앱 시작 제한

인증·설정·가입 검증·기존 계좌·거래 테스트 총 65개가 통과했다.
개발 DB 대신 H2를 지정한 전체 앱 시작 검사에서는 기존 CategoryController가
주입받을 CategoryService 빈이 없어 contextLoads가 실패했다.
현재 CategoryService에는 @Service가 없다.
인증 범위 밖인 카테고리 파일은 수정하지 않았으므로, 전체 앱을 실행하려면 이 별도 문제를 해결해야 한다.

## 11. 검증 범위와 기존 DB

AuthIntegrationTests는 테스트 전용 H2 메모리 DB와 실제 Spring Security 필터를 사용한다.
가입 저장·해시 비교·중복 가입·로그인 실패·세션 ID 교체·인증 유지·CSRF 갱신·로그아웃을 검증한다.
SignupServiceTests는 동시 가입으로 DB에서 중복 오류가 발생한 경로를 모의 검증한다.
H2 의존성은 테스트에만 사용하며 운영 DB는 MySQL 그대로다.

```bash
./gradlew test --tests 'com.jiheon.moneta.auth.*' --tests 'com.jiheon.moneta.config.*' --tests 'com.jiheon.moneta.signup.*'
```

개발 MySQL 데이터와 스키마는 변경하지 않았다.
기존 사용자가 있다면 login_id, password_hash의 필수값·UNIQUE 제약 적용 여부를 확인해야 한다.
JPA의 ddl-auto=update가 기존 사용자의 로그인 ID나 비밀번호 해시를 자동으로 만들어 주지는 않는다.
기존 데이터를 삭제하거나 원본 비밀번호를 DB에 직접 저장하는 방식으로 해결하면 안 된다.

공식 참고:

- https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html
- https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html
- https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
