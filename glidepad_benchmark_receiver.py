#!/usr/bin/env python3
# ==============================================================================
#  GlidePad Latency & Throughput Benchmarking Receiver (Compatibility Wrapper)
# ==============================================================================
#  This file forwards execution to 'glidepad_benchmark_receiver_v1_1.py'.
# ==============================================================================

import os
import sys

script_dir = os.path.dirname(os.path.abspath(__file__))
versioned_script = os.path.join(script_dir, "glidepad_benchmark_receiver_v1_1.py")

if os.path.exists(versioned_script):
    with open(versioned_script, "r") as f:
        code = f.read()
    exec(compile(code, versioned_script, 'exec'))
else:
    print(f"[!] Could not locate {versioned_script}")
    sys.exit(1)
