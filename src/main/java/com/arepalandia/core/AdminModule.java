package com.arepalandia.core;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** /arepa reload|evento|ayuda, /freeze, /invsee, /endersee. */
final class AdminModule implements TabExecutor, Listener {

    private final ArepaCore plugin;
    private final Set<UUID> frozen = new HashSet<>();

    AdminModule(ArepaCore plugin) {
        this.plugin = plugin;
    }

    void register() {
        for (String c : List.of("arepa", "freeze", "invsee", "endersee")) {
            plugin.getCommand(c).setExecutor(this);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName()) {
            case "arepa" -> arepa(sender, args);
            case "freeze" -> {
                Player t = args.length > 0 ? Bukkit.getPlayerExact(args[0]) : null;
                if (t == null) {
                    Msg.send(sender, "&cUso: &e/freeze <jugador> &c(debe estar conectado)");
                    return true;
                }
                if (frozen.remove(t.getUniqueId())) {
                    Msg.send(sender, "&a" + t.getName() + " ya puede moverse.");
                    Msg.send(t, "&aYa puedes moverte.");
                } else {
                    frozen.add(t.getUniqueId());
                    Msg.send(sender, "&e" + t.getName() + " congelado.");
                    Msg.send(t, "&bUn admin te congeló. No puedes moverte por ahora.");
                }
            }
            case "invsee", "endersee" -> {
                if (!(sender instanceof Player p)) {
                    return true;
                }
                Player t = args.length > 0 ? Bukkit.getPlayerExact(args[0]) : null;
                if (t == null) {
                    Msg.send(p, "&cUso: &e/" + cmd.getName() + " <jugador> &c(debe estar conectado)");
                    return true;
                }
                p.openInventory(cmd.getName().equals("invsee") ? t.getInventory() : t.getEnderChest());
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void arepa(CommandSender sender, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase() : "ayuda";
        if (!sub.equals("ayuda") && !sender.hasPermission("arepa.admin")) {
            Msg.send(sender, "&cNo tienes permiso.");
            return;
        }
        switch (sub) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.daily().restartAnnouncer();
                Msg.send(sender, "&aConfiguración recargada.");
            }
            case "evento" -> {
                if (args.length < 3) {
                    Msg.send(sender, "&cUso: &e/arepa evento <xp|sangre> <on|off|auto>");
                    return;
                }
                String state = args[2].toLowerCase();
                if (args[1].equalsIgnoreCase("xp")) {
                    plugin.events().setXpManual(state.equals("auto") ? null : state.equals("on"));
                    Msg.send(sender, "&aXP doble: &e" + state + " &7(ahora " + (plugin.events().xpActive() ? "activa" : "inactiva") + ")");
                } else if (args[1].equalsIgnoreCase("sangre")) {
                    plugin.events().setBloodNight(state.equals("on"));
                    Msg.send(sender, "&aNoche de sangre: &e" + state);
                } else {
                    Msg.send(sender, "&cEventos: xp, sangre");
                }
            }
            default -> {
                sender.sendMessage(Msg.parse("&6&l— ArepaCore " + plugin.getPluginMeta().getVersion() + " —"));
                sender.sendMessage(Msg.parse("&e/sethome [nombre] /home [nombre] /delhome /homes &7— casas"));
                sender.sendMessage(Msg.parse("&e/tpa /tpahere /tpaccept /tpdeny &7— teletransporte entre amigos"));
                sender.sendMessage(Msg.parse("&e/spawn /back &7— spawn y volver a donde moriste"));
                sender.sendMessage(Msg.parse("&e/msg /r &7— mensajes privados"));
                sender.sendMessage(Msg.parse("&e/stats [jugador] /top <categoría> [semana] &7— estadísticas"));
                sender.sendMessage(Msg.parse("&e/dia /afk &7— día del mundo y modo AFK"));
                sender.sendMessage(Msg.parse("&7Talar: agáchate y rompe un tronco con el hacha."));
                if (sender.hasPermission("arepa.admin")) {
                    sender.sendMessage(Msg.parse("&c/arepa reload | evento <xp|sangre> <on|off|auto> &7| &c/freeze /invsee /endersee /setspawn"));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onMove(PlayerMoveEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId()) && event.hasChangedPosition()) {
            event.setTo(event.getFrom().setDirection(event.getTo().getDirection()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCmd(PlayerCommandPreprocessEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId()) && !event.getMessage().toLowerCase().startsWith("/msg")
                && !event.getMessage().toLowerCase().startsWith("/r ")) {
            event.setCancelled(true);
            Msg.send(event.getPlayer(), "&bEstás congelado.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("arepa")) {
            if (args.length == 1) {
                return List.of("ayuda", "reload", "evento");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("evento")) {
                return List.of("xp", "sangre");
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("evento")) {
                return List.of("on", "off", "auto");
            }
            return List.of();
        }
        return null;
    }
}
