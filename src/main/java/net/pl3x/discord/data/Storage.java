package net.pl3x.discord.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Storage {
    private final File propertiesFile = new File("DiscordIRC.properties");
    private Properties properties = new Properties();

    public String getToken() {
        return properties.getProperty("token");
    }

    public void setToken(String token) {
        properties.setProperty("token", token.replace("Bot ", ""));
        storeData();
    }

    public String getPassword() {
        return properties.getProperty("password");
    }

    public void setPassword(String password) {
        properties.setProperty("password", password);
        storeData();
    }

    private void storeData() {
        try {
            FileOutputStream out = new FileOutputStream(propertiesFile);
            properties.store(out, "DiscordIRC");
            out.close();
        } catch (IOException ignore) {
        }
    }

    public void loadStoredData() {
        try {
            FileInputStream in = new FileInputStream(propertiesFile);
            properties = new Properties();
            properties.load(in);
            in.close();
        } catch (IOException ignore) {
        }
    }
}
