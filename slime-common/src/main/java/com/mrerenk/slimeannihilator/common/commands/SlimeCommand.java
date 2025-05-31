package com.mrerenk.slimeannihilator.common.commands;

import com.mrerenk.slimeannihilator.common.SlimeManager;
import com.mrerenk.slimeannihilator.common.config.SlimeConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class SlimeCommand implements CommandExecutor, TabCompleter {

    private final SlimeManager slimeManager;
    private final SlimeConfig config;
    private final Plugin plugin;
    private final Map<String, Long> confirmationRequests = new HashMap<>();

    public SlimeCommand(SlimeManager slimeManager, Plugin plugin) {
        this.slimeManager = slimeManager;
        this.config = slimeManager.getConfig();
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        String[] args
    ) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "nuke":
                handleNuke(sender, args);
                break;
            case "disable":
                handleDisableSpawning(sender, args);
                break;
            case "enable":
                handleEnableSpawning(sender, args);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "confirm":
                handleConfirm(sender);
                break;
            case "exempt":
                handleExempt(sender, args);
                break;
            case "unexempt":
                handleUnexempt(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "config":
                handleConfig(sender, args);
                break;
            case "setflat":
                handleSetFlat(sender, args);
                break;
            case "unsetflat":
                handleUnsetFlat(sender, args);
                break;
            case "listflat":
                handleListFlat(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void handleNuke(CommandSender sender, String[] args) {
        World targetWorld = getTargetWorld(sender, args);
        if (targetWorld == null) return;

        // Check if world is exempt
        if (slimeManager.isWorldExempt(targetWorld)) {
            sender.sendMessage(
                ChatColor.YELLOW +
                "World '" +
                ChatColor.WHITE +
                targetWorld.getName() +
                ChatColor.YELLOW +
                "' is exempt from slime management!"
            );
            sender.sendMessage(
                ChatColor.YELLOW +
                "Use " +
                ChatColor.GOLD +
                "/slimes unexempt " +
                targetWorld.getName() +
                ChatColor.YELLOW +
                " first if you want to manage slimes in this world."
            );
            return;
        }

        final World world = targetWorld;

        // Check if world is flat - if not, require confirmation (if enabled)
        if (
            !slimeManager.isFlatWorld(world) &&
            config.isRequireConfirmationForNonFlatWorlds()
        ) {
            String confirmKey = sender.getName() + ":" + world.getName();

            if (
                !confirmationRequests.containsKey(confirmKey) ||
                System.currentTimeMillis() -
                confirmationRequests.get(confirmKey) >
                config.getConfirmationTimeoutMillis()
            ) {
                confirmationRequests.put(
                    confirmKey,
                    System.currentTimeMillis()
                );
                sender.sendMessage(
                    ChatColor.YELLOW +
                    "Warning: World '" +
                    ChatColor.WHITE +
                    world.getName() +
                    ChatColor.YELLOW +
                    "' is not a flat world!"
                );
                sender.sendMessage(
                    ChatColor.YELLOW +
                    "Are you sure you want to remove all slimes?"
                );
                sender.sendMessage(
                    ChatColor.YELLOW +
                    "Type " +
                    ChatColor.GOLD +
                    "/slimes confirm" +
                    ChatColor.YELLOW +
                    " within " +
                    config.getConfirmationTimeoutSeconds() +
                    " seconds to proceed."
                );
                return;
            }
        }

        int slimeCount = slimeManager.countSlimes(world);
        if (slimeCount == 0) {
            sender.sendMessage(
                ChatColor.GREEN +
                "No slimes found in world '" +
                ChatColor.WHITE +
                world.getName() +
                ChatColor.GREEN +
                "'!"
            );
            return;
        }

        sender.sendMessage(
            ChatColor.YELLOW +
            "Removing " +
            slimeCount +
            " slimes from world '" +
            ChatColor.WHITE +
            world.getName() +
            ChatColor.YELLOW +
            "'..."
        );

        CompletableFuture<Integer> future = slimeManager.removeAllSlimesAsync(
            world
        );
        future.thenAccept(removedCount ->
            sender.sendMessage(
                ChatColor.GREEN +
                "Successfully removed " +
                removedCount +
                " slimes from world '" +
                ChatColor.WHITE +
                world.getName() +
                ChatColor.GREEN +
                "'!"
            )
        );
    }

    private void handleConfirm(CommandSender sender) {
        String playerName = sender.getName();

        String matchingKey = confirmationRequests
            .keySet()
            .stream()
            .filter(key -> key.startsWith(playerName + ":"))
            .findFirst()
            .orElse(null);

        if (matchingKey == null) {
            sender.sendMessage(
                ChatColor.RED + "No pending confirmation request found!"
            );
            return;
        }

        long requestTime = confirmationRequests.get(matchingKey);
        if (
            System.currentTimeMillis() - requestTime >
            config.getConfirmationTimeoutMillis()
        ) {
            confirmationRequests.remove(matchingKey);
            sender.sendMessage(ChatColor.RED + "Confirmation request expired!");
            return;
        }

        String worldName = matchingKey.split(":")[1];
        World world = plugin.getServer().getWorld(worldName);

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World no longer exists!");
            confirmationRequests.remove(matchingKey);
            return;
        }

        confirmationRequests.remove(matchingKey);

        int removedCount = slimeManager.removeAllSlimes(world);
        sender.sendMessage(
            ChatColor.GREEN +
            "Confirmed! Removed " +
            removedCount +
            " slimes from world '" +
            ChatColor.WHITE +
            world.getName() +
            ChatColor.GREEN +
            "'!"
        );
    }

    private void handleDisableSpawning(CommandSender sender, String[] args) {
        World world = getTargetWorld(sender, args);
        if (world == null) return;

        if (slimeManager.isSlimeSpawningDisabled(world)) {
            sender.sendMessage(
                ChatColor.YELLOW +
                "Slime spawning is already disabled in world '" +
                ChatColor.WHITE +
                world.getName() +
                ChatColor.YELLOW +
                "'!"
            );
            return;
        }

        slimeManager.disableSlimeSpawning(world);
        sender.sendMessage(
            ChatColor.GREEN +
            "Slime spawning disabled in world '" +
            ChatColor.WHITE +
            world.getName() +
            ChatColor.GREEN +
            "'!"
        );
    }

    private void handleEnableSpawning(CommandSender sender, String[] args) {
        World world = getTargetWorld(sender, args);
        if (world == null) return;

        if (!slimeManager.isSlimeSpawningDisabled(world)) {
            sender.sendMessage(
                ChatColor.YELLOW +
                "Slime spawning is already enabled in world '" +
                ChatColor.WHITE +
                world.getName() +
                ChatColor.YELLOW +
                "'!"
            );
            return;
        }

        slimeManager.enableSlimeSpawning(world);
        sender.sendMessage(
            ChatColor.GREEN +
            "Slime spawning enabled in world '" +
            ChatColor.WHITE +
            world.getName() +
            ChatColor.GREEN +
            "'!"
        );
    }

    private void handleInfo(CommandSender sender, String[] args) {
        World world = getTargetWorld(sender, args);
        if (world == null) return;

        SlimeManager.SlimeInfo info = slimeManager.getSlimeInfo(world);

        sender.sendMessage(
            ChatColor.GOLD +
            "=== Slime Info for '" +
            ChatColor.WHITE +
            info.getWorldName() +
            ChatColor.GOLD +
            "' ==="
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "Slime Count: " +
            ChatColor.WHITE +
            info.getSlimeCount()
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "World Type: " +
            ChatColor.WHITE +
            (info.isFlat() ? "FLAT" : "NORMAL")
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "Exempt: " +
            (info.isExempt() ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO")
        );

        // Only show spawning configuration if the world is NOT exempt
        if (!info.isExempt()) {
            // Check if each spawn method would actually be prevented in this world
            boolean eggPrevented = slimeManager.shouldPreventSpawning(
                world,
                org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
            );
            boolean commandPrevented = slimeManager.shouldPreventSpawning(
                world,
                org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.COMMAND
            );
            boolean customPrevented = slimeManager.shouldPreventSpawning(
                world,
                org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM
            );

            sender.sendMessage(
                ChatColor.YELLOW +
                "Natural Spawning: " +
                (info.isSpawningDisabled()
                        ? ChatColor.RED + "DISABLED"
                        : ChatColor.GREEN + "ENABLED")
            );
            sender.sendMessage(
                ChatColor.YELLOW +
                "Egg Spawning: " +
                (eggPrevented
                        ? ChatColor.RED + "DISABLED"
                        : ChatColor.GREEN + "ENABLED")
            );
            sender.sendMessage(
                ChatColor.YELLOW +
                "Command Spawning: " +
                (commandPrevented
                        ? ChatColor.RED + "DISABLED"
                        : ChatColor.GREEN + "ENABLED")
            );
            sender.sendMessage(
                ChatColor.YELLOW +
                "Custom Spawning: " +
                (customPrevented
                        ? ChatColor.RED + "DISABLED"
                        : ChatColor.GREEN + "ENABLED")
            );

            if (
                info.isFlat() &&
                config.isPreventSpawningInFlatWorlds() &&
                !info.isExempt()
            ) {
                sender.sendMessage(
                    ChatColor.GRAY +
                    "Note: Natural spawning blocked due to flat world restrictions"
                );
            }
        } else {
            sender.sendMessage(
                ChatColor.GREEN +
                "This world is exempt from all slime management."
            );
            sender.sendMessage(
                ChatColor.GREEN +
                "All slime spawning methods are allowed normally."
            );
        }
    }

    private void handleExempt(CommandSender sender, String[] args) {
        World world = getTargetWorld(sender, args);
        if (world == null) return;

        if (slimeManager.isWorldExempt(world)) {
            sender.sendMessage(
                ChatColor.YELLOW +
                "World '" +
                ChatColor.WHITE +
                world.getName() +
                ChatColor.YELLOW +
                "' is already exempt!"
            );
            return;
        }

        slimeManager.addExemptWorld(world);
        sender.sendMessage(
            ChatColor.GREEN +
            "World '" +
            ChatColor.WHITE +
            world.getName() +
            ChatColor.GREEN +
            "' is now exempt from slime management!"
        );
    }

    private void handleUnexempt(CommandSender sender, String[] args) {
        World world = getTargetWorld(sender, args);
        if (world == null) return;

        if (!slimeManager.isWorldExempt(world)) {
            sender.sendMessage(
                ChatColor.YELLOW +
                "World '" +
                ChatColor.WHITE +
                world.getName() +
                ChatColor.YELLOW +
                "' is not exempt!"
            );
            return;
        }

        slimeManager.removeExemptWorld(world);
        sender.sendMessage(
            ChatColor.GREEN +
            "World '" +
            ChatColor.WHITE +
            world.getName() +
            ChatColor.GREEN +
            "' is no longer exempt from slime management!"
        );
    }

    private void handleReload(CommandSender sender) {
        config.loadConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
    }

    private void handleConfig(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Show current config
            sender.sendMessage(
                ChatColor.GOLD + "=== SlimeAnnihilator Configuration ==="
            );
            sender.sendMessage(
                ChatColor.YELLOW +
                "Auto remove on startup: " +
                ChatColor.WHITE +
                config.isAutoRemoveOnStartup()
            );
            sender.sendMessage(
                ChatColor.YELLOW +
                "Prevent spawning in flat worlds: " +
                ChatColor.WHITE +
                config.isPreventSpawningInFlatWorlds()
            );
            sender.sendMessage(
                ChatColor.YELLOW +
                "Require confirmation for non-flat: " +
                ChatColor.WHITE +
                config.isRequireConfirmationForNonFlatWorlds()
            );
            sender.sendMessage(
                ChatColor.YELLOW +
                "Prevent egg spawning: " +
                ChatColor.WHITE +
                config.isPreventEggSpawning()
            );
            sender.sendMessage(
                ChatColor.YELLOW +
                "Prevent command spawning: " +
                ChatColor.WHITE +
                config.isPreventCommandSpawning()
            );
            sender.sendMessage(
                ChatColor.YELLOW +
                "Prevent custom spawning: " +
                ChatColor.WHITE +
                config.isPreventCustomSpawning()
            );
            sender.sendMessage(
                ChatColor.YELLOW +
                "Debug messages: " +
                ChatColor.WHITE +
                config.isEnableDebugMessages()
            );
            return;
        }

        sender.sendMessage(
            ChatColor.RED +
            "Config modification via commands not implemented yet. Please edit config.yml directly and use /slimes reload."
        );
    }

    private void handleSetFlat(CommandSender sender, String[] args) {
        World world = getTargetWorld(sender, args);
        if (world == null) return;

        if (config.isFlatWorld(world.getName())) {
            sender.sendMessage(
                ChatColor.YELLOW +
                "World '" +
                ChatColor.WHITE +
                world.getName() +
                ChatColor.YELLOW +
                "' is already marked as flat!"
            );
            return;
        }

        config.addFlatWorld(world.getName());
        config.saveConfig();
        sender.sendMessage(
            ChatColor.GREEN +
            "World '" +
            ChatColor.WHITE +
            world.getName() +
            ChatColor.GREEN +
            "' is now marked as a flat world!"
        );
    }

    private void handleUnsetFlat(CommandSender sender, String[] args) {
        World world = getTargetWorld(sender, args);
        if (world == null) return;

        if (!config.isFlatWorld(world.getName())) {
            sender.sendMessage(
                ChatColor.YELLOW +
                "World '" +
                ChatColor.WHITE +
                world.getName() +
                ChatColor.YELLOW +
                "' is not marked as flat!"
            );
            return;
        }

        config.removeFlatWorld(world.getName());
        config.saveConfig();
        sender.sendMessage(
            ChatColor.GREEN +
            "World '" +
            ChatColor.WHITE +
            world.getName() +
            ChatColor.GREEN +
            "' is no longer marked as a flat world!"
        );
    }

    private void handleListFlat(CommandSender sender) {
        Set<String> flatWorlds = config.getFlatWorlds();

        sender.sendMessage(ChatColor.GOLD + "=== Flat Worlds ===");

        if (flatWorlds.isEmpty()) {
            sender.sendMessage(
                ChatColor.YELLOW + "No worlds are manually marked as flat."
            );
        } else {
            sender.sendMessage(
                ChatColor.YELLOW + "Manually configured flat worlds:"
            );
            for (String worldName : flatWorlds) {
                sender.sendMessage(ChatColor.WHITE + "  - " + worldName);
            }
        }

        // Also show auto-detected flat worlds
        sender.sendMessage(ChatColor.YELLOW + "Auto-detected flat worlds:");
        boolean foundAny = false;
        for (World world : plugin.getServer().getWorlds()) {
            if (
                !config.isFlatWorld(world.getName()) &&
                slimeManager.isFlatWorld(world)
            ) {
                sender.sendMessage(
                    ChatColor.GRAY +
                    "  - " +
                    world.getName() +
                    " (auto-detected)"
                );
                foundAny = true;
            }
        }
        if (!foundAny) {
            sender.sendMessage(
                ChatColor.GRAY + "  No auto-detected flat worlds found."
            );
        }
    }

    private World getTargetWorld(CommandSender sender, String[] args) {
        if (args.length > 1) {
            World world = plugin.getServer().getWorld(args[1]);
            if (world == null) {
                sender.sendMessage(
                    ChatColor.RED + "World '" + args[1] + "' not found!"
                );
                return null;
            }
            return world;
        } else if (sender instanceof Player) {
            return ((Player) sender).getWorld();
        } else {
            sender.sendMessage(
                ChatColor.RED +
                "You must specify a world when running from console!"
            );
            return null;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(
            ChatColor.GOLD + "=== Slime Annihilator Commands ==="
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes nuke [world] " +
            ChatColor.WHITE +
            "- Remove all slimes from a world"
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes disable [world] " +
            ChatColor.WHITE +
            "- Disable slime spawning"
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes enable [world] " +
            ChatColor.WHITE +
            "- Enable slime spawning"
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes info [world] " +
            ChatColor.WHITE +
            "- Show slime information"
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes exempt [world] " +
            ChatColor.WHITE +
            "- Exempt world from slime management"
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes unexempt [world] " +
            ChatColor.WHITE +
            "- Remove world exemption"
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes confirm " +
            ChatColor.WHITE +
            "- Confirm pending action"
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes reload " +
            ChatColor.WHITE +
            "- Reload configuration"
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes config " +
            ChatColor.WHITE +
            "- Show current configuration"
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes setflat [world] " +
            ChatColor.WHITE +
            "- Mark world as flat"
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes unsetflat [world] " +
            ChatColor.WHITE +
            "- Unmark world as flat"
        );
        sender.sendMessage(
            ChatColor.YELLOW +
            "/slimes listflat " +
            ChatColor.WHITE +
            "- List all flat worlds"
        );
    }

    @Override
    public List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        String[] args
    ) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList(
                "nuke",
                "disable",
                "enable",
                "info",
                "exempt",
                "unexempt",
                "confirm",
                "reload",
                "config",
                "setflat",
                "unsetflat",
                "listflat"
            );
            subcommands
                .stream()
                .filter(sub ->
                    sub.toLowerCase().startsWith(args[0].toLowerCase())
                )
                .forEach(completions::add);
        } else if (
            args.length == 2 &&
            !args[0].equalsIgnoreCase("confirm") &&
            !args[0].equalsIgnoreCase("reload") &&
            !args[0].equalsIgnoreCase("config") &&
            !args[0].equalsIgnoreCase("listflat")
        ) {
            plugin
                .getServer()
                .getWorlds()
                .stream()
                .map(World::getName)
                .filter(name ->
                    name.toLowerCase().startsWith(args[1].toLowerCase())
                )
                .forEach(completions::add);
        }

        return completions;
    }
}
