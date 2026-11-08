package com.arepalandia.core;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Logros del server anunciados en el chat. */
final class AchievementsModule implements Listener {

    private final ArepaCore plugin;
    private final DataFile data;

    AchievementsModule(ArepaCore plugin) {
        this.plugin = plugin;
        this.data = new DataFile(plugin, "logros.yml");
    }

    void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Horas jugadas: revisa cada minuto.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                long hours = p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L / 3600L;
                for (int h : new int[]{10, 50, 100, 250, 500}) {
                    if (hours >= h) {
                        grant(p, "horas-" + h, "lleva &e" + h + " horas &7jugadas en el server");
                    }
                }
            }
        }, 20L * 60, 20L * 60);
    }

    int count(UUID id) {
        return data.yaml().getStringList(id.toString()).size();
    }

    /** Otorga un logro una sola vez y lo anuncia. Devuelve true si es nuevo. */
    boolean grant(Player p, String id, String text) {
        List<String> got = new ArrayList<>(data.yaml().getStringList(p.getUniqueId().toString()));
        if (got.contains(id)) {
            return false;
        }
        got.add(id);
        data.yaml().set(p.getUniqueId().toString(), got);
        data.save();
        Msg.broadcast("&6&l★ LOGRO &e" + p.getName() + " &7" + text + "&7!");
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Material m = event.getBlock().getType();
        Player p = event.getPlayer();
        if (m == Material.DIAMOND_ORE || m == Material.DEEPSLATE_DIAMOND_ORE) {
            grant(p, "primer-diamante", "encontró sus &bprimeros diamantes");
        } else if (m == Material.ANCIENT_DEBRIS) {
            grant(p, "primer-netherite", "encontró &5escombros ancestrales");
        }
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        World.Environment env = event.getPlayer().getWorld().getEnvironment();
        if (env == World.Environment.NETHER) {
            grant(event.getPlayer(), "nether", "entró al &cNether &7por primera vez");
        } else if (env == World.Environment.THE_END) {
            grant(event.getPlayer(), "end", "llegó al &dEnd");
        }
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (event.getEntity() instanceof EnderDragon) {
            grant(killer, "dragon", "derrotó al &5&lDragón del End");
        } else if (event.getEntity() instanceof Wither) {
            grant(killer, "wither", "derrotó al &8&lWither");
        }
    }
}
