package com.hardwarestore.venda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwarestore.cliente.ClienteServiceApplication;
import com.hardwarestore.common.testsupport.TestSupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Testes de integracao do CDU02 (Gerenciar Venda Eletronica) - sobem a
 * aplicacao inteira (H2 em memoria) e exercitam a API REST de verdade, sem
 * navegador. Complementam a suite Cypress (que cobre a interface): aqui ficam
 * as regras de negocio dificeis de provocar pela tela.
 *
 * Massa de dados (volta a este estado antes de cada teste, ver @BeforeEach):
 *  produtos 1 RTX 4090 (R$ 14.500,00 / 12 un), 2 Ryzen (4.899,90 / 25),
 *  3 Headset (349,90 / 40), 4 Teclado (459,90 / 30), 5 SSD (1.250,00 / 18);
 *  cliente 1 = Joao Silva (endereco 1, cartao 1 final 4242);
 *  pedido 1 ENTREGUE (SSD), pedido 2 EM_TRANSPORTE (Headset);
 *  cupons 1 PROMO-BEMVINDO 50, 2 PROMO-VERAO 30, 3 TROCA-JOAO-PEQUENO 20, 4 TROCA-JOAO-GRANDE 500.
 */
@SpringBootTest(classes = ClienteServiceApplication.class)
@AutoConfigureMockMvc
class VendasFluxoIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TestSupportService testSupport;

    @BeforeEach
    void resetarBase() {
        testSupport.resetar();
    }

    // ------------------------------------------------------------------ helpers

    private JsonNode chamar(MockHttpServletRequestBuilder req, String corpo, int statusEsperado) throws Exception {
        if (corpo != null) req.contentType(MediaType.APPLICATION_JSON).content(corpo);
        var resp = mvc.perform(req).andReturn().getResponse();
        String texto = resp.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(statusEsperado, resp.getStatus(), "HTTP inesperado para " + req.buildRequest(new org.springframework.mock.web.MockServletContext()).getRequestURI() + ": " + texto);
        return texto.isBlank() ? null : json.readTree(texto);
    }

    private JsonNode adicionar(long clienteId, long produtoId, int qtd, int status) throws Exception {
        return chamar(post("/api/clientes/" + clienteId + "/carrinho/itens"),
                "{\"produtoId\":" + produtoId + ",\"quantidade\":" + qtd + "}", status);
    }

    private JsonNode finalizar(long clienteId, String corpo, int status) throws Exception {
        return chamar(post("/api/clientes/" + clienteId + "/pedidos"), corpo, status);
    }

    /** Compra pronta: n headsets pagos com o cartao 1 no valor exato. */
    private JsonNode comprarHeadsets(int n) throws Exception {
        adicionar(1, 3, n, 201);
        // BigDecimal (e nao double) para o valor sair com exatamente 2 casas, como a tela envia.
        java.math.BigDecimal total = new java.math.BigDecimal("349.90").multiply(java.math.BigDecimal.valueOf(n))
                .add(new java.math.BigDecimal("24.90"));
        return finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":" + total.toPlainString() + "}],\"cupomIds\":[]}", 201);
    }

    private int disponivel(long clienteId, long produtoId) throws Exception {
        for (JsonNode p : chamar(get("/api/produtos?clienteId=" + clienteId), null, 200)) {
            if (p.get("id").asLong() == produtoId) return p.get("quantidadeDisponivel").asInt();
        }
        throw new AssertionError("produto nao listado");
    }

    private String criarClienteSegundo() throws Exception {
        // CPF valido (digitos verificadores corretos): 529.982.247-25
        String corpo = """
            {"nome":"Maria Souza","cpf":"529.982.247-25","genero":"F","dataNascimento":"1992-03-10",
             "telefone":{"tipo":"CELULAR","ddd":"11","numero":"91234-5678"},"email":"maria@email.com",
             "senha":"SenhaForte@123","confirmacaoSenha":"SenhaForte@123",
             "endereco":{"apelido":"Casa","tipoResidencia":"Casa","tipoLogradouro":"Rua","logradouro":"Teste",
                         "numero":"10","bairro":"Centro","cep":"01000-000","cidade":"Sao Paulo","estado":"SP","pais":"Brasil"}}
            """;
        return chamar(post("/api/clientes"), corpo, 201).get("id").asText();
    }

    private void aprovar(long pedidoId) throws Exception {
        assertEquals("APROVADA", chamar(patch("/api/pedidos/" + pedidoId + "/validar-pagamento"), null, 200).get("status").asText());
    }

    // ------------------------------------------------------------------ CDU02: fluxo principal

    @Test
    @DisplayName("Fluxo principal completo: compra -> aprovada -> transporte -> entregue -> troca -> trocado (RF0033-RF0045, RF0053/54)")
    void fluxoPrincipalCompleto() throws Exception {
        JsonNode pedido = comprarHeadsets(2);
        long id = pedido.get("id").asLong();
        assertEquals("EM_PROCESSAMENTO", pedido.get("status").asText());          // RF0038
        assertEquals(40, disponivel(1, 3));                                         // estoque so baixa na aprovacao

        aprovar(id);                                                                // RN0037/RN0038
        assertEquals(38, disponivel(1, 3));                                         // RF0053 baixa em estoque
        assertEquals("EM_TRANSPORTE", chamar(patch("/api/pedidos/" + id + "/despachar"), null, 200).get("status").asText());   // RF0039
        assertEquals("ENTREGUE", chamar(patch("/api/pedidos/" + id + "/confirmar-entrega"), null, 200).get("status").asText()); // RF0040

        long itemId = chamar(get("/api/pedidos/" + id), null, 200).get("itens").get(0).get("id").asLong();
        JsonNode troca = chamar(post("/api/pedidos/" + id + "/trocas"),
                "{\"itens\":[{\"itemPedidoId\":" + itemId + ",\"quantidade\":1}],\"motivoCliente\":\"Defeito\"}", 201); // RF0041
        assertEquals("EM_TROCA", troca.get("statusPedido").asText());
        long trocaId = troca.get("id").asLong();

        assertEquals("TROCA_AUTORIZADA", chamar(patch("/api/trocas/" + trocaId + "/autorizar"), null, 200).get("statusPedido").asText()); // RF0042
        JsonNode recebida = chamar(patch("/api/trocas/" + trocaId + "/confirmar-recebimento"), "{\"itensRetornaramEstoque\":true}", 200);
        assertEquals("TROCADO", recebida.get("statusPedido").asText());             // RN0042
        assertEquals(39, disponivel(1, 3));                                         // RF0054 reentrada em estoque
        assertEquals(349.90, recebida.get("cupomGerado").get("valor").asDouble(), 0.001); // RF0045

        boolean cupomDisponivel = false;
        for (JsonNode c : chamar(get("/api/clientes/1/cupons"), null, 200)) {
            if (c.get("codigo").asText().equals(recebida.get("cupomGerado").get("codigo").asText())) cupomDisponivel = true;
        }
        assertTrue(cupomDisponivel, "o cupom de troca gerado deve ficar disponivel ao cliente");

        // RN0046 - cliente notificado (autorizacao + cupom)
        assertEquals(2, chamar(get("/api/clientes/1/notificacoes"), null, 200).size());
    }

    @Test
    @DisplayName("Troca sem retorno ao estoque nao devolve o item (RF0044/RF0054)")
    void trocaSemRetornoAoEstoque() throws Exception {
        long id = comprarHeadsets(1).get("id").asLong();
        aprovar(id);
        chamar(patch("/api/pedidos/" + id + "/despachar"), null, 200);
        chamar(patch("/api/pedidos/" + id + "/confirmar-entrega"), null, 200);
        long itemId = chamar(get("/api/pedidos/" + id), null, 200).get("itens").get(0).get("id").asLong();
        long trocaId = chamar(post("/api/pedidos/" + id + "/trocas"),
                "{\"itens\":[{\"itemPedidoId\":" + itemId + ",\"quantidade\":1}],\"motivoCliente\":\"Arrependimento\"}", 201).get("id").asLong();
        chamar(patch("/api/trocas/" + trocaId + "/autorizar"), null, 200);
        chamar(patch("/api/trocas/" + trocaId + "/confirmar-recebimento"), "{\"itensRetornaramEstoque\":false}", 200);
        assertEquals(39, disponivel(1, 3));   // 40 - 1 comprado, nada devolvido
    }

    @Test
    @DisplayName("Troca recusada devolve o pedido para ENTREGUE e notifica o cliente")
    void trocaRecusada() throws Exception {
        // pedido 1 ja nasce ENTREGUE na massa de dados
        long itemId = chamar(get("/api/pedidos/1"), null, 200).get("itens").get(0).get("id").asLong();
        long trocaId = chamar(post("/api/pedidos/1/trocas"),
                "{\"itens\":[{\"itemPedidoId\":" + itemId + ",\"quantidade\":1}],\"motivoCliente\":\"Nao gostei\"}", 201).get("id").asLong();
        chamar(patch("/api/trocas/" + trocaId + "/recusar"), "{}", 400);          // motivo obrigatorio
        JsonNode recusada = chamar(patch("/api/trocas/" + trocaId + "/recusar"), "{\"motivoRecusa\":\"Fora do prazo\"}", 200);
        assertTrue(recusada.get("recusada").asBoolean());
        assertEquals("ENTREGUE", chamar(get("/api/pedidos/1"), null, 200).get("status").asText());
        chamar(patch("/api/trocas/" + trocaId + "/autorizar"), null, 409);         // ja decidida/pedido nao esta em troca
        assertEquals(1, chamar(get("/api/clientes/1/notificacoes"), null, 200).size());
    }

    // ------------------------------------------------------------------ transicoes de status

    @Test
    @DisplayName("Transicoes de status invalidas sao recusadas (RN0039, RN0040, RN0042, RN0043)")
    void transicoesInvalidas() throws Exception {
        long id = comprarHeadsets(1).get("id").asLong();                           // EM_PROCESSAMENTO
        chamar(patch("/api/pedidos/" + id + "/despachar"), null, 409);             // so APROVADA despacha (RN0039)
        chamar(patch("/api/pedidos/" + id + "/confirmar-entrega"), null, 409);     // so EM_TRANSPORTE (RN0040)
        long itemId = chamar(get("/api/pedidos/" + id), null, 200).get("itens").get(0).get("id").asLong();
        chamar(post("/api/pedidos/" + id + "/trocas"),
                "{\"itens\":[{\"itemPedidoId\":" + itemId + ",\"quantidade\":1}],\"motivoCliente\":\"x\"}", 409); // so ENTREGUE (RN0043)
        aprovar(id);
        chamar(patch("/api/pedidos/" + id + "/validar-pagamento"), null, 409);     // ja foi validado
    }

    @Test
    @DisplayName("Troca: quantidade maior que a comprada, item de outro pedido e item repetido sao recusados")
    void validacoesDeTroca() throws Exception {
        // pedido 1 (ENTREGUE): 1 SSD. item id 1.
        chamar(post("/api/pedidos/1/trocas"),
                "{\"itens\":[{\"itemPedidoId\":1,\"quantidade\":2}],\"motivoCliente\":\"x\"}", 409);
        chamar(post("/api/pedidos/1/trocas"),
                "{\"itens\":[{\"itemPedidoId\":2,\"quantidade\":1}],\"motivoCliente\":\"x\"}", 409);
        chamar(post("/api/pedidos/1/trocas"),
                "{\"itens\":[{\"itemPedidoId\":1,\"quantidade\":1},{\"itemPedidoId\":1,\"quantidade\":1}],\"motivoCliente\":\"x\"}", 409);
        chamar(post("/api/pedidos/1/trocas"), "{\"itens\":[],\"motivoCliente\":\"x\"}", 400);
        chamar(post("/api/pedidos/1/trocas"), "{\"itens\":[{\"itemPedidoId\":1,\"quantidade\":1}],\"motivoCliente\":\"\"}", 400);
    }

    // ------------------------------------------------------------------ pagamento

    @Test
    @DisplayName("Pagamento reprovado: cartao final 0000 -> REPROVADA, estoque preservado, cupom liberado (RN0028, RN0038)")
    void pagamentoReprovado() throws Exception {
        chamar(post("/api/clientes/1/cartoes"),
                "{\"numero\":\"4111111111110000\",\"bandeira\":\"VISA\",\"titular\":\"JOAO SILVA\",\"cvv\":\"123\"}", 201);
        long cartaoRecusado = 2;
        adicionar(1, 3, 1, 201);                                                    // total 374.80
        long id = finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":" + cartaoRecusado
                + ",\"valor\":324.80}],\"cupomIds\":[1]}", 201).get("id").asLong();   // cupom promocional de 50
        JsonNode r = chamar(patch("/api/pedidos/" + id + "/validar-pagamento"), null, 200);
        assertEquals("REPROVADA", r.get("status").asText());
        assertTrue(r.get("motivoReprovacao").asText().contains("0000"));
        assertEquals(40, disponivel(1, 3));                                         // RN0028: estoque intacto
        boolean cupomVoltou = false;
        for (JsonNode c : chamar(get("/api/clientes/1/cupons"), null, 200)) if (c.get("id").asLong() == 1) cupomVoltou = true;
        assertTrue(cupomVoltou, "cupom usado numa compra reprovada deve voltar a ficar disponivel");
        chamar(patch("/api/pedidos/" + id + "/despachar"), null, 409);             // REPROVADA nao despacha
    }

    @Test
    @DisplayName("RN0033: so um cupom promocional por compra")
    void umCupomPromocionalPorCompra() throws Exception {
        adicionar(1, 3, 1, 201);
        finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":294.80}],\"cupomIds\":[1,2]}", 409);
    }

    @Test
    @DisplayName("RN0034/RN0035: minimo de R$ 10,00 por cartao, dispensado quando ha cupom")
    void valorMinimoPorCartao() throws Exception {
        chamar(post("/api/clientes/1/cartoes"),
                "{\"numero\":\"5555666677778888\",\"bandeira\":\"MASTERCARD\",\"titular\":\"JOAO\",\"cvv\":\"321\"}", 201);
        adicionar(1, 3, 1, 201);                                                    // total 374.80
        // dois cartoes, sem cupom, um deles com R$ 5,00 -> RN0034
        finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":369.80},{\"cartaoId\":2,\"valor\":5.00}],\"cupomIds\":[]}", 409);
        // com cupom (R$ 50), o mesmo cartao de R$ 5,00 e aceito -> RN0035
        JsonNode ok = finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":319.80},{\"cartaoId\":2,\"valor\":5.00}],\"cupomIds\":[1]}", 201);
        assertEquals("EM_PROCESSAMENTO", ok.get("status").asText());
    }

    @Test
    @DisplayName("Os pagamentos precisam cobrir exatamente o valor da compra")
    void pagamentoDeveCobrirExatamente() throws Exception {
        adicionar(1, 3, 1, 201);                                                    // total 374.80
        finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":300.00}],\"cupomIds\":[]}", 409);
        finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":400.00}],\"cupomIds\":[]}", 409);
        finalizar(1, "{\"enderecoId\":1,\"cartoes\":[],\"cupomIds\":[]}", 409);
    }

    @Test
    @DisplayName("RN0036: cupom maior que a compra gera cupom de troco; cupom desnecessario e recusado")
    void cupomDeTroco() throws Exception {
        adicionar(1, 3, 1, 201);                                                    // total 374.80
        // 500 + 20 = 520, mas o de 500 sozinho ja cobre -> o de 20 e desnecessario
        finalizar(1, "{\"enderecoId\":1,\"cartoes\":[],\"cupomIds\":[4,3]}", 409);
        // so o de 500: sobra 125.20 -> cupom de troco
        JsonNode pedido = finalizar(1, "{\"enderecoId\":1,\"cartoes\":[],\"cupomIds\":[4]}", 201);
        assertEquals(374.80, pedido.get("valorTotal").asDouble(), 0.001);
        double troco = 0;
        for (JsonNode c : chamar(get("/api/clientes/1/cupons"), null, 200)) {
            if (c.get("codigo").asText().startsWith("TROCO-")) troco = c.get("valor").asDouble();
        }
        assertEquals(125.20, troco, 0.001);
        // compra paga so com cupom nao passa por cartao: e aprovada
        aprovar(pedido.get("id").asLong());
    }

    @Test
    @DisplayName("Cupom ja utilizado nao pode ser usado de novo")
    void cupomUsadoNaoReutiliza() throws Exception {
        adicionar(1, 3, 1, 201);
        finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":324.80}],\"cupomIds\":[1]}", 201);
        adicionar(1, 3, 1, 201);
        finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":324.80}],\"cupomIds\":[1]}", 409);
    }

    @Test
    @DisplayName("Carrinho vazio nao finaliza; endereco inexistente e recusado")
    void finalizacaoInvalida() throws Exception {
        chamar(get("/api/clientes/1/carrinho"), null, 200);                        // cria carrinho vazio
        finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":24.90}],\"cupomIds\":[]}", 409);
        adicionar(1, 3, 1, 201);
        finalizar(1, "{\"enderecoId\":999,\"cartoes\":[{\"cartaoId\":1,\"valor\":374.80}],\"cupomIds\":[]}", 404);
        finalizar(1, "{\"cartoes\":[{\"cartaoId\":1,\"valor\":374.80}],\"cupomIds\":[]}", 400);
    }

    // ------------------------------------------------------------------ carrinho / estoque

    @Test
    @DisplayName("RF0031/RF0032/RN0031: adicionar, alterar, remover; nunca mais que o estoque")
    void carrinhoEEstoque() throws Exception {
        JsonNode c = adicionar(1, 1, 2, 201);                                       // 2 RTX
        long itemId = c.get("itens").get(0).get("id").asLong();
        assertEquals(29000.0, c.get("total").asDouble(), 0.001);
        adicionar(1, 1, 11, 409);                                                   // 2 + 11 > 12
        c = chamar(patch("/api/clientes/1/carrinho/itens/" + itemId), "{\"quantidade\":12}", 200);
        assertEquals(12, c.get("itens").get(0).get("quantidade").asInt());
        chamar(patch("/api/clientes/1/carrinho/itens/" + itemId), "{\"quantidade\":13}", 409);
        chamar(patch("/api/clientes/1/carrinho/itens/" + itemId), "{\"quantidade\":0}", 400);     // DTO exige >= 1
        adicionar(1, 1, 0, 400);                                                    // quantidade deve ser > 0 (DTO)
        c = chamar(delete("/api/clientes/1/carrinho/itens/" + itemId), null, 200);
        assertEquals(0, c.get("itens").size());
        assertFalse(c.get("podeComprar").asBoolean());
    }

    @Test
    @DisplayName("RN0032: estoque mudou antes de finalizar -> quantidade ajustada automaticamente, com alerta")
    void estoqueMudouAntesDeFinalizar() throws Exception {
        adicionar(1, 3, 5, 201);
        testSupport.definirEstoque(3L, 3);                                          // outro cliente comprou
        JsonNode erro = finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":1774.40}],\"cupomIds\":[]}", 409);
        assertTrue(erro.get("mensagem").asText().contains("ajustada de 5 para 3"));
        JsonNode carrinho = chamar(get("/api/clientes/1/carrinho"), null, 200);
        assertEquals(3, carrinho.get("itens").get(0).get("quantidade").asInt());    // ajuste sobreviveu ao erro

        testSupport.definirEstoque(3L, 0);                                          // acabou de vez
        erro = finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":1074.60}],\"cupomIds\":[]}", 409);
        assertTrue(erro.get("mensagem").asText().contains("ficou indisponível"));
        assertEquals(0, chamar(get("/api/clientes/1/carrinho"), null, 200).get("itens").size());
    }

    // ------------------------------------------------------------------ bloqueio de carrinho (RN0044/RN0045/RNF0042)

    @Test
    @DisplayName("RN0044: itens no carrinho ficam bloqueados para outros clientes")
    void bloqueioEntreClientes() throws Exception {
        String maria = criarClienteSegundo();
        adicionar(1, 1, 12, 201);                                                   // Joao bloqueia as 12 RTX
        assertEquals(0, disponivel(Long.parseLong(maria), 1));                      // Maria nao tem nenhuma disponivel
        assertEquals(12, disponivel(1, 1));                                         // para o proprio Joao, a reserva nao conta contra ele
        JsonNode erro = adicionar(Long.parseLong(maria), 1, 1, 409);
        assertTrue(erro.get("mensagem").asText().contains("Disponível: 0"));
        adicionar(Long.parseLong(maria), 2, 1, 201);                                // outros produtos seguem livres
    }

    @Test
    @DisplayName("RN0044: o prazo e renovado a cada item incluido (relativo ao ultimo item)")
    void prazoRelativoAoUltimoItem() throws Exception {
        adicionar(1, 3, 1, 201);
        testSupport.definirBloqueio(1L, 120);                                       // faltam 2 min
        JsonNode antes = chamar(get("/api/clientes/1/carrinho"), null, 200);
        assertTrue(antes.get("segundosParaExpirar").asLong() <= 120);
        assertTrue(antes.get("avisoExpiracao").asBoolean());                        // < 5 min: avisa o cliente
        JsonNode depois = adicionar(1, 4, 1, 201);                                  // novo item renova TODOS
        assertTrue(depois.get("segundosParaExpirar").asLong() > 1500);
        assertFalse(depois.get("avisoExpiracao").asBoolean());
        for (JsonNode i : depois.get("itens")) {
            assertTrue(java.time.LocalDateTime.parse(i.get("bloqueadoAte").asText())
                    .isAfter(java.time.LocalDateTime.now().plusMinutes(25)));
        }
    }

    @Test
    @DisplayName("RN0044/RN0045/RNF0042: bloqueio vencido libera o estoque, lista o item como expirado e desabilita a compra")
    void expiracaoDoBloqueio() throws Exception {
        String maria = criarClienteSegundo();
        adicionar(1, 1, 12, 201);
        testSupport.definirBloqueio(1L, -5);                                        // ja venceu
        assertEquals(1, testSupport.varrerExpirados());                             // job libera
        assertEquals(12, disponivel(Long.parseLong(maria), 1));                     // RN0044: voltou a ficar disponivel

        JsonNode carrinho = chamar(get("/api/clientes/1/carrinho"), null, 200);
        assertEquals(0, carrinho.get("itens").size());                              // RN0045: item saiu do carrinho
        assertEquals(1, carrinho.get("itensExpirados").size());                     // RNF0042: continua listado
        assertEquals(12, carrinho.get("itensExpirados").get(0).get("quantidade").asInt());
        assertFalse(carrinho.get("podeComprar").asBoolean());                       // RNF0042: comprar desabilitado

        // adicionar outro produto nao limpa o aviso: o item expirado precisa ser readicionado
        carrinho = adicionar(1, 3, 1, 201);
        assertFalse(carrinho.get("podeComprar").asBoolean());
        long expiradoId = carrinho.get("itensExpirados").get(0).get("id").asLong();
        carrinho = chamar(post("/api/clientes/1/carrinho/expirados/" + expiradoId + "/readicionar"), null, 200);
        assertEquals(0, carrinho.get("itensExpirados").size());
        assertEquals(2, carrinho.get("itens").size());
        assertTrue(carrinho.get("podeComprar").asBoolean());
    }

    @Test
    @DisplayName("RNF0042: expiracao detectada tambem sem o job (ao consultar o carrinho) e ao finalizar a compra")
    void expiracaoSemJob() throws Exception {
        adicionar(1, 3, 1, 201);
        testSupport.definirBloqueio(1L, -5);
        JsonNode erro = finalizar(1, "{\"enderecoId\":1,\"cartoes\":[{\"cartaoId\":1,\"valor\":374.80}],\"cupomIds\":[]}", 409);
        assertTrue(erro.get("mensagem").asText().contains("expirou"));              // alerta na finalizacao
        JsonNode carrinho = chamar(get("/api/clientes/1/carrinho"), null, 200);
        assertEquals(1, carrinho.get("itensExpirados").size());                     // e o carrinho ja esta corrigido

        // dispensar o aviso tambem reabilita o carrinho (sem readicionar)
        long expiradoId = carrinho.get("itensExpirados").get(0).get("id").asLong();
        carrinho = chamar(delete("/api/clientes/1/carrinho/expirados/" + expiradoId), null, 200);
        assertEquals(0, carrinho.get("itensExpirados").size());
    }

    @Test
    @DisplayName("Readicionar item expirado respeita o estoque (RN0031) quando outro cliente o bloqueou nesse meio tempo")
    void readicionarSemEstoque() throws Exception {
        String maria = criarClienteSegundo();
        adicionar(1, 1, 12, 201);
        testSupport.definirBloqueio(1L, -5);
        testSupport.varrerExpirados();
        adicionar(Long.parseLong(maria), 1, 12, 201);                               // Maria pega as 12 RTX
        long expiradoId = chamar(get("/api/clientes/1/carrinho"), null, 200).get("itensExpirados").get(0).get("id").asLong();
        chamar(post("/api/clientes/1/carrinho/expirados/" + expiradoId + "/readicionar"), null, 409);
    }

    // ------------------------------------------------------------------ consultas / suporte

    @Test
    @DisplayName("Cadastrar endereco e cartao durante a compra devolve o id (a tela precisa dele para seleciona-los) - RF0035/RF0036")
    void enderecoECartaoNovosDevolvemId() throws Exception {
        JsonNode cartao = chamar(post("/api/clientes/1/cartoes"),
                "{\"numero\":\"5555666677778888\",\"bandeira\":\"MASTERCARD\",\"titular\":\"JOAO\",\"cvv\":\"321\"}", 201);
        assertEquals(2, cartao.get("id").asLong());
        assertEquals("8888", cartao.get("ultimosQuatroDigitos").asText());
        JsonNode endereco = chamar(post("/api/clientes/1/enderecos"),
                "{\"apelido\":\"Trabalho\",\"tipoResidencia\":\"Predio\",\"tipoLogradouro\":\"Av\",\"logradouro\":\"Paulista\","
                        + "\"numero\":\"1\",\"bairro\":\"Centro\",\"cep\":\"01000-000\",\"cidade\":\"SP\",\"estado\":\"SP\","
                        + "\"pais\":\"Brasil\",\"cobranca\":true,\"entrega\":true}", 201);
        assertEquals(2, endereco.get("id").asLong());
        // e o cartao/endereco novos ja servem para pagar/entregar uma compra
        adicionar(1, 3, 1, 201);
        finalizar(1, "{\"enderecoId\":2,\"cartoes\":[{\"cartaoId\":2,\"valor\":374.80}],\"cupomIds\":[]}", 201);
    }

    @Test
    @DisplayName("Consultas: pedidos do cliente, painel do admin com filtro de status, trocas (RF0025, RF0043)")
    void consultas() throws Exception {
        assertEquals(2, chamar(get("/api/clientes/1/pedidos"), null, 200).size());
        assertEquals(2, chamar(get("/api/pedidos"), null, 200).size());
        JsonNode soEntregues = chamar(get("/api/pedidos?status=ENTREGUE"), null, 200);
        assertEquals(1, soEntregues.size());
        assertEquals(1, soEntregues.get(0).get("id").asLong());
        assertTrue(chamar(get("/api/pedidos/1"), null, 200).get("clienteNome").asText().endsWith("Silva"));
        assertEquals(0, chamar(get("/api/trocas"), null, 200).size());
        chamar(get("/api/pedidos/999"), null, 404);
        chamar(get("/api/pedidos?status=INVALIDO"), null, 400);
    }

    @Test
    @DisplayName("Reset de teste restaura a massa de dados e os ids originais")
    void resetRestauraMassa() throws Exception {
        comprarHeadsets(1);
        testSupport.resetar();
        assertEquals(2, chamar(get("/api/pedidos"), null, 200).size());
        assertEquals(40, disponivel(1, 3));
        assertEquals(4, chamar(get("/api/clientes/1/cupons"), null, 200).size());
        assertEquals("EM_TRANSPORTE", chamar(get("/api/pedidos/2"), null, 200).get("status").asText());
    }
}
