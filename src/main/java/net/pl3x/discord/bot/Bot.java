package net.pl3x.discord.bot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.ResumedEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.StatusChangeEvent;
import net.dv8tion.jda.core.events.channel.category.CategoryCreateEvent;
import net.dv8tion.jda.core.events.channel.category.CategoryDeleteEvent;
import net.dv8tion.jda.core.events.channel.category.update.CategoryUpdateNameEvent;
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdateNameEvent;
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdateParentEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.pl3x.discord.data.Storage;
import net.pl3x.discord.irc.IRC;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;

public class Bot {
    private JDA discord;
    private Map<Category, IRC> networks = new HashMap<>();
    private String password;
    private Storage storage = new Storage();

    public Storage getStorage() {
        return storage;
    }

    public void connect(String token, String password) {
        this.password = password;
        disconnect();
        try {
            discord = new JDABuilder(AccountType.BOT)
                    .setAudioEnabled(false)
                    .setAutoReconnect(true)
                    .setBulkDeleteSplittingEnabled(false)
                    .setToken(token)
                    .addEventListener(new Listener())
                    .buildAsync();
        } catch (LoginException e) {
            discord = null;
        }
    }

    public void disconnect() {
        networks.values().forEach(IRC::disconnect);
        networks.clear();
        if (discord != null) {
            discord.shutdownNow();
            discord = null;
        }
    }

    private String getNick() {
        return discord.getGuilds().get(0).getSelfMember().getEffectiveName();
    }

    private IRC getNetwork(TextChannel channel) {
        return getNetwork(channel.getParent());
    }

    private IRC getNetwork(Category category) {
        return category != null ? networks.get(category) : null;
    }

    public class Listener extends ListenerAdapter {
        @Override
        public void onReady(ReadyEvent event) {
            storage.setToken(discord.getToken());

            discord.getGuilds().forEach(guild -> {
                guild.getWebhooks().complete().stream()
                        .filter(webhook -> webhook.getName().contains("#"))
                        .forEach(webhook -> webhook.delete().reason("Purge").queue());

                guild.getCategories().forEach(category -> {
                    IRC network = new IRC(Bot.this, category, password);
                    network.connect(getNick());
                    networks.put(category, network);
                    category.getTextChannels().forEach(network::joinChannel);
                });
            });

            System.out.println("Bot connected to Discord!");
        }

        public void onResume(ResumedEvent event) {
            System.out.println("Bot resumed connection to Discord!");
        }

        public void onReconnect(ReconnectedEvent event) {
            System.out.println("Bot re-connected to Discord!");
        }

        public void onDisconnect(DisconnectEvent event) {
            System.out.println("Bot disconnected from Discord!");
        }

        public void onShutdown(ShutdownEvent event) {
            System.out.println("Bot has shutdown!");
        }

        public void onStatusChange(StatusChangeEvent event) {
            System.out.println("Bot status changed: " + event.getOldStatus() + " -> " + event.getNewStatus());
        }

        public void onException(ExceptionEvent event) {
            // TODO ?
        }

        public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
            if (event.isWebhookMessage()) {
                return;
            }
            Message message = event.getMessage();
            IRC network = getNetwork(message.getCategory());
            if (network != null) {
                String content = message.getContentDisplay();
                if (content.startsWith("_") && content.endsWith("_")) {
                    network.sendAction("#" + message.getTextChannel().getName(), content.substring(1, content.length() - 1));
                } else {
                    network.sendMessage("#" + message.getTextChannel().getName(), content);
                }
            }
        }

        public void onTextChannelCreate(TextChannelCreateEvent event) {
            IRC network = getNetwork(event.getChannel());
            if (network != null) {
                network.joinChannel(event.getChannel());
            }
        }

        public void onTextChannelDelete(TextChannelDeleteEvent event) {
            IRC network = getNetwork(event.getChannel());
            if (network != null) {
                network.leaveChannel(event.getChannel());
            }
        }

        public void onTextChannelUpdateName(TextChannelUpdateNameEvent event) {
            // TODO part old channel and join new channel
        }

        public void onTextChannelUpdateParent(TextChannelUpdateParentEvent event) {
            // TODO part channel from old network and join channel in new network
        }

        public void onCategoryCreate(CategoryCreateEvent event) {
            IRC network = new IRC(Bot.this, event.getCategory(), password);
            network.connect(getNick());
            networks.put(event.getCategory(), network);
        }

        public void onCategoryDelete(CategoryDeleteEvent event) {
            IRC network = networks.remove(event.getCategory());
            if (network != null) {
                network.disconnect();
            }
        }

        public void onCategoryUpdateName(CategoryUpdateNameEvent event) {
            // TODO disconnect from old network name and connect to new network name
        }

        public void onGuildMemberNickChange(GuildMemberNickChangeEvent event) {
            if (event.getUser().equals(discord.getSelfUser())) {
                networks.values().forEach(network -> network.changeNick(getNick()));
            }
        }
    }
}
