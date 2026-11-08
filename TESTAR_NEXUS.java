import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Offline checks for the JSON codec and local persistence.
 * Run with: javac ABRIR_NEXUS.java TESTAR_NEXUS.java && java TESTAR_NEXUS
 */
public class TESTAR_NEXUS {
    static int passed = 0;
    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError("FALHOU: " + name);
        passed++;
        System.out.println("OK  " + name);
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("nexus-test-");
        Path file = dir.resolve("dados.json");
        try {
            check(ABRIR_NEXUS.Json.parse("{\"a\":[1,true,null,\"x\\n\"]}") instanceof Map, "Ler JSON");
            String escaped = "Aspas \" e ç/é";
            check(escaped.equals(ABRIR_NEXUS.Json.parse(ABRIR_NEXUS.Json.stringify(escaped))), "Texto com acentos e aspas");
            ABRIR_NEXUS.Store store = new ABRIR_NEXUS.Store(file);
            check(((java.util.List<?>)store.snapshot().get("clientes")).size() == 0, "Começar sem registros fictícios");
            Map<String,Object> client = store.create("clientes", Map.of("nome","Cliente Teste", "empresa","Minha Empresa", "email","teste@exemplo.com", "telefone",""));
            check("Cliente Teste".equals(client.get("nome")), "Criar cliente");
            Map<String,Object> updated = store.update("clientes",client.get("id").toString(),Map.of("nome","Cliente Alterado", "empresa","Minha Empresa", "email","teste@exemplo.com", "telefone",""));
            check("Cliente Alterado".equals(updated.get("nome")), "Editar cliente");
            Map<String,Object> service = store.create("servicos",Map.of("nome","Consultoria", "categoria","Serviço", "preco",350.55,"descricao","Diagnóstico"));
            check(((Number)service.get("preco")).doubleValue()==350.55, "Criar serviço e salvar preço");
            Map<String,Object> order = new LinkedHashMap<>();
            order.put("titulo","Projeto de teste");order.put("clienteId",client.get("id"));order.put("servicoId",service.get("id"));
            order.put("status","novo");order.put("valor",350.55);order.put("prazo","2026-12-31");order.put("observacoes","");order.put("pago",false);
            Map<String,Object> made = store.create("pedidos",order);
            check(made.get("id") != null, "Criar pedido com referências válidas");
            check(Files.size(file)>100, "Dados salvos em disco");
            ABRIR_NEXUS.Store reopened = new ABRIR_NEXUS.Store(file);
            check(((java.util.List<?>)reopened.snapshot().get("pedidos")).size()==1, "Persistência após reabrir");
            boolean conflict=false;
            try { reopened.delete("clientes",client.get("id").toString()); } catch (RuntimeException ex) { conflict=true; }
            check(conflict, "Impedir exclusão de cliente com pedidos");
            boolean badPrice=false;
            try { reopened.create("servicos",Map.of("nome","Inválido","categoria","", "preco",-3,"descricao","")); } catch (RuntimeException ex) { badPrice=true; }
            check(badPrice, "Rejeitar preço negativo");
            reopened.delete("pedidos",made.get("id").toString());
            reopened.delete("clientes",client.get("id").toString());
            reopened.delete("servicos",service.get("id").toString());
            check(((java.util.List<?>)reopened.snapshot().get("pedidos")).size()==0, "Excluir pedido, cliente e serviço");

            reopened.loadExample();
            check(((java.util.List<?>)reopened.snapshot().get("pedidos")).size()==6, "Carregar dados opcionais para explorar");
            boolean noOverwrite=false;
            try { reopened.loadExample(); } catch(RuntimeException expected) { noOverwrite=true; }
            check(noOverwrite, "Não substituir dados existentes pelos exemplos");
            System.out.println("\n" + passed + " TESTES APROVADOS.\n");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }
}
