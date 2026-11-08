package com.arepalandia.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** /sethome, /home, /delhome, /homes. Importa las casas del plugin SetHome la primera vez. */
final class HomesModule implements TabExecutor, Listener {

    private static final String DEFAULT = "casa";

    private final ArepaCore plugin;
    private final DataFile data;

    HomesModule(ArepaCore plugin) {
        this.plugin = plugin;
        this.data = new DataFile(plugin, "homes.yml");
        migrateFromSetHome();
    }

    void register() {
        for (String c : List.of("sethome", "home", "delhome", "homes")) {
            plugin.getCommand(c).setExecutor(this);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    void save() {
        data.saveIfDirty();
    }

    private ConfigurationSection homesOf(UUID id) {
        ConfigurationSection s = data.yaml().getConfigurationSection(id.toString());
        return s == null ? data.yaml().createSection(id.toString()) : s;
    }

    Location getHome(UUID id, String name) {
        ConfigurationSection s = data.yaml().getConfigurationSection(id + "." + name);
        return s == null ? null : read(s);
    }

    private int maxHomes(Player p) {
        if (p.hasPermission("arepa.homes.ilimitadas")) {
            return Integer.MAX_VALUE;
        }
        return plugin.getConfig().getInt("homes.maximo", 3);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }
        String name = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : null;
        Set<String> names = homesOf(p.getUniqueId()).getKeys(false);
        switch (cmd.getName()) {
            case "sethome" -> {
                String n = name == null ? DEFAULT : name;
                if (!n.matches("[a-z0-9_-]{1,16}")) {
                    Msg.send(p, "&cNombre inválido. Usa letras, números, - o _ (máx. 16).");
                    return true;
                }
                if (!names.contains(n) && names.size() >= maxHomes(p)) {
                    Msg.send(p, "&cYa tienes el máximo de casas (&e" + maxHomes(p) + "&c). Borra una con &e/delhome <nombre>&c.");
                    return true;
                }
                write(homesOf(p.getUniqueId()).createSection(n), p.getLocation());
                data.save();
                Msg.send(p, "&aCasa &e" + n + " &aguardada aquí.");
            }
            case "home" -> {
                String n = name;
                if (n == null) {
                    n = names.contains(DEFAULT) || names.size() != 1 ? DEFAULT : names.iterator().next();
                }
                Location loc = getHome(p.getUniqueId(), n);
                if (loc == null) {
                    Msg.send(p, names.isEmpty()
                            ? "&cNo tienes casas. Guarda una con &e/sethome&c."
                            : "&cNo tienes una casa llamada &e" + n + "&c. Tus casas: &f" + String.join(", ", names));
                    return true;
                }
                plugin.teleports().teleport(p, loc, "a tu casa &e" + n + "&7");
            }
            case "delhome" -> {
                String n = name == null ? DEFAULT : name;
                if (!names.contains(n)) {
                    Msg.send(p, "&cNo tienes una casa llamada &e" + n + "&c.");
                    return true;
                }
                homesOf(p.getUniqueId()).set(n, null);
                data.save();
                Msg.send(p, "&aCasa &e" + n + " &aborrada.");
            }
            case "homes" -> Msg.send(p, names.isEmpty()
                    ? "&7No tienes casas. Usa &e/sethome [nombre]&7."
                    : "&7Tus casas (&e" + names.size() + "/" + (maxHomes(p) == Integer.MAX_VALUE ? "∞" : maxHomes(p)) + "&7): &f" + String.join(", ", names));
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player p && args.length == 1 && !cmd.getName().equals("sethome")) {
            List<String> out = new ArrayList<>();
            for (String n : homesOf(p.getUniqueId()).getKeys(false)) {
                if (n.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(n);
                }
            }
            return out;
        }
        return List.of();
    }

    /** Si murió sin cama ni ancla, reaparece en su casa (como hacía SetHome). */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("homes.reaparecer-en-casa", true)
                || event.isBedSpawn() || event.isAnchorSpawn()) {
            return;
        }
        Location home = getHome(event.getPlayer().getUniqueId(), DEFAULT);
        if (home != null) {
            event.setRespawnLocation(home);
        }
    }

    private static void write(ConfigurationSection s, Location l) {
        s.set("world", l.getWorld().getName());
        s.set("x", l.getX());
        s.set("y", l.getY());
        s.set("z", l.getZ());
        s.set("yaw", (double) l.getYaw());
        s.set("pitch", (double) l.getPitch());
    }

    private static Location read(ConfigurationSection s) {
        World w = Bukkit.getWorld(s.getString("world", "world"));
        if (w == null) {
            return null;
        }
        return new Location(w, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }

    /** Copia las casas de plugins/SetHome/homes/*.yml (una sola vez; "main" pasa a llamarse "casa"). */
    private void migrateFromSetHome() {
        if (data.yaml().getBoolean("_migrado-sethome", false)) {
            return;
        }
        File dir = new File(plugin.getDataFolder().getParentFile(), "SetHome/homes");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml") && n.length() > 30);
        int count = 0;
        if (files != null) {
            for (File f : files) {
                UUID id;
                try {
                    id = UUID.fromString(f.getName().replace(".yml", ""));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                ConfigurationSection homes = YamlConfiguration.loadConfiguration(f).getConfigurationSection("Homes");
                if (homes == null) {
                    continue;
                }
                for (String key : homes.getKeys(false)) {
                    ConfigurationSection h = homes.getConfigurationSection(key);
                    if (h == null) {
                        continue;
                    }
                    String n = key.equalsIgnoreCase("main") ? DEFAULT : key.toLowerCase(Locale.ROOT);
                    ConfigurationSection dst = homesOf(id).createSection(n);
                    dst.set("world", h.getString("World", "world"));
                    dst.set("x", h.getDouble("X"));
                    dst.set("y", h.getDouble("Y"));
                    dst.set("z", h.getDouble("Z"));
                    dst.set("yaw", h.getDouble("Yaw"));
                    dst.set("pitch", h.getDouble("Pitch"));
                    count++;
                }
            }
        }
        data.yaml().set("_migrado-sethome", true);
        data.save();
        if (count > 0) {
            plugin.getLogger().info("Importadas " + count + " casas desde SetHome");
        }
    }
}
