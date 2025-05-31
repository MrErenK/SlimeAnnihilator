package com.mrerenk.slimeannihilator.common;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class SlimeSpawnListener implements Listener {

    private final SlimeManager slimeManager;

    public SlimeSpawnListener(SlimeManager slimeManager) {
        this.slimeManager = slimeManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.SLIME) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        if (
            slimeManager.shouldPreventSpawning(
                event.getLocation().getWorld(),
                event.getSpawnReason()
            )
        ) {
            event.setCancelled(true);
        }
    }
}
