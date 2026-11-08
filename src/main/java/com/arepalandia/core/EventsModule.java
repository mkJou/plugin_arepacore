package com.arepalandia.core;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Eventos:
 * - XP doble: automático los fines de semana (configurable) o a mano con /arepa evento xp on|off.
 * - Noche de sangre: al anochecer, con cierta probabilidad. Doble de monstruos y botín doble.
 */
final class EventsModule implements Listener {

    private final ArepaCore plugin;
    private Boolean xpManual; // null = automático
    private boolean bloodNight;
    private boolean wasNight;
    private Boolean lastXpState;

    EventsModule(ArepaCore plugin) {
        this.plugin = plugin;
    }

    void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 100L, 100L);
    }

    boolean xpActive() {
        if (xpManual != null) {
            return xpManual;
        }
        if (!plugin.getConfig().getBoolean("eventos.xp-doble.fines-de-semana", true)) {
            return false;
        }
        DayOfWeek d = LocalDate.now(plugin.zone()).getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    void setXpManual(Boolean on) {
        xpManual = on;
        tick();
    }

    boolean bloodNight() {
        return bloodNight;
    }

    void setBloodNight(boolean on) {
        if (on == bloodNight) {
            return;
        }
        bloodNight = on;
        if (on) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(Title.title(Msg.parse("&4&lNOCHE DE SANGRE"), Msg.parse("&cEl doble de monstruos... y el doble de botín"),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))));
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.6f);
            }
            Msg.broadcast("&4&l☽ &cEsta noche es una &4&lNoche de Sangre&c. ¡Cuidado ahí afuera!");
        } else {
            Msg.broadcast("&6☀ &eLa Noche de Sangre terminó. Sobrevivieron.");
        }
    }

    private void tick() {
        // Anuncio de XP doble cuando cambia.
        boolean xp = xpActive();
        if (lastXpState != null && xp != lastXpState) {
            Msg.broadcast(xp ? "&a&l✦ XP DOBLE &aactivada. ¡A farmear!" : "&7La XP doble terminó.");
        }
        lastXpState = xp;

        World w = Bukkit.getWorlds().getFirst();
        long t = w.getTime();
        boolean night = t >= 13000 && t < 23000;
        if (night && !wasNight) {
            double chance = plugin.getConfig().getDouble("eventos.noche-de-sangre.probabilidad", 0.1);
            if (plugin.getConfig().getBoolean("eventos.noche-de-sangre.activado", true)
                    && ThreadLocalRandom.current().nextDouble() < chance) {
                setBloodNight(true);
            }
        } else if (!night && wasNight && bloodNight) {
            setBloodNight(false);
        }
        wasNight = night;
    }

    @EventHandler(ignoreCancelled = true)
    public void onExp(PlayerExpChangeEvent event) {
        if (xpActive() && event.getAmount() > 0) {
            event.setAmount(event.getAmount() * 2);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (!bloodNight || event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL
                || !(event.getEntity() instanceof Enemy)
                || event.getLocation().getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        Entity e = event.getEntity();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (e.isValid()) {
                e.getWorld().spawnEntity(e.getLocation(), e.getType(), CreatureSpawnEvent.SpawnReason.CUSTOM);
            }
        });
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!bloodNight || !(e instanceof Enemy) || e.getKiller() == null) {
            return;
        }
        List<ItemStack> extra = new ArrayList<>();
        for (ItemStack it : event.getDrops()) {
            extra.add(it.clone());
        }
        event.getDrops().addAll(extra);
        event.setDroppedExp(event.getDroppedExp() * 2);
    }
}
