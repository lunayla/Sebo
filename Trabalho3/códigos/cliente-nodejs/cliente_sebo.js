const http = require("http");
const readline = require("readline");

// Para executar em computadores diferentes, defina SEBO_API_URL com o IP do servidor.
// Exemplo: SEBO_API_URL=http://192.168.1.10:8080
const apiUrl = new URL(
  process.env.SEBO_API_URL || "http://localhost:8080"
);

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
});

const perguntar = (texto) =>
  new Promise((resolve) =>
    rl.question(texto, (resposta) => resolve(resposta.trim()))
  );

let ultimoIdNotificacao = 0;
let ativo = true;
let tokenSessao = null;

function requisicaoHttp(metodo, caminho, payload = null) {
  return new Promise((resolve) => {
    const bodyStr = payload ? JSON.stringify(payload) : null;

    const options = {
      hostname: apiUrl.hostname,
      port: Number(apiUrl.port || 80),
      path: caminho,
      method: metodo,
      headers: {
        "Content-Type": "application/json; charset=UTF-8",
        ...(tokenSessao
          ? { Authorization: `Bearer ${tokenSessao}` }
          : {}),
        ...(bodyStr
          ? { "Content-Length": Buffer.byteLength(bodyStr) }
          : {}),
      },
    };

    const req = http.request(options, (res) => {
      let data = "";

      res.on("data", (chunk) => (data += chunk));

      res.on("end", () => {
        try {
          resolve({
            status: res.statusCode,
            dados: JSON.parse(data),
          });
        } catch {
          resolve({
            status: res.statusCode,
            dados: {
              sucesso: false,
              mensagem: data,
            },
          });
        }
      });
    });

    req.on("error", (err) => {
      resolve({
        status: 500,
        dados: {
          sucesso: false,
          mensagem: `Erro de conexão: ${err.message}`,
        },
      });
    });

    if (bodyStr) {
      req.write(bodyStr);
    }

    req.end();
  });
}

function iniciarMonitorMural() {
  setInterval(async () => {
    if (!ativo) return;

    const { status, dados } = await requisicaoHttp(
      "GET",
      `/api/notificacoes?desde=${ultimoIdNotificacao}`
    );

    if (
      status === 200 &&
      dados.sucesso &&
      Array.isArray(dados.notificacoes)
    ) {
      for (const n of dados.notificacoes) {
        ultimoIdNotificacao = Math.max(
          ultimoIdNotificacao,
          n.id
        );

        console.log(
          `\n\n>>> [NOVA NOTIFICACAO - ${n.tipo}] ` +
          `(${n.autor}): ${n.mensagem}`
        );

        process.stdout.write("Escolha uma opção: ");
      }
    }
  }, 2000);
}

async function main() {
  console.log("=== CLIENTE NODE.JS (JAVASCRIPT) - SEBO ===");

  const usuario = await perguntar("Usuário (cliente | admin): ");
  const senha = await perguntar("Senha (use 1234): ");

  const auth = await requisicaoHttp(
    "POST",
    "/api/auth/login",
    { usuario, senha }
  );

  if (auth.status !== 200 || !auth.dados.sucesso) {
    console.log("Falha no login:", auth.dados.mensagem);
    rl.close();
    return;
  }

  tokenSessao = auth.dados.token;

  const perfil = auth.dados.perfil;

  console.log(`Conectado como '${usuario}'!`);

  iniciarMonitorMural();

  while (ativo) {
    console.log("\n--- MENU DO SEBO (Cliente Node.js) ---");

    console.log("1. Listar acervo");

    if (perfil === "ADMIN") {
      console.log("2. Cadastrar produto");
      console.log("3. Comprar produto");
      console.log("4. Avaliar troca");
      console.log("5. Publicar aviso no Mural");
    } else {
      console.log("2. Comprar produto");
      console.log("3. Avaliar troca");
    }

    console.log("0. Sair");

    const opcao = await perguntar("Escolha uma opção: ");

    if (opcao === "1") {
      const { dados } = await requisicaoHttp(
        "GET",
        "/api/produtos"
      );

      console.log(`\nAcervo de ${dados.loja}:`);

      for (const p of dados.produtos || []) {
        console.log(
          `  ID ${p.id} | [${p.tipo}] ${p.titulo} ` +
          `- R$ ${Number(p.preco).toFixed(2)} ` +
          `(${p.estado}) | Trocável: ${p.trocavel ? "Sim" : "Não"}`
        );
      }

    } else if (opcao === "2" && perfil === "ADMIN") {
      const tipo = (
        await perguntar("Tipo (LIVRO, APOSTILA, CD, EBOOK): ")
      ).toUpperCase();

      const titulo = await perguntar("Título: ");

      const preco = parseFloat(
        (await perguntar("Preço (ex: 35.00): ")).replace(",", ".")
      );

      const estado = await perguntar(
        "Estado (Novo, Seminovo, Usado): "
      );

      const extra = await perguntar(
        "Info extra (Autor / Disciplina / Artista / Formato): "
      );

      const { dados } = await requisicaoHttp(
        "POST",
        "/api/produtos",
        { tipo, titulo, preco, estado, extra }
      );

      console.log("Servidor:", dados.mensagem);

    } else if (opcao === (perfil === "ADMIN" ? "3" : "2")) {
      const id = parseInt(
        await perguntar("ID do produto para comprar: "),
        10
      );

      const { dados } = await requisicaoHttp(
        "POST",
        "/api/negociacoes/comprar",
        { id }
      );

      console.log("Servidor:", dados.mensagem);

    } else if (opcao === (perfil === "ADMIN" ? "4" : "3")) {
      const id = parseInt(
        await perguntar("ID do produto para avaliar troca: "),
        10
      );

      const { dados } = await requisicaoHttp(
        "POST",
        "/api/negociacoes/trocar",
        { id }
      );

      console.log("Servidor:", dados.mensagem);

    } else if (opcao === "5" && perfil === "ADMIN") {
      const tOp = await perguntar(
        "Tipo [1-NOTIFICACAO | 2-ALERTA | 3-ATUALIZACAO]: "
      );

      const tipoAviso =
        tOp === "2"
          ? "ALERTA"
          : tOp === "3"
            ? "ATUALIZACAO"
            : "NOTIFICACAO";

      const mensagem = await perguntar("Mensagem: ");

      const { dados } = await requisicaoHttp(
        "POST",
        "/api/notificacoes",
        {
          tipo: tipoAviso,
          mensagem,
          autor: `${usuario} (Node.js)`,
        }
      );

      console.log("Servidor:", dados.mensagem);

    } else if (opcao === "0") {
      ativo = false;

      console.log("Encerrando cliente Node.js...");

      rl.close();
      process.exit(0);

    } else {
      console.log("Opção inválida.");
    }
  }
}

main();