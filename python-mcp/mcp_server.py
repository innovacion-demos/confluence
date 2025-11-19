import asyncio
import sys
import os
from py4j.java_gateway import JavaGateway, GatewayParameters
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent

# Configuración
PY4J_PORT = int(os.environ.get("PY4J_PORT", "25333"))
DEBUG = os.environ.get("DEBUG") == "1"

def log(msg):
    if DEBUG:
        print(f"[DEBUG] {msg}", file=sys.stderr)

# Inicializar servidor MCP
server = Server("confluence-mcp")
_gateway = None
_mcp = None

async def ensure_gateway():
    global _gateway, _mcp
    if _gateway and _mcp:
        return True, None
    try:
        _gateway = JavaGateway(gateway_parameters=GatewayParameters(port=PY4J_PORT))
        _mcp = _gateway.entry_point
        log(f"Conectado Py4J puerto {PY4J_PORT}")
        return True, None
    except Exception as e:
        log(f"Error conexión Py4J: {e}")
        import traceback
        log(traceback.format_exc())
        return False, str(e)

# Definir tools
@server.list_tools()
async def list_tools():
    return [
        Tool(
            name="listPages",
            description="Lista páginas vectorizadas de Confluence",
            inputSchema={
                "type": "object",
                "properties": {
                    "limit": {"type": "number", "description": "Número máximo de páginas"}
                },
                "required": []
            }
        ),
        Tool(
            name="searchSimilar",
            description="Busca páginas similares a una consulta",
            inputSchema={
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Texto de búsqueda"},
                    "limit": {"type": "number", "description": "Número máximo de resultados"}
                },
                "required": ["query"]
            }
        ),
        Tool(
            name="ingestConfluence",
            description="Inicia ingesta de Confluence",
            inputSchema={
                "type": "object",
                "properties": {},
                "required": []
            }
        ),
        Tool(
            name="getPageById",
            description="Obtiene el contenido completo de una página por su ID",
            inputSchema={
                "type": "object",
                "properties": {
                    "pageId": {"type": "string", "description": "ID único de la página"}
                },
                "required": ["pageId"]
            }
        )
    ]

@server.call_tool()
async def call_tool(name: str, arguments: dict):
    ok, err = await ensure_gateway()
    if not ok:
        return [TextContent(type="text", text=f"Error: Backend no disponible: {err}")]

    try:
        if name == "listPages":
            limit = int(arguments.get("limit", 10))
            log(f"Llamando listPages({limit})")
            try:
                docs = _mcp.listPages(limit)
                log(f"Retorno de Java tipo: {type(docs)}")

                pages = []
                for i, d in enumerate(docs):
                    try:
                        # Extraer datos del objeto Java
                        page_id = d.getId()
                        page_title = d.getTitle()
                        page_url = d.getUrl()
                        page_content = d.getContent()

                        # Convertir a strings seguros
                        page_data = {
                            "id": str(page_id) if page_id else "sin-id",
                            "title": str(page_title) if page_title else "Sin título",
                            "url": str(page_url) if page_url else "Sin URL",
                            "content": str(page_content) if page_content else "Sin contenido"
                        }
                        log(f"Page {i}: {page_data}")
                        pages.append(page_data)
                    except Exception as doc_error:
                        log(f"Error procesando doc {i}: {doc_error}")

                result = f"Se encontraron {len(pages)} páginas:\n\n" + "\n---\n".join([
                    f"**{p['title']}**\nURL: {p['url']}\nID: {p['id']}\n\nContenido:\n{p['content']}"
                    for p in pages
                ])
                return [TextContent(type="text", text=result)]
            except Exception as e:
                log(f"Excepción en listPages: {e}")
                import traceback
                log(traceback.format_exc())
                return [TextContent(type="text", text=f"Error: {str(e)}")]

        elif name == "searchSimilar":
            query = arguments.get("query", "")
            limit = int(arguments.get("limit", 3))
            docs = _mcp.searchSimilar(query, limit)
            pages = [
                {
                    "id": d.getId(),
                    "title": d.getTitle(),
                    "url": d.getUrl(),
                    "content": d.getContent()
                }
                for d in docs
            ]
            result = f"Se encontraron {len(pages)} páginas similares a '{query}':\n\n" + "\n---\n".join(
                [f"**{p['title']}**\nURL: {p['url']}\nID: {p['id']}\n\nContenido:\n{p['content']}" for p in pages]
            )
            return [TextContent(type="text", text=result)]

        elif name == "ingestConfluence":
            log(f"Iniciando ingesta de Confluence...")
            outcome = _mcp.ingestConfluence()
            log(f"Ingesta completada. Resultado: {outcome}")
            result_text = str(outcome) if outcome else "Ingesta completada sin mensaje de retorno"
            return [TextContent(type="text", text=result_text)]

        elif name == "getPageById":
            page_id = arguments.get("pageId", "")
            log(f"Obteniendo página con ID: {page_id}")
            try:
                page = _mcp.getPageById(page_id)
                result = f"**{page.getTitle()}**\n\nURL: {page.getUrl()}\nID: {page_id}\n\nContenido:\n{page.getContent()}"
                return [TextContent(type="text", text=result)]
            except Exception as e:
                log(f"Error obteniendo página: {e}")
                return [TextContent(type="text", text=f"Error: {str(e)}")]

        else:
            return [TextContent(type="text", text=f"Tool {name} no reconocida")]

    except Exception as e:
        log(f"Excepción en tool {name}: {e}")
        return [TextContent(type="text", text=f"Error: {str(e)}")]

async def main():
    log("Servidor MCP iniciando...")
    async with stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            server.create_initialization_options()
        )

if __name__ == "__main__":
    asyncio.run(main())