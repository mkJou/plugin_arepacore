package com.arepalandia.core;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /tpa, /tpahere, /tpaccept, /tpdeny, /tpacancel con botones clicables. */
final class TpaModule implements TabExecutor {

    private final ArepaCore plugin;
    /** destinatario -> (quien pidió -> solicitud) */
    private final Map<UUID, Map<UUID, Request>> requests = new LinkedHashMap<>();

    private record Request(UUID from, boolean here, long expires) {
    }

    TpaModule(ArepaCore plugin) {
        this.plugin = plugin;
    }

    void register() {
        for (String c : List.of("tpa", "tpahere", "tpaccept", "tpdeny", "tpacancel")) {
            plugin.getCommand(c).setExecutor(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }
        purge();
        switch (cmd.getName()) {
            case "tpa", "tpahere" -> {
                if (args.length < 1) {
                    Msg.send(p, "&cUso: &e/" + cmd.getName() + " <jugador>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null || target.equals(p)) {
                    Msg.send(p, "&cEse jugador no está conectado.");
                    return true;
                }
                boolean here = cmd.getName().equals("tpahere");
                int secs = plugin.getConfig().getInt("tpa.expira-segundos", 60);
                requests.computeIfAbsent(target.getUniqueId(), k -> new LinkedHashMap<>())
                        .put(p.getUniqueId(), new Request(p.getUniqueId(), here, System.currentTimeMillis() + secs * 1000L));
                Msg.send(p, "&7Solicitud enviada a &e" + target.getName() + "&7. Expira en " + secs + "s.");
                String name = Msg.esc(p.getName());
                target.sendMessage(Msg.parse(Msg.prefix() + "&e" + p.getName() + (here
                        ? " &7quiere que vayas a donde está."
                        : " &7quiere teletransportarse a ti.")));
                target.sendMessage(Msg.mini("  <green><bold><click:run_command:'/tpaccept " + name + "'><hover:show_text:'Aceptar'>[ACEPTAR]</hover></click></bold></green>"
                        + "  <red><bold><click:run_command:'/tpdeny " + name + "'><hover:show_text:'Rechazar'>[RECHAZAR]</hover></click></bold></red>"));
                target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
            }
            case "tpaccept", "tpdeny" -> {
                Map<UUID, Request> mine = requests.get(p.getUniqueId());
                if (mine == null || mine.isEmpty()) {
                    Msg.send(p, "&cNo tienes solicitudes pendientes.");
                    return true;
                }
                Request r;
                if (args.length > 0) {
                    Player from = Bukkit.getPlayerExact(args[0]);
                    r = from == null ? null : mine.remove(from.getUniqueId());
                } else {
                    UUID last = null;
                    for (UUID k : mine.keySet()) {
                        last = k;
                    }
                    r = mine.remove(last);
                }
                Player from = r == null ? null : Bukkit.getPlayer(r.from());
                if (from == null) {
                    Msg.send(p, "&cEsa solicitud ya no existe o el jugador se desconectó.");
                    return true;
                }
                if (cmd.getName().equals("tpdeny")) {
                    Msg.send(p, "&7Rechazaste la solicitud de &e" + from.getName() + "&7.");
                    Msg.send(from, "&e" + p.getName() + " &crechazó tu solicitud.");
                    return true;
                }
                Msg.send(p, "&aAceptaste la solicitud de &e" + from.getName() + "&a.");
                Msg.send(from, "&e" + p.getName() + " &aaceptó tu solicitud.");
                if (r.here()) {
                    plugin.teleports().teleport(p, from.getLocation(), "a &e" + from.getName() + "&7");
                } else {
                    plugin.teleports().teleport(from, p.getLocation(), "a &e" + p.getName() + "&7");
                }
            }
            case "tpacancel" -> {
                boolean any = false;
                for (Map<UUID, Request> m : requests.values()) {
                    any |= m.remove(p.getUniqueId()) != null;
                }
                Msg.send(p, any ? "&7Solicitudes canceladas." : "&cNo tienes solicitudes enviadas.");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void purge() {
        long now = System.currentTimeMillis();
        for (Iterator<Map<UUID, Request>> it = requests.values().iterator(); it.hasNext(); ) {
            Map<UUID, Request> m = it.next();
            m.values().removeIf(r -> r.expires() < now);
            if (m.isEmpty()) {
                it.remove();
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1 || cmd.getName().equals("tpacancel")) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (!o.equals(sender) && o.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                out.add(o.getName());
            }
        }
        return out;
    }
}
