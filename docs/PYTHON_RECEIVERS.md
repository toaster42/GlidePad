# GlidePad Python PC Receiver Manual

This guide documents the Python receiver scripts, host OS installation requirements, system permissions, and testing tools.

---

## 1. Receiver Scripts Overview

| File | Version | Purpose |
|---|---|---|
| `glidepad_receiver_v2_3.py` | **v2.3** (Current) | Production PC receiver for Windows, macOS, and Linux. Translates UDP datagrams into system input events using `pynput`. |
| `glidepad_benchmark_receiver_v1_1.py` | **v1.1** (Current) | Dedicated benchmarking receiver that echoes timestamps and sequence numbers for network latency stress-testing. |
| `test_receiver_integration.py` | — | Automated standalone loopback test suite validating the minimum-jerk kinematics and UDP socket buffer integrity. |

---

## 2. Host System Permissions

### 2.1. Windows
- No special permissions required.
- If Windows Firewall displays a prompt on first run, allow access for Private Networks on port `8089`.

### 2.2. macOS
- macOS requires Accessibility permissions for the terminal executing Python:
  1. Open **System Settings > Privacy & Security > Accessibility**.
  2. Enable the switch next to **Terminal**, **iTerm2**, or your Python binary.
  3. If using an IDE like VS Code or PyCharm, enable Accessibility for that application as well.

### 2.3. Linux (X11 & Wayland)
On Linux systems, access to virtual input devices requires membership in the `input` group or a udev rule:
```bash
# Add current user to input group
sudo usermod -a -G input $USER

# Or create a udev rule for uinput access without root
echo 'KERNEL=="uinput", MODE="0660", GROUP="input", OPTIONS+="static_node=uinput"' | sudo tee /etc/udev/rules.d/99-uinput.rules
sudo udevadm control --reload-rules && sudo udevadm trigger
```

---

## 3. Running the Receiver Scripts

### Standard Production Receiver (`glidepad_receiver_v2_3.py`)
```bash
python3 glidepad_receiver_v2_3.py
```
Output:
```
================================================================
  GLIDEPAD PC RECEIVER ACTIVE (v2.3)
================================================================
  System       : Linux (my-workstation)
  UDP Port     : 8089
  >>> ENTER THIS IP IN GLIDEPAD APP:  192.168.1.150
================================================================
```

### Network Benchmark Receiver (`glidepad_benchmark_receiver_v1_1.py`)
```bash
python3 glidepad_benchmark_receiver_v1_1.py --port 8089
```

---

## 4. Integration Test Suite (`test_receiver_integration.py`)

The standalone Python test runner executes a live end-to-end loopback simulation on `127.0.0.1` without requiring GUI/display servers or root permissions:
1. Dynamically allocates an ephemeral local UDP port (zero collision).
2. Runs the in-process receiver server.
3. Streams realistic, minimum-jerk human touch trajectories (swipe arcs, ballistic flicks, tap-clicks, and 2-finger scrolls).
4. Verifies zero packet loss and validates coordinate math.

```bash
python3 test_receiver_integration.py
```
Exit code `0` indicates 100% pass rate.
