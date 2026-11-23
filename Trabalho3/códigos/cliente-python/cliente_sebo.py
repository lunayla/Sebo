import json
import threading
import time
import urllib.request
import urllib.error
import os

# Para executar em computadores diferentes, defina SEBO_API_URL com o IP do servidor.
# Exemplo: SEBO_API_URL=http://192.168.1.10:8080
BASE_URL = os.getenv("SEBO_API_URL", "http://localhost:8080").rstrip("/")

ultimo_id_notificacao = 0
executando = True
token_sessao = None


def requisicao_http(metodo: str, caminho: str, payload: dict = None):
    url = BASE_URL + caminho

    dados_bytes = json.dumps(payload).encode("utf-8") if payload is not None else None

    headers = {"Content-Type": "application/json; charset=UTF-8"}

    if token_sessao:
        headers["Authorization"] = f"Bearer {token_sessao}"

    req = urllib.request.Request(
        url,
        data=dados_bytes,
        headers=headers,
        method=metodo
    )

    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))

    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))

    except Exception as e:
        return 500, {
            "sucesso": False,
            "mensagem": f"Erro de conexão: {e}"
        }


def thread_escutar_mural():
    global ultimo_id_notificacao

    while executando:
        status, dados = requisicao_http(
            "GET",
            f"/api/notificacoes?desde={ultimo_id_notificacao}"
        )

        if status == 200 and dados.get("sucesso"):
            for notif in dados.get("notificacoes", []):
                ultimo_id_notificacao = max(
                    ultimo_id_notificacao,
                    notif["id"]
                )

                print(
                    f"\n\n>>> [NOVA NOTIFICACAO - {notif['tipo']}] "
                    f"({notif['autor']}): {notif['mensagem']}"
                )

                print("Escolha uma opção: ", end="", flush=True)

        time.sleep(2)


def main():
    global executando, token_sessao

    print("=== CLIENTE PYTHON - SEBO ===")

    usuario = input("Usuário (cliente | admin): ").strip()
    senha = input("Senha (use 1234): ").strip()

    status, resp_auth = requisicao_http(
        "POST",
        "/api/auth/login",
        {"usuario": usuario, "senha": senha}
    )

    if status != 200 or not resp_auth.get("sucesso"):
        print("Falha no login:", resp_auth.get("mensagem"))
        return

    token_sessao = resp_auth.get("token")
    perfil = resp_auth.get("perfil")

    print(f"Conectado como '{usuario}'!")

    t_mural = threading.Thread(
        target=thread_escutar_mural,
        daemon=True
    )

    t_mural.start()

    while executando:
        print("\n--- MENU DO SEBO (Cliente Python) ---")
        print("1. Listar acervo")

        if perfil == "ADMIN":
            print("2. Cadastrar produto")
            print("3. Comprar produto")
            print("4. Avaliar troca")
            print("5. Publicar aviso no Mural")
        else:
            print("2. Comprar produto")
            print("3. Avaliar troca")

        print("0. Sair")

        opcao = input("Escolha uma opção: ").strip()

        if opcao == "1":
            _, res = requisicao_http("GET", "/api/produtos")

            print(f"\nAcervo de {res.get('loja')}:")

            for p in res.get("produtos", []):
                trocavel_txt = "Sim" if p["trocavel"] else "Não"

                print(
                    f"  ID {p['id']} | [{p['tipo']}] {p['titulo']} "
                    f"- R$ {p['preco']:.2f} ({p['estado']}) "
                    f"| Trocável: {trocavel_txt}"
                )

        elif opcao == "2" and perfil == "ADMIN":
            tipo = input(
                "Tipo (LIVRO, APOSTILA, CD, EBOOK): "
            ).strip().upper()

            titulo = input("Título: ").strip()

            preco = float(
                input("Preço (ex: 40.00): ").replace(",", ".")
            )

            estado = input(
                "Estado (Novo, Seminovo, Usado): "
            ).strip()

            extra = input(
                "Info extra (Autor / Disciplina / Artista / Formato): "
            ).strip()

            _, res = requisicao_http(
                "POST",
                "/api/produtos",
                {
                    "tipo": tipo,
                    "titulo": titulo,
                    "preco": preco,
                    "estado": estado,
                    "extra": extra
                }
            )

            print("Servidor:", res.get("mensagem"))

        elif opcao == ("3" if perfil == "ADMIN" else "2"):
            id_prod = int(
                input("ID do produto para comprar: ").strip()
            )

            _, res = requisicao_http(
                "POST",
                "/api/negociacoes/comprar",
                {"id": id_prod}
            )

            print("Servidor:", res.get("mensagem"))

        elif opcao == ("4" if perfil == "ADMIN" else "3"):
            id_prod = int(
                input("ID do produto para avaliar troca: ").strip()
            )

            _, res = requisicao_http(
                "POST",
                "/api/negociacoes/trocar",
                {"id": id_prod}
            )

            print("Servidor:", res.get("mensagem"))

        elif opcao == "5" and perfil == "ADMIN":
            t_op = input(
                "Tipo [1-NOTIFICACAO | 2-ALERTA | 3-ATUALIZACAO]: "
            ).strip()

            tipo_aviso = (
                "ALERTA" if t_op == "2"
                else "ATUALIZACAO" if t_op == "3"
                else "NOTIFICACAO"
            )

            msg = input("Mensagem: ").strip()

            _, res = requisicao_http(
                "POST",
                "/api/notificacoes",
                {
                    "tipo": tipo_aviso,
                    "mensagem": msg,
                    "autor": f"{usuario} (Python)"
                }
            )

            print("Servidor:", res.get("mensagem"))

        elif opcao == "0":
            executando = False
            print("Encerrando cliente Python...")

        else:
            print("Opção inválida.")


if __name__ == "__main__":
    main()