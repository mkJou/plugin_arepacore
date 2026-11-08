package com.arepalandia.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /spawn, /setspawn, /back, /msg y /r. */
final class SpawnBackMsgModule implements TabExecutor {

    private final ArepaCore plugin;
    private final Map<UUID, UUID> lastMessaged = new HashMap<>();

    SpawnBackMsgModule(ArepaCore plugin) {
        this.plugin = plugin;
    }

    void register() {
        for (String c : List.of("spawn", "setspawn", "back", "msg", "r")) {
            plugin.getCommand(c).setExecutor(this);
        }
    }

    Location spawn() {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("spawn");
        if (s != null && s.contains("world")) {
            World w = Bukkit.getWorld(s.getString("world"));
            if (w != null) {
                return new Location(w, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                        (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
            }
        }
        return Bukkit.getWorlds().getFirst().getSpawnLocation().add(0.5, 0, 0.5);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName()) {
            case "msg" -> {
                if (args.length < 2) {
                    Msg.send(sender, "&cUso: &e/msg <jugador> <mensaje>");
                    return true;
                }
                Player to = Bukkit.getPlayerExact(args[0]);
                if (to == null) {
                    Msg.send(sender, "&cEse jugador no está conectado.");
                    return true;
                }
                whisper(sender, to, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                return true;
            }
            case "r" -> {
                if (!(sender instanceof Player p)) {
                    return true;
                }
                UUID last = lastMessaged.get(p.getUniqueId());
                Player to = last == null ? null : Bukkit.getPlayer(last);
                if (to == null) {
                    Msg.send(p, "&cNo tienes a quién responder.");
                    return true;
                }
                if (args.length == 0) {
                    Msg.send(p, "&cUso: &e/r <mensaje>");
                    return true;
                }
                whisper(p, to, String.join(" ", args));
                return true;
            }
            default -> {
            }
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }
        switch (cmd.getName()) {
            case "spawn" -> plugin.teleports().teleport(p, spawn(), "al spawn");
            case "setspawn" -> {
                Location l = p.getLocation();
                plugin.getConfig().set("spawn.world", l.getWorld().getName());
                plugin.getConfig().set("spawn.x", l.getX());
                plugin.getConfig().set("spawn.y", l.getY());
                plugin.getConfig().set("spawn.z", l.getZ());
                plugin.getConfig().set("spawn.yaw", (double) l.getYaw());
                plugin.getConfig().set("spawn.pitch", (double) l.getPitch());
                plugin.saveConfig();
                l.getWorld().setSpawnLocation(l);
                Msg.send(p, "&aSpawn guardado aquí.");
            }
            case "back" -> {
                Location b = plugin.teleports().backLocation(p);
                if (b == null) {
                    Msg.send(p, "&cNo hay un lugar anterior al que volver.");
                    return true;
                }
                plugin.teleports().teleport(p, b, "a tu última ubicación");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void whisper(CommandSender from, Player to, String text) {
        String fromName = from instanceof Player fp ? fp.getName() : "Consola";
        String t = Msg.esc(text);
        from.sendMessage(Msg.mini("<gray>[<yellow>Tú</yellow> → <yellow>" + Msg.esc(to.getName()) + "</yellow>]</gray> <white>" + t));
        to.sendMessage(Msg.mini("<gray>[<yellow>" + Msg.esc(fromName) + "</yellow> → <yellow>Tú</yellow>]</gray> <white>" + t));
        to.playSound(to.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.4f);
        if (from instanceof Player fp) {
            lastMessaged.put(fp.getUniqueId(), to.getUniqueId());
            lastMessaged.put(to.getUniqueId(), fp.getUniqueId());
        }
        plugin.getLogger().info("[MSG] " + fromName + " -> " + to.getName() + ": " + text);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("msg") && args.length == 1) {
            List<String> out = new ArrayList<>();
            for (Player o : Bukkit.getOnlinePlayers()) {
                if (o.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    out.add(o.getName());
                }
            }
            return out;
        }
        return List.of();
    }
}
