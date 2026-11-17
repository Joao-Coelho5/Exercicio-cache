package com.example.conteudos.controller;

import com.example.conteudos.cache.ConteudoCache;
import com.example.conteudos.dto.EstatisticasCacheDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cache")
public class CacheController {

    private final ConteudoCache cache;

    public CacheController(ConteudoCache cache) {
        this.cache = cache;
    }

    @GetMapping("/estatisticas")
    public EstatisticasCacheDTO estatisticas() {
        return new EstatisticasCacheDTO(
                cache.getTamanho(),
                cache.getLimite(),
                cache.getHits(),
                cache.getMisses(),
                cache.getEvictions(),
                cache.getHitRatio());
    }

    @DeleteMapping
    public ResponseEntity<Void> limpar() {
        cache.limpar();
        return ResponseEntity.noContent().build();
    }
}
