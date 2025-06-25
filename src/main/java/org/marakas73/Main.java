package org.marakas73;

import org.marakas73.run.ConsoleFileScannerRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main implements CommandLineRunner {
    private final ConsoleFileScannerRunner consoleFileScannerRunner;

    public Main(ConsoleFileScannerRunner consoleFileScannerRunner) {
        this.consoleFileScannerRunner = consoleFileScannerRunner;
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) {
        consoleFileScannerRunner.run();
    }
}