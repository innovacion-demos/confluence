#!/usr/bin/env python3
"""
Probe mejorado para MCP + Py4J + Qdrant
Ejecuta después de arrancar la JVM con Py4jSpringBootLauncher.

- Lanza ingesta de Confluence
- Espera a que se indexen los documentos
- Muestra listPages y searchSimilar con todos los campos
"""
import sys
import time
from py4j.java_gateway import JavaGateway, GatewayParameters

HOST = '127.0.0.1'
PORT = 25333
WAIT_INDEX_SECONDS = 10  # tiempo a esperar después de la ingesta

def main():
    # Conectar al gateway
    try:
        g = JavaGateway(gateway_parameters=GatewayParameters(address=HOST, port=PORT))
        ep = g.entry_point
        print('entry_point:', ep)
    except Exception as e:
        print('ERROR: no se pudo conectar al Gateway Py4J:', e)
        sys.exit(2)

    # Lanzar ingesta
    try:
        out = ep.ingestConfluence()
        print('\ningestConfluence() ->', out)
        print(f'Esperando {WAIT_INDEX_SECONDS}s a que Qdrant indexe los datos...')
        time.sleep(WAIT_INDEX_SECONDS)
    except Exception as e:
        print('ERROR ingestConfluence:', e)
        sys.exit(1)

    # Probar listPages
    try:
        pages = ep.listPages(5)
        print('\nlistPages(5) ->')
        for i, p in enumerate(pages):
            try:
                pid = p.getId() if hasattr(p, 'getId') else getattr(p, 'id', None)
                title = p.getTitle() if hasattr(p, 'getTitle') else getattr(p, 'title', None)
                url = p.getUrl() if hasattr(p, 'getUrl') else getattr(p, 'url', None)
                content = p.getContent() if hasattr(p, 'getContent') else getattr(p, 'content', None)
                print(f"[{i}] id={pid} title={title} url={url} content={content[:50] if content else ''}…")
            except Exception:
                print(f"[{i}] (no convertible) -> {p}")
    except Exception as e:
        print('ERROR listPages:', e)

    # Probar searchSimilar
    try:
        query = 'administración del portal'
        docs = ep.searchSimilar(query, 3)
        print(f'\nsearchSimilar("{query}", 3) ->')
        for i, doc in enumerate(docs):
            try:
                pid = doc.getId() if hasattr(doc, 'getId') else getattr(doc, 'id', None)
                title = doc.getTitle() if hasattr(doc, 'getTitle') else getattr(doc, 'title', None)
                url = doc.getUrl() if hasattr(doc, 'getUrl') else getattr(doc, 'url', None)
                content = doc.getContent() if hasattr(doc, 'getContent') else getattr(doc, 'content', None)
                print(f"[{i}] id={pid} title={title} url={url} content={content[:50] if content else ''}…")
            except Exception:
                print(f"[{i}] (no convertible) -> {doc}")
    except Exception as e:
        print('ERROR searchSimilar:', e)

if __name__ == '__main__':
    main()