# GitHub OIDC + AWS SSM 기반 EC2 CI/CD 전환

## 1. 요약

- 기존 `verify → Docker Hub publish → SSH deploy` 구조를 `verify → Docker Hub publish → GitHub OIDC Role Assume → SSM Run Command deploy`로 변경한다.
- PR에서는 테스트만 실행하고, `main` push에서만 이미지 push와 운영 배포를 수행한다.
- 애플리케이션 API와 현재 `nc` 기반 컨테이너 헬스체크는 변경하지 않는다.
- 이미지는 `sha-<commit>`과 `latest` 두 태그로 push하며 실제 배포에는 불변 SHA 태그를 사용한다.

## 2. 구현 변경

### 2.1 GitHub Actions

`.github/workflows/cicd.yml`을 다음과 같이 변경한다.

- 최상위 권한은 `contents: read`로 유지하고 deploy job에만 `id-token: write`를 추가한다.
- 기존 SSH 키 설정과 `ssh` 실행 단계를 제거한다.
- Docker Hub에는 `latest`, `sha-${{ github.sha }}`를 함께 push한다.
- `aws-actions/configure-aws-credentials`의 현재 v6 릴리스를 커밋 SHA로 고정해 `production` IAM Role을 Assume한다.
- `AWS-RunShellScript`로 다음 명령을 단일 EC2에 전송한다.

```shell
cd /opt/hobbyloop && ./deploy-service.sh backend <SHA 이미지 참조>
```

- 반환된 Command ID를 사용해 SSM 상태를 5초 간격으로 최대 6분 폴링한다. 초기 `InvocationDoesNotExist`는 재시도하고, 완료 시 stdout/stderr를 출력한다.
- `Success` 및 원격 `ResponseCode=0`일 때만 성공 처리한다.
- `Failed`, `TimedOut`, `Cancelled`, `Undeliverable`, 폴링 시간 초과는 workflow 실패로 처리한다.
- 기본 SSM waiter는 현재 120초 헬스체크보다 먼저 끝날 수 있으므로 사용하지 않는다.

참고 문서:

- [AWS CLI command-executed waiter](https://docs.aws.amazon.com/cli/latest/reference/ssm/wait/command-executed.html)
- [AWS Configure Credentials Action](https://github.com/aws-actions/configure-aws-credentials)

### 2.2 Docker Compose와 배포 스크립트

`docker-compose.prod.yaml`과 `deploy-service.sh`를 다음과 같이 변경한다.

- backend 이미지 값을 `BACKEND_IMAGE` 환경변수로 주입할 수 있게 하고 기본값은 현재 Docker Hub `latest` 이미지로 유지한다.
- 배포 스크립트 인터페이스를 `deploy-service.sh backend [image-ref]`로 확장한다.
- private Docker Hub용 read-only 토큰을 EC2의 SSM Parameter Store SecureString에서 조회한다.
- 토큰은 `docker login --password-stdin`으로 전달하고 로그에 출력하지 않는다.
- 지정된 SHA 이미지로 `pull → up -d --no-deps --force-recreate → container health 대기`를 수행한다.
- 기존 `flock`, 직전 이미지 보존, health 실패 시 롤백을 유지한다.
- 롤백 성공 여부와 관계없이 신규 배포 실패는 exit code 1로 반환해 SSM과 workflow도 실패시킨다.
- frontend 배포 인터페이스와 동작은 깨지지 않도록 유지한다.

### 2.3 배포 문서

- 기존 SSH 기반 설명과 SSH Secrets 목록을 OIDC/SSM 구조로 교체한다.
- EC2 최초 설치, IAM 정책, Parameter Store, GitHub Environment 설정 및 장애 확인 절차를 기록한다.

## 3. AWS 및 GitHub 설정

### 3.1 GitHub 설정

`production` Environment에 다음 Variables를 등록한다.

| 이름 | 용도 |
|---|---|
| `AWS_REGION` | EC2와 SSM을 사용하는 AWS 리전 |
| `AWS_ROLE_ARN` | GitHub OIDC가 Assume할 IAM Role ARN |
| `EC2_INSTANCE_ID` | 배포 대상 EC2 Instance ID |
| `DOCKERHUB_USERNAME` | Docker Hub namespace |

다음 Secret을 유지한다.

| 이름 | 용도 |
|---|---|
| `DOCKERHUB_TOKEN` | 이미지 push 전용 write 토큰 |

다음 SSH Secrets는 OIDC/SSM 전환 완료 후 제거한다.

- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_PRIVATE_KEY`
- `EC2_KNOWN_HOSTS`

### 3.2 GitHub OIDC Role

OIDC Role의 신뢰 정책은 다음 조건으로 제한한다.

- audience: `sts.amazonaws.com`
- subject: `repo:100-hours-a-week/KTB4_Jayden_Week12_BE:environment:production`

Role에는 다음 최소 권한을 부여한다.

- `AWS-RunShellScript` 문서와 지정된 EC2에 대한 `ssm:SendCommand`
- 명령 결과 확인용 `ssm:GetCommandInvocation`

### 3.3 EC2 설정

- SSM Agent가 online 상태여야 한다.
- Instance Profile에 `AmazonSSMManagedInstanceCore` 상당 권한이 있어야 한다.
- `/opt/hobbyloop`에 최신 Compose 파일, 실행 권한이 있는 배포 스크립트, `.env.prod`가 설치돼 있어야 한다.
- Docker, Docker Compose Plugin, AWS CLI, `flock`, `nc`가 설치돼 있어야 한다.
- `/hobbyloop/production/dockerhub-read-token` SecureString에 별도의 pull-only Docker Hub 토큰을 저장한다.
- Instance Profile에는 해당 Parameter 조회 권한을 추가한다.
- SecureString에 custom KMS key를 사용한다면 해당 키의 `kms:Decrypt` 권한도 추가한다.

## 4. 배포 흐름

1. `main` 대상 PR에서 MySQL 서비스 기반 Gradle 테스트를 실행한다.
2. PR에서는 publish와 deploy job을 실행하지 않는다.
3. `main` push에서 테스트가 성공하면 Docker 이미지를 빌드한다.
4. 동일 이미지를 `latest`와 `sha-${{ github.sha }}`로 Docker Hub에 push한다.
5. deploy job이 GitHub OIDC 토큰으로 AWS IAM Role을 Assume한다.
6. SSM Run Command로 EC2의 `deploy-service.sh`에 SHA 이미지 참조를 전달한다.
7. EC2가 Docker Hub read-only 토큰으로 로그인하고 지정 이미지를 pull한다.
8. Docker Compose가 backend 컨테이너를 교체한다.
9. 배포 스크립트가 제한 시간 동안 컨테이너 health 상태를 확인한다.
10. 정상이라면 원격 명령과 workflow를 성공 처리한다.
11. 비정상이라면 직전 이미지를 복원하고 원격 명령을 실패 처리한다.
12. GitHub Actions는 SSM 상태와 exit code를 확인해 workflow를 실패 처리한다.

## 5. 검증 계획

### 5.1 정적 검증

- GitHub Actions YAML 문법을 검사한다.
- 배포 스크립트의 Shell 문법과 실행 권한을 검사한다.
- `docker compose -f docker-compose.prod.yaml config`를 실행한다.
- AWS 장기 access key, SSH private key, Docker Hub 토큰이 Git에 포함되지 않았는지 확인한다.

### 5.2 CI/CD 검증

- PR에서는 Gradle 테스트만 실행되고 publish/deploy가 생략되는지 확인한다.
- `main` push에서 두 이미지 태그가 동일 digest로 push되는지 확인한다.
- SSM 명령에 `sha-${{ github.sha }}` 이미지가 전달되는지 확인한다.
- 정상 배포 시 SSM 출력, 컨테이너 교체, `healthy`, workflow 성공을 확인한다.

### 5.3 실패 시나리오

- 테스트 또는 Docker build/push 실패 시 deploy가 실행되지 않아야 한다.
- OIDC trust 또는 IAM 권한 오류 시 SSM이 호출되지 않고 workflow가 실패해야 한다.
- EC2가 SSM offline 상태이거나 명령이 timeout되면 workflow가 실패해야 한다.
- 잘못된 Docker Hub read token이나 존재하지 않는 SHA 태그를 사용하면 기존 컨테이너를 유지하고 workflow가 실패해야 한다.
- health 실패 이미지를 배포하면 직전 이미지로 롤백한 후 workflow가 실패해야 한다.
- SSM 출력에 Docker Hub 토큰이나 애플리케이션 비밀값이 노출되지 않아야 한다.

## 6. 완료 기준

- GitHub Actions에 AWS access key와 EC2 SSH key가 존재하지 않는다.
- `main` push에서 테스트, 이미지 build/push, OIDC 인증, SSM 배포가 순서대로 수행된다.
- 배포에는 정확한 커밋 SHA 이미지가 사용된다.
- SSM 원격 명령의 exit code가 GitHub Actions 결과에 반영된다.
- 신규 컨테이너 health 실패 시 직전 이미지로 복구된다.
- EC2는 애플리케이션 소스를 checkout하거나 빌드하지 않고 이미지 pull과 컨테이너 교체만 수행한다.

## 7. 확정된 전제

- 이미지 레지스트리는 private Docker Hub를 사용한다.
- GitHub push 토큰과 EC2 pull-only 토큰을 분리한다.
- AWS 인프라는 저장소에서 IaC로 생성하지 않고 기존 리소스를 설정해 사용한다.
- 배포 대상은 단일 EC2 Instance ID이며 운영 디렉터리는 `/opt/hobbyloop`이다.
- 헬스체크는 현재 Docker Compose의 `nc -z 127.0.0.1 8080` 방식을 유지한다.
- `production` Environment의 승인 규칙은 GitHub 저장소 설정에서 관리한다.
