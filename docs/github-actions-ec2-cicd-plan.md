# GitHub Actions → Docker Hub → EC2 자동 배포 계획

## 1. 목표

코드가 각 GitHub 저장소의 `main` 브랜치에 반영되는 것을 배포 시작점으로 삼는다.

1. GitHub Actions가 코드를 빌드하고 테스트한다.
2. 검증을 통과한 경우 GitHub Actions 실행 환경에서 Docker 이미지를 만든다.
3. 이미지를 Docker Hub에 `latest` 태그로 push한다.
4. GitHub Actions가 SSH로 EC2에 접속해 대상 서비스의 새 이미지를 pull한다.
5. EC2의 Docker Compose가 컨테이너를 교체하고 상태를 확인한다.
6. 새 컨테이너가 정상화되지 않으면 직전 로컬 이미지로 자동 롤백한다.

Java와 React 애플리케이션 코드인 양쪽 저장소의 `src/**`는 수정하지 않는다. 변경 대상은 Dockerfile, Nginx, Docker Compose, GitHub Actions workflow 및 배포 스크립트와 같은 빌드·배포 구성으로 한정한다.

## 2. 배포 구조

BE와 FE는 서로 다른 GitHub 저장소에서 독립적으로 빌드·배포한다.

```text
BE main push
  → Gradle build/test
  → hobbyloop-backend:latest push
  → EC2에서 backend만 교체

FE main push
  → npm test/build
  → React build
  → React dist + Nginx 이미지 생성
  → hobbyloop-frontend:latest push
  → EC2에서 frontend만 교체

사용자
  → EC2 Nginx :80
      ├─ /              → React 정적 파일
      ├─ /api/*         → backend:8080
      ├─ /public/*      → backend:8080
      └─ /ws-chat       → backend:8080 WebSocket

backend
  → 외부 MySQL RDS
  → uploads-data 볼륨
```

FE의 최종 런타임 이미지는 React 빌드 결과를 Nginx가 제공하는 구조이다. 따라서 FE와 Nginx를 별도 이미지로 나누지 않고 다음 두 이미지만 운영한다.

- `jhjhkkk/hobbyloop-backend:latest`
- `jhjhkkk/hobbyloop-frontend:latest`

## 3. 현재 구성에서 수정이 필요한 요소

### 3.1 BE 저장소

- 현재 `../.github/workflows/cicd.yml`은 Gradle 빌드와 테스트만 수행한다.
- Docker Hub 로그인, 이미지 build/push, EC2 배포 job을 추가해야 한다.
- `docker-compose.prod.yaml`의 `1.0.0` 고정 태그를 `latest`로 변경해야 한다.
- 운영 Compose에는 로컬 `build:`를 두지 않고 Docker Hub 이미지 pull만 사용해야 한다.
- 개발용 `docker-compose.yaml`의 `DB_URL: ${UB_URL}` 오타를 실제 수정 단계에서 `DB_URL: ${DB_URL}`로 바로잡아야 한다.
- EC2 배포 동시 실행 방지, health check 및 롤백을 담당할 배포 스크립트가 필요하다.

### 3.2 FE 저장소

- 현재 GitHub Actions workflow가 없으므로 새로 추가해야 한다.
- FE 저장소 자체에서 테스트와 빌드를 실행하고 FE+Nginx 이미지를 만들어야 한다.
- BE 저장소의 workflow에서 로컬 상대 경로인 `../community-ktb`를 참조해서는 안 된다. GitHub 러너에는 다른 저장소가 자동으로 checkout되지 않기 때문이다.
- `VITE_API_BASE_URL=/api`를 production build 인자로 고정해 브라우저가 동일 출처의 Nginx를 통해 API에 접근하게 한다.

### 3.3 EC2

- Docker Engine과 Docker Compose Plugin이 설치돼 있어야 한다.
- `/opt/hobbyloop`에 운영 Compose, 배포 스크립트, 비밀 환경변수 파일을 최초 한 번 설치해야 한다.
- EC2 보안 그룹에서는 외부에 Nginx용 80 포트만 공개하고 BE 8080 포트는 공개하지 않는다.
- FE와 BE 저장소에서 배포가 동시에 실행될 수 있으므로 서버 측 파일 잠금이 필요하다.

## 4. BE GitHub Actions 계획

BE workflow는 `verify`, `publish`, `deploy`의 세 job으로 구성한다.

### 4.1 트리거

- `pull_request` → `main`: 검증만 수행
- `push` → `main`: 검증, 이미지 push, EC2 배포 수행
- 동일 저장소에서 새 배포가 시작되면 이전 미완료 실행을 취소하도록 `concurrency`를 설정한다.

### 4.2 verify

1. 저장소 checkout
2. Temurin Java 21 설정
3. Gradle 캐시 설정
4. `./gradlew clean build --no-daemon` 실행

테스트나 빌드가 실패하면 이후 job은 실행하지 않는다. 테스트에는 RDS 계정을 사용하지 않고 현재 test profile과 H2를 사용한다.

### 4.3 publish

`verify`가 성공한 `main` push에서만 실행한다.

1. Docker Buildx 설정
2. Docker Hub access token으로 로그인
3. `linux/amd64` 플랫폼 이미지 빌드
4. `jhjhkkk/hobbyloop-backend:latest` push

Docker 이미지는 개발자 PC가 아니라 GitHub Actions 러너가 생성한다. Docker Hub 비밀번호 대신 최소 권한 access token을 사용한다.

### 4.4 deploy

`publish`가 성공한 경우에만 SSH로 다음 명령을 실행한다.

```bash
cd /opt/hobbyloop
./deploy-service.sh backend
```

EC2 host key를 사전에 등록한 `known_hosts` 값으로 검증한다. 배포 실패 시 workflow도 실패 처리한다.

## 5. FE+Nginx GitHub Actions 계획

FE workflow도 `verify`, `publish`, `deploy`의 세 job으로 구성한다.

### 5.1 verify

1. 저장소 checkout
2. Node.js 22와 npm 캐시 설정
3. `npm ci` 실행
4. `npm test` 실행
5. `VITE_API_BASE_URL=/api` 환경에서 `npm run build` 실행

PR에서는 이 단계까지만 실행한다.

### 5.2 publish

`main` push에서 검증을 통과한 경우 멀티스테이지 Dockerfile을 빌드한다.

- builder stage: React production build 수행
- runtime stage: 빌드된 `dist`와 Nginx 설정만 포함
- 최종 이미지: `jhjhkkk/hobbyloop-frontend:latest`
- 대상 플랫폼: `linux/amd64`

최종 이미지에는 Node.js, `node_modules`, 테스트 결과, 로컬 환경변수 파일을 포함하지 않는다.

### 5.3 Nginx 동작

- `/`: React 정적 파일 및 SPA fallback 제공
- `/api/`: `/api` 접두사를 제거하고 `http://backend:8080/`으로 전달
- `/public/`: 업로드 파일 요청 전달
- `/ws-chat`: WebSocket Upgrade 헤더를 포함해 전달
- 요청 크기 제한은 Spring의 100MB 제한과 일치시킴
- `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto` 전달
- 해시가 붙은 정적 자산은 장기 캐시하고 `index.html`은 캐시하지 않음

### 5.4 deploy

이미지 push가 성공하면 EC2에서 다음 명령을 실행한다.

```bash
cd /opt/hobbyloop
./deploy-service.sh frontend
```

## 6. EC2 최초 구성

### 6.1 디렉터리

```text
/opt/hobbyloop/
├── docker-compose.prod.yaml
├── deploy-service.sh
└── .env.prod
```

운영 Compose와 배포 스크립트는 배포 전 최초 한 번 EC2에 설치한다. 이후 GitHub Actions는 애플리케이션 소스를 EC2로 복사하거나 checkout하지 않고 이미지 교체만 지시한다.

### 6.2 운영 Compose

운영 Compose에는 다음 조건을 적용한다.

- `backend.image`: `jhjhkkk/hobbyloop-backend:latest`
- `frontend.image`: `jhjhkkk/hobbyloop-frontend:latest`
- 두 서비스 모두 `restart: unless-stopped`
- 두 서비스 모두 health check 구성
- backend는 `expose: 8080`만 사용
- frontend만 `80:80` 공개
- backend와 frontend를 동일한 전용 bridge network에 연결
- 업로드 파일은 `uploads-data` named volume에 저장
- `build:` 항목은 제거

Docker Hub 저장소가 공개라면 EC2 로그인이 필요 없다. 비공개 저장소라면 EC2에 pull 권한만 가진 Docker Hub 토큰으로 최초 한 번 로그인한다.

## 7. RDS 연결

RDS는 BE만 사용한다. FE와 Nginx는 DB에 직접 연결하지 않는다.

EC2의 `.env.prod`에는 다음 런타임 값을 저장한다.

```dotenv
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<rds-endpoint>:3306/<database>
DB_USERNAME=<username>
DB_PASSWORD=<password>
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2
JWT_SECRET=<secret>
JWT_ACCESS_TOKEN_EXPIRATION=180
JWT_REFRESH_TOKEN_EXPIRATION=12096000
UPLOAD_PATH=/var/lib/hobbyloop/uploads
CORS_ALLOWED_ORIGINS=http://<service-domain>
SERVER_PORT=8080
SERVER_ADDRESS=0.0.0.0
```

관리 원칙은 다음과 같다.

- `.env.prod` 파일 권한을 `600`으로 제한한다.
- `.env.prod`를 Git, Docker 이미지, GitHub Actions artifact에 포함하지 않는다.
- RDS 보안 그룹은 EC2 보안 그룹에서 오는 3306 연결만 허용한다.
- RDS는 가능한 한 private subnet에 둔다.
- 현재 운영 설정의 `ddl-auto: validate`를 유지하므로 배포 전에 `schema-mysql.sql`과 호환되는 스키마가 RDS에 있어야 한다.
- 스키마가 맞지 않으면 BE가 시작되지 않으며 이 경우 배포 health check 실패와 롤백으로 처리한다.

## 8. GitHub Variables와 Secrets

BE와 FE GitHub 저장소 각각에 `production` Environment를 만들고 다음 값을 설정한다.

| 구분 | 이름 | 용도 |
|---|---|---|
| Variable | `DOCKERHUB_USERNAME` | Docker Hub namespace |
| Secret | `DOCKERHUB_TOKEN` | 이미지 push용 access token |
| Secret | `EC2_HOST` | EC2 주소 |
| Secret | `EC2_USER` | SSH 사용자 |
| Secret | `EC2_SSH_PRIVATE_KEY` | 배포용 SSH 개인키 |
| Secret | `EC2_KNOWN_HOSTS` | 검증된 EC2 host key |

RDS 비밀번호와 JWT secret은 EC2의 `.env.prod`에만 저장하고 GitHub Secrets에 중복 저장하지 않는다. workflow의 권한은 기본적으로 `contents: read`만 부여한다.

## 9. `latest` 배포와 자동 롤백

사용자가 선택한 정책에 따라 Docker Hub에는 `latest` 태그만 push한다. 커밋 SHA 태그를 사용하지 않으므로 롤백은 EC2에 남아 있는 직전 로컬 이미지 한 개만 보장한다.

`deploy-service.sh`는 다음 순서로 동작한다.

1. `flock`으로 `/opt/hobbyloop`의 배포를 잠근다.
2. 대상 서비스명은 `backend` 또는 `frontend`만 허용한다.
3. 현재 컨테이너의 이미지 ID를 확인한다.
4. 기존 이미지가 있으면 해당 이미지를 서비스별 `rollback` 태그로 보존한다.
5. `docker compose pull <service>`로 새 `latest` 이미지를 받는다.
6. `docker compose up -d --no-deps --force-recreate <service>`로 대상 서비스만 교체한다.
7. 제한 시간 동안 컨테이너의 health 상태를 확인한다.
8. 정상이라면 성공 종료한다.
9. 비정상이면 보존한 이미지를 다시 `latest`로 지정하고 이전 컨테이너를 복원한다.
10. 이전 이미지가 없는 최초 배포 실패라면 롤백 불가를 출력하고 실패 종료한다.

서로 다른 FE와 BE workflow는 GitHub의 concurrency 설정을 공유하지 못하므로 EC2의 `flock`이 반드시 필요하다.

## 10. 검증 계획

### 10.1 정적 검증

- GitHub Actions YAML 문법 검사
- `docker compose -f docker-compose.prod.yaml config --quiet`
- Docker build context에 `.env*`, Git 메타데이터, 빌드 산출물이 포함되지 않는지 확인
- 양쪽 저장소에서 `src/**` 변경이 없는지 확인
- EC2 `.env.prod`와 SSH 키가 Git 추적 대상이 아닌지 확인

### 10.2 CI 검증

- BE PR에서 Gradle 전체 build/test가 통과하고 publish/deploy가 생략되는지 확인
- FE PR에서 `npm ci`, test, production build가 통과하고 publish/deploy가 생략되는지 확인
- 테스트 실패 시 이미지 push와 EC2 접속이 실행되지 않는지 확인
- `main` push에서만 `latest` 이미지가 갱신되는지 확인

### 10.3 EC2 통합 검증

1. Nginx 첫 화면 응답 확인
2. React Router 하위 경로 직접 접근 및 새로고침 확인
3. `/api/`를 통한 로그인과 게시글 API 확인
4. `/public/` 업로드 이미지 응답 확인
5. `/ws-chat` WebSocket 연결과 메시지 송수신 확인
6. backend가 RDS에 연결되고 healthy 상태가 되는지 확인
7. backend 8080 포트가 외부에 노출되지 않았는지 확인
8. backend 재생성 후 업로드 파일이 유지되는지 확인
9. FE와 BE 배포를 동시에 실행해 서버 배포가 직렬화되는지 확인
10. health check 실패 이미지를 테스트 환경에 배포해 직전 이미지로 자동 복구되는지 확인

## 11. 완료 기준

- 각 저장소의 `main` push가 자기 서비스의 CI/CD를 자동 실행한다.
- 빌드 또는 테스트 실패 시 이미지 push와 배포가 실행되지 않는다.
- GitHub Actions 러너가 이미지를 만들고 Docker Hub에 `latest`로 push한다.
- EC2는 소스 코드를 빌드하지 않고 이미지를 pull해 대상 컨테이너만 교체한다.
- 외부 접근은 FE/Nginx 포트로 제한되고 BE는 내부 네트워크에만 노출된다.
- BE가 외부 MySQL RDS에 정상적으로 연결된다.
- REST API, 업로드 파일, React Router, WebSocket이 Nginx를 통해 동작한다.
- 신규 컨테이너 장애 시 직전 로컬 이미지로 자동 복구된다.
- 비밀값이 Git, Docker 이미지 및 Actions 로그에 포함되지 않는다.
- 양쪽 저장소의 애플리케이션 `src/**`에는 변경사항이 없다.

## 12. 확정된 전제

- EC2 아키텍처는 `linux/amd64`이다.
- FE와 Nginx는 하나의 이미지로 배포한다.
- FE와 BE 저장소는 독립적인 workflow를 사용한다.
- EC2 배포는 SSH를 사용한다.
- 운영 파일은 EC2 `/opt/hobbyloop`에 최초 한 번 설치한다.
- Docker Hub 태그는 `latest`만 사용한다.
- 롤백은 EC2에 보존한 직전 로컬 이미지 한 개만 지원한다.
