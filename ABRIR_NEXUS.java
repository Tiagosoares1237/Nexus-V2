import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * NEXUS — a small, local-first work management app.
 *
 * Requires JDK 21+. Uses only the Java standard library.
 * Start by running this file in VS Code or with: java ABRIR_NEXUS.java
 * The web UI is served at http://127.0.0.1:8765/.
 *
 * This is a local desktop-style application, not a public internet service.
 */
public class ABRIR_NEXUS {
    private static final int PORT = 8765;
    private static final String HOST = "127.0.0.1";
    private static final long MAX_BODY_BYTES = 64 * 1024L;

    public static void main(String[] args) {
        // Reopening the launcher should bring back the existing browser window,
        // not start a second writer for the same local JSON file.
        if (reopenExisting()) return;
        try {
            URL webResource = ABRIR_NEXUS.class.getResource("/web/index.html");
            boolean packedApp = webResource != null && "jar".equals(webResource.getProtocol());
            Path projectRoot = packedApp ? null : findRoot();
            Path dataFile = userDataFile();
            Store store = new Store(dataFile);
            HttpServer server = HttpServer.create(new InetSocketAddress(HOST, PORT), 0);
            server.setExecutor(Executors.newFixedThreadPool(8));
            server.createContext("/", exchange -> {
                try {
                    exchange.getResponseHeaders().set("X-NEXUS-App", "NEXUS-Workroom");
                    route(exchange, projectRoot, store);
                } catch (ApiError e) {
                    json(exchange, e.status, Map.of("error", e.getMessage()));
                } catch (Exception e) {
                    System.err.println("Erro interno: " + e.getMessage());
                    json(exchange, 500, Map.of("error", "Não foi possível concluir a operação."));
                } finally {
                    exchange.close();
                }
            });
            server.start();
            String url = "http://" + HOST + ":" + PORT + "/";
            System.out.println("\n  ┌────────────────────────────────────────────┐");
            System.out.println("  │  NEXUS  /  Seu trabalho, no lugar certo. │");
            System.out.println("  └────────────────────────────────────────────┘");
            System.out.println("\n  Aberto em: " + url);
            System.out.println("  Dados em:  " + dataFile);
            System.out.println("\n  Para encerrar o aplicativo, use o botão Encerrar dentro do NEXUS.");
            System.out.println("  Se estiver no terminal, use Ctrl + C.\n");
            openBrowser(url);
            installTray(url, server);
        } catch (IOException e) {
            System.err.println("Não foi possível iniciar o NEXUS: " + e.getMessage());
            System.err.println("Se a porta 8765 estiver ocupada, feche outra execução do NEXUS e tente novamente.");
            try {
                JOptionPane.showMessageDialog(null,
                    "O NEXUS não conseguiu iniciar.\n" + e.getMessage()
                    + "\n\nSe outra execução estiver aberta, feche pelo ícone perto do relógio.",
                    "NEXUS - Erro ao iniciar", JOptionPane.ERROR_MESSAGE);
            } catch (Throwable ignored) {
                // Console fallback in a headless environment.
            }
        }
    }

    private static boolean reopenExisting() {
        HttpURLConnection conn = null;
        try {
            URL endpoint = URI.create("http://" + HOST + ":" + PORT + "/api/state").toURL();
            conn = (HttpURLConnection) endpoint.openConnection();
            conn.setConnectTimeout(450);
            conn.setReadTimeout(450);
            conn.setRequestMethod("GET");
            if ("NEXUS-Workroom".equals(conn.getHeaderField("X-NEXUS-App"))) {
                openBrowser("http://" + HOST + ":" + PORT + "/");
                return true;
            }
        } catch (IOException ignored) {
            // Normal when this is the first NEXUS instance.
        } finally {
            if (conn != null) conn.disconnect();
        }
        return false;
    }

    /** User-owned data must not be written inside Program Files (read-only for most users). */
    private static Path userDataFile() {
        String base = System.getenv("LOCALAPPDATA");
        if (base != null && !base.isBlank()) {
            return Path.of(base, "NEXUS", "dados", "nexus.json");
        }
        return Path.of(System.getProperty("user.home"), ".nexus", "dados", "nexus.json");
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            } else {
                System.out.println("  Abra no navegador: " + url);
            }
        } catch (Throwable ignored) {
            // Headless CI systems can throw AWTError before reporting headless status.
            System.out.println("  Abra no navegador: " + url);
        }
    }

    /** A Windows tray menu keeps the local server controllable after the browser is closed. */
    private static void installTray(String url, HttpServer server) {
        try {
            if (!SystemTray.isSupported()) return;
            BufferedImage image;
            try (InputStream in = ABRIR_NEXUS.class.getResourceAsStream("/app/nexus-tray.png")) {
                if (in == null) return;
                image = ImageIO.read(in);
            }
            PopupMenu menu = new PopupMenu();
            MenuItem open = new MenuItem("Abrir NEXUS");
            open.addActionListener(e -> openBrowser(url));
            menu.add(open);
            MenuItem exit = new MenuItem("Sair do NEXUS");
            exit.addActionListener(e -> {
                server.stop(0);
                System.exit(0);
            });
            menu.add(exit);
            TrayIcon icon = new TrayIcon(image, "NEXUS - Workroom", menu);
            icon.setImageAutoSize(true);
            icon.addActionListener(e -> openBrowser(url));
            SystemTray.getSystemTray().add(icon);
        } catch (Throwable ignored) {
            // Tray availability varies by desktop environment. The API remains usable.
        }
    }

    private static Path findRoot() throws IOException {
        Path here = Path.of("").toAbsolutePath().normalize();
        for (Path path : List.of(here, here.resolve("NEXUS"), here.getParent() == null ? here : here.getParent())) {
            if (Files.isRegularFile(path.resolve("web/index.html"))) return path;
        }
        throw new IOException("Pasta 'web' não encontrada. Abra a pasta NEXUS inteira no VS Code antes de executar.");
    }

    private static void route(HttpExchange ex, Path root, Store store) throws IOException {
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        if (path.equals("/api/state")) {
            requireMethod(method, "GET");
            json(ex, 200, store.snapshot());
            return;
        }
        if (path.equals("/api/example")) {
            requireMethod(method, "POST");
            store.loadExample();
            json(ex, 200, store.snapshot());
            return;
        }
        if (path.equals("/api/exit")) {
            requireMethod(method, "POST");
            String origin = ex.getRequestHeaders().getFirst("Origin");
            if (origin != null && !origin.equals("http://" + HOST + ":" + PORT))
                throw new ApiError(403, "Origem não permitida.");
            json(ex, 200, Map.of("ok", true));
            new Thread(() -> {
                try { Thread.sleep(220); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                System.exit(0);
            }, "nexus-exit").start();
            return;
        }
        if (path.startsWith("/api/")) {
            String[] pieces = path.split("/");
            if (pieces.length < 3 || pieces.length > 4) throw new ApiError(404, "Rota não encontrada.");
            String type = pieces[2];
            if (!List.of("clientes", "servicos", "pedidos").contains(type)) throw new ApiError(404, "Rota não encontrada.");
            String id = pieces.length == 4 ? pieces[3] : null;
            if (id == null && method.equals("POST")) {
                json(ex, 201, store.create(type, readJsonBody(ex)));
            } else if (id != null && method.equals("PUT")) {
                json(ex, 200, store.update(type, id, readJsonBody(ex)));
            } else if (id != null && method.equals("DELETE")) {
                store.delete(type, id);
                json(ex, 200, Map.of("ok", true));
            } else {
                throw new ApiError(405, "Método não permitido para esta rota.");
            }
            return;
        }
        requireMethod(method, "GET");
        String file = switch (path) {
            case "/", "/index.html" -> "index.html";
            case "/styles.css" -> "styles.css";
            case "/app.js" -> "app.js";
            case "/icon.svg" -> "icon.svg";
            default -> null;
        };
        if (file == null) throw new ApiError(404, "Página não encontrada.");
        byte[] body;
        if (root == null) {
            try (InputStream in = ABRIR_NEXUS.class.getResourceAsStream("/web/" + file)) {
                if (in == null) throw new ApiError(404, "Recurso não encontrado.");
                body = in.readAllBytes();
            }
        } else {
            Path target = root.resolve("web").resolve(file);
            body = Files.readAllBytes(target);
        }
        ex.getResponseHeaders().set("Content-Type", switch (file) {
            case "styles.css" -> "text/css; charset=utf-8";
            case "app.js" -> "text/javascript; charset=utf-8";
            case "icon.svg" -> "image/svg+xml; charset=utf-8";
            default -> "text/html; charset=utf-8";
        });
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream stream = ex.getResponseBody()) { stream.write(body); }
    }

    private static void requireMethod(String actual, String expected) {
        if (!actual.equals(expected)) throw new ApiError(405, "Use o método " + expected + ".");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readJsonBody(HttpExchange ex) throws IOException {
        String type = ex.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.toLowerCase().startsWith("application/json"))
            throw new ApiError(415, "Envie um JSON válido.");
        byte[] bytes;
        try (InputStream in = ex.getRequestBody()) {
            bytes = in.readNBytes((int) MAX_BODY_BYTES + 1);
        }
        if (bytes.length > MAX_BODY_BYTES) throw new ApiError(413, "Dados muito grandes.");
        Object value;
        try { value = Json.parse(new String(bytes, StandardCharsets.UTF_8)); }
        catch (RuntimeException e) { throw new ApiError(400, "JSON inválido."); }
        if (!(value instanceof Map<?, ?>)) throw new ApiError(400, "Envie um objeto JSON.");
        return (Map<String, Object>) value;
    }

    private static void json(HttpExchange ex, int status, Object data) throws IOException {
        byte[] bytes = Json.stringify(data).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream stream = ex.getResponseBody()) { stream.write(bytes); }
    }

    private static class ApiError extends RuntimeException {
        final int status;
        ApiError(int status, String message) { super(message); this.status = status; }
    }

    /** Persisted JSON store, serialized access and safe replacement on disk. */
    static class Store {
        final Path file;
        final List<Map<String, Object>> clients = new ArrayList<>();
        final List<Map<String, Object>> services = new ArrayList<>();
        final List<Map<String, Object>> orders = new ArrayList<>();
        boolean demo;

        Store(Path file) throws IOException {
            this.file = file;
            Files.createDirectories(file.getParent());
            if (Files.exists(file)) {
                load();
            } else {
                // Comece com a sua própria lista, não com clientes inventados.
                save();
            }
        }

        @SuppressWarnings("unchecked")
        private void load() throws IOException {
            try {
                Object obj = Json.parse(Files.readString(file, StandardCharsets.UTF_8));
                if (!(obj instanceof Map<?, ?> state)) throw new IllegalArgumentException("bad state");
                clients.addAll((List<Map<String, Object>>) state.get("clientes"));
                services.addAll((List<Map<String, Object>>) state.get("servicos"));
                orders.addAll((List<Map<String, Object>>) state.get("pedidos"));
                demo = Boolean.TRUE.equals(state.get("demonstracao"));
            } catch (Exception e) {
                throw new IOException("Não foi possível ler 'dados/nexus.json'. Faça backup do arquivo e verifique seu conteúdo antes de tentar novamente.", e);
            }
        }

        synchronized void loadExample() throws IOException {
            if (!clients.isEmpty() || !services.isEmpty() || !orders.isEmpty())
                throw new ApiError(409, "O exemplo só pode ser carregado em um espaço vazio.");
            seed();
            save();
        }

        private void seed() {
            demo = true;
            clients.add(make("c1", "nome", "Camila Rocha", "empresa", "Estúdio Aurora", "email", "camila@exemplo.com", "telefone", "(31) 99999-0001"));
            clients.add(make("c2", "nome", "Renato Alves", "empresa", "Café do Pátio", "email", "renato@exemplo.com", "telefone", "(31) 99999-0002"));
            clients.add(make("c3", "nome", "Bianca Moura", "empresa", "Oficina Norte", "email", "bianca@exemplo.com", "telefone", "(31) 99999-0003"));
            clients.add(make("c4", "nome", "Felipe Nunes", "empresa", "Casa Sabiá", "email", "felipe@exemplo.com", "telefone", "(31) 99999-0004"));
            services.add(make("s1", "nome", "Identidade visual", "categoria", "Design", "preco", 1800, "descricao", "Marca, paleta e materiais essenciais."));
            services.add(make("s2", "nome", "Landing page", "categoria", "Web", "preco", 2400, "descricao", "Página de apresentação para negócios."));
            services.add(make("s3", "nome", "Ajustes de site", "categoria", "Suporte", "preco", 450, "descricao", "Pequenas correções e melhorias."));
            services.add(make("s4", "nome", "Automação de atendimento", "categoria", "Automação", "preco", 950, "descricao", "Organização de rotinas de contato."));
            LocalDate t = LocalDate.now();
            orders.add(demoOrder("p1", "Landing page do Café", "c2", "s2", "andamento", 2400, t.plusDays(6)));
            orders.add(demoOrder("p2", "Marca da Casa Sabiá", "c4", "s1", "andamento", 1800, t.plusDays(12)));
            orders.add(demoOrder("p3", "Automação de mensagens", "c1", "s4", "novo", 950, t.plusDays(15)));
            orders.add(demoOrder("p4", "Correções no portfólio", "c1", "s3", "novo", 450, t.plusDays(3)));
            orders.add(demoOrder("p5", "Ajustes de responsividade", "c3", "s3", "concluido", 450, t.minusDays(3)));
            orders.add(demoOrder("p6", "Nova página de contato", "c4", "s2", "concluido", 1200, t.minusDays(8)));
        }

        private static Map<String, Object> demoOrder(String id, String title, String client, String service, String status, int price, LocalDate due) {
            Map<String, Object> row = make(id, "titulo", title, "clienteId", client, "servicoId", service, "status", status, "valor", price, "prazo", due.toString(), "observacoes", "", "pago", status.equals("concluido"), "criadoEm", Instant.now().toString());
            return row;
        }

        synchronized Map<String, Object> snapshot() {
            // Return copies so API serialization never races against a write.
            return make(null,
                "clientes", copyRows(clients),
                "servicos", copyRows(services),
                "pedidos", copyRows(orders),
                "demonstracao", demo);
        }

        private static List<Map<String, Object>> copyRows(List<Map<String, Object>> source) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> item : source) result.add(new LinkedHashMap<>(item));
            return result;
        }

        private List<Map<String, Object>> list(String type) {
            return switch (type) {
                case "clientes" -> clients;
                case "servicos" -> services;
                case "pedidos" -> orders;
                default -> throw new ApiError(404, "Tipo de recurso desconhecido.");
            };
        }

        synchronized Map<String, Object> create(String type, Map<String, Object> input) throws IOException {
            Map<String, Object> clean = validate(type, input, null);
            clean.put("id", UUID.randomUUID().toString().substring(0, 8));
            if (type.equals("pedidos")) clean.put("criadoEm", Instant.now().toString());
            list(type).add(clean);
            save();
            return clean;
        }

        synchronized Map<String, Object> update(String type, String id, Map<String, Object> input) throws IOException {
            List<Map<String, Object>> target = list(type);
            for (int i = 0; i < target.size(); i++) {
                if (Objects.equals(target.get(i).get("id"), id)) {
                    Map<String, Object> clean = validate(type, input, target.get(i));
                    clean.put("id", id);
                    if (type.equals("pedidos")) clean.put("criadoEm", target.get(i).get("criadoEm"));
                    target.set(i, clean);
                    save();
                    return clean;
                }
            }
            throw new ApiError(404, "Registro não encontrado.");
        }

        synchronized void delete(String type, String id) throws IOException {
            if (type.equals("clientes") && orders.stream().anyMatch(o -> id.equals(o.get("clienteId"))))
                throw new ApiError(409, "Esse cliente tem pedidos. Exclua ou altere os pedidos antes.");
            if (type.equals("servicos") && orders.stream().anyMatch(o -> id.equals(o.get("servicoId"))))
                throw new ApiError(409, "Esse serviço está em uso em pedidos. Exclua ou altere os pedidos antes.");
            boolean removed = list(type).removeIf(row -> id.equals(row.get("id")));
            if (!removed) throw new ApiError(404, "Registro não encontrado.");
            save();
        }

        private Map<String, Object> validate(String type, Map<String, Object> v, Map<String, Object> original) {
            return switch (type) {
                case "clientes" -> make(null,
                    "nome", str(v, "nome", true, 90),
                    "empresa", str(v, "empresa", false, 100),
                    "email", str(v, "email", false, 120),
                    "telefone", str(v, "telefone", false, 35));
                case "servicos" -> make(null,
                    "nome", str(v, "nome", true, 100),
                    "categoria", str(v, "categoria", false, 45),
                    "preco", money(v, "preco"),
                    "descricao", str(v, "descricao", false, 280));
                case "pedidos" -> {
                    String client = str(v, "clienteId", true, 70);
                    String service = str(v, "servicoId", true, 70);
                    if (clients.stream().noneMatch(c -> c.get("id").equals(client))) throw new ApiError(400, "Escolha um cliente válido.");
                    if (services.stream().noneMatch(s -> s.get("id").equals(service))) throw new ApiError(400, "Escolha um serviço válido.");
                    String status = str(v, "status", true, 20);
                    if (!List.of("novo", "andamento", "concluido").contains(status)) throw new ApiError(400, "Status inválido.");
                    String due = str(v, "prazo", false, 10);
                    if (!due.isEmpty()) {
                        try { LocalDate.parse(due); } catch (Exception e) { throw new ApiError(400, "Data inválida (use AAAA-MM-DD)."); }
                    }
                    Object paid = v.get("pago");
                    if (!(paid instanceof Boolean)) throw new ApiError(400, "Informe se o pedido está pago.");
                    yield make(null,
                        "titulo", str(v, "titulo", true, 110),
                        "clienteId", client,
                        "servicoId", service,
                        "status", status,
                        "valor", money(v, "valor"),
                        "prazo", due,
                        "observacoes", str(v, "observacoes", false, 500),
                        "pago", paid);
                }
                default -> throw new ApiError(404, "Tipo de recurso desconhecido.");
            };
        }

        private static String str(Map<String, Object> v, String key, boolean required, int limit) {
            Object raw = v.get(key);
            if (!(raw instanceof String)) throw new ApiError(400, "Campo inválido: " + key);
            String value = ((String) raw).trim();
            if (required && value.isBlank()) throw new ApiError(400, "Preencha o campo: " + key);
            if (value.length() > limit) throw new ApiError(400, "Texto longo demais no campo: " + key);
            return value;
        }

        private static double money(Map<String, Object> v, String key) {
            Object raw = v.get(key);
            if (!(raw instanceof Number)) throw new ApiError(400, "Valor inválido: " + key);
            double number = ((Number) raw).doubleValue();
            if (!Double.isFinite(number) || number < 0 || number > 100000000)
                throw new ApiError(400, "Informe um valor entre 0 e 100 milhões.");
            return Math.round(number * 100.0) / 100.0;
        }

        private synchronized void save() throws IOException {
            Path tmp = Files.createTempFile(file.getParent(), "nexus-", ".tmp");
            try {
                Files.writeString(tmp, Json.stringify(snapshot()), StandardCharsets.UTF_8);
                try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(tmp); }
        }
    }

    private static Map<String, Object> make(String id, Object... pairs) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (id != null) m.put("id", id);
        for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
        return m;
    }

    /** Minimal JSON codec: maps, lists, strings, booleans, null and finite numbers. */
    static class Json {
        static Object parse(String text) {
            Parser p = new Parser(text);
            Object value = p.value();
            p.spaces();
            if (p.i != text.length()) throw new IllegalArgumentException("trailing data");
            return value;
        }
        static String stringify(Object value) {
            if (value == null) return "null";
            if (value instanceof String str) return quote(str);
            if (value instanceof Boolean) return value.toString();
            if (value instanceof Number num) {
                if (!Double.isFinite(num.doubleValue())) throw new IllegalArgumentException("nonfinite number");
                return num.toString();
            }
            if (value instanceof Map<?, ?> map) {
                List<String> parts = new ArrayList<>();
                for (var entry : map.entrySet()) parts.add(quote(entry.getKey().toString()) + ":" + stringify(entry.getValue()));
                return "{" + String.join(",", parts) + "}";
            }
            if (value instanceof List<?> list) {
                List<String> parts = new ArrayList<>();
                for (Object item : list) parts.add(stringify(item));
                return "[" + String.join(",", parts) + "]";
            }
            throw new IllegalArgumentException("unsupported type");
        }
        private static String quote(String s) {
            StringBuilder out = new StringBuilder("\"");
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                        else out.append(c);
                    }
                }
            }
            return out.append('"').toString();
        }
        private static class Parser {
            final String s; int i;
            Parser(String s) { this.s = s; }
            void spaces() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
            boolean eat(char ch) { spaces(); if (i < s.length() && s.charAt(i) == ch) { i++; return true; } return false; }
            char next() { if (i >= s.length()) throw new IllegalArgumentException("unexpected end"); return s.charAt(i++); }
            Object value() {
                spaces(); if (i >= s.length()) throw new IllegalArgumentException("empty");
                char c = s.charAt(i);
                if (c == '"') return string();
                if (c == '{') {
                    i++; Map<String, Object> m = new LinkedHashMap<>();
                    if (eat('}')) return m;
                    do { spaces(); if (i >= s.length() || s.charAt(i) != '"') throw new IllegalArgumentException("expected key");
                        String key = string(); if (!eat(':')) throw new IllegalArgumentException("expected colon");
                        m.put(key, value()); if (eat('}')) return m;
                    } while (eat(','));
                    throw new IllegalArgumentException("expected comma");
                }
                if (c == '[') {
                    i++; List<Object> a = new ArrayList<>(); if (eat(']')) return a;
                    do { a.add(value()); if (eat(']')) return a; } while (eat(','));
                    throw new IllegalArgumentException("expected comma");
                }
                for (String literal : List.of("true", "false", "null")) {
                    if (s.startsWith(literal, i)) { i += literal.length(); return switch (literal) {
                        case "true" -> true; case "false" -> false; default -> null;
                    }; }
                }
                int start = i;
                if (s.charAt(i) == '-') i++;
                if (i >= s.length()) throw new IllegalArgumentException("number expected");
                if (s.charAt(i) == '0') i++;
                else {
                    if (!Character.isDigit(s.charAt(i))) throw new IllegalArgumentException("number expected");
                    while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') i++;
                }
                if (i < s.length() && s.charAt(i) == '.') {
                    i++; int begin = i;
                    while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') i++;
                    if (i == begin) throw new IllegalArgumentException("bad decimal");
                }
                if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                    i++; if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+')) i++;
                    int begin = i;
                    while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') i++;
                    if (i == begin) throw new IllegalArgumentException("bad exponent");
                }
                double d = Double.parseDouble(s.substring(start, i));
                if (!Double.isFinite(d)) throw new IllegalArgumentException("number too large");
                return d;
            }
            String string() {
                if (next() != '"') throw new IllegalArgumentException("expected quote");
                StringBuilder out = new StringBuilder();
                while (true) {
                    char c = next();
                    if (c == '"') return out.toString();
                    if (c < 0x20) throw new IllegalArgumentException("control char");
                    if (c != '\\') { out.append(c); continue; }
                    char escaped = next();
                    switch (escaped) {
                        case '"' -> out.append('"'); case '\\' -> out.append('\\'); case '/' -> out.append('/');
                        case 'b' -> out.append('\b'); case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n'); case 'r' -> out.append('\r'); case 't' -> out.append('\t');
                        case 'u' -> {
                            if (i + 4 > s.length()) throw new IllegalArgumentException("bad unicode");
                            out.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4;
                        }
                        default -> throw new IllegalArgumentException("bad escape");
                    }
                }
            }
        }
    }
}
