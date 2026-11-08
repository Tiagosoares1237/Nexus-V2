# NEXUS ✳

**Seu trabalho, em um só lugar.**  
*A quieter home for the messy parts of running a small business.*

> 🇧🇷 [Português](#-português) · 🇺🇸 [English](#-english)

---

## 🇧🇷 Português

### Nem todo projeto começa organizado. Mas pode continuar assim.

Um cliente manda o orçamento pelo WhatsApp. O prazo fica numa anotação. A ideia do que falta fazer está na cabeça. Quando você percebe, passou mais tempo procurando as informações do que trabalhando.

O **NEXUS** é um espaço pequeno e bem pensado para juntar essas peças. Foi feito para freelancers, pequenos estúdios e gente que toca o próprio negócio — sem precisar de uma assinatura, um cadastro ou uma configuração enorme.

O programa roda **localmente no computador**. Você abre o NEXUS e a interface aparece no navegador, mas não é um site hospedado na internet. Os dados ficam guardados no seu próprio Windows.

### Abrir é fácil

**No Windows, com Java instalado:**

1. Baixe o ZIP deste projeto e extraia a pasta inteira.
2. Dê dois cliques em **`01_ABRIR_NEXUS.bat`**.
3. A interface abre automaticamente no navegador.

Pronto. **Não precisa abrir VS Code, digitar comandos ou compilar código.** Se a aba não abrir automaticamente, use `http://127.0.0.1:8765/` enquanto o aplicativo estiver rodando.

> Java ainda é necessário para usar o atalho `.bat`. Se a pessoa não tiver Java, você pode gerar a versão portátil com o runtime incluído (abaixo).

**Para encerrar:** clique em **Encerrar NEXUS** na parte inferior do menu lateral. Fechar apenas a aba não encerra necessariamente o processo Java.

### Quer distribuir como `.exe`?

Se você tem **JDK 21+** no Windows (o JDK 25 funciona):

1. Dê dois cliques em **`02_GERAR_EXE.bat`**.
2. Abra **`dist/NEXUS/NEXUS.exe`**.
3. Para enviar a outra pessoa, compartilhe **`dist/NEXUS_Windows_Portatil.zip`**.

Essa é uma **versão portátil**, não um instalador tradicional. Quem recebe o ZIP não precisa instalar Java, porque o runtime vem dentro da pasta. O `NEXUS.exe` sozinho **não funciona** fora dessa pasta: mantenha juntos `app/` e `runtime/`.

> O EXE precisa ser gerado no Windows. O repositório já traz o JAR pronto para uso e o script que cria o executável; não inclui um EXE pré-compilado.

### O que dá para fazer

- **Início:** visão geral de projetos, valores a receber e prazos.
- **Clientes:** guardar contatos e identificar os trabalhos de cada pessoa.
- **Serviços:** criar um catálogo com categorias, preços e descrições.
- **Projetos:** registrar o que foi combinado, prazo, etapa, valor e pagamento.
- **Busca e filtros:** encontrar o que precisa sem rolar uma lista gigante.
- **CSV:** exportar seus projetos para uma planilha.
- **Explorar:** carregar dados *fictícios* opcionais para conhecer o sistema.

Ao abrir pela primeira vez, seu espaço começa **vazio**. Os exemplos só são carregados se você pedir. A intenção é que pareça seu ambiente de trabalho, não uma apresentação inventada.

### Onde ficam os dados?

No Windows, o arquivo fica em:

```text
%LOCALAPPDATA%\NEXUS\dados\nexus.json
```

Ele é criado automaticamente. Para fazer backup, **feche o NEXUS** e copie esse arquivo para outro lugar. A pasta do aplicativo pode ser atualizada sem apagar seus dados. Se você usava a versão portátil anterior do NEXUS, ela utilizava o mesmo local de armazenamento.

**Importante:** esta versão não tem login, criptografia, sincronização, acesso remoto nem backups automáticos. Não é um CRM empresarial pronto para dados sensíveis. Ela também não integra pagamentos bancários: o valor "a receber" é calculado a partir dos projetos que você cadastrou.

### Como o projeto foi construído

O núcleo usa Java (`HttpServer` da biblioteca padrão). A interface é HTML, CSS e JavaScript puro. Sem frameworks, sem fontes externas, sem trackers. O backend valida os dados recebidos e salva o histórico em JSON com substituição segura de arquivo.

Se quiser aprender, o caminho mais simples é começar por:

- `web/index.html` — a estrutura da interface.
- `web/styles.css` — as cores, a tipografia e o layout.
- `web/app.js` — os botões, páginas e formulários.
- `ABRIR_NEXUS.java` — a API local e a persistência.
- `TESTAR_NEXUS.java` — testes automatizados.

Depois de alterar o código, dê dois cliques em `03_RECOMPILAR_CODIGO.bat` para rodar os testes e atualizar `app/NEXUS.jar`.

O projeto foi desenvolvido com assistência de IA na implementação e na revisão, como parte de um portfólio de estudo. O objetivo é que o código seja legível, testável e possa ser melhorado com o tempo.

---

## 🇺🇸 English

### Work gets messy. Your workspace doesn't have to.

A quote lands in a chat. The deadline lives on a sticky note. The next step is somewhere in your head. Before long, you're spending more time finding information than doing the work itself.

**NEXUS** is a quiet, local-first workspace for freelancers, small studios and service businesses. It brings your clients, services, jobs and deadlines together without a subscription, an account or a complicated setup.

### Run it on Windows

With Java installed, extract the project ZIP and **double-click `01_ABRIR_NEXUS.bat`**. The interface opens in your browser. No IDE, build step or command line is required. Close the app using **Encerrar NEXUS** in the sidebar.

To create a **self-contained Windows portable application**, use **`02_GERAR_EXE.bat`** on Windows with JDK 21+. It produces `dist/NEXUS/NEXUS.exe` and a ZIP to share. The recipient does **not** need Java, but must keep the full application folder (including `app` and `runtime`). This is a portable app, not an MSI installer. The Windows executable is built on the user's Windows machine, not provided precompiled here.

### What you can do

Manage contacts, services, project stages, deadlines, agreed prices and payment flags. Search your records, export projects as CSV, and keep everything on your own computer. First launch starts with an empty workspace; optional sample data is clearly labeled and only loaded when requested.

### Local by design

NEXUS runs a Java HTTP server **only on `127.0.0.1`**. It opens a browser-based user interface, but is not deployed as a public website. On Windows, data lives in `%LOCALAPPDATA%\NEXUS\dados\nexus.json`. Back up that file manually. There is no account, cloud sync, encryption, automatic backup or banking integration. Do not expose the server to the internet or treat this prototype as a production CRM for sensitive information.

### For developers

The project uses the Java standard library, HTML, CSS and vanilla JavaScript. No dependencies, hosted assets or tracking scripts. Edit the source files, then run `03_RECOMPILAR_CODIGO.bat` to run the checks and rebuild the bundled JAR. Automated tests live in `TESTAR_NEXUS.java`.

Developed with AI assistance for implementation and review as a learning portfolio project, with readable code intended for continued study and improvements.

---

**NEXUS** · Seu trabalho, no lugar certo. ✳
