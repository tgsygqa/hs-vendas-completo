package com.hardwarestore.venda.service;

import com.hardwarestore.cliente.model.Endereco;
import com.hardwarestore.venda.dto.FreteResponseDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * RF0034 - "calcular o frete da compra com base nos itens selecionados e o
 * endereco apontado pelo cliente". O DRS nao define a formula; a regra
 * abaixo foi definida pelo grupo e esta documentada em
 * docs/JUSTIFICATIVA_FRETE.md:
 *
 *   frete = tarifa da regiao do estado de entrega
 *           + R$ 3,00 por unidade a partir da 2a unidade do carrinho
 *
 *   tarifa da regiao (UF do endereco de entrega):
 *     SP                                   R$ 24,90
 *     demais estados do Sudeste e do Sul   R$ 29,90   (RJ, MG, ES, PR, SC, RS)
 *     demais regioes                       R$ 39,90   (Norte, Nordeste e Centro-Oeste)
 *
 * "Endereco" entra pela UF; "itens" entram pela quantidade de unidades. O valor
 * calculado e gravado no pedido (Pedido.frete) e nao muda depois.
 */
@Service
public class FreteService {

    public static final BigDecimal TARIFA_SP = new BigDecimal("24.90");
    public static final BigDecimal TARIFA_SUDESTE_SUL = new BigDecimal("29.90");
    public static final BigDecimal TARIFA_DEMAIS_REGIOES = new BigDecimal("39.90");
    public static final BigDecimal ADICIONAL_POR_UNIDADE = new BigDecimal("3.00");

    private static final Set<String> SUDESTE_SUL = Set.of("RJ", "MG", "ES", "PR", "SC", "RS");

    // O campo "estado" do endereco e texto livre: aceita a sigla ("SP") ou o nome ("Sao Paulo").
    private static final Map<String, String> UF_POR_NOME = Map.ofEntries(
            Map.entry("ACRE", "AC"), Map.entry("ALAGOAS", "AL"), Map.entry("AMAPA", "AP"), Map.entry("AMAZONAS", "AM"),
            Map.entry("BAHIA", "BA"), Map.entry("CEARA", "CE"), Map.entry("DISTRITO FEDERAL", "DF"),
            Map.entry("ESPIRITO SANTO", "ES"), Map.entry("GOIAS", "GO"), Map.entry("MARANHAO", "MA"),
            Map.entry("MATO GROSSO", "MT"), Map.entry("MATO GROSSO DO SUL", "MS"), Map.entry("MINAS GERAIS", "MG"),
            Map.entry("PARA", "PA"), Map.entry("PARAIBA", "PB"), Map.entry("PARANA", "PR"),
            Map.entry("PERNAMBUCO", "PE"), Map.entry("PIAUI", "PI"), Map.entry("RIO DE JANEIRO", "RJ"),
            Map.entry("RIO GRANDE DO NORTE", "RN"), Map.entry("RIO GRANDE DO SUL", "RS"), Map.entry("RONDONIA", "RO"),
            Map.entry("RORAIMA", "RR"), Map.entry("SANTA CATARINA", "SC"), Map.entry("SAO PAULO", "SP"),
            Map.entry("SERGIPE", "SE"), Map.entry("TOCANTINS", "TO"));

    public FreteResponseDTO calcular(Endereco endereco, int unidades) {
        String uf = normalizarUf(endereco.getEstado());
        BigDecimal tarifa;
        String regiao;
        if ("SP".equals(uf)) {
            tarifa = TARIFA_SP;
            regiao = "São Paulo";
        } else if (uf != null && SUDESTE_SUL.contains(uf)) {
            tarifa = TARIFA_SUDESTE_SUL;
            regiao = "Demais estados do Sudeste e do Sul";
        } else {
            tarifa = TARIFA_DEMAIS_REGIOES;
            regiao = "Demais regiões";
        }
        BigDecimal adicional = ADICIONAL_POR_UNIDADE.multiply(BigDecimal.valueOf(Math.max(0, unidades - 1)));
        return new FreteResponseDTO(endereco.getId(), uf, regiao, unidades, tarifa, adicional, tarifa.add(adicional));
    }

    /** Sigla do estado em maiusculas, ou null se o texto nao for reconhecido. */
    static String normalizarUf(String estado) {
        if (estado == null) return null;
        String semAcento = Normalizer.normalize(estado.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase(Locale.ROOT);
        if (semAcento.length() == 2) return semAcento;
        return UF_POR_NOME.get(semAcento);
    }
}
