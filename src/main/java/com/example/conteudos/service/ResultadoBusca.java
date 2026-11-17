package com.example.conteudos.service;

import com.example.conteudos.model.Conteudo;

/**
 * Resultado de uma busca: o conteudo (entidade) e se veio do cache (hit)
 * ou da fonte lenta (miss). A controller usa o "hit" para montar o header X-Cache.
 */
public record ResultadoBusca(Conteudo conteudo, boolean hit) {
}
