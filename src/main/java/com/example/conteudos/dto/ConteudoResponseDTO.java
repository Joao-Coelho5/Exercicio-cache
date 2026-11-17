package com.example.conteudos.dto;

import java.time.LocalDateTime;

public record ConteudoResponseDTO(Long id, String titulo, String corpo, String autor, LocalDateTime atualizadoEm) {
}
