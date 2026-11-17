package com.example.conteudos.dto;

public record EstatisticasCacheDTO(int tamanhoAtual, int limite, long hits, long misses, long evictions, double hitRatio) {
}
