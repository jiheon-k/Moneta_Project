# Moneta

Spring Boot·React로 만든 기본 가계부입니다.
회원가입·로그인 → 계좌 생성·선택 → 지출 등록 → 거래 내역·잔액 확인을 지원합니다.
지출 단건 조회·수정·삭제는 백엔드 API에서 지원합니다.

## 바로 체험하기 (MySQL 불필요)

Java 21과 Node.js가 설치된 환경에서 프로젝트 루트의 터미널을 두 개 열어 실행합니다.

백엔드:

```bash
./gradlew bootRun --args='--spring.profiles.active=demo'
```

프런트엔드:

```bash
cd frontend
npm ci
npm run dev:demo
```

- 화면: http://localhost:5174
- 백엔드: http://127.0.0.1:8081
- 예제 아이디: `demo_user`
- 예제 비밀번호: `moneta-demo-1234` (공개된 실험용 계정)
- 예제: 생활비 계좌 100,000원에서 점심 8,000원 지출 → 잔액 92,000원

`demo`는 실행 설정 묶음인 Spring 프로필입니다. 이 모드에서는 MySQL 대신
`.local/moneta-demo.mv.db`에 실험 데이터를 저장합니다. 재시작해도 기록이 유지되고
예제 데이터를 중복 생성하지 않습니다. 이 DB 파일은 Git에서 제외됩니다.
기존 MySQL 데이터는 변경하지 않습니다. `.env.local`에 `API_PROXY_TARGET`을 별도로
설정했다면 demo 백엔드 주소로 맞추거나 해당 설정을 제거해야 합니다.

## 기본 기능과 규칙

- 본인 계좌 목록·상세 조회, 계좌 생성·수정·삭제
- 지출 등록: 1원~1조 원 정수, 오늘까지의 날짜, 내용 최대 255자, 카테고리 선택 가능
- 거래 조회: 시작일 포함·종료일 제외, 날짜·유형 조건, 최신순
- 잔액: 초기 잔액 + 유입 - 유출. 초기 잔액은 기록할 거래들 직전의 잔액으로 입력
- 계좌 생성일 이전의 과거 지출도 입력 가능. 별도 잔액 기준일 기능은 없음
- 거래 등록 후 초기 잔액 변경과 거래가 연결된 계좌·카테고리 삭제는 409로 거절
- 공통 카테고리는 조회·지출 분류에 사용하며, 개인 카테고리만 수정·삭제
- 요청에서 사용자 ID를 받지 않고 로그인 세션의 소유권을 검사

## 지출 API

| 요청 | 성공 응답 |
| --- | --- |
| `POST /expenses` | 201과 새 지출 ID |
| `GET /expenses/{id}` | 200과 지출 상세 |
| `PUT /expenses/{id}` | 200과 수정된 지출 상세 |
| `DELETE /expenses/{id}` | 204 |

`PUT`은 지출 전체를 수정하므로 `accountId`, `amount`, `transactionDate`가 필요합니다.
선택 필드인 `memo`, `categoryId`를 생략하면 기존 값이 비워집니다.
다른 본인 계좌로 옮길 수 있으며, 변경된 거래를 기준으로 두 계좌의 잔액을 계산합니다.
존재하지 않는 지출, 타인 소유 지출, 수입·이체 ID를 요청하면 404를 반환합니다.
등록·수정·삭제 요청에는 로그인 세션과 CSRF 토큰이 필요합니다.

## 기존 MySQL 실행

기존 로컬 DB 설정을 사용하려면 `./gradlew bootRun`과 `cd frontend && npm run dev`를 실행합니다.
화면은 http://localhost:5173 입니다. 기존 DB의 이전 컬럼·제약조건은 자동 삭제하지 않으므로
기존 DB를 사용할 때는 `docs/db/transaction-account-migration.md`를 참고하세요.

## 코드 읽는 순서

`TransactionController`(입력 검증) → `TransactionService`(소유권 확인) →
`TransactionRepository`(저장) → `AccountService`(내역 조회·잔액 계산) 순서로 확인하면 됩니다.
각 Controller·Service·Repository 위에 짧은 역할 주석을 추가했습니다.

## 검증

기능 구현을 마친 후 테스트를 모아서 실행합니다.

```bash
./gradlew test --offline --console=plain
cd frontend
npm test
npm run build
npm run lint
```

백엔드 통합 테스트는 별도의 H2 메모리 DB를 사용합니다. 실험용 파일 DB와 개발 MySQL은 사용하지 않습니다.
