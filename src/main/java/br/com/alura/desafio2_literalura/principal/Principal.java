package br.com.alura.desafio2_literalura.principal;


import br.com.alura.desafio2_literalura.model.Autor;
//import br.com.alura.desafio2_literalura.model.AutorDTO;
import br.com.alura.desafio2_literalura.model.Livro;
import br.com.alura.desafio2_literalura.model.LivroDTO;
import br.com.alura.desafio2_literalura.repository.LivroRepository;
import br.com.alura.desafio2_literalura.service.ConsumoAPI;
import br.com.alura.desafio2_literalura.service.ConverteDados;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Principal {

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private ConsumoAPI consumoAPI;

    @Autowired
    private ConverteDados converteDados;

    private final Scanner leitura = new Scanner(System.in);

    public Principal(LivroRepository livroRepository, ConsumoAPI consumoAPI, ConverteDados converteDados) {
        this.livroRepository = livroRepository;
        this.consumoAPI = consumoAPI;
        this.converteDados = converteDados;
    }

    public void executar() {
        boolean running = true;
        while (running) {
            exibirMenu();
            var opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1 -> buscarLivrosPeloTitulo();
                case 2 -> listarLivrosRegistrados();
                case 3 -> listarAutoresRegistrados();
                case 4 -> listarAutoresVivos();
                case 5 -> listarLivrosPorIdioma();
                case 0 -> {
                    System.out.println("Obrigada por utilizar o Literalura!");
                    running = false;
                }
                default -> System.out.println("Opção inválida. Por favor, insira uma opção válida.");
            }
        }
    }

    private void exibirMenu() {
        System.out.println("""
        *********************************************************

        Digite a opção desejada:
        1- Buscar na API Gutendex e inserir no banco de dados
        2- Listar livros registrados
        3- Listar autores registrados
        4- Listar autores vivos em um determinado ano. *Não aplicável quando data de nascimento ou falecimento desconhecidas*.
        5- Listar livros em um determinado idioma
        0- Sair
            """);
    }

    private void salvarLivros(List<Livro> livros) {
        livros.forEach(livroRepository::save);
    }


    private void buscarLivrosPeloTitulo() {
        String baseURL = "https://gutendex.com/books?search=";

        try {
            System.out.println("Digite o título do livro: ");
            String titulo = leitura.nextLine();
            String endereco = baseURL + titulo.replace(" ", "%20");
            System.out.println("URL da API: " + endereco);

            String jsonResponse = consumoAPI.obterDados(endereco);
            System.out.println("Resposta da API: " + jsonResponse);

            if (jsonResponse.isEmpty()) {
                System.out.println("Resposta da API está vazia.");
                return;
            }

            // Extrai a lista de livros da chave "results"
            JsonNode rootNode = converteDados.getObjectMapper().readTree(jsonResponse);
            JsonNode resultsNode = rootNode.path("results");

            if (resultsNode.isEmpty()) {
                System.out.println("Não foi possível encontrar o livro buscado.");
                return;
            }

            // Converte os resultados da API em objetos LivroDTO
            List<LivroDTO> livrosDTO = converteDados.getObjectMapper()
                    .readerForListOf(LivroDTO.class)
                    .readValue(resultsNode);

            // Remove as duplicatas existentes no banco de dados
            List<Livro> livrosExistentes = livroRepository.findByTitulo(titulo);
            if (!livrosExistentes.isEmpty()) {
                System.out.println("Removendo livros duplicados já existentes no banco de dados...");
                for (Livro livroExistente : livrosExistentes) {
                    livrosDTO.removeIf(livroDTO -> livroExistente.getTitulo().equals(livroDTO.titulo()));
                }
            }

            // Salva os novos livros no banco de dados
            if (!livrosDTO.isEmpty()) {
                System.out.println("Salvando novos livros encontrados...");
                List<Livro> novosLivros = livrosDTO.stream().map(Livro::new).collect(Collectors.toList());
                salvarLivros(novosLivros);
                System.out.println("Livros salvos com sucesso!");
            } else {
                System.out.println("Todos os livros já estão registrados no banco de dados.");
            }

            // Exibe os livros encontrados
            if (!livrosDTO.isEmpty()) {
                System.out.println("Livros encontrados:");
                Set<String> titulosExibidos = new HashSet<>(); // Para controlar títulos já exibidos
                for (LivroDTO livro : livrosDTO) {
                    if (!titulosExibidos.contains(livro.titulo())) {
                        System.out.println(livro);
                        titulosExibidos.add(livro.titulo());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao buscar livros: " + e.getMessage());
        }
    }



    private void listarLivrosRegistrados() {
        List<Livro> livros = livroRepository.findAll();
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro registrado.");
        } else {
            livros.forEach(System.out::println);
        }
    }

    private void listarAutoresRegistrados() {
        List<Livro> livros = livroRepository.findAll();

        if (livros.isEmpty()) {
            System.out.println("Nenhum autor registrado.");
        } else {
            // Usa um Set para armazenar autores já impressos e evitar duplicação.
            Set<String> autoresImpressos = new HashSet<>();

            livros.forEach(livro -> {
                String nomeAutor = livro.getAutor().getAutor();

                // Se o autor ainda não foi impresso, adicione-o e imprima.
                if (autoresImpressos.add(nomeAutor)) {
                    System.out.println(nomeAutor);
                }
            });
        }
    }


    private void listarAutoresVivos() {
        System.out.println("Digite o ano: ");

        try {
            int ano = leitura.nextInt();
            leitura.nextLine(); // Consome a nova linha pendente após a leitura do número.

            Year year = Year.of(ano);

            List<Autor> autores = livroRepository.findAutoresVivosRefinado(year);
            if (autores.isEmpty()) {
                System.out.println("Nenhum autor vivo encontrado.");
            } else {
                System.out.println("Autor(es) nascido(s) no ano de " + ano + ":\n");

                // Usa um Set para garantir que cada autor seja impresso apenas uma vez.
                Set<String> autoresImpressos = new HashSet<>();

                autores.forEach(autor -> {
                    // Adiciona o nome do autor ao Set e verifica se ele foi adicionado com sucesso.
                    // Se 'add' retorna true, significa que o autor não estava no Set, então é a primeira vez que o vemos.
                    if (autoresImpressos.add(autor.getAutor())) {
                        if (Autor.possuiAno(autor.getAnoNascimento()) && Autor.possuiAno(autor.getAnoFalecimento())) {
                            String nomeAutor = autor.getAutor();
                            String anoNascimento = autor.getAnoNascimento().toString();
                            String anoFalecimento = autor.getAnoFalecimento().toString();
                            System.out.println(nomeAutor + " (" + anoNascimento + " - " + anoFalecimento + ")");
                        }
                    }
                });
            }
        } catch (InputMismatchException e) {
            System.out.println("Valor inválido. Por favor, insira um ano em formato numérico.");
            leitura.nextLine();
        }
    }
    private void listarLivrosPorIdioma() {
        System.out.println("""
            Insira o idioma para realizar a busca:
            es - espanhol
            en - inglês
            fr - francês
            pt - português
            de - desconhecido
           """);
        String idioma = leitura.nextLine();

        List<Livro> livros = livroRepository.findByIdioma(idioma);
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro encontrado no idioma informado.");
        } else {
            livros.forEach(livro -> {
                String titulo = livro.getTitulo();
                String autor = livro.getAutor().getAutor();
                String idiomaLivro = livro.getIdioma();

                System.out.println("Título: " + titulo);
                System.out.println("Autor: " + autor);
                System.out.println("Idioma: " + idiomaLivro);
                System.out.println("----------------------------------------");
            });
        }
    }


}
