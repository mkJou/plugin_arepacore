package com.arepalandia.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.DateTimeException;
import java.time.ZoneId;

public final class ArepaCore extends JavaPlugin {

    private Teleports teleports;
    private HomesModule homes;
    private StatsModule stats;
    private AchievementsModule achievements;
    private EventsModule events;
    private DailyAfkModule daily;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        Msg.init(this);

        teleports = new Teleports(this);
        Bukkit.getPluginManager().registerEvents(teleports, this);

        daily = new DailyAfkModule(this);
        daily.register();
        new JoinQuitListener(this).register();
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        homes = new HomesModule(this);
        homes.register();
        new TpaModule(this).register();
        new SpawnBackMsgModule(this).register();
        stats = new StatsModule(this);
        stats.register();
        achievements = new AchievementsModule(this);
        achievements.register();
        events = new EventsModule(this);
        events.register();
        new AdminModule(this).register();
        Bukkit.getPluginManager().registerEvents(new DeathModule(this), this);
        Bukkit.getPluginManager().registerEvents(new TimberListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MotdListener(this), this);

        getLogger().info("arepaCore activado");
    }

    @Override
    public void onDisable() {
        if (homes != null) {
            homes.save();
        }
        if (stats != null) {
            stats.save();
        }
    }

    Teleports teleports() {
        return teleports;
    }

    AchievementsModule achievements() {
        return achievements;
    }

    EventsModule events() {
        return events;
    }

    DailyAfkModule daily() {
        return daily;
    }

    /** Día actual del mundo principal (empieza en 1). */
    long worldDay() {
        return Bukkit.getWorlds().getFirst().getFullTime() / 24000L + 1;
    }

    /** Zona horaria del server para la recompensa diaria, fines de semana y semanas. */
    ZoneId zone() {
        try {
            return ZoneId.of(getConfig().getString("zona-horaria", "America/Caracas"));
        } catch (DateTimeException e) {
            return ZoneId.systemDefault();
        }
    }
}
