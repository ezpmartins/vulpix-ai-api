package com.vulpix.api.Controller;

import com.vulpix.api.Service.EmpresaService;
import com.vulpix.api.Service.Usuario.Autenticacao.UsuarioAutenticadoUtil;
import com.vulpix.api.Utils.Enum.TipoIntegracao;
import com.vulpix.api.Dto.Publicacao.GetPublicacaoDto;
import com.vulpix.api.Dto.Publicacao.PostPublicacaoDto;
import com.vulpix.api.Dto.Publicacao.PostPublicacaoResponse;
import com.vulpix.api.Entity.Empresa;
import com.vulpix.api.Entity.Integracao;
import com.vulpix.api.Entity.Publicacao;
import com.vulpix.api.Repository.EmpresaRepository;
import com.vulpix.api.Repository.PublicacaoRepository;
import com.vulpix.api.Service.Integracoes.Graph.PublicacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
@Tag(name = "Controller de Publicação")
public class PublicacaoController {
    @Autowired
    private PublicacaoService publicacaoService;
    @Autowired
    private PublicacaoRepository publicacaoRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private EmpresaService empresaService;
    @Autowired
    private UsuarioAutenticadoUtil usuarioAutenticadoUtil;

    @GetMapping()
    @Operation(summary = "Buscar posts por empresa",
            description = "Retorna uma lista de publicações associadas a uma empresa especificada pelo ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de publicações retornada com sucesso.",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = "[{\"id\":\"1\",\"legenda\":\"Post 1\",\"tipoMidia\":\"image\",\"urlMidia\":\"http://exemplo.com/post1\",\"dataPublicacao\":\"2024-11-01T10:00:00Z\",\"likeCount\":10}]"),
                                    @ExampleObject(value = "[{\"id\":\"2\",\"legenda\":\"Post 2\",\"tipoMidia\":\"video\",\"urlMidia\":\"http://exemplo.com/post2\",\"dataPublicacao\":\"2024-11-02T10:00:00Z\",\"likeCount\":20}]"),
                                    @ExampleObject(value = "[{\"id\":\"3\",\"legenda\":\"Post 3\",\"tipoMidia\":\"image\",\"urlMidia\":\"http://exemplo.com/post3\",\"dataPublicacao\":\"2024-11-03T10:00:00Z\",\"likeCount\":30}]"),
                                    @ExampleObject(value = "[{\"id\":\"4\",\"legenda\":\"Post 4\",\"tipoMidia\":\"image\",\"urlMidia\":\"http://exemplo.com/post4\",\"dataPublicacao\":\"2024-11-04T10:00:00Z\",\"likeCount\":40}]"),
                                    @ExampleObject(value = "[{\"id\":\"5\",\"legenda\":\"Post 5\",\"tipoMidia\":\"video\",\"urlMidia\":\"http://exemplo.com/post5\",\"dataPublicacao\":\"2024-11-05T10:00:00Z\",\"likeCount\":50}]"),
                                    @ExampleObject(value = "[{\"id\":\"6\",\"legenda\":\"Post 6\",\"tipoMidia\":\"image\",\"urlMidia\":\"http://exemplo.com/post6\",\"dataPublicacao\":\"2024-11-06T10:00:00Z\",\"likeCount\":60}]")
                            })),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"message\": \"Erro: Empresa não encontrada.\" }")))
    })
    public ResponseEntity<List<GetPublicacaoDto>> buscarPosts() {
        UserDetails userDetails = usuarioAutenticadoUtil.getUsuarioDetalhes();
        String emailUsuario = userDetails.getUsername();
        Empresa empresa = empresaService.buscarEmpresaPeloUsuario(emailUsuario);

        return publicacaoService.buscarPosts(empresa.getId());
    }

    @PostMapping()
    @Operation(summary = "Criar um novo post",
            description = "Cria um novo post para a empresa informada. O post deve incluir a legenda e a URL da mídia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Post criado com sucesso.",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = "{\"legenda\":\"Novo Post\",\"id\":\"1\",\"fkEmpresa\":\"empresa-1\"}"),
                                    @ExampleObject(value = "{\"legenda\":\"Post de Verão\",\"id\":\"2\",\"fkEmpresa\":\"empresa-1\"}"),
                                    @ExampleObject(value = "{\"legenda\":\"Post de Natal\",\"id\":\"3\",\"fkEmpresa\":\"empresa-1\"}"),
                                    @ExampleObject(value = "{\"legenda\":\"Post de Aniversário\",\"id\":\"4\",\"fkEmpresa\":\"empresa-1\"}"),
                                    @ExampleObject(value = "{\"legenda\":\"Post de Lançamento\",\"id\":\"5\",\"fkEmpresa\":\"empresa-1\"}"),
                                    @ExampleObject(value = "{\"legenda\":\"Post de Black Friday\",\"id\":\"6\",\"fkEmpresa\":\"empresa-1\"}")
                            })),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"message\": \"Erro: Empresa não encontrada.\" }")))
    })
    public ResponseEntity<PostPublicacaoResponse> criarPost(@RequestBody PostPublicacaoDto post) {
        UserDetails userDetails = usuarioAutenticadoUtil.getUsuarioDetalhes();
        String emailUsuario = userDetails.getUsername();
        Empresa empresa = empresaService.buscarEmpresaPeloUsuario(emailUsuario);

        if (empresa == null) {
            return ResponseEntity.status(404).build();
        }

        Publicacao novoPost = new Publicacao();
        novoPost.setLegenda(post.getCaption());
        novoPost.setUrlMidia(post.getImageUrl());
        novoPost.setEmpresa(empresa);
        novoPost.setCreated_at(LocalDateTime.now());
        novoPost.setIdReturned(post.getIdReturned());

        OffsetDateTime dataAgendamento = post.getAgendamento();
        if (dataAgendamento != null) {
            Duration delay = Duration.between(LocalDateTime.now(), dataAgendamento.toLocalDateTime());
            if (!delay.isNegative()) {
                return ResponseEntity.status(201).body(createResponseDto(novoPost));
            }
        }

        Integracao integracao = empresa.getIntegracoes().stream()
                .filter(i -> TipoIntegracao.INSTAGRAM.equals(i.getTipo()))
                .findFirst()
                .orElse(null);

        if (integracao == null) {
            return ResponseEntity.status(404).build();
        }
        Long containerId = publicacaoService.criarContainer(integracao, novoPost);
        String postIdReturned = publicacaoService.criarPublicacao(integracao, containerId);
        novoPost.setIdReturned(postIdReturned);
        Publicacao savedPost = publicacaoRepository.save(novoPost);

        return ResponseEntity.status(201).body(createResponseDto(savedPost));
    }

    private PostPublicacaoResponse createResponseDto(Publicacao post) {
        PostPublicacaoResponse responseDto = new PostPublicacaoResponse();
        responseDto.setLegenda(post.getLegenda());
        responseDto.setId(post.getId());
        responseDto.setFkEmpresa(post.getEmpresa().getId());
        return responseDto;
    }

    @GetMapping("/somar-likes-publicacao")
    @Operation(summary = "Somar likes das publicações utilizando Recursão",
            description = "Retorna a soma total de likes de todas as publicações da empresa especificada pelo ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Soma total de likes retornada com sucesso.",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = "150"), // Exemplo de soma total
                                    @ExampleObject(value = "200"), // Outro exemplo
                                    @ExampleObject(value = "300"), // E mais exemplos
                                    @ExampleObject(value = "50"),
                                    @ExampleObject(value = "75"),
                                    @ExampleObject(value = "100")
                            })),
            @ApiResponse(responseCode = "204", description = "Nenhuma publicação encontrada para somar os likes.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"message\": \"Nenhuma publicação encontrada.\" }")))
    })
    public ResponseEntity<Integer> somarLikes() {
        ResponseEntity<List<GetPublicacaoDto>> responseEntity = buscarPosts();
        List<GetPublicacaoDto> posts = responseEntity.getBody();
        if (posts != null && !posts.isEmpty()) {
            int somaLikes = posts.stream().mapToInt(GetPublicacaoDto::getLikeCount).sum();
            return ResponseEntity.ok(somaLikes);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/somar-likes-publicacao-iterativo")
    @Operation(summary = "Somar likes das publicações utilizando Iteração",
            description = "Retorna a soma total de likes de todas as publicações da empresa especificada pelo ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Soma total de likes retornada com sucesso.",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = "150"), // Exemplo de soma total
                                    @ExampleObject(value = "200"), // Outro exemplo
                                    @ExampleObject(value = "300"), // E mais exemplos
                                    @ExampleObject(value = "50"),
                                    @ExampleObject(value = "75"),
                                    @ExampleObject(value = "100")
                            })),
            @ApiResponse(responseCode = "204", description = "Nenhuma publicação encontrada para somar os likes.",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"message\": \"Nenhuma publicação encontrada.\" }")))
    })
    public ResponseEntity<Integer> somarLikesIterativo() {
        ResponseEntity<List<GetPublicacaoDto>> responseEntity = buscarPosts();
        List<GetPublicacaoDto> posts = responseEntity.getBody();
        if (posts != null && !posts.isEmpty()) {
            int somaLikes = 0;
            for (GetPublicacaoDto post : posts) {
                somaLikes += post.getLikeCount();
            }
            return ResponseEntity.ok(somaLikes);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar uma publicação",
            description = "Deleta uma publicação específica pelo ID.")
    public ResponseEntity<Void> deletarPublicacao(@PathVariable UUID id) {
        Optional<Publicacao> publicacao = publicacaoRepository.findById(id);
        if (publicacao.isPresent()) {
            publicacaoRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}