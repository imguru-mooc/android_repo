# Android Framework 16 심화 교육 — 강사 스크립트 (정답 포함)

> **과정:** 4일 32시간 (하루 8시간)  
> **대상:** Android 시스템/Framework 개발자 (고급)  
> **실습 환경:** Windows 11 + Android Studio Emulator (API 36)  
> **슬라이드:** 102장

---

## 📋 수업 운영 가이드

### 시간 배분 원칙
| 구분 | 시간 | 비율 |
|------|------|------|
| 이론 강의 | 4시간 | 50% |
| 실습 | 2.5시간 | 31% |
| Q&A / 토론 | 1시간 | 13% |
| 휴식 | 30분 | 6% |

### 하루 일정 템플릿
- **09:00~10:30** — 오전 1교시 (90분)
- **10:30~10:45** — 휴식
- **10:45~12:15** — 오전 2교시 (90분)
- **12:15~13:15** — 점심
- **13:15~14:45** — 오후 1교시 (90분)
- **14:45~15:00** — 휴식
- **15:00~16:30** — 오후 2교시 (90분)
- **16:30~17:00** — 정리 / Q&A

### 강의 팁
- 슬라이드를 읽지 말고 다이어그램을 가리키며 **스토리텔링** 방식으로 진행
- 코드 슬라이드는 **★ 핵심 패턴**에 집중
- 트러블슈팅 슬라이드는 **수강생 경험을 먼저 물어본 뒤** 해결법 공개
- 퀴즈는 즉석 토론 → 강사가 정답/해설 제시 순서로 진행

---

# 🔷 DAY 1: Android 16 아키텍처 & Binder IPC 기초

### ⏰ 시간 배분
| 시간 | 내용 |
|------|------|
| 09:00~09:20 | 과정 소개, 환경 확인 (#1~#4) |
| 09:20~10:30 | Ch 0: 아키텍처 개요 (#5~#12) |
| 10:45~12:15 | Ch 1: Binder IPC (#13~#21) |
| 13:15~14:45 | Ch 2: AIDL 실습 (#22~#30) |
| 15:00~16:30 | Ch 3: IPC 서비스 개발 (#31~#35) |
| 16:30~17:00 | Day 1 정리 / Q&A |

---

## 슬라이드 #1 — 타이틀

> "안녕하세요, 4일간의 Android Framework 16 심화 교육을 시작합니다."

> "이 과정은 앱 개발이 아니라, **Android OS 내부에서 무슨 일이 일어나는지** 깊이 이해하는 게 목표입니다. `startActivity()` 한 줄이 커널까지 어떤 여정을 거치는지, 그 전체 그림을 그리게 됩니다."

> "실습은 Windows 11 + Android Studio 에뮬레이터, API 36 타겟입니다."

**[수강생에게]** "에뮬레이터 설정이 안 되신 분 계신가요?"

---

## 슬라이드 #2 — 실습 환경 구성

**Windows 11 카드:**
> "Hyper-V와 WHPX가 모두 활성화되어야 합니다. HAXM이 설치되어 있으면 **반드시 제거** — Hyper-V와 충돌합니다."

**Android Studio 카드:**
> "SDK Manager에서 Android 16(API 36)을 다운받고, 에뮬레이터는 **Google APIs 포함 x86_64 이미지**를 사용합니다. Google APIs 이미지여야 `adb root`가 가능합니다."

**AOSP 카드:**
> "직접 빌드하진 않지만, `cs.android.com`에서 소스를 읽으며 학습합니다."

**디버깅 도구 카드:**
> "가장 많이 쓸 도구는 `adb shell dumpsys`입니다. Perfetto는 systrace를 대체하는 Android 16 기본 프로파일러입니다."

---

## 슬라이드 #3 — 교육 일정 개요

> "4일의 큰 그림입니다."

> "Day 1: 아키텍처 + Binder 기초, Day 2: Binder 심화 + System Service, Day 3: Framework Core + Overlay + Jetpack, Day 4: SystemUI + APEX."

> "각 챕터마다 **현업 오류 + 디버깅 방법**이 있습니다."

---

## 슬라이드 #4 — DAY 1 구분

> "Day 1을 시작합니다. 오늘의 키워드는 **아키텍처**와 **Binder**."

---

## Chapter 0: Android 시스템 아키텍처 개요

### 슬라이드 #6 — 아키텍처 계층도

**[아래에서 위로 설명]**

> "맨 아래 **Linux Kernel** — Binder Driver가 여기 있습니다. 오후에 깊이 다룹니다."

> "**HAL** — Android 16에서 **HIDL이 완전 deprecated**, 새 HAL은 반드시 AIDL입니다."

> "**Native + ART** — ART는 AOT + JIT 하이브리드 컴파일."

> "**Java API Framework** — `getSystemService()`로 접근하는 서비스들."

> "**Applications** — 시스템 앱과 사용자 앱. 모든 계층 간 통신은 **Binder**를 통합니다."

---

### 슬라이드 #7 — Android 16 주요 변경사항

**[번호 하나씩 짚으며]**

> "1번 AIDL HAL — 기존 HIDL HAL은 유지보수만 가능, 신규는 AIDL 필수."

> "5번 16KB Page Size — Native 코드에서 4KB 페이지를 가정한 코드가 있다면 깨질 수 있습니다."

**[수강생에게]** "HIDL HAL 아직 사용하시는 분? AIDL 전환 계획은?"

---

### 슬라이드 #8 — 부팅 시퀀스

> "Bootloader → Kernel → Init → Zygote → System Server → Launcher"

> "**Zygote** — 모든 앱의 부모. ART VM + 클래스를 미리 로딩하고, 앱 실행 시 `fork()`를 합니다. **Copy-on-Write** 방식이라 메모리를 실제 복사하지 않고 공유합니다."

---

### 슬라이드 #10 — 현업 트러블슈팅: 부팅

> "**Zygote crash loop** — `logcat -b main -s Zygote` + `dmesg`로 SELinux 정책 또는 preloaded-classes 충돌 확인."

> "**에뮬레이터 부팅 실패** — Hyper-V + WHPX 모두 활성화 확인, HAXM 제거."

---

### 슬라이드 #11 — Chapter 0 퀴즈

#### Q1. Android 16에서 HIDL 대신 AIDL HAL을 사용하는 이유를 설명하시오.

**✅ 정답 및 해설:**

> "AIDL HAL의 장점은 크게 4가지입니다."

1. **IPC 메커니즘 통합**: AIDL은 Framework와 동일한 Binder IPC를 사용합니다. HIDL은 별도의 `hwservicemanager`가 필요했지만, AIDL은 기존 `servicemanager`와 통합되어 시스템 복잡도가 줄어듭니다.

2. **개발 도구 통합**: AIDL은 Framework 개발에 쓰는 동일한 도구(aidl 컴파일러, dumpsys, service call)로 HAL도 디버깅할 수 있습니다. HIDL은 별도 도구 체인이 필요했습니다.

3. **안정성 인터페이스(Stable AIDL)**: Vendor와 Framework 간의 인터페이스를 버전 관리하면서도 하위 호환성을 보장합니다. Treble 아키텍처의 목표를 더 잘 달성합니다.

4. **코드 중복 제거**: HIDL은 AIDL과 별도의 IDL 문법과 런타임을 가졌으나, AIDL로 통합하면 하나의 IDL로 Framework IPC와 HAL IPC를 모두 처리할 수 있습니다.

---

#### Q2. Zygote의 fork() + COW가 메모리 효율에 미치는 영향을 설명하시오.

**✅ 정답 및 해설:**

> "핵심은 **Copy-on-Write(COW)** 메커니즘입니다."

1. **fork() 호출 시**: Zygote 프로세스의 메모리를 **실제로 복사하지 않습니다**. 대신 부모와 자식 프로세스의 페이지 테이블이 **동일한 물리 페이지**를 가리키도록 설정하고, 해당 페이지를 **읽기 전용**으로 표시합니다.

2. **쓰기 발생 시**: 어느 한쪽이 페이지에 쓰기를 시도하면, 커널이 **Page Fault**를 발생시키고, 그때서야 해당 페이지만 복사합니다. 이것이 "Copy-on-Write"입니다.

3. **메모리 절약**: Zygote가 미리 로딩한 수천 개의 Framework 클래스, 리소스, 공유 라이브러리 등은 대부분 **읽기 전용**이므로 실제로 복사가 거의 발생하지 않습니다. 100개 앱이 실행되어도 이 공통 부분은 물리 메모리에 **한 벌만 존재**합니다.

4. **빠른 시작**: 새 프로세스를 처음부터 만드는 것(exec)에 비해, fork()는 페이지 테이블 복사만 하면 되므로 **수십 밀리초** 수준으로 빠릅니다.

> "한 줄 요약: fork + COW = 메모리 공유로 절약 + 빠른 프로세스 생성."

---

#### Q3. System Server 시작 후 Launcher까지 도달하는 과정을 설명하시오.

**✅ 정답 및 해설:**

1. **Zygote가 System Server를 fork**: `ZygoteInit.forkSystemServer()`를 통해 system_server 프로세스가 생성됩니다.

2. **System Server 초기화**: `SystemServer.main()` → `run()` 메서드에서 핵심 서비스들을 순차적으로 시작합니다.
   - `startBootstrapServices()`: AMS, PMS, PowerManagerService 등 부팅에 필수적인 서비스
   - `startCoreServices()`: BatteryService, UsageStatsService 등
   - `startOtherServices()`: WMS, InputMethodManager, StatusBarService 등 나머지

3. **AMS가 시스템 준비 완료 선언**: `AMS.systemReady()`가 호출되면, 시스템이 사용자 상호작용을 받을 준비가 되었음을 의미합니다.

4. **Home Intent 발송**: AMS가 `Intent.CATEGORY_HOME`을 가진 Intent를 발송합니다. PMS의 Intent Resolution이 Launcher 앱을 찾습니다.

5. **Launcher 프로세스 생성**: 해당 Launcher 앱의 프로세스가 없으면 Zygote에게 fork를 요청하고, ActivityThread.main()이 실행되어 Launcher의 Activity가 시작됩니다.

---

#### Q4. 에뮬레이터 부팅 실패 시 디버깅 절차를 단계별로 설명하시오.

**✅ 정답 및 해설:**

1. **하드웨어 가속 확인**: Windows 기능에서 Hyper-V + Windows Hypervisor Platform(WHPX) 활성화 확인. 설정 → 앱 → 선택적 기능 → 추가 Windows 기능에서 체크.

2. **HAXM 충돌 확인**: Intel HAXM이 설치되어 있으면 제거. Hyper-V와 HAXM은 공존 불가.

3. **에뮬레이터 로그 확인**: Android Studio → Logcat에서 에뮬레이터 부팅 로그 확인. 또는 `%LOCALAPPDATA%\Android\Sdk\emulator\` 경로에서 직접 에뮬레이터를 `-verbose` 옵션으로 실행.

4. **AVD 재생성**: 기존 AVD를 삭제하고 새로 생성. API 36 Google APIs x86_64 이미지 확인.

5. **Cold Boot**: AVD Manager에서 "Cold Boot Now" 선택 (스냅샷 문제 회피).

---

## Chapter 1: Binder 개념 및 IPC 이해

### 슬라이드 #12~13 — IPC 비교 + Binder 아키텍처

> "왜 Binder인가? — **성능**(1회 복사), **보안**(커널 UID/PID), **편의성**(객체지향 RPC)."

**[다이어그램에서 Client→Proxy→Driver→Stub→Service 흐름 설명]**

> "핵심: **mmap**으로 커널 버퍼와 서버 주소 공간이 동일 물리 페이지 → 서버 측 zero-copy."

---

### 슬라이드 #17 — 현업 트러블슈팅: Binder

> "**TransactionTooLargeException** — 1MB 제한. Intent에 Bitmap 넣으면 바로 터집니다."

> "**DeadObjectException** — `linkToDeath()` 사전 등록으로 감지."

---

### 슬라이드 #19 — Chapter 1 퀴즈

#### Q1. TransactionTooLargeException의 발생 원인과 해결 방법을 3가지 설명하시오.

**✅ 정답 및 해설:**

**발생 원인**: Binder 트랜잭션 버퍼는 프로세스당 약 **1MB**로 제한됩니다. 단일 트랜잭션이 이를 초과하거나, 여러 트랜잭션이 동시에 처리되어 합산이 1MB를 넘으면 발생합니다.

**흔한 발생 시나리오:**
- `Intent.putExtra()`에 대용량 Bitmap이나 ArrayList를 넣는 경우
- `onSaveInstanceState()`에서 대량 데이터를 Bundle에 저장하는 경우
- Content Provider의 `query()` 결과가 CursorWindow 1MB를 초과하는 경우

**해결 방법 3가지:**

1. **ContentProvider / FileProvider 사용**: 대용량 데이터는 직접 전달하지 말고, ContentProvider로 URI를 전달하여 수신측이 필요할 때 가져가도록 합니다.

2. **공유 메모리(ashmem/memfd) 활용**: Bitmap 등은 `Bundle.putBinder()`를 통해 공유 메모리 기반으로 전달합니다. Android 16에서는 `memfd`가 ashmem을 대체하고 있습니다.

3. **데이터 구조 최적화**: Parcelable 직렬화 시 불필요한 필드를 `transient`로 제외하거나, 대용량 리스트를 페이징 처리합니다. `onSaveInstanceState()`에는 ID/키만 저장하고 실제 데이터는 ViewModel이나 Repository에서 복원합니다.

> "💡 디버깅 팁: `adb shell dumpsys activity provider`로 CursorWindow 크기를 확인할 수 있습니다."

---

#### Q2. mmap을 통한 1회 복사 원리를 물리 페이지 관점에서 설명하시오.

**✅ 정답 및 해설:**

1. **서버 프로세스가 Binder를 열 때**: `binder_mmap()`이 호출되어 서버의 가상 주소 공간에 일정 영역을 매핑합니다. 이때 **커널 버퍼의 물리 페이지**와 **서버 mmap 영역의 페이지 테이블 엔트리(PTE)**가 **동일한 물리 프레임**을 가리키도록 설정됩니다.

2. **Client가 데이터를 보낼 때**: `copy_from_user()`로 클라이언트 사용자 공간에서 **커널 버퍼로 1회 복사**합니다. 이것이 유일한 데이터 복사입니다.

3. **서버가 데이터를 읽을 때**: 커널 버퍼에 쓰인 데이터는 이미 서버의 mmap 영역에서 **자동으로 보입니다** — 같은 물리 페이지이므로. 추가 복사(`copy_to_user()`)가 필요 없습니다.

4. **비교 (Socket의 경우)**: 
   - 1회: Client → Kernel 소켓 버퍼 (`copy_from_user()`)
   - 2회: Kernel 소켓 버퍼 → Server (`copy_to_user()`)
   - 총 2회 복사 → Binder는 1회만.

> "핵심: 페이지 테이블 트릭으로 커널 버퍼 == 서버 사용자 공간. 물리적으로 같은 메모리."

---

#### Q3. FLAG_ONEWAY 사용 시 주의사항과 콜백 패턴을 설명하시오.

**✅ 정답 및 해설:**

**FLAG_ONEWAY 동작:**
- Client의 `transact()`가 **즉시 반환**됩니다 (블로킹 없음)
- 서버에서 처리 결과를 Client에게 **직접 반환할 수 없습니다** (reply가 없음)
- 서버에서 발생한 **예외가 Client에 전달되지 않습니다**

**주의사항:**
1. **순서 보장**: 같은 Binder 객체에 대한 oneway 호출은 **순서가 보장**됩니다. 하지만 다른 Binder 객체에 대한 호출은 순서가 보장되지 않습니다.
2. **에러 핸들링**: 서버가 크래시하거나 예외가 발생해도 Client는 알 수 없으므로, 별도의 에러 통지 메커니즘이 필요합니다.
3. **버퍼 관리**: oneway 호출도 Binder 버퍼를 사용하므로, 대량의 oneway 호출이 연속되면 버퍼가 가득 찰 수 있습니다.

**콜백 패턴:**
```
// 서버에서 결과를 전달하는 콜백 AIDL
interface ICalcCallback {
    oneway void onResult(int result);    // 콜백도 oneway 권장
    oneway void onError(String message);
}

// 메인 AIDL
interface ICalcService {
    oneway void calcAsync(int a, int b, ICalcCallback callback);
}
```

> "Client가 콜백 객체를 넘기면, 서버가 처리 후 콜백의 onResult()를 호출합니다. 콜백도 oneway로 선언하면 서버도 블로킹되지 않습니다."

---

## Chapter 2: Binder 실습 (AIDL)

### 슬라이드 #20~24 — AIDL 컴파일, 서비스 구현, 클라이언트

> "AIDL 파일 하나로 Stub + Proxy 자동 생성. 개발자는 Stub 구현과 Client 호출만 작성합니다."

**핵심 패턴 2가지:**
1. `Binder.getCallingUid()` — 호출자 확인
2. `clearCallingIdentity()` / `restoreCallingIdentity()` — 시스템 서비스에서 필수

---

### 슬라이드 #27 — Chapter 2 퀴즈

#### Q1. Stub.asInterface()의 내부 동작을 같은/다른 프로세스 경우로 나누어 설명하시오.

**✅ 정답 및 해설:**

**`Stub.asInterface(IBinder obj)` 내부 코드 흐름:**

```java
public static IMyService asInterface(IBinder obj) {
    if (obj == null) return null;
    
    // 1단계: 로컬 인터페이스 확인
    IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
    
    if (iin != null && iin instanceof IMyService) {
        // 같은 프로세스: Stub 객체 직접 반환
        return (IMyService) iin;
    }
    
    // 다른 프로세스: Proxy 객체 생성
    return new Stub.Proxy(obj);
}
```

**같은 프로세스인 경우:**
- `obj`는 실제 `Stub` 구현체 자체 (BinderProxy가 아닌 Binder)
- `queryLocalInterface()`가 Stub 자신을 반환
- **IPC가 발생하지 않음** — 직접 메서드 호출 (일반 Java 메서드 호출과 동일)
- 성능 오버헤드 거의 없음

**다른 프로세스인 경우:**
- `obj`는 `BinderProxy` 객체 (커널의 binder_ref에 대응)
- `queryLocalInterface()`가 null 반환
- `Stub.Proxy` 객체를 새로 생성하여 반환
- 이후 메서드 호출마다 **Parcel 직렬화 → transact() → ioctl → 커널 → onTransact()** 경로를 거침
- Binder IPC 오버헤드 발생

> "이 구분이 중요한 이유: Local Bound Service는 IPC 없이 직접 호출되므로 성능이 좋고, Remote Bound Service는 매 호출마다 직렬화/역직렬화 비용이 발생합니다."

---

#### Q2. AIDL Parcelable 직렬화 순서 불일치 시 발생하는 문제와 디버깅 방법을 설명하시오.

**✅ 정답 및 해설:**

**문제 현상:**
- **크래시가 나지 않을 수 있어서** 더 위험합니다
- String 필드에 int 값이 들어가거나, int에 엉뚱한 큰 숫자가 들어감
- Unmarshalling 과정에서 `BadParcelableException`이 발생할 수도 있음
- 가장 흔한 증상: 데이터가 "이상하게" 변질되어 로직 오류 발생

**원인 예시:**
```java
// writeToParcel: name → age 순서
public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name);  // 순서 1
    dest.writeInt(age);      // 순서 2
}

// createFromParcel: age → name 순서 (잘못됨!)
public CalcResult createFromParcel(Parcel in) {
    int age = in.readInt();      // String 데이터를 int로 읽음!
    String name = in.readString(); // int 데이터를 String으로 읽음!
}
```

**디버깅 방법:**
1. `writeToParcel()`과 `createFromParcel()`을 **나란히 놓고** read/write 순서 1:1 대응 확인
2. `Parcel.dataSize()`로 직렬화 전후 크기 비교 — 비정상적으로 크거나 작으면 타입 불일치
3. `Parcel.marshall()` / `unmarshall()`로 바이트 배열을 덤프하여 수동 분석
4. **단위 테스트 작성**: Parcelable 객체를 write → read 후 equals()로 비교하는 테스트

> "예방 최선책: `@AutoValue`나 Kotlin `data class`의 `@Parcelize`를 사용하면 컴파일러가 자동 생성하므로 순서 실수가 원천 차단됩니다."

---

#### Q3. clearCallingIdentity()가 필요한 구체적 시나리오를 3가지 제시하시오.

**✅ 정답 및 해설:**

**시나리오 1: 시스템 서비스가 다른 시스템 서비스를 호출할 때**
```
앱(UID 10055) → MyService.getData() → 내부에서 ContentResolver.query() 호출
```
- ContentProvider는 호출자 UID로 권한을 체크함
- clearCallingIdentity() 없이 호출하면 호출자가 "앱(UID 10055)"으로 남아있어 시스템 전용 Provider 접근이 거부됨
- clear 후에는 호출자가 system_server(UID 1000)로 바뀌어 접근 가능

**시나리오 2: 시스템 서비스가 Settings.System/Settings.Secure에 값을 쓸 때**
```
앱 → MyService.setConfig(value) → Settings.Secure.putInt()
```
- Settings.Secure 쓰기는 시스템 권한이 필요
- 호출자가 앱으로 남아있으면 SecurityException 발생
- clearCallingIdentity()로 시스템 UID로 전환 필요

**시나리오 3: 시스템 서비스가 PackageManager 정보를 조회할 때**
```
앱 → MyService.checkApp(pkg) → PackageManager.getPackageInfo(pkg, flags)
```
- 특정 flags(예: GET_SIGNING_CERTIFICATES)는 시스템 권한이 필요
- 호출자가 일반 앱이면 해당 정보를 받을 수 없음
- clearCallingIdentity()로 시스템으로 전환하여 전체 정보 조회

> "**반드시 finally 블록에서 restoreCallingIdentity()**를 호출하세요. 안 하면 이후 모든 호출의 calling identity가 꼬입니다."

---

## Chapter 3: 간단한 IPC 서비스 개발

### 슬라이드 #33 — Chapter 3 퀴즈

#### Q1. Content Provider가 Binder를 사용하는 방식과 CursorWindow의 역할을 설명하시오.

**✅ 정답 및 해설:**

**Binder 사용 방식:**
- Content Provider는 `IContentProvider` AIDL 인터페이스를 구현합니다
- Client의 `ContentResolver.query()`는 내부적으로 `IContentProvider.query()`를 **Binder IPC**로 호출합니다
- Provider가 다른 프로세스에 있으면 실제 Binder Transaction이 발생
- 같은 프로세스에 있으면 직접 호출 (asInterface() 메커니즘)

**CursorWindow의 역할:**
- query() 결과를 담는 **공유 메모리 기반 버퍼**입니다
- 내부적으로 `ashmem` (Android 16에서는 `memfd`)를 사용하여 프로세스 간 메모리를 공유합니다
- Binder Transaction의 1MB 제한을 우회하는 메커니즘 — CursorWindow 자체의 **파일 디스크립터**만 Binder로 전달
- 기본 크기: **2MB** (설정 변경 가능하나 1MB 이상의 단일 행이 있으면 실패)

> "CursorWindow의 FD가 Binder를 통해 전달되면, Client는 그 FD를 mmap하여 Provider의 결과 데이터에 직접 접근합니다. 데이터 자체가 Binder 버퍼를 거치지 않으므로 대량 데이터도 전달 가능합니다."

---

#### Q2. android:process 분리 시 주의사항 3가지를 설명하시오.

**✅ 정답 및 해설:**

1. **Application.onCreate() 중복 실행**: 각 프로세스마다 Application 인스턴스가 새로 생성되므로, onCreate()가 프로세스별로 한 번씩 호출됩니다. 무거운 초기화(Firebase, Analytics 등)가 불필요하게 중복 실행됩니다.
   - **해결**: `getProcessName()`으로 현재 프로세스를 확인하고, 메인 프로세스에서만 초기화하도록 분기 처리

2. **static 변수 비공유**: Java의 static 변수는 프로세스별로 독립입니다. 메인 프로세스에서 설정한 싱글톤이 remote 프로세스에서는 초기화되지 않은 상태입니다.
   - **해결**: 프로세스 간 데이터 공유는 반드시 Binder IPC(AIDL, ContentProvider, SharedPreferences-MODE_MULTI_PROCESS) 사용

3. **메모리 증가**: 각 프로세스가 별도의 Dalvik/ART VM을 갖으므로 메모리 사용량이 증가합니다. Zygote COW로 일부 공유되지만, 각 프로세스의 힙은 독립적입니다.
   - **해결**: 진짜 필요한 경우에만 프로세스 분리, 불필요한 분리는 제거

---

#### Q3. FileUriExposedException의 발생 원인과 해결 방법을 설명하시오.

**✅ 정답 및 해설:**

**발생 원인:** Android 7.0(API 24)부터 StrictMode의 `VmPolicy`에 의해, `file://` URI를 **Intent를 통해 다른 앱에 전달하는 것이 금지**되었습니다. 다른 앱의 private 파일 경로가 노출되는 보안 문제를 방지하기 위함입니다.

**해결 방법:**
1. `FileProvider`를 AndroidManifest에 선언
2. `res/xml/file_paths.xml`에 공유할 디렉토리 정의
3. `FileProvider.getUriForFile()`로 `content://` URI 생성
4. Intent에 `FLAG_GRANT_READ_URI_PERMISSION` 추가

```java
// 잘못된 코드 (크래시)
Uri uri = Uri.fromFile(file);

// 올바른 코드
Uri uri = FileProvider.getUriForFile(context, 
    "com.example.fileprovider", file);
intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
```

---

# 🔷 DAY 2: Binder 심화 & 커스텀 System Service

### Day 1 복습 (5분)

> "Binder에서 데이터 복사가 몇 번? → 1회 (mmap 덕분에 서버 zero-copy)"

---

## Chapter 4: Binder 심화 분석

### 슬라이드 #34 — mmap Zero-Copy 다이어그램

**[핵심 슬라이드 — 충분한 시간 할당]**

> "Client → Kernel: `copy_from_user()`로 1회 복사. Kernel Buffer와 Server mmap 영역이 동일 물리 페이지 → 서버 추가 복사 불필요."

💡 **강사 팁:** 화이트보드에 페이지 테이블을 그려서 Kernel PTE와 Server PTE가 같은 Physical Frame을 가리키는 그림을 보여주세요.

---

### 슬라이드 #39 — Chapter 4 퀴즈

#### Q1. Binder mmap zero-copy 원리를 물리 페이지 관점에서 설명하시오.

**✅ 정답 및 해설:**

(Ch1 Q2와 동일 원리이나 더 깊은 수준으로 설명)

**커널 레벨 동작:**
1. `binder_mmap()` 호출 시:
   - 서버 프로세스의 가상 주소 공간에 **vm_area_struct** 할당 (사용자 공간)
   - 대응하는 커널 버퍼 영역 할당
   - **물리 페이지는 on-demand**로 할당 (처음에는 비어있음)

2. 트랜잭션 발생 시:
   - `binder_alloc_buf()`가 커널 버퍼에서 트랜잭션 크기만큼 할당
   - **물리 페이지 할당 + 매핑**: 
     - `alloc_page(GFP_KERNEL)` → 물리 페이지 확보
     - 커널 가상 주소에 매핑 (`kmap()`)
     - **동시에** 서버 사용자 공간의 해당 오프셋에도 매핑 (`vm_insert_page()` 또는 `remap_pfn_range()`)
   - `copy_from_user()`로 Client 데이터를 이 물리 페이지에 복사

3. 서버가 `onTransact()`에서 Parcel을 읽을 때:
   - Parcel 내부의 데이터 포인터가 **서버 mmap 영역의 가상 주소**를 가리킴
   - 이 가상 주소는 위에서 매핑한 것과 동일한 물리 페이지 → **zero-copy 읽기**

> "핵심 트릭: `vm_insert_page()`가 커널 버퍼의 물리 페이지를 서버의 사용자 공간 VMA에 삽입하는 것."

---

#### Q2. Binder Thread Pool 데드락 시나리오와 해결 방법을 설명하시오.

**✅ 정답 및 해설:**

**데드락 시나리오:**
```
Process A (최대 스레드 15):
  스레드 1~15 모두: B의 서비스를 동기 호출 중 (reply 대기)

Process B:
  A의 요청을 처리하기 위해 A의 서비스를 동기 호출
  → A의 Binder Thread가 모두 B 응답 대기 중이므로
  → B의 요청을 처리할 A의 스레드가 없음
  → 교착 상태 (양쪽 모두 영원히 대기)
```

**실제 발생 사례:**
- ActivityManagerService → WindowManagerService → ActivityManagerService 순환 호출
- 앱의 서비스가 시스템 서비스를 호출하고, 시스템 서비스가 다시 앱의 콜백을 호출

**해결 방법:**
1. **oneway 사용**: 순환의 한 방향을 비동기로 전환하면 블로킹이 없으므로 스레드가 풀림
2. **호출 방향 단일화**: A→B만 허용, B→A는 콜백 인터페이스로 비동기 전달
3. **최대 스레드 수 증가**: `BINDER_SET_MAX_THREADS`로 풀 크기를 늘림 (근본 해결은 아님)
4. **아키텍처 재설계**: 순환 의존성 자체를 제거 — 중간 서비스를 도입하거나 이벤트 기반으로 전환

---

#### Q3. Perfetto로 Binder 성능 이슈를 분석하는 절차를 설명하시오.

**✅ 정답 및 해설:**

**1단계: 트레이스 수집**
```bash
adb shell perfetto -c - --txt -o /data/misc/perfetto-traces/trace <<EOF
buffers: { size_kb: 65536 }
duration_ms: 10000
data_sources: { config {
    name: "linux.ftrace"
    ftrace_config {
        ftrace_events: "binder/binder_transaction"
        ftrace_events: "binder/binder_transaction_received"
        ftrace_events: "binder/binder_lock"
        ftrace_events: "sched/sched_switch"
    }
}}
EOF
```

**2단계: 트레이스 가져오기 + 열기**
```bash
adb pull /data/misc/perfetto-traces/trace trace.pb
# ui.perfetto.dev에서 파일 열기
```

**3단계: 분석 포인트**
- **binder_transaction** 이벤트: 발신 프로세스/스레드, 수신 프로세스/스레드, 데이터 크기
- **binder_transaction_received** 이벤트: 수신 시각 → 발신~수신 시간차 = **IPC 지연시간**
- **sched_switch** 이벤트: Binder Thread가 스케줄링에서 밀리는지 확인
- 특정 프로세스의 Binder 스레드가 모두 **sleeping 상태**면 Thread Pool 고갈 의심

**4단계: SQL 분석 (Perfetto UI)**
```sql
SELECT ts, dur, pid, tid, name FROM slice 
WHERE name LIKE 'binder%' 
ORDER BY dur DESC LIMIT 20
```

---

## Chapter 5: Native vs Java Binder, 보안

### 슬라이드 #47 — Chapter 5 퀴즈

#### Q1. Java BinderProxy.transact()에서 커널까지 전체 경로를 설명하시오.

**✅ 정답 및 해설:**

```
Java Layer:
  1. IMyService.Stub.Proxy.add(1, 2)
     → data = Parcel.obtain(); data.writeInt(1); data.writeInt(2);
     → mRemote.transact(ADD, data, reply, 0);

  2. android.os.BinderProxy.transact()
     → native 메서드 호출

JNI Layer:
  3. android_os_BinderProxy_transact() 
     (frameworks/base/core/jni/android_util_Binder.cpp)
     → Java Parcel → C++ Parcel 변환
     → IBinder->transact() 호출

Native Layer:
  4. BpBinder::transact()
     (frameworks/native/libs/binder/BpBinder.cpp)
     → IPCThreadState::self()->transact() 호출

  5. IPCThreadState::transact()
     (frameworks/native/libs/binder/IPCThreadState.cpp)
     → writeTransactionData(): BC_TRANSACTION 명령 + 데이터를 mOut 버퍼에 기록
     → waitForResponse(): 실제 커널 호출

Kernel Layer:
  6. ioctl(mProcess->mDriverFD, BINDER_WRITE_READ, &bwr)
     → Binder Driver가 트랜잭션을 수신
     → 대상 프로세스의 binder_thread에 작업 큐잉
     → Client Thread는 블로킹 (동기의 경우)
```

---

#### Q2. SELinux binder_call 거부 시 디버깅 절차를 단계별로 설명하시오.

**✅ 정답 및 해설:**

**1단계: 거부 로그 확인**
```bash
adb logcat | grep "avc:.*denied"
# 또는
adb shell dmesg | grep "avc"
```
출력 예시:
```
avc: denied { call } for pid=1234 comm="my_app" 
scontext=u:r:untrusted_app:s0 tcontext=u:r:my_service:s0 
tclass=binder permissive=0
```

**2단계: 거부 내용 해석**
- `scontext`: 호출자 도메인 (untrusted_app)
- `tcontext`: 대상 도메인 (my_service)
- `tclass`: 접근 유형 (binder)
- `{ call }`: 시도한 작업

**3단계: 허용 규칙 생성**
```bash
adb shell dmesg | grep avc | audit2allow -p sepolicy
# 출력:
# allow untrusted_app my_service:binder call;
```

**4단계: 정책 파일에 규칙 추가**
- `system/sepolicy/private/` 또는 `device/<vendor>/sepolicy/`
- 해당 `.te` 파일에 `allow` 규칙 추가

**5단계: neverallow 충돌 확인**
- AOSP에는 보안을 위한 `neverallow` 규칙이 있음
- 새 allow 규칙이 neverallow와 충돌하면 빌드 실패
- 이 경우 서비스 도메인 재설계 필요

---

## Chapter 6: 커스텀 System Service

### 슬라이드 #55 — Chapter 6 퀴즈

#### Q1. System Service 추가 시 변경해야 하는 모든 파일을 나열하시오.

**✅ 정답 및 해설:**

| 파일 | 위치 | 역할 |
|------|------|------|
| `IMyService.aidl` | `frameworks/base/core/java/android/app/` | 인터페이스 정의 |
| `Android.bp` (filegroup) | `frameworks/base/` | AIDL 빌드 등록 |
| `MyService.java` | `frameworks/base/services/core/java/` | 서비스 구현 (Stub 상속) |
| `SystemServer.java` | `frameworks/base/services/java/` | `addService()` 호출 |
| `MyManager.java` | `frameworks/base/core/java/android/app/` | 앱 접근 API |
| `SystemServiceRegistry.java` | `frameworks/base/core/java/android/app/` | Manager 등록 |
| `Context.java` | `frameworks/base/core/java/android/content/` | 서비스 상수 추가 |
| `service_contexts` | `system/sepolicy/private/` | SELinux 서비스 타입 |
| `my_service.te` | `system/sepolicy/private/` | SELinux 접근 정책 |

> "최소 **9개 파일**을 변경해야 합니다. SELinux를 빠뜨리는 것이 가장 흔한 실수입니다."

---

#### Q2. Binder Thread에서 긴 작업 수행 시 발생하는 문제와 해결 패턴을 설명하시오.

**✅ 정답 및 해설:**

**문제:**
- SystemServer의 Binder Thread가 긴 작업(DB 쿼리, 파일 I/O, 네트워크)으로 블로킹
- 다른 앱/서비스에서 이 서비스를 호출하면 **모두 대기** → 앱 ANR (5초)
- 최악의 경우: **Watchdog ANR** (60초) → system_server 강제 종료 → 기기 재부팅

**해결 패턴: HandlerThread 위임**
```java
public class MyService extends IMyService.Stub {
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;
    
    public void init() {
        mWorkerThread = new HandlerThread("MyServiceWorker");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());
    }
    
    @Override
    public void heavyWork(ICallback callback) {
        // Binder Thread에서 즉시 반환
        mWorkerHandler.post(() -> {
            // 무거운 작업을 Worker Thread에서 수행
            String result = doHeavyComputation();
            try {
                callback.onResult(result);  // 콜백으로 결과 전달
            } catch (RemoteException e) { }
        });
    }
}
```

---

#### Q3. Watchdog ANR이 발생하는 원인과 디버깅 방법을 설명하시오.

**✅ 정답 및 해설:**

**원인:**
- SystemServer 내의 Watchdog 스레드가 주기적으로(60초) **모니터 대상 서비스**의 lock을 체크
- 서비스가 lock을 **60초 이상 잡고 있으면** Watchdog이 system_server를 kill
- 주요 원인: synchronized 블록 내 오래 걸리는 I/O, 데드락, 무한 루프

**디버깅:**
1. `/data/anr/traces.txt` 분석 — Watchdog이 kill하기 전 모든 스레드 덤프를 남김
2. `"Watchdog"` 스레드의 스택에서 **어떤 monitor를 기다리는지** 확인
3. 해당 monitor를 잡고 있는 스레드의 스택에서 **무엇을 하고 있는지** 확인
4. `adb logcat -s Watchdog`으로 상세 로그 확인

```
예시 traces.txt:
"Watchdog" prio=5 tid=12 MONITOR
  - waiting to lock <0x12345678> (android.app.MyService)
  - held by thread 15

"binder:1234_5" prio=5 tid=15 NATIVE
  - locked <0x12345678> (android.app.MyService)
  at android.database.sqlite.SQLiteDatabase.rawQueryWithFactory()
  → DB 쿼리가 60초 이상 블로킹 중!
```

---

# 🔷 DAY 3: Framework Core & Overlay & Jetpack

### Day 2 복습 (5분)

> "System Service 추가 시 변경 파일은? → AIDL, Service, SystemServer, Manager, Registry, Context, Android.bp, SELinux (service_contexts + .te) — 최소 9개."

---

## Chapter 7: Android Framework Core 분석

### 슬라이드 #59 — startActivity() 흐름 다이어그램

> "Activity.startActivity() → Instrumentation → AMS → ATMS → (프로세스 필요 시) ZygoteProcess.start() → fork → ActivityThread.main() → onCreate()"

> "⚠ 프로세스 생성이 필요하면 수백ms 이상 — 콜드 스타트가 느린 이유."

---

### 슬라이드 #62 — OomAdjuster

> "FOREGROUND(0): 절대 안 죽임. VISIBLE(100): 보이지만 포커스 없음. SERVICE(500): 서비스 실행. CACHED(900+): 메모리 부족 시 우선 종료."

---

### 슬라이드 #65 — Chapter 7 퀴즈

#### Q1. startActivity()에서 새 프로세스 생성까지의 전체 호출 체인을 그리시오.

**✅ 정답 및 해설:**

```
[앱 프로세스]
Activity.startActivity(intent)
 → Activity.startActivityForResult()
 → Instrumentation.execStartActivity()
 → IActivityTaskManager.Stub.Proxy.startActivity()  ← Binder IPC 발생!

[system_server 프로세스]
 → ActivityTaskManagerService.startActivity()
 → ActivityTaskManagerService.startActivityAsUser()
 → ActivityStarter.execute()
 → ActivityStarter.startActivityInner()
 → RootWindowContainer.resumeTopActivity()
 → TaskFragment.resumeTopActivity()

[프로세스 존재 여부 확인]
 → ActivityTaskSupervisor.startSpecificActivity()
   → 프로세스가 없으면:
     → ActivityManagerService.startProcess()
     → ProcessList.startProcessLocked()
     → ZygoteProcess.start()  ← Socket을 통해 Zygote에 요청

[Zygote 프로세스]
 → ZygoteServer.runSelectLoop()
 → ZygoteConnection.processCommand()
 → Zygote.forkAndSpecialize()  ← fork() 시스템 콜!

[새로 생성된 앱 프로세스]
 → ActivityThread.main()
 → ActivityThread.attach()
 → IActivityManager.Stub.Proxy.attachApplication()  ← Binder IPC

[다시 system_server]
 → ActivityManagerService.attachApplicationLocked()
 → ActivityTaskSupervisor.attachApplicationLocked()
 → RealStartActivityLocked()

[앱 프로세스]
 → ActivityThread.handleLaunchActivity()
 → Activity.performCreate() → onCreate()
 → Activity.performResume() → onResume()
```

**핵심 포인트:**
- Binder IPC가 **최소 3번** 발생 (startActivity, attachApplication, scheduleLaunchActivity)
- Zygote와의 통신은 Binder가 아닌 **Unix Domain Socket** 사용
- 프로세스 생성은 **fork()** 이므로 Zygote의 사전 로딩 혜택을 받음

---

#### Q2. ANR 발생 시 traces.txt에서 확인해야 할 핵심 정보 3가지를 설명하시오.

**✅ 정답 및 해설:**

**1. 메인 스레드 (main/tid=1)의 스택 트레이스:**
```
"main" prio=5 tid=1 Blocked
  at com.example.MyActivity.onResume(MyActivity.java:45)
  - waiting to lock <0x0a1b2c3d> (com.example.DataManager)
  - held by thread "AsyncTask #3"
```
- 메인 스레드가 **무엇을 하다가 블로킹**되었는지 확인
- `Blocked`, `Waiting`, `TimedWaiting` 상태면 다른 스레드가 잡고 있는 lock 확인
- `Native` 상태면 JNI 호출이나 시스템 콜에서 블로킹

**2. Lock을 잡고 있는 스레드의 상태:**
```
"AsyncTask #3" prio=5 tid=15 Runnable
  at android.database.sqlite.SQLiteConnection.nativeExecute(Native Method)
  at android.database.sqlite.SQLiteSession.executeForString(SQLiteSession.java:192)
```
- 메인 스레드가 기다리는 lock을 **누가 잡고 있는지** 추적
- 그 스레드가 왜 lock을 오래 잡고 있는지 스택으로 판단

**3. Binder Thread 상태:**
```
"binder:1234_2" prio=5 tid=8 Native
  at android.os.BinderProxy.transactNative(Native Method)
  at android.app.IActivityManager$Stub$Proxy.getMemoryInfo(...)
```
- Binder Thread가 **다른 서비스 호출을 기다리며 블로킹**되어 있는지 확인
- system_server의 Binder Thread가 모두 대기 중이면 Thread Pool 고갈 의심

> "분석 순서: ①main 스레드 상태 → ②lock 소유자 추적 → ③Binder 대기 확인 → 원인 특정"

---

#### Q3. OomAdjuster의 프로세스 우선순위 계산 기준을 5가지 이상 나열하시오.

**✅ 정답 및 해설:**

| 기준 | 설명 | 영향 |
|------|------|------|
| **Foreground Activity** | 사용자와 직접 상호작용 중인 Activity가 있는 프로세스 | adj = 0 (최고 우선순위) |
| **Visible Activity** | 보이지만 포커스가 없는 Activity (예: 투명 Activity 뒤) | adj = 100 |
| **Foreground Service** | `startForeground()`로 실행 중인 서비스가 있는 프로세스 | adj = 200 |
| **Bound Service (Client visible)** | Foreground Activity에 바인딩된 서비스 | Client의 adj에 연동 |
| **Backup** | 백업 작업 중인 프로세스 | adj = 300 |
| **Heavy Weight** | 고비용 앱 (manifest에 선언) | adj = 400 |
| **Service (started)** | startService()로 시작된 서비스 (최근 30분 이내 활동) | adj = 500 |
| **Home (Launcher)** | 홈 앱 프로세스 | adj = 600 |
| **Previous Activity** | 바로 직전에 보던 Activity | adj = 700 |
| **Cached (empty)** | 백그라운드, 활성 컴포넌트 없음 | adj = 900~999 |

**추가 요소:**
- **프로세스가 가진 ContentProvider의 클라이언트 수**: 클라이언트가 Foreground이면 Provider 프로세스 우선순위도 높아짐
- **최근 사용 시간**: CACHED 프로세스 중에서도 오래된 것이 먼저 종료
- **프로세스 그룹**: `persistent` 플래그가 있으면 절대 종료하지 않음 (system_server, Phone 등)

---

## Chapter 8: RRO (Runtime Resource Overlays)

### 슬라이드 #72 — Chapter 8 퀴즈

#### Q1. RRO가 타겟 앱의 리소스를 교체하는 내부 메커니즘을 설명하시오.

**✅ 정답 및 해설:**

1. **오버레이 APK 설치**: `PackageManagerService`가 오버레이 APK를 설치하고, `<overlay>` 태그에서 `targetPackage`를 확인합니다.

2. **OverlayManagerService(OMS) 등록**: OMS가 오버레이를 등록하고 활성화/비활성화 상태를 관리합니다.

3. **리소스 테이블 수정**: 오버레이가 활성화되면, 타겟 앱의 **AssetManager에 오버레이 리소스 경로가 추가**됩니다. 이때 타겟 APK 자체는 수정하지 않습니다.

4. **리소스 해석 시 우선순위**: 앱이 `R.color.primary_color`를 요청하면, AssetManager는 다음 순서로 검색합니다:
   - ① 활성화된 RRO 오버레이 (우선순위 높은 것부터)
   - ② 타겟 APK의 원본 리소스
   - 오버레이에 해당 리소스가 있으면 원본 대신 오버레이 값을 반환

5. **idmap 파일**: 오버레이 리소스 ID와 타겟 리소스 ID를 매핑하는 파일. `idmap2`가 생성하며, 이를 통해 빠른 리소스 매핑이 가능합니다.

> "핵심: 타겟 APK를 수정하지 않고, AssetManager의 검색 경로에 오버레이를 추가하여 런타임에 리소스를 교체."

---

#### Q2. 정적 RRO와 동적 RRO의 차이점과 활용 시나리오를 설명하시오.

**✅ 정답 및 해설:**

| 구분 | 정적 RRO (`isStatic="true"`) | 동적 RRO (`isStatic="false"`) |
|------|------|------|
| **활성화** | 항상 활성 (비활성화 불가) | `cmd overlay enable/disable`로 토글 가능 |
| **설치 위치** | `/system/overlay/` 또는 `/vendor/overlay/` | 어디든 설치 가능 (사용자 설치도 가능) |
| **서명 요구** | 시스템 서명 필수 | 시스템 서명 권장 (미서명도 가능하나 제한적) |
| **우선순위** | `android:priority` 속성으로 결정 | `cmd overlay set-priority`로 변경 가능 |
| **활용** | OEM 기본 테마, 통신사 커스터마이징, HW별 설정 | 사용자 테마, Material You 동적 컬러, 다크모드 |

---

## Chapter 9: SRO

### 슬라이드 #76 — Chapter 9 퀴즈

#### Q1. PRODUCT_PACKAGE_OVERLAYS와 DEVICE_PACKAGE_OVERLAYS의 우선순위 차이를 설명하시오.

**✅ 정답 및 해설:**

- **PRODUCT_PACKAGE_OVERLAYS**: 제품(product) 수준 오버레이. **높은 우선순위**.
- **DEVICE_PACKAGE_OVERLAYS**: 디바이스(device) 수준 오버레이. **낮은 우선순위**.

**적용 순서 (높은 것이 이김):**
```
PRODUCT_PACKAGE_OVERLAYS > DEVICE_PACKAGE_OVERLAYS > 원본 리소스
```

**사용 예시:**
- `DEVICE_PACKAGE_OVERLAYS`: 하드웨어별 기본 설정 (화면 크기, 센서 구성)
- `PRODUCT_PACKAGE_OVERLAYS`: 제품 브랜딩 (로고, 색상, 기본 앱 설정) — device 설정을 덮어쓸 수 있음

---

#### Q2. SRO+RRO 동시 사용 시 최종 적용 우선순위를 설명하시오.

**✅ 정답 및 해설:**

```
최종 우선순위 (높은 것이 이김):
  1. 동적 RRO (가장 높은 priority의 활성화된 오버레이)
  2. 정적 RRO
  3. SRO (PRODUCT_PACKAGE_OVERLAYS)
  4. SRO (DEVICE_PACKAGE_OVERLAYS)
  5. 원본 리소스 (가장 낮음)
```

> "RRO는 **항상 SRO보다 우선**합니다. SRO로 빌드 시 기본값을 설정하고, RRO로 런타임에 덮어쓰는 패턴이 일반적입니다."

---

## Chapter 10: Jetpack

### 슬라이드 #83 — Chapter 10 퀴즈

#### Q1. Compose Recomposition과 Framework 렌더링 연동을 설명하시오.

**✅ 정답 및 해설:**

1. **State 변경 감지**: `mutableStateOf()` 등의 State가 변경되면, Compose 런타임이 해당 State를 읽는 Composable을 **재호출 대상으로 마킹**합니다.

2. **Recomposition**: 다음 프레임에서 마킹된 Composable 함수가 재호출됩니다. SlotTable에 저장된 이전 값과 비교하여 **변경된 부분만 갱신**합니다.

3. **Layout Phase**: Recomposition 결과를 바탕으로 LayoutNode 트리의 **측정(measure) + 배치(placement)** 수행. Framework의 `View.measure()` + `View.layout()`에 해당합니다.

4. **Drawing Phase**: `Canvas` API를 통해 실제 그리기 수행. 내부적으로 **Android Framework의 `android.graphics.Canvas`** (SKIA 또는 HWUI 렌더러)를 사용합니다.

5. **RenderNode**: 그리기 결과가 `RenderNode` (DisplayList)에 기록되고, Framework의 **RenderThread**가 GPU에서 실제 렌더링합니다.

6. **VSync 동기화**: Choreographer가 VSync 신호에 맞춰 Recomposition → Layout → Draw 파이프라인을 실행합니다.

> "핵심: Compose는 자체 트리 관리(SlotTable)를 하지만, 최종 렌더링은 **Framework의 Canvas/RenderNode/RenderThread**를 사용합니다."

---

#### Q2. WorkManager가 내부적으로 사용하는 Framework API를 설명하시오.

**✅ 정답 및 해설:**

WorkManager는 **API 레벨에 따라 다른 Framework API**를 선택합니다:

| API 레벨 | 사용하는 Framework API | 설명 |
|----------|----------------------|------|
| API 23+ | `JobScheduler` | 기본 스케줄러. 네트워크, 충전 등 Constraint 지원 |
| API 14~22 | `AlarmManager` + `BroadcastReceiver` | JobScheduler 미지원 기기 폴백 |
| 모든 버전 | `SQLite` (Room 경유) | 작업 메타데이터 영속 저장 |
| 모든 버전 | `ContentProvider` | 앱 초기화 시 WorkManager 자동 시작 (process lifecycle) |
| Foreground 작업 | `ForegroundService` | `setForeground()` 호출 시 Notification 필수 |

**내부 동작 흐름:**
1. `enqueue()` → Room DB에 WorkRequest 저장
2. `Scheduler`가 적절한 시점에 `JobScheduler.schedule()` 호출
3. Constraint 충족 시 JobScheduler가 `SystemJobService.onStartJob()` 트리거
4. WorkManager가 DB에서 해당 작업을 읽어 `Worker.doWork()` 실행
5. 결과를 DB에 저장하고 `JobScheduler`에 완료 알림

---

#### Q3. SystemUI에서 Compose 도입의 이점과 과제를 논하시오.

**✅ 정답 및 해설:**

**이점:**
1. **선언적 UI**: 기존 XML + Java View 조합보다 코드가 간결하고 상태 관리가 명확
2. **빠른 프로토타이핑**: UI 변경 시 Preview + Hot Reload로 빠른 반복 개발
3. **상태 관리 개선**: 기존 SystemUI의 복잡한 Controller 패턴을 State + Recomposition으로 단순화
4. **테스트 용이성**: Compose UI 테스트가 View 테스트보다 간편

**과제:**
1. **성능 민감성**: SystemUI는 **매 프레임 60fps**를 유지해야 하는 성능 크리티컬 앱. Recomposition이 과도하면 프레임 드롭
2. **기존 코드 공존**: 대규모 레거시 View 코드와 Compose가 혼재하는 과도기. `ComposeView`와 View의 상호운용 비용
3. **시스템 레벨 제약**: 일반 앱과 달리 SystemUI는 부팅 초기부터 실행. Compose 런타임 초기화가 부팅 속도에 영향
4. **Dagger 의존성**: 기존 Dagger DI와 Compose의 `CompositionLocal`이 혼재하여 의존성 관리 복잡

---

# 🔷 DAY 4: System UI & APEX 모듈 업데이트

### Day 3 복습 (5분)

> "startActivity()에서 Binder IPC는 최소 몇 번 발생? → 3번 이상 (startActivity, attachApplication, scheduleLaunchActivity)"

---

## Chapter 11: WindowManager 및 System UI 구조

### 슬라이드 #91 — Chapter 11 퀴즈

#### Q1. Window 계층의 DisplayContent → Task → ActivityRecord → WindowState 관계를 설명하시오.

**✅ 정답 및 해설:**

```
RootWindowContainer (전체 디스플레이 관리)
 └── DisplayContent (물리 디스플레이 1개 = 1개 인스턴스)
      ├── TaskDisplayArea (앱 윈도우 영역)
      │    ├── Task (Activity 그룹 = Back Stack)
      │    │    ├── ActivityRecord (Activity 인스턴스 1개)
      │    │    │    └── WindowState (실제 윈도우 1개)
      │    │    │         - Surface, 크기, 위치, z-order 관리
      │    │    │         - SurfaceFlinger의 Layer에 대응
      │    │    └── ActivityRecord (스택 내 다른 Activity)
      │    └── Task (다른 태스크)
      ├── WindowToken (TYPE_STATUS_BAR)
      │    └── WindowState (상태바 윈도우)
      └── WindowToken (TYPE_NAVIGATION_BAR)
           └── WindowState (네비게이션 바 윈도우)
```

**핵심 관계:**
- **DisplayContent**: 멀티 디스플레이 지원을 위한 최상위 컨테이너
- **Task**: Back Stack의 단위. 사용자가 "최근 앱"에서 보는 카드 1개 = Task 1개
- **ActivityRecord**: Activity의 Framework 측 표현. 생명주기, Intent, 권한 정보 관리
- **WindowState**: 실제 화면에 그려지는 윈도우. Surface를 소유하며 SurfaceFlinger에게 합성을 요청

---

#### Q2. 윈도우 누수(Window Leak)의 원인과 디버깅 절차를 설명하시오.

**✅ 정답 및 해설:**

**원인:**
- Activity가 `finish()`/`onDestroy()` 되었지만, 해당 Activity의 `WindowManager`에 추가된 View(Dialog, PopupWindow, Toast 커스텀 뷰 등)가 **아직 제거되지 않은 상태**
- Activity의 WindowToken이 무효화되면서 해당 View가 "leaked window"가 됨

**흔한 시나리오:**
```java
// 문제 코드
AlertDialog dialog = new AlertDialog.Builder(this).create();
dialog.show();
// Activity가 회전이나 finish()로 destroy되어도 dialog가 dismiss 안 됨

// 안전한 코드
@Override
protected void onDestroy() {
    super.onDestroy();
    if (dialog != null && dialog.isShowing()) {
        dialog.dismiss();
    }
}
```

**디버깅 절차:**
1. **Logcat 확인**: `"has leaked window"` 메시지 검색 — Activity 이름과 윈도우 타입 정보 포함
2. **StrictMode 활성화**: `StrictMode.setVmPolicy(new VmPolicy.Builder().detectLeakedClosableObjects().build())` — 누수 시 로그 또는 크래시
3. **dumpsys window**: `adb shell dumpsys window windows`에서 orphaned 윈도우 확인
4. **Memory Profiler**: Android Studio에서 Activity 인스턴스가 GC되지 않는 경우 leak suspect

---

#### Q3. InsetsState 모델의 동작 원리를 설명하시오.

**✅ 정답 및 해설:**

**기존 방식 (Android 10 이전):**
- `fitSystemWindows(Rect insets)`로 시스템 바 영역 정보를 전달
- 단일 Rect만 전달 → 상태바, 네비바, IME 등을 구분할 수 없음
- 앱이 각 insets의 **소스를 알 수 없어** 정밀 제어 불가

**InsetsState 모델 (Android 11+):**
- 각 insets의 **소스 타입**을 구분: `statusBars()`, `navigationBars()`, `ime()`, `displayCutout()` 등
- 앱이 **소스별로 선택적 처리** 가능: "상태바 영역은 투명하게 그리고, 네비바는 피하겠다"
- `WindowInsetsController`로 시스템 바 **표시/숨김을 애니메이션**과 함께 제어
- `WindowInsetsAnimation`으로 IME 올라오는 애니메이션과 앱 UI를 **동기화**

```java
// 앱에서 사용
ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
    Insets statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars());
    Insets navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
    v.setPadding(0, statusBar.top, 0, navBar.bottom);
    return WindowInsetsCompat.CONSUMED;
});
```

---

## Chapter 12: System UI 커스터마이징

### 슬라이드 #98 — Chapter 12 퀴즈

#### Q1. QSTileImpl 상태 관리와 refreshState() 동작을 설명하시오.

**✅ 정답 및 해설:**

**상태 관리 흐름:**
1. `handleClick()`: 사용자 탭 시 호출. 내부 상태 변경 (Settings DB, 시스템 속성 등) 후 `refreshState()` 호출
2. `refreshState()`: **즉시 UI를 갱신하지 않음**. 대신 `mHandler`에 메시지를 포스트하여 **다음 핸들러 루프**에서 `handleUpdateState()` 호출을 예약
3. `handleUpdateState(State state, Object arg)`: 현재 상태를 읽어 `state` 객체에 반영. `state.label`, `state.icon`, `state.state`(ACTIVE/INACTIVE/UNAVAILABLE)을 설정
4. QS 패널이 `state` 객체를 읽어 UI를 갱신 (아이콘 색상, 라벨 텍스트, 토글 상태)

**핵심:** `refreshState()`는 **비동기**입니다. 여러 번 연속 호출해도 handleUpdateState()는 **한 번만 실행**됩니다 (Handler 메시지 중복 제거). 이는 빠른 토글 시 UI 깜박임을 방지합니다.

---

#### Q2. RRO 다크모드 대응 시 필요한 리소스 파일을 모두 나열하시오.

**✅ 정답 및 해설:**

```
overlay_apk/
├── res/
│   ├── values/
│   │   ├── colors.xml          # 라이트 모드 색상
│   │   ├── styles.xml          # 라이트 모드 스타일
│   │   └── dimens.xml          # 공통 크기
│   ├── values-night/
│   │   ├── colors.xml          # ★ 다크 모드 색상 (필수!)
│   │   └── styles.xml          # 다크 모드 스타일
│   ├── drawable/
│   │   └── ic_custom.xml       # 라이트 모드 아이콘
│   └── drawable-night/
│       └── ic_custom.xml       # ★ 다크 모드 아이콘 (필요 시)
└── AndroidManifest.xml
```

> "가장 흔한 실수: `values/colors.xml`만 오버레이하고 `values-night/colors.xml`을 빠뜨리는 것. 다크모드로 전환하면 오버레이 색상이 적용되지 않고 원본 색상이 나타납니다."

---

#### Q3. SystemUI 부팅 루프 발생 시 복구 절차를 설명하시오.

**✅ 정답 및 해설:**

1. **adb 연결 확인**: 부팅 루프 중에도 adb는 보통 연결됨. `adb devices`로 확인
2. **Safe Mode 진입**: 전원 버튼 길게 → "전원 끄기" 길게 터치 → Safe Mode 선택 (에뮬레이터: `adb reboot --safe-mode`)
3. **문제 오버레이 비활성화**: `adb shell cmd overlay disable <오버레이_패키지명>`
4. **문제 APK 제거**: 수동 설치한 SystemUI라면 `adb uninstall <패키지>` 또는 `adb shell pm uninstall --user 0 <패키지>`
5. **factory SystemUI 복원**: `/system/priv-app/SystemUI/`의 원본 APK가 존재하면 자동 복원됨
6. **최후 수단**: `adb shell wipe data` 또는 에뮬레이터 Cold Boot + AVD 재생성

---

## Chapter 13: APEX 및 모듈 업데이트

### 슬라이드 #107 — Chapter 13 퀴즈

#### Q1. APEX와 APK의 구조적 차이와 설치 메커니즘을 설명하시오.

**✅ 정답 및 해설:**

| 구분 | APEX | APK |
|------|------|-----|
| **내부 구조** | ext4 이미지(apex_payload.img) + 매니페스트 | DEX + 리소스 + Native libs |
| **마운트 방식** | loop device로 `/apex/<name>@<ver>/`에 마운트 | 패키지 디렉토리에 설치 |
| **무결성** | dm-verity로 런타임 검증 | APK 서명 검증 (설치 시) |
| **관리 데몬** | apexd | PackageManagerService |
| **설치 방식** | Staged Install (재부팅 후 적용) | 즉시 적용 |
| **롤백** | 자동 롤백 (실패 시 이전 버전) | 수동 (uninstall → reinstall) |
| **업데이트 경로** | Google Play / OTA | Google Play / sideload |
| **포함 콘텐츠** | 시스템 라이브러리, 바이너리, 설정 | 앱 코드, 리소스, assets |

---

#### Q2. apexd의 롤백 메커니즘을 단계별로 설명하시오.

**✅ 정답 및 해설:**

1. **Staged Install 완료**: 새 APEX가 `/data/apex/active/`에 저장됨. 재부팅 대기 상태.

2. **재부팅 시 apexd 시작**: init에 의해 apexd가 시작되면, `/data/apex/active/`에 새 APEX가 있는지 확인합니다.

3. **활성화 시도**: 
   - 서명 검증 (apex_pubkey와 certificate 비교)
   - dm-verity 설정 (apex_payload.img의 무결성 보장)
   - loop device 생성 및 마운트

4. **부팅 완료 확인 (Boot Completion)**:
   - apexd는 `sys.boot_completed` 속성을 모니터링
   - 부팅이 **성공적으로 완료**되면 → 새 APEX를 **영구 활성화** (기존 factory 버전 위에 유지)
   - 부팅이 **실패**하면 (crashing, watchdog 등) → **다음 재부팅에서 롤백**

5. **롤백 동작**:
   - `/data/apex/active/`에서 새 APEX 제거
   - `/system/apex/`의 **factory 버전**으로 폴백
   - 롤백 사실을 로그에 기록 (`logcat -s apexd`)

6. **Rollback API** (Android 12+):
   - `StagedRollbackReporter`가 롤백 원인을 기록
   - `RollbackManager`를 통해 프로그래밍 방식으로 롤백 가능

> "핵심: apexd는 '부팅 성공'을 확인한 후에만 새 APEX를 영구화합니다. 부팅 실패 시 자동 롤백으로 기기 브릭을 방지합니다."

---

#### Q3. APEX 설치 실패 시 디버깅 절차를 설명하시오.

**✅ 정답 및 해설:**

**1단계: 설치 시도 및 에러 확인**
```bash
$ adb install --staged my_module.apex
# 실패 시 에러 메시지 확인
# "Failure [INSTALL_FAILED_...]"
```

**2단계: apexd 로그 확인**
```bash
$ adb logcat -s apexd
# 서명 검증 실패, 의존성 문제, 버전 충돌 등 상세 원인
```

**3단계: Staged Session 상태 확인**
```bash
$ adb shell pm get-stagedsessions
# sessionId, status (STAGED_SESSION_FAILED 등), 에러 메시지
```

**4단계: 흔한 원인별 해결**

| 에러 | 원인 | 해결 |
|------|------|------|
| `INSTALL_FAILED_VERIFICATION_FAILURE` | 서명 불일치 | apex_pubkey와 signing key 일치 확인 |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | 현재보다 낮은 버전 설치 시도 | apex_manifest.json의 version 확인 |
| `PRE_REBOOT_VERIFICATION_FAILED` | dm-verity 검증 실패 | apex_payload.img 무결성 확인, 재빌드 |
| `POST_REBOOT_ACTIVATION_FAILED` | 마운트 후 서비스 시작 실패 | 의존 라이브러리 호환성 확인 |

**5단계: 강제 초기화 (최후 수단)**
```bash
$ adb shell rm /data/apex/active/my_module*.apex
$ adb reboot
# factory 버전으로 복원됨
```

---

#### Q4. Project Mainline이 보안 업데이트에 미치는 영향을 논하시오.

**✅ 정답 및 해설:**

**기존 방식의 문제:**
- 보안 취약점 발견 → OEM이 패치 통합 → 통신사 테스트 → OTA 배포
- 전체 과정: **수 주 ~ 수 개월** 소요
- OEM/통신사 의지에 따라 패치가 아예 안 되는 기기도 존재

**Project Mainline의 개선:**
1. **직접 배포 경로**: Google → Play Store → 기기. OEM/통신사 없이 **수 일 내** 배포 가능
2. **범위 확장**: Android 16 기준 30개+ 시스템 모듈이 APEX화 — 보안 공격 표면의 상당 부분을 빠르게 패치 가능
3. **일관성**: 모든 기기에 **동일한 버전**의 모듈이 적용 → OS 파편화로 인한 보안 격차 감소
4. **핵심 모듈 예시**:
   - `com.android.conscrypt`: TLS/SSL 라이브러리 → 암호화 취약점 빠른 대응
   - `com.android.resolv`: DNS Resolver → DNS 스푸핑 취약점 대응
   - `com.android.media`: 미디어 코덱 → Stagefright급 취약점 빠른 패치

**한계:**
- 커널 취약점은 APEX로 패치 불가 (GKI 별도 메커니즘)
- HAL 레벨 취약점도 APEX 범위 밖
- 기기가 네트워크에 연결되어 있어야 업데이트 수신 가능

---

## 📌 슬라이드 #115 — 교육 완료 (마무리 멘트)

> "4일간의 여정을 마칩니다."

> "이 교육에서 가장 기억해야 할 한 가지: **Android의 모든 것은 Binder로 연결되어 있고, Binder를 이해하면 시스템의 어떤 문제든 디버깅할 수 있다.**"

> "트러블슈팅 슬라이드와 부록의 명령어 모음을 현업에서 활용하시기 바랍니다."

> "수고 많으셨습니다."

---

## 📌 실습 코드 배포 안내

| 챕터 | 배포 파일 |
|------|-----------|
| Ch 2 | `ICalculatorService.aidl`, `CalculatorService.java`, `ClientActivity.java` 완성본 |
| Ch 3 | Content Provider + AIDL 콜백 서비스 완성본 |
| Ch 6 | AOSP 패치 diff (AIDL, Service, SystemServer, SELinux) |
| Ch 12 | 커스텀 QS 타일 + RRO 오버레이 APK 샘플 |

---

*이 강사 스크립트는 102장 슬라이드에 대응하며, 모든 퀴즈의 정답과 상세 해설을 포함합니다.*
