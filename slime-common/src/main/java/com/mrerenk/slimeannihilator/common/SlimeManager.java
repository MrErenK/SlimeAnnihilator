package com.mrerenk.slimeannihilator.common;

import com.mrerenk.slimeannihilator.common.config.SlimeConfig;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.Plugin;

public class SlimeManager {

    private final Plugin plugin;
    private final SlimeConfig config;

    public SlimeManager(Plugin plugin, SlimeConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Checks if a world is a flat world
     */
    public boolean isFlatWorld(World world) {
        // First check if it's manually configured as flat
        if (config.isFlatWorld(world.getName())) {
            debugLog(
                "World '" + world.getName() + "' is manually configured as flat"
            );
            return true;
        }

        // Then try automatic detection
        try {
            // Check generator
            if (world.getGenerator() != null) {
                String generatorName = world
                    .getGenerator()
                    .getClass()
                    .getSimpleName()
                    .toLowerCase();
                if (
                    generatorName.contains("flat") ||
                    generatorName.contains("void")
                ) {
                    debugLog(
                        "World '" +
                        world.getName() +
                        "' detected as flat via generator: " +
                        generatorName
                    );
                    return true;
                }
            }

            // Check world name patterns
            String worldName = world.getName().toLowerCase();
            if (
                worldName.contains("flat") ||
                worldName.contains("creative") ||
                worldName.contains("build")
            ) {
                debugLog(
                    "World '" +
                    world.getName() +
                    "' detected as flat via name pattern"
                );
                return true;
            }
        } catch (Exception e) {
            debugLog(
                "Could not auto-detect if world '" +
                world.getName() +
                "' is flat: " +
                e.getMessage()
            );
        }

        return false;
    }

    /**
     * Checks if a world is exempt from slime management
     */
    public boolean isWorldExempt(World world) {
        return config.isWorldExempt(world.getName());
    }

    /**
     * Removes all slimes from flat worlds (respecting exemptions)
     */
    public void removeSlimesFromFlatWorlds() {
        if (!config.isAutoRemoveOnStartup()) {
            debugLog("Auto-remove on startup is disabled");
            return;
        }

        plugin.getLogger().info("Scanning worlds for flat world detection...");

        // Get all worlds and log them
        var allWorlds = plugin.getServer().getWorlds();
        plugin
            .getLogger()
            .info(
                "Found " +
                allWorlds.size() +
                " worlds to check: " +
                allWorlds.stream().map(WorldInfo::getName).toList()
            );

        var flatWorlds = allWorlds.stream().filter(this::isFlatWorld).toList();

        plugin
            .getLogger()
            .info(
                "Detected " +
                flatWorlds.size() +
                " flat worlds: " +
                flatWorlds.stream().map(WorldInfo::getName).toList()
            );

        var eligibleWorlds = flatWorlds
            .stream()
            .filter(world -> !isWorldExempt(world))
            .toList();

        plugin
            .getLogger()
            .info(
                "Found " +
                eligibleWorlds.size() +
                " eligible flat worlds (after exemptions): " +
                eligibleWorlds.stream().map(WorldInfo::getName).toList()
            );

        if (eligibleWorlds.isEmpty()) {
            plugin
                .getLogger()
                .info("No eligible flat worlds found for slime removal.");
            return;
        }

        eligibleWorlds.forEach(world -> {
            int removed = removeAllSlimes(world);
            if (removed > 0) {
                plugin
                    .getLogger()
                    .info(
                        "Removed " +
                        removed +
                        " slimes from flat world: " +
                        world.getName()
                    );
                plugin
                    .getLogger()
                    .info(
                        "Hello, slimeless flat world '" +
                        world.getName() +
                        "'! Your slime problem has been... flattened!"
                    );
            } else {
                // Even if no slimes were removed, still greet the flat world
                plugin
                    .getLogger()
                    .info(
                        "Hello, already slimeless flat world '" +
                        world.getName() +
                        "'! Staying clean and slime-free!"
                    );
            }
        });

        plugin.getLogger().info("Flat world greeting process completed!");
    }

    /**
     * Removes all slimes from a specific world
     */
    public int removeAllSlimes(World world) {
        List<Entity> entities = world.getEntities();
        int removedCount = 0;

        for (Entity entity : entities) {
            if (entity.getType() == EntityType.SLIME) {
                entity.remove();
                removedCount++;
            }
        }

        debugLog(
            "Removed " + removedCount + " slimes from world: " + world.getName()
        );
        return removedCount;
    }

    /**
     * Removes all slimes from a specific world asynchronously
     */
    public CompletableFuture<Integer> removeAllSlimesAsync(World world) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return plugin
                    .getServer()
                    .getScheduler()
                    .callSyncMethod(plugin, () -> removeAllSlimes(world))
                    .get();
            } catch (Exception e) {
                plugin
                    .getLogger()
                    .severe("Error removing slimes: " + e.getMessage());
                return 0;
            }
        });
    }

    /**
     * Counts slimes in a world
     */
    public int countSlimes(World world) {
        return (int) world
            .getEntities()
            .stream()
            .filter(entity -> entity.getType() == EntityType.SLIME)
            .count();
    }

    /**
     * Disables slime spawning for a world
     */
    public void disableSlimeSpawning(World world) {
        config.addWorldWithSpawningDisabled(world.getName());
        config.saveConfig();
        debugLog("Disabled slime spawning for world: " + world.getName());
    }

    /**
     * Enables slime spawning for a world
     */
    public void enableSlimeSpawning(World world) {
        config.removeWorldWithSpawningDisabled(world.getName());
        config.saveConfig();
        debugLog("Enabled slime spawning for world: " + world.getName());
    }

    /**
     * Checks if slime spawning is disabled for a world (for display purposes)
     * This is different from shouldPreventSpawning which considers spawn reasons
     */
    public boolean isSlimeSpawningDisabled(World world) {
        // Check if spawning is manually disabled
        if (config.isWorldSpawningDisabled(world.getName())) {
            return true;
        }

        // Check if it's a flat world and flat world spawning is disabled for natural spawning
        return (
            isFlatWorld(world) &&
            config.isPreventSpawningInFlatWorlds() &&
            !isWorldExempt(world)
        );
    }

    /**
     * Checks if slime spawning should be prevented based on spawn reason
     */
    public boolean shouldPreventSpawning(
        World world,
        org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason
    ) {
        if (isWorldExempt(world)) {
            return false;
        }

        // Check if spawning is manually disabled for this world (always takes precedence)
        if (config.isWorldSpawningDisabled(world.getName())) {
            debugLog(
                "Preventing slime spawn in " +
                world.getName() +
                " (manually disabled)"
            );
            return true;
        }

        // Check if spawning is manually enabled for this world (overrides global flat world setting)
        if (config.isWorldSpawningEnabled(world.getName())) {
            debugLog(
                "Allowing slime spawn in " +
                world.getName() +
                " (manually enabled, overriding global setting)"
            );
            return false;
        }

        // Check specific spawn reasons first (these can override flat world restrictions)
        switch (reason) {
            case SPAWNER_EGG:
                if (config.isPreventEggSpawning()) {
                    debugLog(
                        "Preventing slime egg spawn in " +
                        world.getName() +
                        " (egg spawning disabled)"
                    );
                    return true;
                }
                // If egg spawning is allowed, don't prevent it even in flat worlds
                debugLog(
                    "Allowing slime egg spawn in " +
                    world.getName() +
                    " (egg spawning enabled)"
                );
                return false;
            case COMMAND:
                if (config.isPreventCommandSpawning()) {
                    debugLog(
                        "Preventing slime command spawn in " +
                        world.getName() +
                        " (command spawning disabled)"
                    );
                    return true;
                }
                // If command spawning is allowed, don't prevent it even in flat worlds
                debugLog(
                    "Allowing slime command spawn in " +
                    world.getName() +
                    " (command spawning enabled)"
                );
                return false;
            case CUSTOM:
                if (config.isPreventCustomSpawning()) {
                    debugLog(
                        "Preventing slime custom spawn in " +
                        world.getName() +
                        " (custom spawning disabled)"
                    );
                    return true;
                }
                // If custom spawning is allowed, don't prevent it even in flat worlds
                debugLog(
                    "Allowing slime custom spawn in " +
                    world.getName() +
                    " (custom spawning enabled)"
                );
                return false;
        }

        // For natural spawning and other reasons, check flat world restrictions
        if (isFlatWorld(world) && config.isPreventSpawningInFlatWorlds()) {
            debugLog(
                "Preventing slime natural spawn in flat world: " +
                world.getName() +
                " (reason: " +
                reason +
                ")"
            );
            return true;
        }

        return false;
    }

    /**
     * Gets slime information for a world
     */
    public SlimeInfo getSlimeInfo(World world) {
        int count = countSlimes(world);
        boolean spawningDisabled = isSlimeSpawningDisabled(world);
        boolean isFlat = isFlatWorld(world);
        boolean isExempt = isWorldExempt(world);

        return new SlimeInfo(
            world.getName(),
            count,
            spawningDisabled,
            isFlat,
            isExempt
        );
    }

    /**
     * Adds a world to the exempt list
     */
    public void addExemptWorld(World world) {
        config.addExemptWorld(world.getName());
        config.saveConfig();
        debugLog("Added world to exempt list: " + world.getName());
    }

    /**
     * Removes a world from the exempt list
     */
    public void removeExemptWorld(World world) {
        config.removeExemptWorld(world.getName());
        config.saveConfig();
        debugLog("Removed world from exempt list: " + world.getName());
    }

    /**
     * Gets the configuration
     */
    public SlimeConfig getConfig() {
        return config;
    }

    /**
     * Logs a debug message if debug mode is enabled
     */
    private void debugLog(String message) {
        if (config.isEnableDebugMessages()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    public static class SlimeInfo {

        private final String worldName;
        private final int slimeCount;
        private final boolean spawningDisabled;
        private final boolean isFlat;
        private final boolean isExempt;

        public SlimeInfo(
            String worldName,
            int slimeCount,
            boolean spawningDisabled,
            boolean isFlat,
            boolean isExempt
        ) {
            this.worldName = worldName;
            this.slimeCount = slimeCount;
            this.spawningDisabled = spawningDisabled;
            this.isFlat = isFlat;
            this.isExempt = isExempt;
        }

        public String getWorldName() {
            return worldName;
        }

        public int getSlimeCount() {
            return slimeCount;
        }

        public boolean isSpawningDisabled() {
            return spawningDisabled;
        }

        public boolean isFlat() {
            return isFlat;
        }

        public boolean isExempt() {
            return isExempt;
        }
    }
}
