package de.xxmiltenxx.plugin.commands;

import de.xxmiltenxx.plugin.database.HomeDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HomeCommand implements CommandExecutor {
    public final HomeDatabase homeDatabase;

    public HomeCommand(HomeDatabase homeDatabase) {
        this.homeDatabase = homeDatabase;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String labelLower = label.toLowerCase();

        // Behandlung des /setmaxhomes-Befehls
        if (labelLower.equals("setmaxhomes")) {
            if (args.length != 1) {
                sender.sendMessage("Usage: /setmaxhomes <zahl>");
                return true;
            }
            int newMax;
            try {
                newMax = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage("Bitte gib eine gültige Zahl ein!");
                return true;
            }
            if (!(sender instanceof Player)) {
                if (Bukkit.getOnlinePlayers().size() == 1) {
                    Player target = Bukkit.getOnlinePlayers().iterator().next();
                    homeDatabase.setMaxHomes(target, newMax);
                    sender.sendMessage("Setze maximale Homes für " + target.getName() + " auf " + newMax);
                } else {
                    sender.sendMessage("Wenn du den Befehl von der Konsole ausführst, muss genau ein Spieler online sein.");
                }
                return true;
            } else {
                Player player = (Player) sender;
                if (!player.isOp()) {
                    player.sendMessage("Du hast keine Berechtigung, diesen Befehl zu nutzen.");
                    return true;
                }
                homeDatabase.setMaxHomes(player, newMax);
                player.sendMessage("Deine maximale Anzahl an Homes wurde auf " + newMax + " gesetzt!");
                return true;
            }
        }

        // Alle anderen Befehle dürfen nur von Spielern ausgeführt werden
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen!");
            return true;
        }
        Player player = (Player) sender;

        // Behandlung von /listhomes, das keine Argumente benötigt
        if (labelLower.equals("listhomes")) {
            listHomes(player);
            return true;
        }

        // Für die restlichen Befehle (sethome, home, delhome) wird mindestens ein Argument benötigt
        if (args.length == 0) {
            player.sendMessage("Nutze /home <name>, /sethome <name> oder /delhome <name>");
            return true;
        }
        String homeName = args[0];

        // Überprüfen, ob der Spieler die maximale Anzahl an Home-Punkten erreicht hat
        int maxHomes = homeDatabase.getMaxHomes(player);
        if (homeDatabase.getHomeCount(player) >= maxHomes && !homeDatabase.homeExists(player, homeName)) {
            player.sendMessage("Du kannst nur " + maxHomes + " Homes setzen!");
            return true;
        }

        switch (labelLower) {
            case "sethome":
                homeDatabase.setHome(player, homeName, player.getLocation());
                player.sendMessage("Home " + homeName + " wurde gespeichert!");
                break;
            case "home":
                Location home = homeDatabase.getHome(player, homeName);
                if (home != null) {
                    player.teleport(home);
                    player.sendMessage("Du wurdest zu deinem Home " + homeName + " teleportiert!");
                } else {
                    player.sendMessage("Dieses Home existiert nicht!");
                }
                break;
            case "delhome":
                homeDatabase.deleteHome(player, homeName);
                player.sendMessage("Home " + homeName + " wurde gelöscht!");
                break;
            default:
                player.sendMessage("Unbekannter Befehl!");
                break;
        }
        return true;
    }

    // Methode zum Auflisten der Home-Punkte eines Spielers
    private void listHomes(Player player) {
        List<String> homes = homeDatabase.getHomes(player);
        if (homes.isEmpty()) {
            player.sendMessage("Du hast noch keine Homes gesetzt.");
        } else {
            player.sendMessage("Deine Homes:");
            for (String home : homes) {
                player.sendMessage("- " + home);
            }
        }
    }
}