package com.arepalandia.core;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Projectile;

import java.util.List;
import java.util.Map;
import org.bukkit.entity.EntityType;
import java.util.concurrent.ThreadLocalRandom;

/** Mensajes de muerte graciosos en español + contador de muertes. */
final class DeathModule implements Listener {

    private final ArepaCore plugin;

    DeathModule(ArepaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("muertes.activado", true)) {
            return;
        }
        Player p = event.getPlayer();
        String key = "otro";
        String killer = "";
        EntityDamageEvent last = p.getLastDamageCause();
        if (p.getKiller() != null) {
            key = "jugador";
            killer = p.getKiller().getName();
        } else if (last != null) {
            if (last instanceof EntityDamageByEntityEvent byEntity) {
                Entity d = byEntity.getDamager();
                if (d instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
                    d = shooter;
                }
                killer = nombre(d);
                key = d instanceof Player ? "jugador" : "mob";
                if (last.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                    key = "explosion";
                }
            } else {
                key = switch (last.getCause()) {
                    case FALL -> "caida";
                    case LAVA -> "lava";
                    case FIRE, FIRE_TICK, HOT_FLOOR -> "fuego";
                    case DROWNING -> "ahogado";
                    case BLOCK_EXPLOSION -> "explosion";
                    case VOID -> "vacio";
                    case STARVATION -> "hambre";
                    case FREEZE -> "congelado";
                    default -> "otro";
                };
            }
        }
        List<String> options = plugin.getConfig().getStringList("muertes.mensajes." + key);
        if (options.isEmpty()) {
            options = plugin.getConfig().getStringList("muertes.mensajes.otro");
        }
        if (!options.isEmpty()) {
            String msg = options.get(ThreadLocalRandom.current().nextInt(options.size()))
                    .replace("{player}", p.getName()).replace("{killer}", killer);
            event.deathMessage(Msg.parse("&c☠ &7" + msg));
        }
        // Un tick después la estadística ya está actualizada.
        Bukkit.getScheduler().runTask(plugin, () -> {
            int deaths = p.getStatistic(Statistic.DEATHS);
            Msg.broadcast("&8   (" + p.getName() + " lleva &c" + deaths + "&8 " + (deaths == 1 ? "muerte" : "muertes") + ")");
        });
    }

    private static final Map<EntityType, String> NOMBRES = Map.ofEntries(
            Map.entry(EntityType.ZOMBIE, "un Zombie"), Map.entry(EntityType.SKELETON, "un Esqueleto"),
            Map.entry(EntityType.CREEPER, "un Creeper"), Map.entry(EntityType.SPIDER, "una Araña"),
            Map.entry(EntityType.CAVE_SPIDER, "una Araña de cueva"), Map.entry(EntityType.ENDERMAN, "un Enderman"),
            Map.entry(EntityType.WITCH, "una Bruja"), Map.entry(EntityType.DROWNED, "un Ahogado"),
            Map.entry(EntityType.HUSK, "un Zombie momificado"), Map.entry(EntityType.STRAY, "un Esqueleto glacial"),
            Map.entry(EntityType.PHANTOM, "un Phantom"), Map.entry(EntityType.SLIME, "un Slime"),
            Map.entry(EntityType.BLAZE, "un Blaze"), Map.entry(EntityType.GHAST, "un Ghast"),
            Map.entry(EntityType.PIGLIN, "un Piglin"), Map.entry(EntityType.PIGLIN_BRUTE, "un Piglin bruto"),
            Map.entry(EntityType.ZOMBIFIED_PIGLIN, "un Piglin zombificado"), Map.entry(EntityType.HOGLIN, "un Hoglin"),
            Map.entry(EntityType.WITHER_SKELETON, "un Esqueleto del Wither"), Map.entry(EntityType.WARDEN, "el Warden"),
            Map.entry(EntityType.ENDER_DRAGON, "el Dragón"), Map.entry(EntityType.WITHER, "el Wither"),
            Map.entry(EntityType.PILLAGER, "un Saqueador"), Map.entry(EntityType.VINDICATOR, "un Vindicador"),
            Map.entry(EntityType.RAVAGER, "un Devastador"), Map.entry(EntityType.IRON_GOLEM, "un Gólem de hierro"),
            Map.entry(EntityType.WOLF, "un Lobo"), Map.entry(EntityType.BEE, "una Abeja"),
            Map.entry(EntityType.GUARDIAN, "un Guardián"), Map.entry(EntityType.SILVERFISH, "un Lepisma"),
            Map.entry(EntityType.BREEZE, "un Breeze"), Map.entry(EntityType.BOGGED, "un Esqueleto pantanoso"));

    private static String nombre(Entity e) {
        if (e instanceof Player pl) {
            return pl.getName();
        }
        if (e.customName() != null) {
            return e.getName();
        }
        return NOMBRES.getOrDefault(e.getType(), e.getName());
    }
}
