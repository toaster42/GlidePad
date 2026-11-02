#!/usr/bin/env python3
# ==============================================================================
#  GlidePad Latency & Throughput Benchmarking Receiver (v1.1)
# ==============================================================================
#  Dedicated receiver script for measuring network performance between Android
#  device and PC host over Wi-Fi / Local Area Network.
#
#  Measures:
#    - Round-Trip Time (RTT) & One-Way Latency (Min, Mean, P95, P99, Max)
#    - Inter-packet Arrival Jitter (RFC 3550 standard)
#    - Packet Throughput (Hz / packets per second & Bandwidth in KB/s)
#    - Packet Loss, Sequence Gaps, and Out-of-Order Delivery
#
#  Usage:
#    python glidepad_benchmark_receiver_v1_1.py [--port 8089] [--headless] [--output results.json]
#
#  Requirements:
#    - Python 3.7+
#    - pynput (Optional: for executing live cursor moves during benchmarks)
# ==============================================================================

import argparse
import json
import math
import os
import platform
import socket
import sys
import time
from datetime import datetime

# Optional pynput support for virtual cursor movement
PYNPUT_AVAILABLE = False
try:
    from pynput.mouse import Button, Controller as MouseController
    from pynput.keyboard import Key, KeyCode, Controller as KeyController
    mouse = MouseController()
    keyboard = KeyController()
    PYNPUT_AVAILABLE = True
except (ImportError, Exception):
    mouse = None
    keyboard = None


class BenchmarkSession:
    def __init__(self, session_id="default"):
        self.session_id = session_id
        self.start_time = None
        self.end_time = None
        self.expected_seq = 0
        self.total_packets_received = 0
        self.packets_lost = 0
        self.out_of_order = 0
        self.latencies_ms = []
        self.inter_arrival_times_ms = []
        self.last_arrival_time = None
        self.total_bytes = 0
        self.jitter = 0.0  # RFC 3550 moving average

    def start(self):
        self.start_time = time.time()
        self.expected_seq = 0
        self.total_packets_received = 0
        self.packets_lost = 0
        self.out_of_order = 0
        self.latencies_ms.clear()
        self.inter_arrival_times_ms.clear()
        self.last_arrival_time = None
        self.total_bytes = 0
        self.jitter = 0.0

    def record_packet(self, seq, client_timestamp_ms, data_len):
        now = time.time()
        now_ms = now * 1000.0
        self.total_bytes += data_len
        self.total_packets_received += 1

        # Calculate one-way transit if clocks are in ballpark or RTT
        transit_ms = max(0.0, now_ms - client_timestamp_ms) if client_timestamp_ms > 0 else 0.0
        self.latencies_ms.append(transit_ms)

        # Inter-arrival jitter calculation (RFC 3550)
        if self.last_arrival_time is not None:
            interval_ms = (now - self.last_arrival_time) * 1000.0
            self.inter_arrival_times_ms.append(interval_ms)
            if len(self.inter_arrival_times_ms) > 1:
                diff = abs(interval_ms - self.inter_arrival_times_ms[-2])
                self.jitter += (diff - self.jitter) / 16.0
        self.last_arrival_time = now

        # Sequence gap checking
        if seq > self.expected_seq:
            self.packets_lost += (seq - self.expected_seq)
            self.expected_seq = seq + 1
        elif seq < self.expected_seq:
            self.out_of_order += 1
        else:
            self.expected_seq += 1

    def finish(self):
        self.end_time = time.time()
        duration = max(0.001, self.end_time - (self.start_time or self.end_time))
        avg_hz = self.total_packets_received / duration
        kb_per_sec = (self.total_bytes / 1024.0) / duration

        # Latency statistics
        sorted_lats = sorted(self.latencies_ms) if self.latencies_ms else [0.0]
        n = len(sorted_lats)
        avg_lat = sum(sorted_lats) / n if n > 0 else 0.0
        min_lat = sorted_lats[0]
        p50_lat = sorted_lats[int(n * 0.50)] if n > 0 else 0.0
        p95_lat = sorted_lats[int(n * 0.95)] if n > 0 else 0.0
        p99_lat = sorted_lats[int(n * 0.99)] if n > 0 else 0.0
        max_lat = sorted_lats[-1]

        total_expected = self.total_packets_received + self.packets_lost
        loss_pct = (self.packets_lost / total_expected * 100.0) if total_expected > 0 else 0.0

        return {
            "session_id": self.session_id,
            "duration_sec": round(duration, 3),
            "packets_received": self.total_packets_received,
            "packets_lost": self.packets_lost,
            "packet_loss_pct": round(loss_pct, 2),
            "out_of_order": self.out_of_order,
            "throughput_hz": round(avg_hz, 1),
            "bandwidth_kb_s": round(kb_per_sec, 2),
            "jitter_ms": round(self.jitter, 3),
            "latency": {
                "min_ms": round(min_lat, 2),
                "avg_ms": round(avg_lat, 2),
                "p50_ms": round(p50_lat, 2),
                "p95_ms": round(p95_lat, 2),
                "p99_ms": round(p99_lat, 2),
                "max_ms": round(max_lat, 2)
            }
        }


def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip


def print_banner(local_ip, port, headless):
    print("\n" + "=" * 70)
    print("     GLIDEPAD HIGH-PRECISION BENCHMARK RECEIVER (v1.1)     ")
    print("=" * 70)
    print(f"  Target Host OS : {platform.system()} ({socket.gethostname()})")
    print(f"  Local IP Addr  : {local_ip}")
    print(f"  UDP Port       : {port}")
    print(f"  Pynput HID     : {'Enabled (Live emulation)' if (PYNPUT_AVAILABLE and not headless) else 'Disabled (Headless Benchmark Mode)'}")
    print("-" * 70)
    print(f"  >>> Enter this IP into GlidePad app: {local_ip}")
    print("  Listening for Live Control, Ping RTT, & High-Speed Stream Tests...")
    print("=" * 70 + "\n")


def print_summary_card(summary):
    print("\n" + "╔" + "═" * 68 + "╗")
    print("║" + "             BENCHMARK TRIAL COMPLETED SUMMARY".center(68) + "║")
    print("╠" + "═" * 68 + "╣")
    print(f"║  Duration         : {summary['duration_sec']} seconds".ljust(69) + "║")
    print(f"║  Packets Received : {summary['packets_received']} pkts".ljust(69) + "║")
    print(f"║  Packets Lost     : {summary['packets_lost']} pkts ({summary['packet_loss_pct']}%)".ljust(69) + "║")
    print(f"║  Out of Order     : {summary['out_of_order']} pkts".ljust(69) + "║")
    print(f"║  Stream Frequency : {summary['throughput_hz']} Hz (Target: 120-240Hz)".ljust(69) + "║")
    print(f"║  Data Bandwidth   : {summary['bandwidth_kb_s']} KB/s".ljust(69) + "║")
    print(f"║  Network Jitter   : {summary['jitter_ms']} ms".ljust(69) + "║")
    print("╟" + "─" * 68 + "╢")
    lat = summary['latency']
    print(f"║  RTT / Transit Latencies:".ljust(69) + "║")
    print(f"║    • Min Latency  : {lat['min_ms']} ms".ljust(69) + "║")
    print(f"║    • Mean Latency : {lat['avg_ms']} ms".ljust(69) + "║")
    print(f"║    • Median (P50) : {lat['p50_ms']} ms".ljust(69) + "║")
    print(f"║    • 95th %tile   : {lat['p95_ms']} ms".ljust(69) + "║")
    print(f"║    • 99th %tile   : {lat['p99_ms']} ms".ljust(69) + "║")
    print(f"║    • Max Latency  : {lat['max_ms']} ms".ljust(69) + "║")
    print("╚" + "═" * 68 + "╝\n")


def main():
    parser = argparse.ArgumentParser(description="GlidePad Wi-Fi Latency & Throughput Benchmark Server (v1.1)")
    parser.add_argument("--port", type=int, default=8089, help="UDP listening port (default: 8089)")
    parser.add_argument("--headless", action="store_true", help="Run without simulating real mouse cursor moves")
    parser.add_argument("--output", type=str, default="", help="Path to save JSON benchmark logs")
    args = parser.parse_args()

    local_ip = get_local_ip()
    print_banner(local_ip, args.port, args.headless)

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 65536)
    except Exception:
        pass
    sock.bind(('0.0.0.0', args.port))

    session = BenchmarkSession()
    all_summaries = []

    # Real-time counter for terminal display
    last_stat_print = time.time()
    packet_rate_window = 0
    recent_rtts = []

    try:
        while True:
            data, addr = sock.recvfrom(4096)
            recv_time_ms = time.time() * 1000.0
            packet_rate_window += 1

            try:
                msg = json.loads(data.decode('utf-8'))
            except Exception:
                continue

            msg_type = msg.get("type", "")

            # 1. BENCHMARK BURST START
            if msg_type == "benchmark_start":
                session_id = msg.get("session_id", f"trial_{int(time.time())}")
                session = BenchmarkSession(session_id)
                session.start()
                ack = json.dumps({"type": "benchmark_start_ack", "session_id": session_id, "status": "ready"}).encode('utf-8')
                sock.sendto(ack, addr)
                print(f"[BENCHMARK] Trial Started: '{session_id}' from {addr[0]}")

            # 2. BENCHMARK BURST STREAM PACKET
            elif msg_type == "benchmark_packet":
                seq = msg.get("seq", 0)
                client_time = msg.get("time", 0)
                session.record_packet(seq, client_time, len(data))

            # 3. BENCHMARK BURST FINISH
            elif msg_type == "benchmark_finish":
                summary = session.finish()
                all_summaries.append(summary)
                print_summary_card(summary)

                # Return summary back to Android client for on-screen metrics display
                resp_payload = {
                    "type": "benchmark_summary",
                    "summary": summary
                }
                sock.sendto(json.dumps(resp_payload).encode('utf-8'), addr)

                if args.output:
                    try:
                        with open(args.output, "w") as f:
                            json.dump(all_summaries, f, indent=2)
                        print(f"[BENCHMARK] Saved results to {args.output}")
                    except Exception as e:
                        print(f"[!] Failed to write output file: {e}")

            # 4. BENCHMARK HIGH-PRECISION PING/PONG (RTT MEASUREMENT)
            elif msg_type == "benchmark_ping":
                client_seq = msg.get("seq", 0)
                client_time = msg.get("time", 0)
                send_time_ms = time.time() * 1000.0
                rtt_estimate = max(0.0, send_time_ms - client_time) if client_time > 0 else 0.0
                recent_rtts.append(rtt_estimate)
                if len(recent_rtts) > 50:
                    recent_rtts.pop(0)

                resp = json.dumps({
                    "type": "benchmark_pong",
                    "seq": client_seq,
                    "client_time": client_time,
                    "server_recv_time": recv_time_ms,
                    "server_send_time": send_time_ms
                }).encode('utf-8')
                client_port = msg.get("client_port", addr[1])
                sock.sendto(resp, (addr[0], client_port if client_port else addr[1]))

            # 5. STANDARD GLIDEPAD PING & DISCOVERY
            elif msg_type in ("ping", "discover"):
                client_port = msg.get("client_port", addr[1])
                resp = json.dumps({
                    "type": "pong" if msg_type == "ping" else "announce",
                    "name": f"{socket.gethostname()} (Benchmark PC)",
                    "benchmark_supported": True,
                    "status": "ok"
                }).encode('utf-8')
                sock.sendto(resp, (addr[0], client_port if client_port else addr[1]))

            # 6. STANDARD MOUSE PACKETS (with optional execution)
            elif msg_type == "mouse":
                dx = msg.get("dx", 0)
                dy = msg.get("dy", 0)
                wheel = msg.get("wheel", 0)
                if PYNPUT_AVAILABLE and not args.headless:
                    if dx != 0 or dy != 0:
                        mouse.move(dx, dy)
                    if wheel != 0:
                        mouse.scroll(0, wheel)

            # Periodic terminal status (every 2 seconds)
            now = time.time()
            if now - last_stat_print >= 2.0:
                elapsed = now - last_stat_print
                hz = packet_rate_window / elapsed
                avg_rtt = (sum(recent_rtts) / len(recent_rtts)) if recent_rtts else 0.0
                sys.stdout.write(f"\r[STATUS] Ingress: {hz:.1f} pkts/s | Avg RTT: {avg_rtt:.1f}ms | Jitter: {session.jitter:.2f}ms   ")
                sys.stdout.flush()
                packet_rate_window = 0
                last_stat_print = now

    except KeyboardInterrupt:
        print("\n\n[*] Benchmarking server stopped by user.")
        if all_summaries:
            print(f"[*] Completed {len(all_summaries)} benchmark trials in this run.")
        sys.exit(0)


if __name__ == "__main__":
    main()
