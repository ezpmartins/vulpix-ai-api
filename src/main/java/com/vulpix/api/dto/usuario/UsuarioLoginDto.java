package com.vulpix.api.dto.usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioLoginDto {
    private String email;
    private String senha;
    private String dispositivoCode;
}
