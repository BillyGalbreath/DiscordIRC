package net.pl3x.discord.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.pl3x.discord.bot.Bot;

public class Window extends Application {
    private Stage stage;
    private Bot bot;

    public Window() {
        bot = new Bot();
        bot.getStorage().loadStoredData();
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;

        stage.setTitle("DiscordIRC");
        stage.setOnCloseRequest(event -> {
            event.consume();
            handleClose();
        });

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/scene.fxml"));
        Parent root = fxmlLoader.load();

        Controller controller = fxmlLoader.getController();
        controller.setBot(bot);

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/style.css");

        stage.setScene(scene);
        stage.show();

        if (controller.token.getText() != null && !controller.token.getText().isEmpty()) {
            controller.connect.requestFocus();
        }
    }

    private void handleClose() {
        bot.disconnect();
        stage.close();
    }
}
