package net.pl3x.discord.irc;

import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import net.pl3x.discord.bot.Bot;
import net.pl3x.discord.listener.IRCListener;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRC {
    private final Bot bot;
    private final Category category;
    private final String password;
    private final String address;

    private Client client;

    private final Map<TextChannel, Webhook> webhooks = new HashMap<>();

    public IRC(Bot bot, Category category, String password) {
        this.bot = bot;
        this.category = category;
        this.password = password;
        this.address = category.getName();
    }

    public Client getClient() {
        return client;
    }

    public Bot getBot() {
        return bot;
    }

    public String getPassword() {
        return password;
    }

    public String getAddress() {
        return address;
    }

    public void changeNick(String nick) {
        client.setNick(nick);
    }

    public void connect(String nick) {
        disconnect();
        client = Client.builder()
                .realName(nick)
                .nick(nick)
                .user(nick)
                .serverHost(category.getName())
                .buildAndConnect();
        client.getEventManager().registerEventListener(new IRCListener(this));
    }

    public void disconnect() {
        if (client != null) {
            client.getChannels().forEach(c -> {
                TextChannel channel = getChannel(c);
                channel.deleteWebhookById(webhooks.get(channel).getId()).complete();
            });

            client.shutdown("Quit");
        }
    }

    public void joinChannel(TextChannel channel) {
        client.addChannel("#" + channel.getName());
        webhooks.put(channel, channel.createWebhook(category.getName() + "_#" + channel.getName()).complete());
    }

    public void leaveChannel(TextChannel channel) {
        client.removeChannel("#" + channel.getName());
        Webhook webhook = webhooks.remove(channel);
        if (webhook != null) {
            channel.deleteWebhookById(webhook.getId()).complete();
        }
    }

    private TextChannel getChannel(Channel channel) {
        String name = channel.getName().replace("#", "");
        return category.getTextChannels().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findAny().orElse(null);
    }

    public void sendMessage(String channel, String message) {
        client.sendMessage(channel, message);
    }

    public void sendAction(String channel, String message) {
        client.sendCtcpMessage(channel, "ACTION " + message);
    }

    public void sendToDiscord(Channel channel, String username, String message) {
        Webhook webhook = webhooks.get(getChannel(channel));
        if (webhook == null) {
            return;
        }
        message = message
                .replace("\u0002", "") // bold
                .replace("\u001D", "") // italics
                .replaceAll("\u0003(?:[\\d]{1,2}(?:,[\\d]{1,2})?)?", "") // color codes
        ;

        List<String> split = Arrays.asList(message.split(" "));
        for (String word : split) {
            if (word.equalsIgnoreCase(webhook.getGuild().getSelfMember().getEffectiveName())) {
                split.set(split.indexOf(word), webhook.getGuild().getOwner().getAsMention());
            }
        }
        message = String.join(" ", split);

        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setContent(message);
        builder.setUsername(username);

        WebhookClient client = webhook.newClient().build();
        client.send(builder.build());
        client.close();
    }
}
