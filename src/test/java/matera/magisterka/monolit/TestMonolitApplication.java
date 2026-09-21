package matera.magisterka.monolit;

import org.springframework.boot.SpringApplication;

public class TestMonolitApplication {

    public static void main(String[] args) {
        SpringApplication.from(MonolitApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
