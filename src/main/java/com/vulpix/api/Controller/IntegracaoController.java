package com.vulpix.api.Controller;

import com.vulpix.api.dto.Integracao.Resquest.IntegracaoDto;
import com.vulpix.api.dto.Integracao.Resquest.IntegracaoMapper;
import com.vulpix.api.dto.Integracao.Resquest.IntegracaoUpdateDto;
import com.vulpix.api.Entity.Integracao;
import com.vulpix.api.Entity.Empresa;
import com.vulpix.api.Utils.Enum.TipoIntegracao;
import com.vulpix.api.Services.EmpresaService;
import com.vulpix.api.Services.IntegracaoService;
import com.vulpix.api.Services.Usuario.Autenticacao.UsuarioAutenticadoUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/integracoes")
public class IntegracaoController {

    @Autowired
    private IntegracaoService integracaoService;
    @Autowired
    private UsuarioAutenticadoUtil usuarioAutenticadoUtil;
    @Autowired
    private EmpresaService empresaService;

    @Operation(summary = "Habilita uma nova integração",
            description = "Cria uma nova integração para a empresa do usuário autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Integração habilitada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados da nova integração inválidos."),
            @ApiResponse(responseCode = "409", description = "Já existe uma integração ativa do mesmo tipo.")
    })
    @PostMapping
    public ResponseEntity<Integracao> habilitar(@RequestBody IntegracaoDto novaIntegracao) {
        if (novaIntegracao == null) return ResponseEntity.badRequest().build();

        UserDetails userDetails = usuarioAutenticadoUtil.getUsuarioDetalhes();
        String emailUsuario = userDetails.getUsername();
        Empresa empresa = empresaService.buscarEmpresaPeloUsuario(emailUsuario);

        Integracao integracao = IntegracaoMapper.criaEntidadeIntegracao(novaIntegracao, empresa);

        Optional<Integracao> integracaoAtiva = integracaoService.findByEmpresaAndTipo(empresa, integracao.getTipo());
        if (integracaoAtiva.isPresent()) {
            return ResponseEntity.status(409).build();
        }

        return ResponseEntity.status(201).body(integracaoService.save(integracao));
    }
    @Operation(summary = "Atualiza uma integração existente",
            description = "Atualiza os dados de uma integração para a empresa do usuário autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Integração atualizada com sucesso."),
            @ApiResponse(responseCode = "404", description = "Integração não encontrada.")
    })
    @PatchMapping()
    public ResponseEntity<Integracao> atualizar(@RequestBody IntegracaoUpdateDto integracaoAtualizada) {
        UserDetails userDetails = usuarioAutenticadoUtil.getUsuarioDetalhes();
        String emailUsuario = userDetails.getUsername();
        Empresa empresa = empresaService.buscarEmpresaPeloUsuario(emailUsuario);

        Optional<Integracao> integracaoExistente = integracaoService.findByEmpresaAndTipo(empresa, TipoIntegracao.INSTAGRAM);

        if (integracaoExistente.isEmpty()) return ResponseEntity.status(404).build();

        Integracao integracao = IntegracaoMapper.criaEntidadeAtualizada(empresa, integracaoAtualizada);

        Integracao integracaoAtualizadaSalva = integracaoService.atualizaIntegracao(integracaoExistente.get().getId(), integracao);
        return ResponseEntity.status(200).body(integracaoAtualizadaSalva);
    }
    @Operation(summary = "Deleta uma integração existente",
            description = "Remove uma integração da empresa do usuário autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Integração deletada com sucesso."),
            @ApiResponse(responseCode = "404", description = "Integração não encontrada.")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        Optional<Integracao> integracaoExistente = integracaoService.getIntegracaoById(id);
        if (integracaoExistente.isEmpty()) {
            return ResponseEntity.status(404).build();
        }

        integracaoService.deleteById(id);
        return ResponseEntity.status(204).build();
    }

}
