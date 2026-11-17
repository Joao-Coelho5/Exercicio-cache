package com.example.conteudos.service;

import com.example.conteudos.cache.ConteudoCache;
import com.example.conteudos.exception.ConteudoNaoEncontradoException;
import com.example.conteudos.model.Conteudo;
import com.example.conteudos.repository.ConteudoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ConteudoService {

    private final ConteudoRepository repository;
    private final ConteudoCache cache;
    private final long atrasoMs;

    public ConteudoService(ConteudoRepository repository,
                           ConteudoCache cache,
                           @Value("${app.fonte.atraso-ms}") long atrasoMs) {
        this.repository = repository;
        this.cache = cache;
        this.atrasoMs = atrasoMs;
    }

    /** Busca primeiro no cache; se der MISS, busca na fonte lenta e guarda no cache. */
    public ResultadoBusca buscarPorId(Long id) {
        long inicio = System.currentTimeMillis();

        Optional<Conteudo> doCache = cache.buscar(id);
        if (doCache.isPresent()) {
            cache.registrarLog(true, id, inicio);
            return new ResultadoBusca(doCache.get(), true);
        }

        // MISS: vai na fonte lenta
        Conteudo conteudo = buscarNaFonteLenta(id);
        cache.guardar(conteudo);
        cache.registrarLog(false, id, inicio);
        return new ResultadoBusca(conteudo, false);
    }

    public Conteudo cadastrar(Conteudo conteudo) {
        conteudo.setId(null);
        conteudo.setAtualizadoEm(LocalDateTime.now());
        return repository.save(conteudo);
    }

    public Conteudo atualizar(Long id, Conteudo novosDados) {
        Conteudo existente = repository.findById(id)
                .orElseThrow(() -> new ConteudoNaoEncontradoException(id));

        existente.setTitulo(novosDados.getTitulo());
        existente.setCorpo(novosDados.getCorpo());
        existente.setAutor(novosDados.getAutor());
        existente.setAtualizadoEm(LocalDateTime.now());

        Conteudo salvo = repository.save(existente);
        cache.invalidar(id); // o dado do cache ficou velho
        return salvo;
    }

    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new ConteudoNaoEncontradoException(id);
        }
        repository.deleteById(id);
        cache.invalidar(id);
    }

    /** Simula uma fonte lenta (banco sobrecarregado, servico externo, etc). */
    private Conteudo buscarNaFonteLenta(Long id) {
        try {
            Thread.sleep(atrasoMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Busca interrompida", e);
        }
        return repository.findById(id)
                .orElseThrow(() -> new ConteudoNaoEncontradoException(id));
    }
}
