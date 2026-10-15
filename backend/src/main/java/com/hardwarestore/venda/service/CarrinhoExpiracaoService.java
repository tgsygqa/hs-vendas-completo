package com.hardwarestore.venda.service;

import com.hardwarestore.venda.model.Carrinho;
import com.hardwarestore.venda.model.ItemCarrinho;
import com.hardwarestore.venda.model.ItemCarrinhoExpirado;
import com.hardwarestore.venda.repository.CarrinhoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RN0044/RN0045/RNF0042 - libera os itens cujo prazo de bloqueio acabou.
 * Cada item vencido e retirado do carrinho (RN0045: todo o item daquele
 * produto sai do carrinho que gerou o bloqueio) e fica registrado em
 * Carrinho.itensExpirados para ser exibido ao cliente (RNF0042).
 *
 * Mora num service proprio (e nao dentro do CarrinhoService) para poder ser
 * chamado pelo job agendado, pelo proprio CarrinhoService e pelo
 * PedidoService sem auto-invocacao - que nao passaria pelo proxy
 * transacional do Spring.
 */
@Service
public class CarrinhoExpiracaoService {

    private final CarrinhoRepository carrinhoRepository;

    public CarrinhoExpiracaoService(CarrinhoRepository carrinhoRepository) {
        this.carrinhoRepository = carrinhoRepository;
    }

    /** Usado pelo job: abre a propria transacao para um carrinho. */
    @Transactional
    public List<String> expirarVencidosDoCarrinho(Long carrinhoId) {
        return carrinhoRepository.findById(carrinhoId)
                .map(c -> expirarVencidos(c, LocalDateTime.now()))
                .orElse(List.of());
    }

    public boolean temItemVencido(Carrinho carrinho, LocalDateTime agora) {
        return carrinho.getItens().stream().anyMatch(i -> vencido(i, agora));
    }

    /**
     * Sem transacao propria: roda dentro da transacao de quem chama (que
     * precisa estar aberta para o orphanRemoval apagar os itens).
     *
     * @return nomes dos produtos que acabaram de expirar (vazio se nenhum)
     */
    public List<String> expirarVencidos(Carrinho carrinho, LocalDateTime agora) {
        List<ItemCarrinho> vencidos = carrinho.getItens().stream()
                .filter(i -> vencido(i, agora))
                .toList();
        List<String> nomes = new ArrayList<>();
        for (ItemCarrinho item : vencidos) {
            Long produtoId = item.getProduto().getId();
            // Um aviso antigo do mesmo produto e substituido pelo mais recente.
            carrinho.getItensExpirados().removeIf(e -> e.getProduto().getId().equals(produtoId));
            carrinho.getItensExpirados().add(ItemCarrinhoExpirado.builder()
                    .carrinho(carrinho)
                    .produto(item.getProduto())
                    .quantidade(item.getQuantidade())
                    .expiradoEm(agora)
                    .build());
            carrinho.getItens().remove(item);
            nomes.add(item.getProduto().getNome());
        }
        if (!vencidos.isEmpty()) {
            carrinhoRepository.save(carrinho);
        }
        return nomes;
    }

    private boolean vencido(ItemCarrinho item, LocalDateTime agora) {
        return item.getBloqueadoAte() != null && item.getBloqueadoAte().isBefore(agora);
    }
}
