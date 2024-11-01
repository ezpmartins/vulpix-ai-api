package com.vulpix.api.Controller;

import com.vulpix.api.Utils.Enum.TipoIntegracao;
import com.vulpix.api.Dto.Publicacao.GetPublicacaoDto;
import com.vulpix.api.Dto.Publicacao.PostPublicacaoDto;
import com.vulpix.api.Dto.Publicacao.PostPublicacaoResponse;
import com.vulpix.api.Entity.Empresa;
import com.vulpix.api.Entity.Integracao;
import com.vulpix.api.Entity.Publicacao;
import com.vulpix.api.Repository.EmpresaRepository;
import com.vulpix.api.Repository.PublicacaoRepository;
import com.vulpix.api.Services.Integracoes.Graph.PublicacaoService;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
public class PublicacaoController {

    private PublicacaoService publicacaoService;

    @Autowired
    private PublicacaoRepository publicacaoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @GetMapping("/{empresaId}")
    @Operation(summary = "Buscar posts por empresa",
            description = "Retorna uma lista de publicações associadas a uma empresa especificada pelo ID.")
    public ResponseEntity<List<GetPublicacaoDto>> buscarPosts(@PathVariable UUID empresaId) {
        return publicacaoService.buscarPosts(empresaId);
    }

    @PostMapping
    @Operation(summary = "Criar um novo post",
            description = "Cria um novo post para a empresa informada. O post deve incluir a legenda e a URL da mídia.")
    public ResponseEntity<PostPublicacaoResponse> criarPost(@RequestBody PostPublicacaoDto post) {
        Optional<Empresa> empresa = empresaRepository.findById(post.getFkEmpresa());

        if (empresa.isEmpty()) {
            return ResponseEntity.status(404).build();
        }

        Publicacao novoPost = new Publicacao();
        novoPost.setLegenda(post.getCaption());
        novoPost.setUrlMidia(post.getImageUrl());
        novoPost.setEmpresa(empresa.get());
        novoPost.setCreated_at(LocalDateTime.now());
        novoPost.setIdReturned(post.getIdReturned());

        OffsetDateTime dataAgendamento = post.getAgendamento();
        if (dataAgendamento != null) {
            Duration delay = Duration.between(LocalDateTime.now(), dataAgendamento.toLocalDateTime());
            if (!delay.isNegative()) {
                return ResponseEntity.status(201).body(createResponseDto(novoPost));
            }
        }

        Integracao integracao = empresa.get().getIntegracoes().stream()
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

    @GetMapping("/somar-likes-publicacao/{empresaId}")
    @Operation(summary = "Somar likes das publicações utilizando Recursão",
            description = "Retorna a soma total de likes de todas as publicações da empresa especificada pelo ID.")
    public ResponseEntity<Integer> somarLikes(@PathVariable UUID empresaId) {
        ResponseEntity<List<GetPublicacaoDto>> responseEntity = buscarPosts(empresaId);
        List<GetPublicacaoDto> posts = responseEntity.getBody();
        if (posts != null && !posts.isEmpty()) {
            int somaTotalLikes = somarLikePosts(posts, 0);
            return ResponseEntity.ok(somaTotalLikes);
        } else {
            return ResponseEntity.status(204).build();
        }
    }

    private int somarLikePosts(List<GetPublicacaoDto> posts, int indice) {
        if (indice == posts.size()) {
            return 0;
        }
        return posts.get(indice).getLikeCount() + somarLikePosts(posts, indice + 1);
    }

    @GetMapping("/buscar-por-data/{empresaId}/{dataPublicacao}")
    @Operation(summary = "Buscar publicações por data utilizando Busca Binária",
            description = "Busca uma publicação específica pela data informada.")
    public ResponseEntity<GetPublicacaoDto> buscarPorData(
            @PathVariable UUID empresaId,
            @PathVariable String dataPublicacao) {
        try {
            OffsetDateTime dataBusca = OffsetDateTime.parse(dataPublicacao);
            List<GetPublicacaoDto> posts = buscarPosts(empresaId).getBody();

            if (posts == null || posts.isEmpty()) return ResponseEntity.noContent().build();
            posts.sort(Comparator.comparing(GetPublicacaoDto::getDataPublicacao));

            return posts.stream()
                    .filter(post -> post.getDataPublicacao().isEqual(dataBusca))
                    .findFirst()
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/csv/{empresaId}")
    @Operation(summary = "Exportar publicações para CSV",
            description = "Exporta as publicações da empresa para um arquivo CSV.")
    public ResponseEntity<InputStreamResource> exportarPublicacoesCSV(@PathVariable UUID empresaId) {
        List<GetPublicacaoDto> posts = buscarPosts(empresaId).getBody();

        if (posts == null || posts.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        String arquivo = "publicacao.csv";
        try (OutputStream file = new FileOutputStream(arquivo);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file))) {

            writer.write("ID,Legenda,Tipo Mídia,URL Mídia,Data Publicação,Likes");
            writer.newLine();

            for (GetPublicacaoDto post : posts) {
                writer.write(String.format("%s,%s,%s,%s,%s,%d%n",
                        post.getId(),
                        post.getLegenda(),
                        post.getTipoMidia(),
                        post.getUrlMidia(),
                        post.getDataPublicacao() != null ? post.getDataPublicacao().toString() : "",
                        post.getLikeCount() != null ? post.getLikeCount() : 0));
            }

            InputStreamResource resource = new InputStreamResource(new FileInputStream(arquivo));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + arquivo)
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(resource);

        } catch (IOException e) {
            System.err.println("Erro ao exportar os dados: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}
