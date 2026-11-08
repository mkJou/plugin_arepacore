package com.arepalandia.core;

import org.bukkit.GameMode;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Talar el árbol entero: agachado + hacha. Solo si hay hojas naturales cerca (para no
 * destrozar casas de madera). Gasta durabilidad como si rompieras cada tronco y se detiene
 * antes de romper el hacha. Respeta protecciones porque usa Player#breakBlock.
 */
final class TimberListener implements Listener {

    private final ArepaCore plugin;
    private boolean breaking;

    TimberListener(ArepaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (breaking || !plugin.getConfig().getBoolean("talar.activado", true)) {
            return;
        }
        Player p = event.getPlayer();
        Block start = event.getBlock();
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (p.getGameMode() == GameMode.CREATIVE
                || !Tag.LOGS.isTagged(start.getType())
                || !tool.getType().name().endsWith("_AXE")
                || (plugin.getConfig().getBoolean("talar.requiere-agacharse", true) && !p.isSneaking())) {
            return;
        }
        int max = plugin.getConfig().getInt("talar.maximo-troncos", 200);
        List<Block> logs = new ArrayList<>();
        int naturalLeaves = 0;
        Set<Block> seen = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && logs.size() < max) {
            Block b = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Block n = b.getRelative(dx, dy, dz);
                        if (!seen.add(n)) {
                            continue;
                        }
                        if (Tag.LOGS.isTagged(n.getType())) {
                            logs.add(n);
                            queue.add(n);
                        } else if (n.getBlockData() instanceof Leaves leaves && !leaves.isPersistent()) {
                            naturalLeaves++;
                        }
                    }
                }
            }
        }
        if (logs.isEmpty() || naturalLeaves < 3) {
            return;
        }
        breaking = true;
        try {
            for (Block log : logs) {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (!hand.getType().name().endsWith("_AXE")) {
                    break;
                }
                if (hand.getItemMeta() instanceof Damageable d
                        && hand.getType().getMaxDurability() - d.getDamage() <= 2) {
                    Msg.send(p, "&eTu hacha está a punto de romperse; dejé de talar.");
                    break;
                }
                p.breakBlock(log);
            }
        } finally {
            breaking = false;
        }
    }
}
