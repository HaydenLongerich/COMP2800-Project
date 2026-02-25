package org.example.comp2800_sas;

import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class Comp2800SasApplication {

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}
