package com.example.conteudos.cache;

import com.example.conteudos.model.Conteudo;

/**
 * Uma "linha" do cache: guarda o conteudo e as informacoes
 * necessarias para o TTL (armazenadoEm) e para o LRU (ultimoAcesso).
 */
public class EntradaCache {

    private final Conteudo conteudo;
    private final long armazenadoEm;          // momento em que entrou no cache (ms)
    private volatile long ultimoAcesso;       // ultima vez que foi lido (ms)

    public EntradaCache(Conteudo conteudo, long agora) {
        this.conteudo = conteudo;
        this.armazenadoEm = agora;
        this.ultimoAcesso = agora;
    }

    public Conteudo getConteudo() {
        return conteudo;
    }

    public long getArmazenadoEm() {
        return armazenadoEm;
    }

    public long getUltimoAcesso() {
        return ultimoAcesso;
    }

    public void setUltimoAcesso(long ultimoAcesso) {
        this.ultimoAcesso = ultimoAcesso;
    }
}
