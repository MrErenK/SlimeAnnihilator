package com.mrerenk.slimeAnnihilator;

import com.mrerenk.slimeannihilator.common.SlimeManager;
import com.mrerenk.slimeannihilator.common.SlimeSpawnListener;
import com.mrerenk.slimeannihilator.common.commands.SlimeCommand;
import com.mrerenk.slimeannihilator.common.config.SlimeConfig;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class SlimeAnnihilator extends JavaPlugin {

    private SlimeManager slimeManager;
    private SlimeConfig slimeConfig;

    @Override
    public void onEnable() {
        getLogger().info("SlimeAnnihilator is starting up...");

        // Initialize configuration
        slimeConfig = new SlimeConfig(this);

        // Initialize the slime manager
        slimeManager = new SlimeManager(this, slimeConfig);

        // Register event listeners
        getServer()
            .getPluginManager()
            .registerEvents(new SlimeSpawnListener(slimeManager), this);

        // Register commands (removed adventure parameter)
        SlimeCommand slimeCommand = new SlimeCommand(slimeManager, this);
        Objects.requireNonNull(getCommand("slimes")).setExecutor(slimeCommand);
        Objects.requireNonNull(getCommand("slimes")).setTabCompleter(
            slimeCommand
        );

        // Remove slimes from flat worlds on startup (if enabled)
        if (slimeConfig.isAutoRemoveOnStartup()) {
            getLogger()
                .info(
                    "Auto-remove on startup is enabled. Scheduling world scan..."
                );
            getServer()
                .getScheduler()
                .runTaskLater(
                    this,
                    () -> {
                        getLogger()
                            .info(
                                "Starting flat world scan and slime removal..."
                            );
                        slimeManager.removeSlimesFromFlatWorlds();
                        getLogger()
                            .info(
                                "Auto-removal task completed for flat worlds!"
                            );
                    },
                    20L
                ); // Wait 1 second for worlds to load
        } else {
            getLogger().info("Auto-remove on startup is disabled.");
        }

        getLogger().info("SlimeAnnihilator has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save configuration on shutdown
        if (slimeConfig != null) {
            slimeConfig.saveConfig();
        }
        getLogger().info("SlimeAnnihilator has been disabled!");
    }
}
