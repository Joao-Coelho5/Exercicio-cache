package com.example.conteudos.cache;

import com.example.conteudos.model.Conteudo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache simples de conteudos feito "na mao" com ConcurrentHashMap.
 *
 * - Limite de itens configuravel (app.cache.max-itens)
 * - Expiracao por TTL (app.cache.ttl-segundos)
 * - Remocao LRU (remove o item usado ha mais tempo) quando enche
 * - Estatisticas: hits, misses e evictions
 */
@Component
public class ConteudoCache {

    private static final Logger log = LoggerFactory.getLogger(ConteudoCache.class);

    private final Map<Long, EntradaCache> mapa = new ConcurrentHashMap<>();

    private final int maxItens;
    private final long ttlMillis;

    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();

    public ConteudoCache(@Value("${app.cache.max-itens}") int maxItens,
                         @Value("${app.cache.ttl-segundos}") long ttlSegundos) {
        this.maxItens = maxItens;
        this.ttlMillis = ttlSegundos * 1000;
    }

    /**
     * Procura um conteudo no cache.
     * Se nao existir ou estiver expirado (passou do TTL), conta como MISS.
     */
    public Optional<Conteudo> buscar(Long id) {
        EntradaCache entrada = mapa.get(id);

        if (entrada == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }

        long agora = System.currentTimeMillis();

        // expirou? entao trata como MISS e remove a entrada velha
        if (agora - entrada.getArmazenadoEm() > ttlMillis) {
            mapa.remove(id, entrada);
            misses.incrementAndGet();
            return Optional.empty();
        }

        // HIT: atualiza o ultimo acesso (usado pelo LRU)
        entrada.setUltimoAcesso(agora);
        hits.incrementAndGet();
        return Optional.of(entrada.getConteudo());
    }

    /**
     * Guarda um conteudo no cache. Se o cache estiver cheio e o item for novo,
     * remove antes o item menos recentemente usado (LRU).
     * E synchronized para que duas threads nao estourem o limite ao mesmo tempo.
     */
    public synchronized void guardar(Conteudo conteudo) {
        Long id = conteudo.getId();

        if (!mapa.containsKey(id) && mapa.size() >= maxItens) {
            removerMenosRecentementeUsado();
        }

        mapa.put(id, new EntradaCache(conteudo, System.currentTimeMillis()));
    }

    /** Percorre as entradas e remove a que tem o menor "ultimoAcesso". */
    private void removerMenosRecentementeUsado() {
        Long idMaisAntigo = null;
        long menorAcesso = Long.MAX_VALUE;

        for (Map.Entry<Long, EntradaCache> e : mapa.entrySet()) {
            if (e.getValue().getUltimoAcesso() < menorAcesso) {
                menorAcesso = e.getValue().getUltimoAcesso();
                idMaisAntigo = e.getKey();
            }
        }

        if (idMaisAntigo != null) {
            mapa.remove(idMaisAntigo);
            evictions.incrementAndGet();
            log.info("Cache EVICTION - removido id={} (menos usado recentemente)", idMaisAntigo);
        }
    }

    /** Remove uma entrada (usado no PUT e no DELETE). */
    public void invalidar(Long id) {
        if (mapa.remove(id) != null) {
            log.info("Cache INVALIDADO - id={}", id);
        }
    }

    /** Limpa todo o cache (nao zera as estatisticas). */
    public void limpar() {
        mapa.clear();
        log.info("Cache LIMPO");
    }

    /** Gera o log de HIT ou MISS com o tempo total da operacao. */
    public void registrarLog(boolean hit, Long id, long inicioMs) {
        long tempo = System.currentTimeMillis() - inicioMs;
        if (hit) {
            log.info("Cache HIT  - id={} - {} ms", id, tempo);
        } else {
            log.info("Cache MISS - id={} - {} ms", id, tempo);
        }
    }

    // ---------- estatisticas ----------

    public int getTamanho() {
        return mapa.size();
    }

    public int getLimite() {
        return maxItens;
    }

    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }

    public long getEvictions() {
        return evictions.get();
    }

    public double getHitRatio() {
        long total = hits.get() + misses.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) hits.get() / total;
    }
}
