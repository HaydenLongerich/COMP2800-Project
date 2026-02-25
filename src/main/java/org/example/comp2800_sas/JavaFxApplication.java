package org.example.comp2800_sas;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {
    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // Initializes the Spring context
        this.context = new SpringApplicationBuilder(Comp2800SasApplication.class).run();
    }

    @Override
    public void start(Stage primaryStage) {
        // This is where your JavaFX UI code begins
        primaryStage.setTitle("COMP2800 Group Project");
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Closes Spring when the window is closed
        this.context.close();
        Platform.exit();
    }
}