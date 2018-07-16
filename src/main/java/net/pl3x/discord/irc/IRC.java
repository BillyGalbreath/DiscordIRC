package net.pl3x.discord.irc;

import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import net.engio.mbassy.listener.Handler;
import net.pl3x.discord.bot.Bot;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.command.UserModeCommand;
import org.kitteh.irc.client.library.defaults.element.mode.DefaultUserMode;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.ChannelCtcpEvent;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelKickEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelNoticeEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTargetedMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTargetedNoticeEvent;
import org.kitteh.irc.client.library.event.client.ClientNegotiationCompleteEvent;
import org.kitteh.irc.client.library.event.helper.ServerMessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRC {
    private Bot bot;
    private Client client;
    private Category category;
    private String password;

    private Map<TextChannel, Webhook> webhooks = new HashMap<>();

    public IRC(Bot bot, Category category, String password) {
        this.bot = bot;
        this.category = category;
        this.password = password;
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
        client.getEventManager().registerEventListener(new Listener());
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

    private void sendToDiscord(Channel channel, String username, String message) {
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

    public class Listener {
        @Handler
        public void on(ClientNegotiationCompleteEvent event) {
            if (password != null && !password.isEmpty()) {
                client.sendMessage("NickServ", "identify " + password);
                bot.getStorage().setPassword(password);
            }

            new UserModeCommand(client).add(true, new DefaultUserMode(client, 'g')).execute();
        }

        @Handler
        public void onJoinChannel(PrivateNoticeEvent event) {
            System.out.println(event.getActor().getNick() + ": " + event.getMessage());
        }

        @Handler
        public void onJoinChannel(ChannelJoinEvent event) {
            if (event.getClient().isUser(event.getActor())) {
                return; // ignore echo
            }
            sendToDiscord(event.getChannel(), "* " + event.getActor().getNick(), "_has joined_");
        }

        @Handler
        public void onPartChannel(ChannelPartEvent event) {
            if (event.getClient().isUser(event.getActor())) {
                return; // ignore echo
            }
            sendToDiscord(event.getChannel(), "* " + event.getActor().getNick(), "_has left (" + event.getMessage() + ")_");
        }

        @Handler
        public void onKickChannel(ChannelKickEvent event) {
            if (event.getClient().isUser(event.getUser())) {
                return; // ignore echo
            }
            sendToDiscord(event.getChannel(), "* " + event.getUser().getNick(), "_was kicked (" + event.getMessage() + ")_");
        }

        @Handler
        public void onUserQuitServer(UserQuitEvent event) {
            if (event.getClient().isUser(event.getActor())) {
                return; // ignore echo
            }
            event.getActor().getChannels().forEach(name ->
                    client.getChannel(name).ifPresent(channel ->
                            sendToDiscord(channel, "* " + event.getActor().getNick(), "_has quit (" + event.getMessage() + ")_")));
        }

        @Handler
        public void onNoticeChannel(ChannelNoticeEvent event) {
            if (event.getClient().isUser(event.getActor())) {
                return; // ignore echo
            }
            sendToDiscord(event.getChannel(), "* " + event.getActor().getNick(), "_**" + event.getMessage() + "**_");
        }

        @Handler
        public void onTargetNoticeChannel(ChannelTargetedNoticeEvent event) {
            if (event.getClient().isUser(event.getActor())) {
                return; // ignore echo
            }
            sendToDiscord(event.getChannel(), "* " + event.getActor().getNick(), "_**" + event.getMessage() + "**_");
        }

        @Handler
        public void onMessageReceived(ChannelMessageEvent event) {
            if (event.getClient().isUser(event.getActor())) {
                return; // ignore echo
            }
            sendToDiscord(event.getChannel(), event.getActor().getNick(), event.getMessage());
        }

        @Handler
        public void onTargetMessageReceived(ChannelTargetedMessageEvent event) {
            if (event.getClient().isUser(event.getActor())) {
                return; // ignore echo
            }
            sendToDiscord(event.getChannel(), event.getActor().getNick(), event.getMessage());
        }

        @Handler
        public void onActionReceived(ChannelCtcpEvent event) {
            if (event.getClient().isUser(event.getActor())) {
                return; // ignore echo
            }
            if (event.getMessage().startsWith("ACTION ")) {
                sendToDiscord(event.getChannel(), "* " + event.getActor().getNick(), "_**" + event.getMessage().substring(7) + "**_");
            }
        }

        // test for any missing/unknown events
        @Handler
        public void onMessageReceived(ServerMessageEvent event) {
            //System.out.println("RAW: " + event);
        }
    }
}
