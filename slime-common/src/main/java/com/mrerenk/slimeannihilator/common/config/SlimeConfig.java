package com.mrerenk.slimeannihilator.common.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class SlimeConfig {

    private final Plugin plugin;
    private FileConfiguration config;

    // Default values
    private boolean autoRemoveOnStartup = true;
    private boolean preventSpawningInFlatWorlds = true;
    private boolean requireConfirmationForNonFlatWorlds = true;
    private int confirmationTimeoutSeconds = 30;
    private boolean enableDebugMessages = false;
    private Set<String> flatWorlds = new HashSet<>();
    private Set<String> exemptWorlds = new HashSet<>();
    private Set<String> worldsWithSpawningDisabled = new HashSet<>();
    private final Set<String> worldsWithSpawningEnabled = new HashSet<>();
    private boolean preventEggSpawning = false;
    private boolean preventCommandSpawning = false;
    private boolean preventCustomSpawning = false;

    public SlimeConfig(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load configuration values
        autoRemoveOnStartup = config.getBoolean("auto-remove-on-startup", true);
        preventSpawningInFlatWorlds = config.getBoolean(
            "prevent-spawning-in-flat-worlds",
            true
        );
        requireConfirmationForNonFlatWorlds = config.getBoolean(
            "require-confirmation-for-non-flat-worlds",
            true
        );
        confirmationTimeoutSeconds = config.getInt(
            "confirmation-timeout-seconds",
            30
        );
        enableDebugMessages = config.getBoolean("debug-messages", false);
        preventEggSpawning = config.getBoolean("prevent-egg-spawning", false);
        preventCommandSpawning = config.getBoolean(
            "prevent-command-spawning",
            false
        );
        preventCustomSpawning = config.getBoolean(
            "prevent-custom-spawning",
            false
        );

        // Load flat worlds
        List<String> flatWorldsList = config.getStringList("flat-worlds");
        flatWorlds = new HashSet<>(flatWorldsList);

        // Load exempt worlds
        List<String> exemptWorldsList = config.getStringList("exempt-worlds");
        exemptWorlds = new HashSet<>(exemptWorldsList);

        // Load worlds with spawning disabled
        List<String> disabledWorldsList = config.getStringList(
            "worlds-with-spawning-disabled"
        );
        worldsWithSpawningDisabled = new HashSet<>(disabledWorldsList);

        if (enableDebugMessages) {
            plugin.getLogger().info("Configuration loaded:");
            plugin
                .getLogger()
                .info("  Auto remove on startup: " + autoRemoveOnStartup);
            plugin
                .getLogger()
                .info(
                    "  Prevent spawning in flat worlds: " +
                    preventSpawningInFlatWorlds
                );
            plugin
                .getLogger()
                .info("  Prevent egg spawning: " + preventEggSpawning);
            plugin
                .getLogger()
                .info("  Prevent command spawning: " + preventCommandSpawning);
            plugin
                .getLogger()
                .info("  Prevent custom spawning: " + preventCustomSpawning);
            plugin.getLogger().info("  Flat worlds: " + flatWorlds);
            plugin.getLogger().info("  Exempt worlds: " + exemptWorlds);
            plugin
                .getLogger()
                .info(
                    "  Worlds with spawning disabled: " +
                    worldsWithSpawningDisabled
                );
        }
    }

    public void saveConfig() {
        config.set("auto-remove-on-startup", autoRemoveOnStartup);
        config.set(
            "prevent-spawning-in-flat-worlds",
            preventSpawningInFlatWorlds
        );
        config.set(
            "require-confirmation-for-non-flat-worlds",
            requireConfirmationForNonFlatWorlds
        );
        config.set("confirmation-timeout-seconds", confirmationTimeoutSeconds);
        config.set("debug-messages", enableDebugMessages);
        config.set("prevent-egg-spawning", preventEggSpawning);
        config.set("prevent-command-spawning", preventCommandSpawning);
        config.set("prevent-custom-spawning", preventCustomSpawning);
        config.set("flat-worlds", flatWorlds.toArray(new String[0]));
        config.set("exempt-worlds", exemptWorlds.toArray(new String[0]));
        config.set(
            "worlds-with-spawning-disabled",
            worldsWithSpawningDisabled.toArray(new String[0])
        );

        plugin.saveConfig();
    }

    // Getters
    public boolean isAutoRemoveOnStartup() {
        return autoRemoveOnStartup;
    }

    public boolean isPreventSpawningInFlatWorlds() {
        return preventSpawningInFlatWorlds;
    }

    public boolean isRequireConfirmationForNonFlatWorlds() {
        return requireConfirmationForNonFlatWorlds;
    }

    public int getConfirmationTimeoutSeconds() {
        return confirmationTimeoutSeconds;
    }

    public boolean isEnableDebugMessages() {
        return enableDebugMessages;
    }

    public boolean isPreventEggSpawning() {
        return preventEggSpawning;
    }

    public boolean isPreventCommandSpawning() {
        return preventCommandSpawning;
    }

    public boolean isPreventCustomSpawning() {
        return preventCustomSpawning;
    }

    public long getConfirmationTimeoutMillis() {
        return confirmationTimeoutSeconds * 1000L;
    }

    // World management methods
    public void addExemptWorld(String worldName) {
        exemptWorlds.add(worldName);
    }

    public void removeExemptWorld(String worldName) {
        exemptWorlds.remove(worldName);
    }

    public boolean isWorldExempt(String worldName) {
        return exemptWorlds.contains(worldName);
    }

    public void addWorldWithSpawningDisabled(String worldName) {
        worldsWithSpawningDisabled.add(worldName);
        // Remove from enabled list if present
        worldsWithSpawningEnabled.remove(worldName);
    }

    public void removeWorldWithSpawningDisabled(String worldName) {
        worldsWithSpawningDisabled.remove(worldName);
    }

    public boolean isWorldSpawningDisabled(String worldName) {
        return worldsWithSpawningDisabled.contains(worldName);
    }

    public boolean isWorldSpawningEnabled(String worldName) {
        return worldsWithSpawningEnabled.contains(worldName);
    }

    // Flat world management methods
    public void addFlatWorld(String worldName) {
        flatWorlds.add(worldName);
    }

    public void removeFlatWorld(String worldName) {
        flatWorlds.remove(worldName);
    }

    public boolean isFlatWorld(String worldName) {
        return flatWorlds.contains(worldName);
    }

    public Set<String> getFlatWorlds() {
        return new HashSet<>(flatWorlds);
    }
}
