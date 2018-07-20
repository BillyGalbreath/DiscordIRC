package net.pl3x.discord;

import javafx.application.Application;
import net.pl3x.discord.gui.Window;
import net.pl3x.discord.headless.Headless;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            if (args[0].equalsIgnoreCase("--help")) {
                System.out.println("  --help           Display this help menu");
                System.out.println("  --nogui          Run bot in headless mode");
                System.out.println("  --use-storage    Use last known token and password");
                return;
            }

            Set<String> arguments = Arrays.stream(args)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            if (arguments.contains("--nogui")) {
                new Headless(arguments.contains("--use-storage"));
                return;
            }

            System.out.println("Unknown argument. Please use --help for valid arguments");
            return;
        }

        // run bot in JavaFX window
        Application.launch(Window.class, args);
    }
}
