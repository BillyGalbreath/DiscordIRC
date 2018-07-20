package net.pl3x.discord.listener;

import net.dv8tion.jda.core.entities.Message;
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
import net.pl3x.discord.bot.Bot;
import net.pl3x.discord.irc.IRC;

public class DiscordListener extends ListenerAdapter {
    private final Bot bot;

    public DiscordListener(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Discord: Connected");

        bot.getStorage().setToken(bot.getClient().getToken());

        bot.getClient().getGuilds().forEach(guild -> {
            guild.getWebhooks().complete().stream()
                    .filter(webhook -> webhook.getName().contains("#"))
                    .forEach(webhook -> webhook.delete().reason("Purge").queue());

            guild.getCategories().forEach(category -> {
                IRC network = new IRC(bot, category, bot.getPassword());
                network.connect(bot.getNick());
                bot.getNetworks().put(category, network);
                category.getTextChannels().forEach(network::joinChannel);
            });
        });
    }

    public void onResume(ResumedEvent event) {
        System.out.println("Discord: Resumed connection");
    }

    public void onReconnect(ReconnectedEvent event) {
        System.out.println("Discord: Re-connected");
    }

    public void onDisconnect(DisconnectEvent event) {
        System.out.println("Discord: Disconnected");
    }

    public void onShutdown(ShutdownEvent event) {
        System.out.println("Discord: Shutdown");
    }

    public void onStatusChange(StatusChangeEvent event) {
        System.out.println("Discord: Status changed: " + event.getOldStatus() + " -> " + event.getNewStatus());
    }

    public void onException(ExceptionEvent event) {
        System.out.println("Discord: Exception: ");
        System.out.println(event);
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.isWebhookMessage()) {
            return;
        }
        Message message = event.getMessage();
        IRC network = bot.getNetwork(message.getCategory());
        if (network != null) {
            String content = message.getContentDisplay();
            if (content.startsWith("_") && content.endsWith("_") ||
                    content.startsWith("*") && content.endsWith("*")) {
                network.sendAction("#" + message.getTextChannel().getName(), content.substring(1, content.length() - 1));
            } else {
                network.sendMessage("#" + message.getTextChannel().getName(), content);
            }
        }
    }

    public void onTextChannelCreate(TextChannelCreateEvent event) {
        System.out.println("Discord: Channel created: " + event.getChannel().getName());
        IRC network = bot.getNetwork(event.getChannel());
        if (network != null) {
            network.joinChannel(event.getChannel());
        }
    }

    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        System.out.println("Discord: Channel deleted: " + event.getChannel().getName());
        IRC network = bot.getNetwork(event.getChannel());
        if (network != null) {
            network.leaveChannel(event.getChannel());
        }
    }

    public void onTextChannelUpdateName(TextChannelUpdateNameEvent event) {
        System.out.println("Discord: Channel name change: " + event.getOldName() + " -> " + event.getNewName());
        IRC network = bot.getNetworks().get(event.getChannel().getParent());
        if (network != null) {
            network.getClient().removeChannel("#" + event.getOldName());
            network.getClient().addChannel("#" + event.getNewName());
        }
    }

    public void onTextChannelUpdateParent(TextChannelUpdateParentEvent event) {
        System.out.println("Discord: Channel moved categories: (" + event.getChannel().getName() + ") " + event.getOldParent().getName() + " -> " + event.getNewParent().getName());
        IRC network = bot.getNetworks().get(event.getOldParent());
        if (network != null) {
            network.leaveChannel(event.getChannel());
        }
        network = bot.getNetworks().get(event.getNewParent());
        if (network != null) {
            network.joinChannel(event.getChannel());
        }
    }

    public void onCategoryCreate(CategoryCreateEvent event) {
        System.out.println("Discord: Category created: " + event.getCategory().getName());
        IRC network = new IRC(bot, event.getCategory(), bot.getPassword());
        network.connect(bot.getNick());
        bot.getNetworks().put(event.getCategory(), network);
    }

    public void onCategoryDelete(CategoryDeleteEvent event) {
        System.out.println("Discord: Category deleted: " + event.getCategory().getName());
        IRC network = bot.getNetworks().remove(event.getCategory());
        if (network != null) {
            network.disconnect();
        }
    }

    public void onCategoryUpdateName(CategoryUpdateNameEvent event) {
        System.out.println("Discord: Category name change: " + event.getOldName() + " -> " + event.getNewName());
        IRC network = bot.getNetworks().remove(event.getCategory());
        if (network != null) {
            network.disconnect();
        }
        network = new IRC(bot, event.getCategory(), bot.getPassword());
        network.connect(bot.getNick());
        bot.getNetworks().put(event.getCategory(), network);
        event.getCategory().getTextChannels().forEach(network::joinChannel);
    }

    public void onGuildMemberNickChange(GuildMemberNickChangeEvent event) {
        System.out.println("Discord: Nickname change: " + event.getPrevNick() + " -> " + event.getNewNick());
        if (event.getUser().equals(bot.getClient().getSelfUser())) {
            bot.getNetworks().values().forEach(network -> network.changeNick(bot.getNick()));
        }
    }
}
