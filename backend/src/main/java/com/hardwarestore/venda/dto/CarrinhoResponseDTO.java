package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.Carrinho;
import com.hardwarestore.venda.model.ItemCarrinho;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CarrinhoResponseDTO {

    private Long id;
    private List<ItemCarrinhoResponseDTO> itens;
    private BigDecimal total;

    // RN0044 - ate quando os itens ficam bloqueados (o prazo e relativo ao
    // ultimo item incluido, entao e o mesmo para todos) e quantos segundos
    // faltam. Nulos quando o carrinho esta vazio.
    private LocalDateTime bloqueadoAte;
    private Long segundosParaExpirar;
    // RN0044 - true quando faltam 5 minutos ou menos (aviso ao cliente);
    // o limite vem de app.carrinho.aviso-minutos.
    private boolean avisoExpiracao;
    // Quantos minutos antes o cliente e avisado (app.carrinho.aviso-minutos).
    private int avisoMinutos;

    // RNF0042 - itens retirados por expiracao, ainda listados no carrinho.
    private List<ItemCarrinhoExpiradoResponseDTO> itensExpirados;
    // RNF0042 - a opcao "comprar" fica desabilitada se o carrinho esta vazio
    // ou ainda tem itens expirados nao readicionados.
    private boolean podeComprar;

    public static CarrinhoResponseDTO fromEntity(Carrinho c, LocalDateTime agora, int avisoMinutos) {
        List<ItemCarrinhoResponseDTO> itens = c.getItens().stream()
                .map(ItemCarrinhoResponseDTO::fromEntity)
                .toList();
        BigDecimal total = itens.stream()
                .map(ItemCarrinhoResponseDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime bloqueadoAte = c.getItens().stream()
                .map(ItemCarrinho::getBloqueadoAte)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        Long segundos = bloqueadoAte == null ? null : Math.max(0, Duration.between(agora, bloqueadoAte).getSeconds());
        boolean aviso = segundos != null && segundos <= avisoMinutos * 60L;

        List<ItemCarrinhoExpiradoResponseDTO> expirados = c.getItensExpirados().stream()
                .map(ItemCarrinhoExpiradoResponseDTO::fromEntity)
                .toList();
        boolean podeComprar = !itens.isEmpty() && expirados.isEmpty();

        return new CarrinhoResponseDTO(c.getId(), itens, total, bloqueadoAte, segundos, aviso, avisoMinutos, expirados, podeComprar);
    }
}
