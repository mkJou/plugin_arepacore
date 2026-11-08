package com.arepalandia.core;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Teletransportes con cuenta atrás: se cancelan si el jugador se mueve o recibe daño.
 * También recuerda el último lugar (antes de teletransportarse o donde murió) para /back.
 */
final class Teleports implements Listener {

    private final ArepaCore plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final Map<UUID, Location> back = new HashMap<>();

    private record Pending(Location from, BukkitTask task) {
    }

    Teleports(ArepaCore plugin) {
        this.plugin = plugin;
    }

    /** Teletransporta con la cuenta atrás configurada. {@code label} es lo que se muestra ("a tu casa"). */
    void teleport(Player player, Location target, String label) {
        cancel(player, null);
        int seconds = plugin.getConfig().getInt("teletransporte.espera-segundos", 3);
        if (seconds <= 0 || player.hasPermission("arepa.bypass.espera")) {
            doTeleport(player, target, label);
            return;
        }
        Msg.send(player, "&7Teletransportando " + label + " en &e" + seconds + "s&7. No te muevas.");
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pending.remove(player.getUniqueId());
            if (player.isOnline()) {
                doTeleport(player, target, label);
            }
        }, seconds * 20L);
        pending.put(player.getUniqueId(), new Pending(player.getLocation(), task));
    }

    private void doTeleport(Player player, Location target, String label) {
        back.put(player.getUniqueId(), player.getLocation());
        player.teleportAsync(target).thenAccept(ok -> {
            if (ok) {
                player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);
                Msg.send(player, "&aTeletransportado " + label + ".");
            } else {
                Msg.send(player, "&cNo se pudo teletransportar.");
            }
        });
    }

    Location backLocation(Player player) {
        return back.get(player.getUniqueId());
    }

    private void cancel(Player player, String reason) {
        Pending p = pending.remove(player.getUniqueId());
        if (p != null) {
            p.task().cancel();
            if (reason != null) {
                Msg.send(player, "&cTeletransporte cancelado: " + reason + ".");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Pending p = pending.get(event.getPlayer().getUniqueId());
        if (p == null || !event.hasChangedBlock()) {
            return;
        }
        cancel(event.getPlayer(), "te moviste");
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && pending.containsKey(player.getUniqueId())) {
            cancel(player, "recibiste daño");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        cancel(player, null);
        back.put(player.getUniqueId(), player.getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer(), null);
    }
}
