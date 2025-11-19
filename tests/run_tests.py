#!/usr/bin/env python3
"""
Pruebas básicas para el servidor MCP (stdio JSON-RPC framed + raw)
Ejecutar desde la raíz del repo:

DEBUG=1 python3 tests/run_tests.py

Qué hace:
- Lanza python-mcp/mcp_server.py como subprocess (usa mismo intérprete).
- Envía requests framed (Content-Length) de prueba: initialize, list_tools, call_tool listPages.
- Lee y muestra las respuestas.

Nota: initialize y list_tools no necesitan que Py4J esté disponible; si esos fallan, el protocolo/framing del servidor está mal.
"""
import os
import sys
import json
import subprocess
import time

ROOT = os.path.dirname(os.path.dirname(__file__))
SERVER_PATH = os.path.join(ROOT, 'python-mcp', 'mcp_server.py')


def start_server():
    env = os.environ.copy()
    env['DEBUG'] = env.get('DEBUG', '1')
    env.setdefault('PY4J_HOST', '127.0.0.1')
    env.setdefault('PY4J_PORT', '25333')
    proc = subprocess.Popen([sys.executable, SERVER_PATH], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return proc


def send_framed(proc, obj):
    body = json.dumps(obj, ensure_ascii=False)
    hdr = f"Content-Length: {len(body.encode('utf-8'))}\r\n\r\n"
    proc.stdin.write(hdr.encode('utf-8') + body.encode('utf-8'))
    proc.stdin.flush()
    # leer cabeceras de salida
    headers = b""
    while True:
        line = proc.stdout.readline()
        if not line:
            raise RuntimeError('EOF leyendo respuesta (server puede haber salido)')
        if line.strip() == b"":
            break
        headers += line
    # parsear content-length
    cl = 0
    for l in headers.splitlines():
        try:
            parts = l.decode('utf-8').split(':', 1)
            if parts[0].strip().lower() == 'content-length':
                cl = int(parts[1].strip())
        except Exception:
            continue
    if cl == 0:
        # tal vez el servidor usa modo raw (linea JSON). Intentar leer una línea
        line = proc.stdout.readline()
        if not line:
            raise RuntimeError('No hay body en la respuesta')
        return json.loads(line.decode('utf-8'))
    body = proc.stdout.read(cl)
    return json.loads(body.decode('utf-8'))


def main():
    print('Iniciando servidor de prueba...')
    proc = start_server()
    # dar un pequeño tiempo para que arranque
    time.sleep(0.2)

    tests = [
        {"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}},
        {"jsonrpc": "2.0", "id": 2, "method": "list_tools", "params": {}},
        {"jsonrpc": "2.0", "id": 3, "method": "call_tool", "params": {"name": "listPages", "arguments": {"limit": 1}}}
    ]

    try:
        for req in tests:
            print('\n-- Request:')
            print(json.dumps(req, ensure_ascii=False))
            resp = send_framed(proc, req)
            print('-- Response:')
            print(json.dumps(resp, ensure_ascii=False, indent=2))
            # si initialize falla, no continuar
            if req['method'] == 'initialize' and 'result' not in resp:
                print('initialize failed; detener pruebas')
                break
    except Exception as e:
        print('Error durante la prueba:', str(e))
    finally:
        try:
            proc.kill()
        except Exception:
            pass

if __name__ == '__main__':
    main()
