package com.example.conteudos.mapper;

import com.example.conteudos.dto.ConteudoRequestDTO;
import com.example.conteudos.dto.ConteudoResponseDTO;
import com.example.conteudos.model.Conteudo;
import org.springframework.stereotype.Component;

@Component
public class ConteudoMapper {

    public Conteudo toEntity(ConteudoRequestDTO dto) {
        Conteudo conteudo = new Conteudo();
        conteudo.setTitulo(dto.titulo());
        conteudo.setCorpo(dto.corpo());
        conteudo.setAutor(dto.autor());
        return conteudo;
    }

    public ConteudoResponseDTO toResponse(Conteudo conteudo) {
        return new ConteudoResponseDTO(
                conteudo.getId(),
                conteudo.getTitulo(),
                conteudo.getCorpo(),
                conteudo.getAutor(),
                conteudo.getAtualizadoEm());
    }
}
