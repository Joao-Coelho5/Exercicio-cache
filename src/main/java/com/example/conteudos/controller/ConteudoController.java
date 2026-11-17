package com.example.conteudos.controller;

import com.example.conteudos.dto.ConteudoRequestDTO;
import com.example.conteudos.dto.ConteudoResponseDTO;
import com.example.conteudos.mapper.ConteudoMapper;
import com.example.conteudos.model.Conteudo;
import com.example.conteudos.service.ConteudoService;
import com.example.conteudos.service.ResultadoBusca;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conteudos")
public class ConteudoController {

    private final ConteudoService service;
    private final ConteudoMapper mapper;

    public ConteudoController(ConteudoService service, ConteudoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConteudoResponseDTO> buscar(@PathVariable Long id) {
        ResultadoBusca resultado = service.buscarPorId(id);
        String xCache = resultado.hit() ? "HIT" : "MISS";

        return ResponseEntity.ok()
                .header("X-Cache", xCache)
                .body(mapper.toResponse(resultado.conteudo()));
    }

    @PostMapping
    public ResponseEntity<ConteudoResponseDTO> cadastrar(@RequestBody ConteudoRequestDTO dto) {
        Conteudo salvo = service.cadastrar(mapper.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConteudoResponseDTO> atualizar(@PathVariable Long id,
                                                         @RequestBody ConteudoRequestDTO dto) {
        Conteudo atualizado = service.atualizar(id, mapper.toEntity(dto));
        return ResponseEntity.ok(mapper.toResponse(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
