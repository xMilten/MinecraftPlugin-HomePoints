package de.xxmiltenxx.plugin;

import de.xxmiltenxx.plugin.commands.HomeCommand;
import de.xxmiltenxx.plugin.database.HomeDatabase;
import org.bukkit.plugin.java.JavaPlugin;

public class TutorialPlugin extends JavaPlugin {
    private HomeDatabase homeDatabase;

    @Override
    public void onEnable() {
        homeDatabase = new HomeDatabase();

        getCommand("sethome").setExecutor(new HomeCommand(homeDatabase));
        getCommand("home").setExecutor(new HomeCommand(homeDatabase));
        getCommand("listhomes").setExecutor(new HomeCommand(homeDatabase));
        getCommand("delhome").setExecutor(new HomeCommand(homeDatabase));
        getCommand("setmaxhomes").setExecutor(new HomeCommand(homeDatabase));

        getLogger().info("TutorialPlugin wurde aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TutorialPlugin wurde deaktiviert!");
    }
}