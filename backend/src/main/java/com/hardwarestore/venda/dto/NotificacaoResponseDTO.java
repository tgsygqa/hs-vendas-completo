package com.hardwarestore.venda.dto;

import com.hardwarestore.venda.model.Notificacao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** RN0046 - notificacao exibida ao cliente. */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NotificacaoResponseDTO {

    private Long id;
    private String mensagem;
    private LocalDateTime criadaEm;
    private boolean lida;

    public static NotificacaoResponseDTO fromEntity(Notificacao n) {
        return new NotificacaoResponseDTO(n.getId(), n.getMensagem(), n.getCriadaEm(), n.isLida());
    }
}
