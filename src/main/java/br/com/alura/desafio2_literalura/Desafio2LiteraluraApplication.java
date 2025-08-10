package br.com.alura.desafio2_literalura;

import br.com.alura.desafio2_literalura.principal.Principal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;



@SpringBootApplication
public class Desafio2LiteraluraApplication implements CommandLineRunner {

	//Adicionei
	@Autowired
	private Principal principal;

	public Desafio2LiteraluraApplication(Principal principal) {
		this.principal = principal;
	}

	public static void main(String[] args) {

		SpringApplication.run(Desafio2LiteraluraApplication.class, args);
	}

	//Adicionei
	@Override
	public void run(String... args) throws Exception {
		principal.executar();
	}


}


