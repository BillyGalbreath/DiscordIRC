package net.pl3x.discord.headless;

import net.pl3x.discord.bot.Bot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Headless {
    public Headless(boolean useStorage) {
        Bot bot = new Bot();

        String token;
        String password;

        if (useStorage) {
            bot.getStorage().loadStoredData();
            token = bot.getStorage().getToken();
            password = bot.getStorage().getPassword();
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("");

            System.out.print("  Bot token: ");
            try {
                token = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            System.out.print("  Nickserv password: ");
            try {
                password = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            System.out.println("");
        }

        bot.connect(token, password);
    }
}
