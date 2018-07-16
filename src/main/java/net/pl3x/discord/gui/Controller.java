package net.pl3x.discord.gui;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import net.pl3x.discord.bot.Bot;

public class Controller {
    private Bot bot;

    public TextField token;
    public PasswordField password;

    public Button connect;
    public Button disconnect;

    public void connect() {
        if (bot != null) {
            bot.connect(token.getText(), password.getText());
        }
    }

    public void disconnect() {
        if (bot != null) {
            bot.disconnect();
        }
    }

    void setBot(Bot bot) {
        this.bot = bot;
        token.setText(bot.getStorage().getToken());
        password.setText(bot.getStorage().getPassword());
    }
}
