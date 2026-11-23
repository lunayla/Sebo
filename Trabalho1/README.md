# Trabalho 1 — Comunicação entre processos: sistema de venda de livros (sebo)

**Disciplina:** QXD0043 — Sistemas Distribuídos  
**Universidade:** Universidade Federal do Ceará — Campus de Quixadá  
**Estudante:** Luna Ayla  
**Professor:** Antonio Rafael Braga

Este repositório reúne implementações em Java dos exercícios de comunicação entre processos do Trabalho 1, usando streams personalizados, sockets TCP, serialização de objetos e UDP multicast. O domínio utilizado é um sebo de livros e outros produtos.

## 1. Organização do projeto

```text
sebo-distribuido/
├── src/
│   ├── model/          # Entidades do sebo, subclasses e interface de troca
│   ├── service/        # Operações de estoque e negociação
│   ├── streams/        # Streams personalizados e testes local/arquivo/TCP
│   ├── serializacao/   # Mensagens Request/Reply serializadas e TCP
│   └── multicast/      # Autenticação TCP e notificações via UDP multicast
└── README.md
```

### Entidades e serviços

- `Produto`: classe-base abstrata para os produtos do catálogo.
- `Livro`, `Apostila`, `CD` e `Ebook`: tipos concretos de produto.
- `Trocavel`: interface que define `podeTrocar()` e `calcularValorTroca()`.
- `Loja`: mantém o acervo de produtos.
- `EstoqueService`: cadastra, busca e lista produtos.
- `NegociacaoService`: realiza vendas e avalia trocas.

`Livro`, `Apostila` e `CD` implementam `Trocavel`; `Ebook` não é trocável. A regra de troca depende do estado de conservação, e o crédito corresponde a 50% do preço quando o produto está elegível.

## 2. Streams personalizados (OutputStream e InputStream)

O exercício implementa:

- `ProdutoOutputStream`: stream de saída que recebe um `OutputStream` de destino, um array de produtos e a quantidade a transmitir. Escreve a quantidade e, para cada produto, seus atributos comuns e os campos específicos do subtipo.
- `ProdutoInputStream`: stream de entrada que lê os bytes e reconstruí os objetos `Livro`, `Apostila`, `CD` e `Ebook`, preservando os campos específicos.

A codificação dos campos é feita por `DataOutputStream`/`DataInputStream`, com tipos como `writeInt`, `writeUTF` e `writeDouble`. O stream não usa serialização Java automática; define explicitamente a sequência dos campos, que deve ser igual no envio e na leitura.

### Testes de streams

`TesteStreamsLocal` demonstra os testes locais e de arquivo:

- **System.out:** envia os bytes para a saída padrão (o teste captura os bytes para evitar poluir o terminal e informa o tamanho).
- **FileOutputStream/FileInputStream:** grava produtos em `produtos.dat`, lê o arquivo e reconstrói os objetos.
- **System.in:** usa os bytes capturados para testar a leitura pela entrada padrão.

### Streams por TCP

O teste TCP usa a porta **5001** e é demonstrado por um servidor e um cliente. O servidor aceita uma conexão, lê os produtos usando `ProdutoInputStream` e imprime os produtos recebidos.

Terminal 1 — servidor:
```bash
java -cp bin streams.ServidorStreamTCP
```

Terminal 2 — cliente:
```bash
java -cp bin streams.ClienteStreamTCP
```

O cliente transmite um `Livro` e um `Ebook` de exemplo. O servidor de streams atual aceita uma conexão por execução; reinicie-o para realizar outra demonstração.

## 3. Serialização e comunicação Request/Reply por TCP

O pacote `serializacao` implementa a troca de mensagens cliente-servidor:

- `MensagemRequest`: contém a operação e o identificador do produto.
- `MensagemReply`: contém o indicador de sucesso e o texto de resultado.
- As classes oferecem `empacotar()` e `desempacotar()` usando `ObjectOutputStream` e `ObjectInputStream`.
- O protocolo TCP utiliza um prefixo de tamanho (`int`) antes de cada mensagem serializada, para delimitar os bytes recebidos.

O servidor TCP na porta **5002** oferece as operações:

| Operação | Descrição |
|---|---|
| `LISTAR` | Retorna os produtos do acervo |
| `COMPRAR` | Realiza a venda de um produto pelo ID |
| `AVALIAR_TROCA` | Avalia a elegibilidade e o crédito de troca pelo ID |

Terminal 1 — servidor:
```bash
java -cp bin serializacao.ServidorSerializacaoTCP
```

Terminal 2 — cliente:
```bash
java -cp bin serializacao.ClienteSerializacaoTCP
```

O cliente executa exemplos de listagem, avaliação de troca de Ebook e Livro e compra do Livro. O servidor de serialização processa uma conexão por vez no código atual.

## 4. Autenticação TCP e notificações UDP Multicast

A implementação multicast usa duas etapas de comunicação:

1. **Autenticação via TCP (unicast):** cliente envia `usuario:senha` ao servidor na porta **5003**. A autenticação demonstrativa aceita usuário não vazio e senha `1234`. Em caso de sucesso, o servidor retorna `AUTH_OK` com o endereço do grupo e a porta multicast.
2. **Avisos via UDP multicast:** os clientes autenticados entram no grupo **230.0.0.1:4446** e recebem mensagens enviadas pelo servidor. O formato da mensagem é JSON, com `tipo`, `mensagem` e `timestamp`.

Tipos de avisos disponíveis:
- `NOTIFICACAO`
- `ALERTA`
- `ATUALIZACAO`

O servidor apresenta um menu no terminal para selecionar o tipo de aviso e digitar a mensagem. O envio é submetido a um executor de tarefas, permitindo que o servidor encaminhe os envios de forma concorrente. A autenticação TCP também usa tarefas do executor para atender clientes.

### Executar multicast

Terminal do servidor:
```bash
java -cp bin multicast.ServidorMulticastSebo
```

Terminal de cada cliente:
```bash
java -cp bin multicast.ClienteMulticastSebo
```

No cliente, informe usuário e senha `1234`. Depois da autenticação, o cliente entra no grupo UDP e mantém uma thread dedicada à recepção de mensagens, enquanto a thread principal permite interação no terminal. Digite `sair` para executar `leaveGroup()` e encerrar a participação no grupo.

O multicast depende do suporte da rede e de configuração de firewall. Em geral, os clientes devem estar em uma rede que permita tráfego multicast; o funcionamento entre redes diferentes não é garantido.

## 5. Compilação

Requisito: JDK instalado e `javac`/`java` disponíveis no terminal.

Na raiz do projeto:

### Linux/macOS
```bash
mkdir -p bin
javac -encoding UTF-8 -d bin $(find src -name "*.java")
```

### Windows PowerShell
```powershell
mkdir bin
javac -encoding UTF-8 -d bin (Get-ChildItem src -Recurse -Filter *.java | ForEach-Object FullName)
```

## 6. Portas e endereços usados

| Componente | Transporte | Porta/endereço |
|---|---|---|
| Streams TCP | TCP | `5001` |
| Serialização Request/Reply | TCP | `5002` |
| Autenticação multicast | TCP | `5003` |
| Grupo de notificações | UDP multicast | `230.0.0.1:4446` |

Nos clientes TCP, o host padrão é `localhost`. Para executar cliente e servidor em máquinas diferentes, configure a variável de ambiente `SEBO_SERVER_HOST` no cliente com o IP do servidor.

Exemplo Linux/macOS:
```bash
export SEBO_SERVER_HOST=192.168.1.10
```

Exemplo PowerShell:
```powershell
$env:SEBO_SERVER_HOST="192.168.1.10"
```

## 7. Limitações e segurança

- As credenciais são fixas e destinadas apenas à demonstração (`1234`).
- A autenticação multicast não cria uma sessão de segurança: o servidor verifica a senha e entrega os dados de grupo e porta, mas o tráfego multicast UDP não é criptografado.
- UDP multicast não garante entrega, ordenação ou retransmissão de mensagens perdidas.
- Os servidores de streams e serialização são exemplos didáticos; o código atual atende conexões sequencialmente e não implementa persistência.
- Os dados de estoque são mantidos em memória e reiniciam com o processo servidor.

## 8. Roteiro de demonstração

1. Compile o projeto.
2. Execute `streams.TesteStreamsLocal` para demonstrar `System.out`, arquivo e `System.in`.
3. Inicie `streams.ServidorStreamTCP` e, em outro terminal, `streams.ClienteStreamTCP`.
4. Inicie `serializacao.ServidorSerializacaoTCP` e execute `serializacao.ClienteSerializacaoTCP`.
5. Inicie `multicast.ServidorMulticastSebo`; abra dois ou mais clientes `multicast.ClienteMulticastSebo`, autentique-os e envie avisos pelo terminal servidor. Verifique que os clientes conectados recebem as mensagens simultaneamente.

## 9. Relação com os requisitos do Trabalho 1

- **Streams:** subclasses de `OutputStream` e `InputStream` transmitem e reconstroem produtos, com testes de saída padrão, arquivo e TCP.
- **Serialização:** cliente e servidor empacotam e desempacotam `MensagemRequest` e `MensagemReply` e trocam bytes via TCP.
- **Multicast:** autenticação unicast TCP, entrada e saída do grupo UDP multicast, avisos JSON e recepção por thread nos clientes.
- **Organização e demonstração:** classes de domínio, serviços, clientes e servidores separados em pacotes, com comandos de execução documentados.

