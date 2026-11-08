package com.arepalandia.core;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.List;

/** MOTD de la lista de servidores. Admite {dia} y {jugadores}. */
final class MotdListener implements Listener {

    private final ArepaCore plugin;

    MotdListener(ArepaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        List<String> lines = plugin.getConfig().getStringList("motd.lineas");
        if (lines.isEmpty()) {
            return;
        }
        String day = String.valueOf(plugin.worldDay());
        String players = String.valueOf(event.getNumPlayers());
        Component motd = Component.empty();
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            if (i > 0) {
                motd = motd.append(Component.newline());
            }
            motd = motd.append(Msg.parse(lines.get(i).replace("{dia}", day).replace("{jugadores}", players)));
        }
        event.motd(motd);
        int max = plugin.getConfig().getInt("motd.maximo-jugadores", 0);
        if (max > 0) {
            event.setMaxPlayers(max);
        }
    }
}
