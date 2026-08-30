# 🔬 Android Framework 16 심화 교육 — Day 2 실습자료

## Binder 심화 분석 & 커스텀 System Service

> **소요 시간:** 8시간 (Chapter 4~6)  
> **환경:** AOSP 16 빌드 완료 + 에뮬레이터 (`sdk_car_x86_64-userdebug`)  
> **사전 조건:** Day 1 완료 (AOSP 빌드 환경, Binder 기초 이해)

---

## 📋 실습 환경 사전 점검

실습 시작 전 아래 환경이 정상 동작하는지 확인합니다.

```bash
# AOSP 환경 초기화
cd ~/aosp
source build/envsetup.sh
lunch sdk_car_x86_64-userdebug

# 에뮬레이터 실행 확인
emulator -no-window -no-audio &
adb wait-for-device
adb shell getprop ro.build.display.id
# 출력 예: BP4A.251205.006

# Binder 디버그 인터페이스 접근 확인
adb root
adb shell ls /sys/kernel/debug/binder/
# 출력: failed_transaction_log  state  stats  transaction_log  transactions
```

> ⚠️ `debugfs`가 마운트되지 않은 경우:
> ```bash
> adb shell mount -t debugfs none /sys/kernel/debug
> ```

---

## Chapter 4 실습: Binder 심화 분석

### 실습 4-1. Binder mmap Zero-Copy 메커니즘 관찰

**목표:** Binder의 mmap 메모리 매핑을 직접 확인하고, Zero-Copy가 실제로 동작하는 원리를 이해합니다.

#### Step 1: system_server의 Binder mmap 영역 확인

```bash
# system_server PID 확인
adb shell pidof system_server
# 출력 예: 1842

# system_server의 메모리 맵에서 binder mmap 영역 확인
adb shell cat /proc/$(pidof system_server)/maps | grep "/dev/binder\|/dev/hwbinder\|/dev/vndbinder"
```

**예상 출력:**
```
7f8a00000000-7f8a000ff000 r--p 00000000 00:06 1234  /dev/binder
```

> 📝 **분석 포인트:**
> - `r--p`: 읽기 전용 + 프라이빗 매핑 → 서버는 복사 없이 커널 버퍼를 직접 읽음
> - 매핑 크기: 약 1MB (`0xff000` = 약 1,044,480 바이트) — Binder 트랜잭션 최대 크기와 일치

#### Step 2: 여러 프로세스의 Binder mmap 비교

```bash
# 주요 프로세스들의 binder mmap 매핑 비교
for proc in system_server surfaceflinger audioserver; do
    pid=$(adb shell pidof $proc)
    if [ -n "$pid" ]; then
        echo "=== $proc (PID: $pid) ==="
        adb shell cat /proc/$pid/maps | grep "/dev/binder"
        echo ""
    fi
done
```

#### Step 3: Binder 메모리 사용량 확인

```bash
# Binder 전체 상태 확인
adb shell cat /sys/kernel/debug/binder/state | head -30

# 특정 프로세스의 Binder 할당 버퍼 크기 확인
adb shell cat /sys/kernel/debug/binder/proc/$(pidof system_server) | grep -A5 "allocated"
```

**✏️ 기록 과제:**

| 항목 | 확인 결과 |
|------|----------|
| system_server binder mmap 시작 주소 | |
| system_server binder mmap 크기 | |
| mmap 권한 (r/w/x) | |
| 현재 할당된 Binder 버퍼 수 | |

> 💡 **핵심 개념:** Client → Kernel은 `copy_from_user()`로 1회 복사하지만, Kernel → Server는 mmap으로 물리 페이지를 공유하므로 추가 복사가 0회입니다. 이것이 "Zero-Copy"의 본질입니다.

---

### 실습 4-2. Binder Thread Pool 모니터링 및 데드락 시나리오

**목표:** Binder Thread Pool의 동작을 실시간 관찰하고, 데드락 발생 조건을 이해합니다.

#### Step 1: Binder Thread 현황 확인

```bash
# system_server의 Binder 스레드 목록
adb shell cat /sys/kernel/debug/binder/proc/$(pidof system_server) | grep -E "thread|ready|free"
```

```bash
# 좀 더 읽기 쉬운 형태로 정리
adb shell cat /sys/kernel/debug/binder/proc/$(pidof system_server) | \
    awk '/^  thread/{tid=$2} /looper/{print "Thread", tid, $0}'
```

**예상 출력 분석:**
```
thread 1842: l 11  ← Main Binder Thread (looper 비트 0x01 = started, 0x02 = registered)
thread 1860: l 12  ← 추가 Binder Thread (BR_SPAWN_LOOPER로 생성됨)
thread 1861: l 12
...
```

> 📝 **Looper 상태 비트:**
> - `0x01`: `BINDER_LOOPER_STATE_REGISTERED` — 메인 스레드
> - `0x02`: `BINDER_LOOPER_STATE_ENTERED` — 추가 스레드
> - `0x04`: `BINDER_LOOPER_STATE_EXITED` — 종료됨
> - `0x08`: `BINDER_LOOPER_STATE_INVALID` — 잘못된 상태
> - `0x10`: `BINDER_LOOPER_STATE_WAITING` — 대기 중
> - `0x20`: `BINDER_LOOPER_STATE_POLL` — epoll 대기

#### Step 2: Binder Thread 수 제한 확인

```bash
# 최대 Binder Thread 수 확인 (기본값: 15)
adb shell cat /sys/kernel/debug/binder/proc/$(pidof system_server) | grep "max threads"

# 실시간 thread 수 모니터링 (5초간)
for i in $(seq 1 5); do
    count=$(adb shell cat /sys/kernel/debug/binder/proc/$(pidof system_server) | grep -c "^  thread")
    echo "$(date +%H:%M:%S) - Active Binder Threads: $count"
    sleep 1
done
```

#### Step 3: 데드락 시나리오 이해 — 의사 코드 분석

다음은 데드락이 발생하는 전형적인 패턴입니다. 코드를 분석하고 문제점을 파악하세요.

```java
// ❌ 데드락 위험 시나리오: Service A ↔ Service B 순환 동기 호출

// Service A (system_server 내)
class ServiceA extends IServiceA.Stub {
    @Override
    public String getData() {
        // B를 동기 호출 → B의 Binder Thread 점유
        IBinder b = ServiceManager.getService("service_b");
        IServiceB svcB = IServiceB.Stub.asInterface(b);
        return svcB.processData("from_A");  // ← 동기 호출 (blocking)
    }
}

// Service B (system_server 내)
class ServiceB extends IServiceB.Stub {
    @Override
    public String processData(String input) {
        // A를 다시 동기 호출 → 데드락!
        IBinder a = ServiceManager.getService("service_a");
        IServiceA svcA = IServiceA.Stub.asInterface(a);
        return svcA.getData();  // ← A가 이미 B를 기다리는 중!
    }
}
```

**✏️ 분석 과제:**

1. 위 시나리오에서 데드락이 발생하는 정확한 조건은 무엇인가?
2. Binder Thread Pool의 최대 스레드 수(15+1)와 데드락 발생의 관계를 설명하시오.
3. 아래 3가지 해결 방법 중 각각의 장단점을 정리하시오:

| 해결 방법 | 장점 | 단점 |
|-----------|------|------|
| `oneway` (비동기) 전환 | | |
| 최대 스레드 수 증가 | | |
| 호출 방향 단일화 | | |

---

### 실습 4-3. Binder 트랜잭션 추적 — Perfetto 활용

**목표:** Perfetto를 사용하여 Binder 트랜잭션의 지연 시간을 측정하고 분석합니다.

#### Step 1: Perfetto 트레이스 수집

```bash
# Binder 이벤트 포함 Perfetto 트레이스 수집 (10초)
adb shell perfetto -c - --txt -o /data/misc/perfetto-traces/binder_trace.perfetto-trace <<EOF
buffers: {
    size_kb: 32768
    fill_policy: RING_BUFFER
}
data_sources: {
    config {
        name: "linux.ftrace"
        ftrace_config {
            ftrace_events: "binder/binder_transaction"
            ftrace_events: "binder/binder_transaction_received"
            ftrace_events: "binder/binder_lock"
            ftrace_events: "binder/binder_unlock"
            ftrace_events: "sched/sched_switch"
        }
    }
}
duration_ms: 10000
EOF
```

#### Step 2: 트레이스 수집 중 부하 발생

```bash
# 별도 터미널에서: 트레이스 수집 중 Binder 트래픽 발생시키기
adb shell am start -W com.android.settings/.Settings
adb shell dumpsys activity
adb shell dumpsys window
adb shell dumpsys package com.android.settings
```

#### Step 3: 트레이스 파일 추출 및 분석

```bash
# 트레이스 파일을 호스트로 복사
adb pull /data/misc/perfetto-traces/binder_trace.perfetto-trace ~/

# 브라우저에서 분석:
# https://ui.perfetto.dev 접속 → 파일 드래그 앤 드롭
echo "브라우저에서 https://ui.perfetto.dev 접속 후 트레이스 파일을 열어주세요"
```

#### Step 4: Binder 통계 확인 (CLI 방식)

```bash
# Binder 호출 통계 덤프
adb shell dumpsys binder_calls_stats

# 트랜잭션 로그 (최근 32개)
adb shell cat /sys/kernel/debug/binder/transaction_log | tail -32

# 실패한 트랜잭션 확인
adb shell cat /sys/kernel/debug/binder/failed_transaction_log
```

**✏️ 기록 과제:**

Perfetto UI 또는 CLI 출력에서 다음 정보를 찾아 기록하세요:

| 측정 항목 | 값 |
|----------|-----|
| 가장 빈번한 Binder 호출 (서비스 이름) | |
| 가장 긴 트랜잭션 지연 시간 (ms) | |
| 10초간 총 Binder 트랜잭션 수 | |
| failed_transaction_log 항목 수 | |

---

### 실습 4-4. TransactionTooLargeException 재현

**목표:** Binder 트랜잭션의 1MB 제한을 직접 확인합니다.

#### Step 1: 테스트 앱 코드 작성

`~/aosp` 외부 또는 standalone 프로젝트에서 다음 코드를 분석합니다:

```java
// TransactionTooLargeException 재현 코드 (개념 이해용)
// 실제 앱에서는 이런 패턴을 피해야 합니다

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

public class BinderLimitTestActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1MB 이상의 데이터를 Intent에 담아 전송 시도
        Intent intent = new Intent(this, ReceiverActivity.class);
        Bundle bundle = new Bundle();

        // 약 1.5MB 바이트 배열 생성 → TransactionTooLargeException 발생!
        byte[] largeData = new byte[1500 * 1024];
        bundle.putByteArray("large_data", largeData);
        intent.putExtras(bundle);

        try {
            startActivity(intent);
        } catch (RuntimeException e) {
            // android.os.TransactionTooLargeException
            android.util.Log.e("BinderTest", "예외 발생: " + e.getMessage());
        }
    }
}
```

#### Step 2: 실제 Binder 버퍼 크기 확인

```bash
# Binder 프로세스별 버퍼 할당 현황
adb shell cat /sys/kernel/debug/binder/state | grep -A3 "allocated"

# Binder mmap 영역 크기 확인 (프로세스별)
adb shell cat /sys/kernel/debug/binder/state | grep "buffer size"
```

**✏️ 분석 과제:**

1MB 제한을 초과하는 데이터를 서비스 간에 전달해야 할 때 사용할 수 있는 대안을 정리하세요:

| 대안 기술 | 최대 크기 | 장점 | 사용 시나리오 |
|-----------|----------|------|-------------|
| ashmem (Anonymous Shared Memory) | | | |
| File Descriptor (FD) 전달 | | | |
| ContentProvider | | | |
| MemoryFile | | | |

---

## Chapter 5 실습: Native Binder vs Java Binder, 보안

### 실습 5-1. Java → Native Binder 호출 스택 추적

**목표:** Java 레이어에서 커널까지의 Binder 호출 경로를 실제 코드에서 추적합니다.

#### Step 1: AOSP 소스에서 호출 경로 추적

```bash
# 1. Java BinderProxy.transact() 소스 확인
find ~/aosp/frameworks -name "Binder.java" -path "*/os/*" | head -1
# → frameworks/base/core/java/android/os/Binder.java

# transact() 메서드 위치 찾기
grep -n "public boolean transact" ~/aosp/frameworks/base/core/java/android/os/Binder.java
```

```bash
# 2. JNI 브릿지 소스 확인
find ~/aosp/frameworks -name "android_util_Binder.cpp" | head -1
# → frameworks/base/core/jni/android_util_Binder.cpp

# transact JNI 함수 찾기
grep -n "android_os_BinderProxy_transact" ~/aosp/frameworks/base/core/jni/android_util_Binder.cpp | head -5
```

```bash
# 3. Native libbinder 소스 확인
find ~/aosp/frameworks -name "BpBinder.cpp" | head -1
# → frameworks/native/libs/binder/BpBinder.cpp

grep -n "BpBinder::transact" ~/aosp/frameworks/native/libs/binder/BpBinder.cpp | head -5
```

```bash
# 4. IPCThreadState → ioctl 소스 확인
find ~/aosp/frameworks -name "IPCThreadState.cpp" | head -1
# → frameworks/native/libs/binder/IPCThreadState.cpp

grep -n "ioctl" ~/aosp/frameworks/native/libs/binder/IPCThreadState.cpp | head -10
```

#### Step 2: 호출 스택 다이어그램 완성

아래 호출 스택의 빈칸을 AOSP 소스에서 찾은 실제 파일명과 함수명으로 채우세요:

```
┌─────────────────────────────────────────────────────┐
│ App: mService.add(1, 2)                             │
│                                                     │
│ 파일: _________________________________             │
│ 함수: ICalcService.Stub.Proxy.add()                 │
│       → Parcel.writeInt(1), Parcel.writeInt(2)      │
│       → mRemote.transact(TRANSACTION_add, ...)      │
├─────────────────────────────────────────────────────┤
│ 파일: _________________________________             │
│ 함수: BinderProxy.transactNative() [native method]  │
├─────────────────────────────────────────────────────┤
│ 파일: _________________________________             │
│ 함수: android_os_BinderProxy_transact()             │
│       → BpBinder::transact() 호출                   │
├─────────────────────────────────────────────────────┤
│ 파일: _________________________________             │
│ 함수: BpBinder::transact()                          │
│       → IPCThreadState::self()->transact()          │
├─────────────────────────────────────────────────────┤
│ 파일: _________________________________             │
│ 함수: IPCThreadState::talkWithDriver()              │
│       → ioctl(mProcess->mDriverFD,                  │
│              BINDER_WRITE_READ, &bwr)               │
├─────────────────────────────────────────────────────┤
│ 커널: drivers/android/binder.c                      │
│ 함수: binder_ioctl() → binder_thread_write()        │
│       → binder_transaction()                        │
└─────────────────────────────────────────────────────┘
```

---

### 실습 5-2. Binder 보안 메커니즘 검증

**목표:** Binder의 5대 보안 계층을 실습을 통해 이해합니다.

#### Step 1: 커널 UID/PID 주입 확인

```bash
# 현재 프로세스의 Binder 호출자 정보 확인
adb shell service call activity 1 2>/dev/null
# (activity 서비스에 임의 트랜잭션 전송)

# Binder 트랜잭션 로그에서 UID/PID 확인
adb shell cat /sys/kernel/debug/binder/transaction_log | tail -5
```

**예상 출력 분석:**
```
12345: call  from 2000:2000:1543 to 1000:1000:1842 ...
       ↑                  ↑ PID        ↑ PID
       트랜잭션 ID    호출자 UID   수신자 UID
```

> 📝 **핵심:** UID/PID는 커널이 주입하므로 userspace에서 위변조가 불가능합니다. 이것이 Binder 보안의 기반입니다.

#### Step 2: SELinux binder_call 정책 확인

```bash
# 현재 SELinux 상태 확인
adb shell getenforce
# 출력: Enforcing

# Binder 관련 SELinux 정책 검색
adb shell sesearch --allow -s untrusted_app -c binder 2>/dev/null || \
    echo "sesearch 미설치 → AOSP 소스에서 확인"

# AOSP 소스에서 binder_call 정책 확인
grep -r "binder_call" ~/aosp/system/sepolicy/private/ | head -10
grep -r "binder_call" ~/aosp/system/sepolicy/public/ | head -10
```

#### Step 3: SELinux 거부 로그 분석

```bash
# SELinux 거부 로그 실시간 모니터링
adb logcat -b events | grep -i "avc:.*denied" &

# 일반 앱에서 system_server 서비스 직접 호출 시도 (권한 부족 예상)
adb shell service call clipboard 1
adb shell service call package 1

# 거부 로그 확인
adb logcat | grep "avc:.*denied.*binder" | tail -5
```

#### Step 4: clearCallingIdentity 패턴 실습

다음 코드의 보안 문제를 분석하세요:

```java
// ❌ 보안 취약 코드
public class UnsafeService extends IMyService.Stub {
    @Override
    public void writeLog(String message) {
        // 호출자 권한 확인
        mContext.enforceCallingPermission("com.example.WRITE_LOG", TAG);

        // 문제: 아래 호출이 '호출자'의 UID/PID로 실행됨!
        // 일반 앱의 UID로는 시스템 파일 쓰기 권한이 없어 실패
        mContext.getContentResolver().insert(LOG_URI, values);
    }
}

// ✅ 올바른 코드
public class SafeService extends IMyService.Stub {
    @Override
    public void writeLog(String message) {
        // 1. 호출자 권한 확인
        mContext.enforceCallingPermission("com.example.WRITE_LOG", TAG);

        // 2. 시스템 서비스 자신의 UID로 전환
        final long token = Binder.clearCallingIdentity();
        try {
            // 이제 system_server(UID 1000)의 권한으로 실행
            mContext.getContentResolver().insert(LOG_URI, values);
        } finally {
            // 3. 반드시 원래 호출자 UID 복원
            Binder.restoreCallingIdentity(token);
        }
    }
}
```

**✏️ 분석 과제:**

| 보안 계층 | 구현 위치 | 역할 | 우회 가능 여부 |
|-----------|----------|------|-------------|
| 커널 UID/PID 주입 | binder.c | | |
| SELinux binder_call | sepolicy/*.te | | |
| enforceCallingPermission | Binder.java | | |
| Signature Permission | AndroidManifest.xml | | |
| AppOps | AppOpsManager | | |

---

### 실습 5-3. SELinux 거부 디버깅 실전

**목표:** SELinux binder_call 거부 시 실제 디버깅 절차를 익힙니다.

#### Step 1: avc denied 로그 수집

```bash
# dmesg에서 SELinux 거부 로그 확인
adb shell dmesg | grep "avc:.*denied" | tail -10

# 또는 logcat에서
adb logcat -d | grep "avc:.*denied" | tail -10
```

#### Step 2: audit2allow로 규칙 생성 (개발 환경 전용)

```bash
# 호스트에서 audit2allow 사용 (AOSP 빌드 환경)
adb shell dmesg | grep "avc:.*denied" > /tmp/avc_denials.txt

# audit2allow로 필요한 SELinux 규칙 생성
cat /tmp/avc_denials.txt | audit2allow -p ~/aosp/out/target/product/emulator_x86_64/root/sepolicy

# 출력 예:
# allow untrusted_app my_service_service:service_manager find;
# allow untrusted_app my_service:binder call;
```

#### Step 3: neverallow 충돌 확인

```bash
# neverallow 규칙 확인 — 생성된 규칙이 neverallow와 충돌하면 적용 불가
grep -r "neverallow" ~/aosp/system/sepolicy/public/ | grep "binder" | head -10
grep -r "neverallow" ~/aosp/system/sepolicy/private/ | grep "binder" | head -10
```

**✏️ 디버깅 절차 정리:**

```
SELinux Binder 거부 디버깅 흐름도:

1. avc:denied 로그 수집
   └─→ adb logcat | grep "avc:.*denied"

2. 거부 내용 분석
   └─→ source_type, target_type, class, permission 파악

3. audit2allow로 규칙 생성
   └─→ 개발 단계에서만 사용!

4. neverallow 충돌 확인
   ├─→ 충돌 없음 → .te 파일에 규칙 추가
   └─→ 충돌 있음 → 아키텍처 재설계 필요

5. 빌드 및 검증
   └─→ mm → 에뮬레이터 재시작 → 로그 재확인
```

---

## Chapter 6 실습: 커스텀 System Service 추가

### 실습 6-1. AIDL 인터페이스 정의

**목표:** AOSP에 커스텀 System Service의 AIDL 인터페이스를 추가합니다.

#### Step 1: AIDL 파일 작성

```bash
# 디렉토리 생성
mkdir -p ~/aosp/frameworks/base/core/java/android/os/custom/
```

```bash
# AIDL 파일 작성
cat > ~/aosp/frameworks/base/core/java/android/os/custom/IDeviceInfoService.aidl << 'EOF'
/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0
 */

package android.os.custom;

/**
 * 디바이스 정보 조회 시스템 서비스 인터페이스
 *
 * 학습 목적: System Service 추가 전체 과정 실습
 */
interface IDeviceInfoService {
    /**
     * 디바이스 모델명 반환
     * @return 현재 디바이스 모델명
     */
    String getDeviceModel();

    /**
     * 현재 시스템 가동 시간(초) 반환
     * @return uptime in seconds
     */
    long getUptimeSeconds();

    /**
     * 커스텀 속성 설정 (권한 필요)
     * @param key 속성 키
     * @param value 속성 값
     * @return 설정 성공 여부
     */
    boolean setCustomProperty(String key, String value);

    /**
     * 커스텀 속성 조회
     * @param key 속성 키
     * @return 속성 값 (없으면 null)
     */
    String getCustomProperty(String key);
}
EOF

echo "✅ AIDL 파일 생성 완료"
```

#### Step 2: Android.bp에 AIDL 등록

```bash
# frameworks/base/Android.bp에서 filegroup 찾기
grep -n "framework-aidl-export" ~/aosp/frameworks/base/Android.bp | head -5

# AIDL 파일을 빌드 시스템에 등록
# frameworks/base/core/java/android/os/custom/ 경로의 .aidl 파일이
# framework-aidl-export filegroup에 포함되어야 함
```

> ⚠️ **주의:** Android.bp의 정확한 수정 위치는 AOSP 버전마다 다릅니다. `filegroup` 블록 내의 `srcs`에 AIDL 파일 경로를 추가하세요.

#### Step 3: AIDL 빌드 테스트

```bash
# AIDL 빌드 테스트
cd ~/aosp
m framework-minus-apex -j4 2>&1 | tail -20
# 또는 전체 빌드
# m -j4
```

---

### 실습 6-2. System Service 구현 (Stub 상속)

**목표:** AIDL Stub를 상속하여 서비스를 구현하고, 보안 패턴을 적용합니다.

#### Step 1: 서비스 구현 코드 작성

```bash
mkdir -p ~/aosp/frameworks/base/services/core/java/com/android/server/custom/
```

```bash
cat > ~/aosp/frameworks/base/services/core/java/com/android/server/custom/DeviceInfoService.java << 'JAVAEOF'
/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0
 */

package com.android.server.custom;

import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.SystemClock;
import android.os.custom.IDeviceInfoService;
import android.util.Slog;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 디바이스 정보 시스템 서비스 구현
 *
 * ★ 핵심 학습 포인트:
 * 1. Binder Stub 상속 패턴
 * 2. enforceCallingPermission() 보안 체크
 * 3. clearCallingIdentity() / restoreCallingIdentity() 패턴
 * 4. ConcurrentHashMap을 통한 스레드 안전성
 */
public class DeviceInfoService extends IDeviceInfoService.Stub {

    private static final String TAG = "DeviceInfoService";
    private static final String PERM_WRITE = "android.permission.WRITE_DEVICE_INFO";

    private final Context mContext;

    // ★ 스레드 안전: Binder Thread Pool에서 동시 접근 가능
    // synchronized 대신 ConcurrentHashMap 사용
    private final ConcurrentHashMap<String, String> mProperties = new ConcurrentHashMap<>();

    public DeviceInfoService(Context context) {
        mContext = context;
        Slog.i(TAG, "DeviceInfoService initialized");
    }

    @Override
    public String getDeviceModel() {
        // 읽기 전용 → 별도 권한 불필요
        return Build.MODEL;
    }

    @Override
    public long getUptimeSeconds() {
        // 읽기 전용 → 별도 권한 불필요
        return SystemClock.elapsedRealtime() / 1000;
    }

    @Override
    public boolean setCustomProperty(String key, String value) {
        // ★ 보안 패턴 1: 호출자 권한 검증
        mContext.enforceCallingOrSelfPermission(PERM_WRITE, TAG);

        // ★ 보안 패턴 2: 시스템 권한으로 전환
        final long token = Binder.clearCallingIdentity();
        try {
            if (key == null || key.isEmpty()) {
                return false;
            }
            mProperties.put(key, value != null ? value : "");
            Slog.d(TAG, "Property set: " + key + " by UID=" +
                   Binder.getCallingUid());
            return true;
        } finally {
            // ★ 보안 패턴 3: 반드시 원래 호출자 복원
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public String getCustomProperty(String key) {
        if (key == null) return null;
        return mProperties.get(key);
    }

    // ★ dumpsys 지원: adb shell dumpsys device_info 로 상태 확인
    @Override
    protected void dump(java.io.FileDescriptor fd,
                        java.io.PrintWriter pw,
                        String[] args) {
        // dump 권한 체크
        if (!DumpUtils.checkDumpPermission(mContext, TAG, pw)) return;

        pw.println("=== DeviceInfoService Status ===");
        pw.println("Device Model: " + Build.MODEL);
        pw.println("Uptime: " + getUptimeSeconds() + "s");
        pw.println("Custom Properties (" + mProperties.size() + "):");
        for (var entry : mProperties.entrySet()) {
            pw.println("  " + entry.getKey() + " = " + entry.getValue());
        }
    }
}
JAVAEOF

echo "✅ 서비스 구현 파일 생성 완료"
```

> 📝 **코드 리뷰 체크리스트:**
> - [ ] `enforceCallingOrSelfPermission()` — 쓰기 작업에 권한 체크
> - [ ] `clearCallingIdentity()` / `restoreCallingIdentity()` — try-finally 패턴
> - [ ] `ConcurrentHashMap` — synchronized 대신 lock-free 자료구조
> - [ ] `dump()` — `dumpsys` 디버깅 지원
> - [ ] `DumpUtils.checkDumpPermission()` — dump 접근 제어

---

### 실습 6-3. SystemServer 등록 및 Manager 작성

**목표:** SystemServer에 서비스를 등록하고 앱에서 접근할 Manager를 작성합니다.

#### Step 1: SystemServer.java에 서비스 등록

```bash
# SystemServer.java 위치 확인
find ~/aosp/frameworks -name "SystemServer.java" -path "*/server/*" | head -1
```

다음 코드를 `SystemServer.java`의 `startOtherServices()` 메서드에 추가합니다:

```java
// === DeviceInfoService 등록 ===
traceBeginAndSlog("StartDeviceInfoService");
try {
    ServiceManager.addService("device_info",
        new com.android.server.custom.DeviceInfoService(context));
    Slog.i(TAG, "DeviceInfoService added");
} catch (Throwable e) {
    reportWtf("starting DeviceInfoService", e);
}
traceEnd();
```

#### Step 2: Manager 클래스 작성

```bash
cat > ~/aosp/frameworks/base/core/java/android/os/custom/DeviceInfoManager.java << 'JAVAEOF'
/*
 * Copyright (C) 2026 The Android Open Source Project
 */

package android.os.custom;

import android.annotation.SystemService;
import android.content.Context;
import android.os.RemoteException;

/**
 * 앱에서 DeviceInfoService에 접근하기 위한 Manager
 *
 * 사용법:
 *   DeviceInfoManager mgr = context.getSystemService(DeviceInfoManager.class);
 *   String model = mgr.getDeviceModel();
 */
@SystemService(Context.DEVICE_INFO_SERVICE)
public class DeviceInfoManager {

    private final IDeviceInfoService mService;

    /** @hide */
    public DeviceInfoManager(IDeviceInfoService service) {
        mService = service;
    }

    /**
     * 디바이스 모델명 반환
     */
    public String getDeviceModel() {
        try {
            return mService.getDeviceModel();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * 시스템 가동 시간(초) 반환
     */
    public long getUptimeSeconds() {
        try {
            return mService.getUptimeSeconds();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * 커스텀 속성 설정 (WRITE_DEVICE_INFO 권한 필요)
     */
    public boolean setCustomProperty(String key, String value) {
        try {
            return mService.setCustomProperty(key, value);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * 커스텀 속성 조회
     */
    public String getCustomProperty(String key) {
        try {
            return mService.getCustomProperty(key);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
JAVAEOF

echo "✅ Manager 클래스 생성 완료"
```

#### Step 3: Context 상수 및 SystemServiceRegistry 등록

`Context.java`에 상수 추가:

```java
// frameworks/base/core/java/android/content/Context.java
/**
 * DeviceInfoService 접근용 상수
 * @see #getSystemService(Class)
 * @see DeviceInfoManager
 */
public static final String DEVICE_INFO_SERVICE = "device_info";
```

`SystemServiceRegistry.java`에 등록:

```java
// frameworks/base/core/java/android/app/SystemServiceRegistry.java
// static {} 블록 내에 추가

registerService(Context.DEVICE_INFO_SERVICE, DeviceInfoManager.class,
    new CachedServiceFetcher<DeviceInfoManager>() {
        @Override
        public DeviceInfoManager createService(ContextImpl ctx)
                throws ServiceNotFoundException {
            IBinder b = ServiceManager.getServiceOrThrow(
                Context.DEVICE_INFO_SERVICE);
            IDeviceInfoService service =
                IDeviceInfoService.Stub.asInterface(b);
            return new DeviceInfoManager(service);
        }
    });
```

---

### 실습 6-4. SELinux 정책 작성

**목표:** 커스텀 System Service를 위한 SELinux 정책을 작성합니다.

#### Step 1: service_contexts 등록

```bash
# service_contexts 파일에 서비스 타입 등록
echo 'device_info                       u:object_r:device_info_service:s0' >> \
    ~/aosp/system/sepolicy/private/service_contexts
```

#### Step 2: 서비스 타입 정의

```bash
# 서비스 타입 정의 파일 생성
cat > ~/aosp/system/sepolicy/public/device_info_service.te << 'EOF'
# DeviceInfoService SELinux 타입 정의
type device_info_service, system_api_service, system_server_service, service_manager_type;
EOF
```

#### Step 3: 접근 규칙 추가

```bash
# system_server가 서비스를 등록할 수 있도록 허용
cat >> ~/aosp/system/sepolicy/private/system_server.te << 'EOF'

# DeviceInfoService
allow system_server device_info_service:service_manager add;
EOF
```

```bash
# 앱에서 서비스를 찾을 수 있도록 허용
cat >> ~/aosp/system/sepolicy/private/untrusted_app_all.te << 'EOF'

# DeviceInfoService 접근 허용
allow untrusted_app_all device_info_service:service_manager find;
EOF
```

**✏️ SELinux 정책 체크리스트:**

- [ ] `service_contexts`: 서비스명 → SELinux 타입 매핑
- [ ] 서비스 타입 정의 (.te): `service_manager_type` 포함
- [ ] `system_server.te`: `add` 권한
- [ ] 앱 도메인 `.te`: `find` 권한
- [ ] neverallow 충돌 없음 확인

---

### 실습 6-5. 빌드, 배포, 테스트

**목표:** 수정된 AOSP를 빌드하고 에뮬레이터에서 커스텀 서비스를 테스트합니다.

#### Step 1: 빌드

```bash
cd ~/aosp
source build/envsetup.sh
lunch sdk_car_x86_64-userdebug

# 전체 빌드 (변경이 많으므로)
m -j$(nproc)

# 또는 framework만 빌드 (시간 절약)
m framework-minus-apex services -j$(nproc)
```

> ⚠️ **빌드 오류 대응:**
> - `AIDL unknown type` → Android.bp의 filegroup에 .aidl 경로 확인
> - `cannot find symbol` → import 경로 확인, make clean 후 재빌드
> - `SELinux neverallow` → 정책 수정 필요 (아키텍처 재검토)

#### Step 2: 에뮬레이터 실행

```bash
# 에뮬레이터 시작
emulator &

# 부팅 완료 대기
adb wait-for-device
adb shell getprop sys.boot_completed
# "1" 출력까지 대기
```

#### Step 3: service call 테스트

```bash
# 서비스 등록 확인
adb shell service check device_info
# 출력: Service device_info: found

# 서비스 목록에서 확인
adb shell service list | grep device_info

# service call로 직접 호출 테스트
# getDeviceModel() 호출 (트랜잭션 코드 1)
adb shell service call device_info 1
# 출력 예: Result: Parcel(00000000 00000007 00640073 005f006b ...)
# → UTF-16 인코딩된 모델명

# getUptimeSeconds() 호출 (트랜잭션 코드 2)
adb shell service call device_info 2
# 출력 예: Result: Parcel(00000000 00000000 0000012c 00000000 ...)
# → long 값 (uptime 초)
```

#### Step 4: dumpsys 테스트

```bash
# dumpsys로 서비스 상태 덤프
adb shell dumpsys device_info

# 예상 출력:
# === DeviceInfoService Status ===
# Device Model: sdk_car_x86_64
# Uptime: 342s
# Custom Properties (0):
```

#### Step 5: 앱에서 Manager 사용 테스트 (adb shell 환경)

```bash
# app_process로 간단 테스트 (Java 코드 직접 실행)
adb shell "am instrument -w -e class android.os.custom.DeviceInfoServiceTest \
    android.test/android.test.InstrumentationTestRunner" 2>/dev/null || \
    echo "테스트 앱이 없는 경우 → service call로 검증"
```

**✏️ 테스트 결과 기록:**

| 테스트 항목 | 명령어 | 예상 결과 | 실제 결과 | Pass/Fail |
|------------|--------|----------|----------|-----------|
| 서비스 등록 확인 | `service check device_info` | found | | |
| getDeviceModel() | `service call device_info 1` | 모델명 Parcel | | |
| getUptimeSeconds() | `service call device_info 2` | long 값 | | |
| dumpsys 출력 | `dumpsys device_info` | 상태 정보 | | |
| SELinux 거부 없음 | `logcat \| grep avc` | 거부 로그 없음 | | |

---

## 📝 Day 2 종합 과제

### 과제 1: Binder 성능 분석 보고서

아래 항목을 포함하는 분석 보고서를 작성하세요:

1. **system_server의 Binder mmap 매핑 정보** (주소, 크기, 권한)
2. **Binder Thread Pool 현황** (활성 스레드 수, 최대값, 대기 상태)
3. **10초간 Perfetto 트레이스 분석 결과** (상위 5개 Binder 호출, 평균 지연시간)
4. **failed_transaction_log 분석** (있을 경우)

### 과제 2: 커스텀 System Service 확장

실습에서 만든 `DeviceInfoService`에 다음 기능을 추가하세요:

1. **`getMemoryInfo()` 메서드**: 현재 메모리 사용량(total, available, used)을 반환하는 메서드 추가
   - 힌트: `ActivityManager.MemoryInfo` 또는 `/proc/meminfo` 파싱
2. **콜백 메커니즘**: 속성 변경 시 등록된 리스너에게 알림
   - 힌트: `RemoteCallbackList<IOnPropertyChangedListener>` 사용
3. **Rate Limiting**: 특정 UID의 초당 호출 횟수를 제한하는 로직 추가
   - 힌트: `getCallingUid()` + `ConcurrentHashMap<Integer, AtomicInteger>`

### 과제 3: 보안 코드 리뷰

다음 코드의 보안 문제점을 **모두** 찾아 수정하세요:

```java
public class InsecureService extends IMyService.Stub {
    private HashMap<String, String> mData = new HashMap<>();  // 문제 1

    @Override
    public void writeData(String key, String value) {
        // 문제 2: 권한 체크 누락
        mData.put(key, value);

        // 문제 3: clearCallingIdentity 누락
        Settings.System.putString(mContext.getContentResolver(), key, value);
    }

    @Override
    public String readData(String key) {
        // 문제 4: 입력값 검증 누락
        return mData.get(key);
    }

    // 문제 5: dump() 메서드 누락
}
```

**수정 사항 체크리스트:**

| 문제 | 설명 | 수정 방법 |
|------|------|----------|
| 문제 1 | | |
| 문제 2 | | |
| 문제 3 | | |
| 문제 4 | | |
| 문제 5 | | |

---

## 🔗 참고 자료

| 자료 | 경로/URL |
|------|---------|
| Binder Driver 소스 | `~/aosp/drivers/android/binder.c` |
| libbinder (Native) | `~/aosp/frameworks/native/libs/binder/` |
| Java Binder | `~/aosp/frameworks/base/core/java/android/os/Binder.java` |
| JNI Bridge | `~/aosp/frameworks/base/core/jni/android_util_Binder.cpp` |
| SystemServer | `~/aosp/frameworks/base/services/java/com/android/server/SystemServer.java` |
| SELinux 정책 | `~/aosp/system/sepolicy/` |
| Perfetto 문서 | https://perfetto.dev/docs/ |
| AIDL 가이드 | https://source.android.com/docs/core/architecture/aidl |

---

> **📌 강사 메모:** 실습 6번(커스텀 System Service)은 빌드 시간이 오래 걸릴 수 있으므로, Step 1~4(코드 작성)까지 먼저 완료하고 빌드를 시작한 후, 빌드 대기 중에 Chapter 4~5 연습문제 풀이 및 코드 리뷰를 진행하는 것을 권장합니다.
