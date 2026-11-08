package com.arepalandia.core;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToLongFunction;

/** /stats [jugador] y /top <categoría> [semana]. */
final class StatsModule implements TabExecutor, Listener {

    enum Cat {
        HORAS("horas", "Horas jugadas"),
        MUERTES("muertes", "Muertes"),
        MOBS("mobs", "Mobs matados"),
        BLOQUES("bloques", "Bloques minados"),
        DIAMANTES("diamantes", "Diamantes minados");

        final String key;
        final String title;

        Cat(String key, String title) {
            this.key = key;
            this.title = title;
        }
    }

    private final ArepaCore plugin;
    private final DataFile data;

    StatsModule(ArepaCore plugin) {
        this.plugin = plugin;
        this.data = new DataFile(plugin, "stats.yml");
    }

    void register() {
        plugin.getCommand("stats").setExecutor(this);
        plugin.getCommand("top").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Cada minuto: guarda la "foto" de inicio de semana de los conectados, y guarda cambios.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ensureWeekSnapshot(p);
            }
            data.saveIfDirty();
        }, 20L * 60, 20L * 60);
    }

    void save() {
        data.saveIfDirty();
    }

    /** Valor actual (total histórico) de una categoría. Horas en minutos para precisión. */
    long value(OfflinePlayer p, Cat c) {
        try {
            return switch (c) {
                case HORAS -> p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L / 60L; // minutos
                case MUERTES -> p.getStatistic(Statistic.DEATHS);
                case MOBS -> p.getStatistic(Statistic.MOB_KILLS);
                case BLOQUES -> data.yaml().getLong("total." + p.getUniqueId() + ".bloques");
                case DIAMANTES -> data.yaml().getLong("total." + p.getUniqueId() + ".diamantes");
            };
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private String weekId() {
        return LocalDate.now(plugin.zone()).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
    }

    private void ensureWeekSnapshot(OfflinePlayer p) {
        String path = "semana." + weekId() + "." + p.getUniqueId();
        if (data.yaml().contains(path)) {
            return;
        }
        // Borra semanas viejas.
        ConfigurationSection weeks = data.yaml().getConfigurationSection("semana");
        if (weeks != null) {
            for (String k : weeks.getKeys(false)) {
                if (!k.equals(weekId())) {
                    weeks.set(k, null);
                }
            }
        }
        for (Cat c : Cat.values()) {
            data.yaml().set(path + "." + c.key, value(p, c));
        }
        data.markDirty();
    }

    long weekly(OfflinePlayer p, Cat c) {
        String path = "semana." + weekId() + "." + p.getUniqueId() + "." + c.key;
        if (!data.yaml().contains(path)) {
            return 0;
        }
        return Math.max(0, value(p, c) - data.yaml().getLong(path));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        ensureWeekSnapshot(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        String base = "total." + event.getPlayer().getUniqueId() + ".";
        data.yaml().set(base + "bloques", data.yaml().getLong(base + "bloques") + 1);
        Material m = event.getBlock().getType();
        if (m == Material.DIAMOND_ORE || m == Material.DEEPSLATE_DIAMOND_ORE) {
            data.yaml().set(base + "diamantes", data.yaml().getLong(base + "diamantes") + 1);
        }
        data.markDirty();
    }

    private static String fmt(Cat c, long v) {
        return c == Cat.HORAS ? String.format(Locale.ROOT, "%.1f h", v / 60.0) : String.valueOf(v);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("stats")) {
            OfflinePlayer target;
            if (args.length > 0) {
                target = Bukkit.getOfflinePlayerIfCached(args[0]);
                if (target == null) {
                    Msg.send(sender, "&cNo conozco a ese jugador.");
                    return true;
                }
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage("Uso: /stats <jugador>");
                return true;
            }
            sender.sendMessage(Msg.parse("&6&l— Estadísticas de &e&l" + target.getName() + " &6&l—"));
            for (Cat c : Cat.values()) {
                sender.sendMessage(Msg.parse("&7" + c.title + ": &f" + fmt(c, value(target, c))
                        + " &8(esta semana: " + fmt(c, weekly(target, c)) + ")"));
            }
            sender.sendMessage(Msg.parse("&7Logros del server: &f" + plugin.achievements().count(target.getUniqueId())
                    + " &8| &7Día del mundo: &f" + plugin.worldDay()));
            return true;
        }
        // /top
        if (args.length < 1) {
            Msg.send(sender, "&cUso: &e/top <horas|muertes|mobs|bloques|diamantes> [semana]");
            return true;
        }
        Cat cat = null;
        for (Cat c : Cat.values()) {
            if (c.key.equalsIgnoreCase(args[0])) {
                cat = c;
            }
        }
        if (cat == null) {
            Msg.send(sender, "&cCategorías: horas, muertes, mobs, bloques, diamantes");
            return true;
        }
        boolean week = args.length > 1 && args[1].equalsIgnoreCase("semana");
        Cat fc = cat;
        ToLongFunction<OfflinePlayer> f = week ? p -> weekly(p, fc) : p -> value(p, fc);
        List<Map.Entry<String, Long>> rows = new ArrayList<>();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getName() == null) {
                continue;
            }
            long v = f.applyAsLong(p);
            if (v > 0) {
                rows.add(Map.entry(p.getName(), v));
            }
        }
        rows.sort(Comparator.comparingLong((Map.Entry<String, Long> e) -> e.getValue()).reversed());
        sender.sendMessage(Msg.parse("&6&l— Top " + cat.title + (week ? " (esta semana)" : "") + " —"));
        if (rows.isEmpty()) {
            sender.sendMessage(Msg.parse("&7Todavía no hay datos."));
        }
        for (int i = 0; i < Math.min(10, rows.size()); i++) {
            String medal = i == 0 ? "&6" : i == 1 ? "&f" : i == 2 ? "&c" : "&7";
            sender.sendMessage(Msg.parse(medal + "#" + (i + 1) + " &e" + rows.get(i).getKey() + " &7— &f" + fmt(cat, rows.get(i).getValue())));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("top")) {
            if (args.length == 1) {
                List<String> o = new ArrayList<>();
                for (Cat c : Cat.values()) {
                    o.add(c.key);
                }
                return o;
            }
            if (args.length == 2) {
                return List.of("semana");
            }
            return List.of();
        }
        return null; // nombres de jugadores por defecto
    }
}
