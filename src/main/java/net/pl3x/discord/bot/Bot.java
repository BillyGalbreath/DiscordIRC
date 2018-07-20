package net.pl3x.discord.bot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.TextChannel;
import net.pl3x.discord.data.Storage;
import net.pl3x.discord.irc.IRC;
import net.pl3x.discord.listener.DiscordListener;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;

public class Bot {
    private JDA client;
    private final Map<Category, IRC> networks = new HashMap<>();
    private final Storage storage = new Storage();
    private String password;

    public JDA getClient() {
        return client;
    }

    public Map<Category, IRC> getNetworks() {
        return networks;
    }

    public Storage getStorage() {
        return storage;
    }

    public String getPassword() {
        return password;
    }

    public void connect(String token, String password) {
        this.password = password;
        disconnect();
        try {
            client = new JDABuilder(AccountType.BOT)
                    .setAudioEnabled(false)
                    .setAutoReconnect(true)
                    .setBulkDeleteSplittingEnabled(false)
                    .setToken(token)
                    .addEventListener(new DiscordListener(this))
                    .buildAsync();
        } catch (LoginException e) {
            client = null;
        }
    }

    public void disconnect() {
        networks.values().forEach(IRC::disconnect);
        networks.clear();
        if (client != null) {
            client.shutdownNow();
            client = null;
        }
    }

    public String getNick() {
        return client.getGuilds().get(0).getSelfMember().getEffectiveName();
    }

    public IRC getNetwork(TextChannel channel) {
        return getNetwork(channel.getParent());
    }

    public IRC getNetwork(Category category) {
        return category != null ? networks.get(category) : null;
    }
}
