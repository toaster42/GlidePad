#!/usr/bin/env python3
# =====================================================================
#  GlidePad Ultra-Low-Latency PC Receiver (v2.2 - Wi-Fi Power Edition)
# =====================================================================
#  Requirements:
#    pip install pynput
#
#  Usage:
#    python glidepad_receiver_v2_2.py
# =====================================================================

import socket
import json
import sys
import platform
import subprocess
import webbrowser

try:
    from pynput.mouse import Button, Controller as MouseController
    from pynput.keyboard import Key, KeyCode, Controller as KeyController
except ImportError:
    print("\n[!] Missing requirement: 'pynput'")
    print("    Please run:  pip install pynput\n")
    sys.exit(1)

mouse = MouseController()
keyboard = KeyController()

UDP_PORT = 8089

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

local_ip = get_local_ip()
hostname = socket.gethostname()

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind(('0.0.0.0', UDP_PORT))

print("=" * 64)
print("  GLIDEPAD PC RECEIVER ACTIVE (v2.2)")
print("=" * 64)
print(f"  System       : {platform.system()} ({hostname})")
print(f"  UDP Port     : {UDP_PORT}")
print(f"  >>> ENTER THIS IP IN GLIDEPAD APP:  {local_ip}")
print("=" * 64)
print("  Ready for touch gestures, keys, URLs & system commands...\n")

SPECIAL_KEYS = {
    'Enter': Key.enter, 'Return': Key.enter,
    'Backspace': Key.backspace,
    'Tab': Key.tab,
    'Space': Key.space,
    'Esc': Key.esc, 'Escape': Key.esc,
    'Del': Key.delete, 'Delete': Key.delete,
    'Up': Key.up, 'Down': Key.down, 'Left': Key.left, 'Right': Key.right,
    'Home': Key.home, 'End': Key.end,
    'PgUp': Key.page_up, 'PgDn': Key.page_down,
    'Shift': Key.shift, 'Ctrl': Key.ctrl, 'Alt': Key.alt, 'Win': Key.cmd,
    'F1': Key.f1, 'F2': Key.f2, 'F3': Key.f3, 'F4': Key.f4,
    'F5': Key.f5, 'F6': Key.f6, 'F7': Key.f7, 'F8': Key.f8,
    'F9': Key.f9, 'F10': Key.f10, 'F11': Key.f11, 'F12': Key.f12,
    'MediaPlayPause': Key.media_play_pause,
    'AudioVolUp': Key.media_volume_up,
    'AudioVolDown': Key.media_volume_down,
    'AudioMute': Key.media_volume_mute
}

prev_btn = 0

while True:
    try:
        data, addr = sock.recvfrom(2048)
        msg = json.loads(data.decode('utf-8'))
        msg_type = msg.get('type')

        # 1. PING / HEARTBEAT / AUTO-DISCOVER
        if msg_type in ('ping', 'discover'):
            client_port = msg.get('client_port', addr[1])
            resp = json.dumps({
                'type': 'pong' if msg_type == 'ping' else 'announce',
                'name': f"{hostname} ({platform.system()})",
                'status': 'ok'
            }).encode('utf-8')
            sock.sendto(resp, (addr[0], client_port if client_port else addr[1]))

        # 2. MOUSE MOVEMENT, CLICKS, & SCROLL
        elif msg_type == 'mouse':
            dx = msg.get('dx', 0)
            dy = msg.get('dy', 0)
            btn = msg.get('btn', 0)
            wheel = msg.get('wheel', 0)

            if dx != 0 or dy != 0:
                mouse.move(dx, dy)
            if wheel != 0:
                mouse.scroll(0, wheel)

            if btn != prev_btn:
                # Left Click
                if (btn & 1) and not (prev_btn & 1):
                    mouse.press(Button.left)
                elif not (btn & 1) and (prev_btn & 1):
                    mouse.release(Button.left)
                # Right Click
                if (btn & 2) and not (prev_btn & 2):
                    mouse.press(Button.right)
                elif not (btn & 2) and (prev_btn & 2):
                    mouse.release(Button.right)
                # Middle Click
                if (btn & 4) and not (prev_btn & 4):
                    mouse.press(Button.middle)
                elif not (btn & 4) and (prev_btn & 4):
                    mouse.release(Button.middle)
                prev_btn = btn

        # 3. DIRECT TEXT ENTRY
        elif msg_type == 'text':
            text = msg.get('text', '')
            keyboard.type(text)
            print(f"[TEXT] Typed: {text}")

        # 4. KEYBOARD SHORTCUTS & HOTKEYS
        elif msg_type == 'keyboard':
            action = msg.get('action')
            keys_str = msg.get('keys', '')
            parts = keys_str.split('+')
            held = []

            for k in parts:
                k = k.strip()
                target_key = SPECIAL_KEYS.get(k)
                if not target_key:
                    if len(k) == 1:
                        target_key = KeyCode.from_char(k.lower())
                if target_key:
                    keyboard.press(target_key)
                    held.append(target_key)

            for target_key in reversed(held):
                keyboard.release(target_key)

        # 5. OPEN WEB URL IN DEFAULT BROWSER
        elif msg_type == 'open_url':
            url = msg.get('url', '')
            if url:
                webbrowser.open(url)
                print(f"[URL] Opened in browser: {url}")

        # 6. SYSTEM COMMANDS (Terminal, Explorer, Task Manager, Lock)
        elif msg_type == 'system_cmd':
            cmd = msg.get('command', '')
            print(f"[CMD] Received: {cmd}")
            sys_name = platform.system()
            if cmd == 'terminal':
                if sys_name == 'Windows':
                    subprocess.Popen('cmd.exe')
                elif sys_name == 'Darwin':
                    subprocess.Popen(['open', '-a', 'Terminal'])
                else:
                    subprocess.Popen('x-terminal-emulator')
            elif cmd == 'explorer':
                if sys_name == 'Windows':
                    subprocess.Popen('explorer.exe')
                elif sys_name == 'Darwin':
                    subprocess.Popen(['open', '.'])
                else:
                    subprocess.Popen(['xdg-open', '.'])
            elif cmd == 'taskmgr':
                if sys_name == 'Windows':
                    subprocess.Popen('taskmgr.exe')
                elif sys_name == 'Darwin':
                    subprocess.Popen(['open', '-a', 'Activity Monitor'])
            elif cmd == 'lock':
                if sys_name == 'Windows':
                    import ctypes
                    ctypes.windll.user32.LockWorkStation()
                elif sys_name == 'Darwin':
                    subprocess.Popen(['pmset', 'displaysleepnow'])

    except KeyboardInterrupt:
        print("\n[*] GlidePad Receiver stopped.")
        break
    except Exception as e:
        pass
