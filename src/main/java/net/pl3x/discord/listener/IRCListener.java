package net.pl3x.discord.listener;

import net.engio.mbassy.listener.Handler;
import net.pl3x.discord.irc.IRC;
import org.kitteh.irc.client.library.command.UserModeCommand;
import org.kitteh.irc.client.library.defaults.element.mode.DefaultUserMode;
import org.kitteh.irc.client.library.event.channel.ChannelCtcpEvent;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelKickEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelNoticeEvent;
import org.kitteh.irc.client.library.event.channel.ChannelPartEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTargetedMessageEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTargetedNoticeEvent;
import org.kitteh.irc.client.library.event.client.ClientConnectionEndedEvent;
import org.kitteh.irc.client.library.event.client.ClientNegotiationCompleteEvent;
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;

public class IRCListener {
    private final IRC network;

    public IRCListener(IRC network) {
        this.network = network;
    }

    @Handler
    public void onClientConnected(ClientNegotiationCompleteEvent event) {
        System.out.println("IRC: Connected to " + network.getAddress());

        if (network.getPassword() != null && !network.getPassword().isEmpty()) {
            network.getClient().sendMessage("NickServ", "identify " + network.getPassword());
            network.getBot().getStorage().setPassword(network.getPassword());
        }

        new UserModeCommand(network.getClient()).add(true, new DefaultUserMode(network.getClient(), 'g')).execute();
    }

    @Handler
    public void onClientDisconnect(ClientConnectionEndedEvent event) {
        System.out.println("IRC: Disconnected from " + network.getAddress());
    }

    @Handler
    public void onPrivateNotice(PrivateNoticeEvent event) {
        System.out.println(event.getActor().getNick() + ": " + event.getMessage());
    }

    @Handler
    public void onJoinChannel(ChannelJoinEvent event) {
        if (event.getClient().isUser(event.getActor())) {
            System.out.println("IRC: Joined " + event.getChannel().getName() + " on " + network.getAddress());
            return; // ignore echo
        }
        network.sendToDiscord(event.getChannel(), "* " + event.getActor().getNick(), "_has joined_");
    }

    @Handler
    public void onPartChannel(ChannelPartEvent event) {
        if (event.getClient().isUser(event.getActor())) {
            System.out.println("IRC: Parted " + event.getChannel().getName() + " on " + network.getAddress());
            return; // ignore echo
        }
        network.sendToDiscord(event.getChannel(), "* " + event.getActor().getNick(), "_has left (" + event.getMessage() + ")_");
    }

    @Handler
    public void onKickChannel(ChannelKickEvent event) {
        if (event.getClient().isUser(event.getUser())) {
            System.out.println("IRC: Kicked from " + event.getChannel().getName() + " on " + network.getAddress() + " for " + event.getMessage());
            return; // ignore echo
        }
        network.sendToDiscord(event.getChannel(), "* " + event.getUser().getNick(), "_was kicked (" + event.getMessage() + ")_");
    }

    @Handler
    public void onUserQuitServer(UserQuitEvent event) {
        if (event.getClient().isUser(event.getActor())) {
            return; // ignore echo
        }
        event.getActor().getChannels().forEach(name ->
                network.getClient().getChannel(name).ifPresent(channel ->
                        network.sendToDiscord(channel, "* " + event.getActor().getNick(), "_has quit (" + event.getMessage() + ")_")));
    }

    @Handler
    public void onNoticeChannel(ChannelNoticeEvent event) {
        if (event.getClient().isUser(event.getActor())) {
            return; // ignore echo
        }
        network.sendToDiscord(event.getChannel(), "* " + event.getActor().getNick(), "_**" + event.getMessage() + "**_");
    }

    @Handler
    public void onTargetNoticeChannel(ChannelTargetedNoticeEvent event) {
        if (event.getClient().isUser(event.getActor())) {
            return; // ignore echo
        }
        network.sendToDiscord(event.getChannel(), "* " + event.getActor().getNick(), "_**" + event.getMessage() + "**_");
    }

    @Handler
    public void onMessageReceived(ChannelMessageEvent event) {
        if (event.getClient().isUser(event.getActor())) {
            return; // ignore echo
        }
        network.sendToDiscord(event.getChannel(), event.getActor().getNick(), event.getMessage());
    }

    @Handler
    public void onTargetMessageReceived(ChannelTargetedMessageEvent event) {
        if (event.getClient().isUser(event.getActor())) {
            return; // ignore echo
        }
        network.sendToDiscord(event.getChannel(), event.getActor().getNick(), event.getMessage());
    }

    @Handler
    public void onActionReceived(ChannelCtcpEvent event) {
        if (event.getClient().isUser(event.getActor())) {
            return; // ignore echo
        }
        if (event.getMessage().startsWith("ACTION ")) {
            network.sendToDiscord(event.getChannel(), "* " + event.getActor().getNick(), "_**" + event.getMessage().substring(7) + "**_");
        }
    }
}
