# UDP Diagnostics, Benchmarking & Network Resilience Testing

A comprehensive plan to expand GlidePad's network benchmarking framework with deep UDP diagnostics (latency percentiles, RFC 3550 jitter, throughput, packet loss distribution), build a robust unit testing suite for network resilience and UDP socket timeout retry flows, and prepare the release bump to version 5.0.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and test decisions have been aligned based on user clarification:
>
> * **Primary Diagnostic Scope**: Deep UDP diagnostic information including sub-millisecond round-trip time (RTT), RFC 3550 inter-packet jitter, packet throughput (Hz and KB/s), packet loss percentage, out-of-order sequence detection, and Wi-Fi interface telemetry (link speed, frequency band, signal strength).
> * **Test Priority**: Dedicated unit and integration tests focusing on **network resilience**, socket timeout handling, exponential backoff/retry policies, and reconnection state transitions.
> * **Trigger Model**: On-demand execution initiated directly from the **Wi-Fi Connection Dialog** on the mobile device, communicating with `glidepad_benchmark_receiver.py` on the PC.
> * **Release Management**: Increment `versionCode` from 4 to 5 and update `versionName` to `"5.0"` in `app/build.gradle.kts`.

---

### 1. Overview & Core Concept

* **What It Does**:
  1. **Advanced UDP Diagnostics & Benchmark**: Enhances the on-device benchmarking engine and the standalone PC receiver (`glidepad_benchmark_receiver.py`) to provide real-time network telemetry: minimum, average, p50, p95, and maximum RTT latency, RFC 3550 jitter variance, packet loss rate, and transmission stability scoring.
  2. **Network Resilience & Retry Architecture**: Hardens the UDP communication layer with deterministic retry policies, sequence gap tracking, heartbeat failure detection, and graceful recovery when Wi-Fi packets drop.
  3. **Resilience Unit Test Suite**: Implements isolated JVM unit tests using Robolectric and mock UDP sockets to verify socket timeout behavior, packet loss recovery, heartbeat timeouts, and state transitions without requiring a physical PC host.
* **Target Audience**: Gamers, productivity enthusiasts, and wireless touchpad users who need ultra-low latency cursor response and transparent visibility into their local Wi-Fi performance.
* **Key Value**: Delivers tangible validation that the network link is esports-ready (<6ms RTT) and guarantees the app remains stable during intermittent network drops.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Initiating Benchmark & Diagnostics**:
   * User opens the **Wi-Fi Connection Dialog** from the top app bar.
   * Taps **"Run Network Latency Benchmark"**.
   * A full diagnostic modal displays live progress across 3 phases:
     * *Phase 1*: RTT ping-pong trials (40 pings at 40Hz) with live latency readout and jitter gauge.
     * *Phase 2*: High-frequency streaming burst (360 packets at 120Hz) measuring throughput and packet drops.
     * *Phase 3*: Statistical analysis card showing min/avg/p95 latency, packet loss %, jitter, and an esports performance grade (`A+` to `C`).
2. **Reviewing Detailed UDP Diagnostics**:
   * An expandable **"Diagnostic Details"** accordion reveals Wi-Fi frequency band (5GHz vs 2.4GHz), link speed (Mbps), signal strength (RSSI), socket buffer sizes, and packet loss distribution.
   * User can tap **"Copy Report"** or **"Attach to Diagnostics"** to submit data to the cloud error reporting system if issues are found.
3. **Network Hiccup & Recovery Feedback**:
   * If packets timeout during touchpad use or benchmark, the app seamlessly transitions through an unblocked reconnect attempt without freezing the UI, displaying an unobtrusive connection badge.

#### Visual Styling & Hierarchy
* **Palette**: Dark Cyber-Slate surface (`#12161F`) with electric cyan (`#00F0FF`) and neon green accents for high grades (`A+`), amber for warning (`B`), and coral red for high jitter/loss (`C`).
* **Telemetry Displays**: Monospaced tabular numbers for latencies (`X.X ms`) and throughput (`XXX Hz`), preventing layout jitter during rapid updates.
* **Progress Visualization**: Linear gradient progress bar with phase step chips indicating the active benchmarking stage.

---

### 3. Key Product Decisions & Trade-Offs

* **Decision 1: UDP Datagram Ping-Pong vs. ICMP Echo (Standard Ping)**
  * *Chosen Approach*: Use application-layer UDP datagrams matching the actual cursor protocol port (`8089`).
  * *Why*: ICMP ping requires root privileges on Android and does not reflect actual UDP socket scheduling, firewall rules, or receiver script responsiveness. UDP testing measures real-world touchpad performance.
* **Decision 2: RFC 3550 Moving Average Jitter Calculation**
  * *Chosen Approach*: Implement standard VoIP/RTP RFC 3550 inter-arrival jitter algorithm `J = J + (|D| - J) / 16`.
  * *Why*: Provides an industry-standard, smooth metric that captures packet burstiness and Wi-Fi airtime contention without being skewed by a single isolated outlier.
* **Decision 3: Mock Socket Testing for Network Resilience**
  * *Chosen Approach*: Decouple socket I/O from packet processing and heartbeat state management, allowing unit tests to simulate timeouts, dropped packets, and out-of-order packets deterministically on the JVM.
  * *Why*: Eliminates flaky tests that rely on external physical network adapters while enabling 100% reproducible test runs in CI/Gradle.

---

### 4. Technical Architecture & Data Strategy

```
 ┌─────────────────────────────────────────────────────────────┐
 │                     ConnectionDialog UI                     │
 │  ┌───────────────────────────────────────────────────────┐  │
 │  │      BenchmarkDialog (Live RTT, Jitter, Grade)        │  │
 │  └──────────────────────────┬────────────────────────────┘  │
 └─────────────────────────────┼───────────────────────────────┘
                               │
 ┌─────────────────────────────▼───────────────────────────────┐
 │                       MainViewModel                         │
 │  ┌──────────────────────────┴────────────────────────────┐  │
 │  │        BenchmarkState (Idle, Running, Completed)      │  │
 │  └──────────────────────────┬────────────────────────────┘  │
 └─────────────────────────────┼───────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
 ┌─────────────────────────────┐       ┌─────────────────────────────┐
 │       BenchmarkEngine       │       │    NetworkStreamManager     │
 │  - RTT ping-pong trials     │       │  - UDP packet transmission  │
 │  - 120Hz throughput burst   │       │  - Heartbeat & keep-alive   │
 │  - RFC 3550 jitter math     │       │  - Timeout & retry policies │
 │  - Diagnostic synthesis     │       │  - Wi-Fi link telemetry     │
 └──────────────┬──────────────┘       └──────────────┬──────────────┘
                │                                     │
                └──────────────────┬──────────────────┘
                                   │ UDP (Port 8089)
                                   ▼
 ┌─────────────────────────────────────────────────────────────┐
 │         PC Host: glidepad_benchmark_receiver.py             │
 │  - Echoes benchmark pings with server timestamps            │
 │  - Records 120Hz packet stream and measures arrival gaps    │
 │  - Generates JSON summary and packet loss report            │
 └─────────────────────────────────────────────────────────────┘
```

#### Planned Unit Test Suite Expansion

1. **`NetworkResilienceTest.kt`**:
   * **Test Case 1 (Socket Timeout & State Transition)**: Verifies that when a ping times out after `soTimeout`, the manager transitions `testState` to `WifiConnectionTestState.Error` without throwing an unhandled exception or leaking the socket.
   * **Test Case 2 (Heartbeat Drop Detection)**: Verifies that when no pong is received for >6000ms, `isPcOnline` correctly flips from `true` to `false`.
   * **Test Case 3 (Heartbeat Recovery)**: Verifies that subsequent incoming pongs restore `isPcOnline` to `true` and update `liveLatencyMs`.
   * **Test Case 4 (Exponential Backoff on Reconnect)**: Validates that reconnect attempts throttle appropriately rather than flooding the network with rapid socket requests.
   * **Test Case 5 (Corrupt / Partial Packet Resilience)**: Verifies that malformed or truncated JSON packets sent to `handleIncomingPacket()` are safely logged without crashing the receiver coroutine.

2. **`BenchmarkEngineTest.kt` (Extended)**:
   * **Test Case 6 (Jitter Convergence)**: Validates that RFC 3550 jitter calculation converges accurately across simulated variable packet intervals (e.g. 5ms, 25ms, 10ms, 40ms).
   * **Test Case 7 (Extreme Packet Loss Handling)**: Tests grading and recommendation outputs when packet loss is 20%, 50%, or 100%.
   * **Test Case 8 (Zero Latency Edge Case)**: Tests sub-millisecond round-trip clamping (`coerceAtLeast(1)`) to ensure divide-by-zero errors never occur.

3. **Release Bump Execution**:
   * Update `app/build.gradle.kts`:
     * `versionCode = 5`
     * `versionName = "5.0"`
   * Update `MainViewModel.kt` diagnostics default `appVersion = "5.0"`.

---

### 5. Implementation Roadmap (Upon Approval)

1. **Version Code Bump**: Update `app/build.gradle.kts` (`versionCode = 5`, `versionName = "5.0"`) and sync version strings across the codebase.
2. **Network Resilience Hardening**: Refactor `NetworkStreamManager` to support configurable timeouts, retry listeners, and resilient packet decoding.
3. **Resilience Unit Tests**: Create `NetworkResilienceTest.kt` covering all timeout, retry, and packet drop edge cases using JUnit 4 and Kotlin coroutines test runners.
4. **Benchmark Engine Enhancements**: Add percentile latency distribution (p50, p95, p99) and detailed UDP network interface telemetry to `BenchmarkEngine`.
5. **Verification**: Run `gradle :app:testDebugUnitTest` and `compile_applet` to confirm 100% test pass rate and clean build.
