package com.arepalandia.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mensajes de entrada y salida.
 * Con AuthMe: el "[+] ha entrado" se anuncia recién al iniciar sesión (o registrarse),
 * y el "[-] ha salido" solo si antes se anunció su entrada.
 * Sin AuthMe: se anuncia al conectarse.
 */
final class JoinQuitListener implements Listener {

    private static final String[] AUTHME_EVENTS = {
            "fr.xephi.authme.events.LoginEvent",
            "fr.xephi.authme.events.RegisterEvent",
    };

    private final ArepaCore plugin;
    private final Set<UUID> announced = ConcurrentHashMap.newKeySet();
    private boolean authMeHooked;

    JoinQuitListener(ArepaCore plugin) {
        this.plugin = plugin;
    }

    void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Plugin authMe = Bukkit.getPluginManager().getPlugin("AuthMe");
        if (authMe == null || !authMe.isEnabled()) {
            plugin.getLogger().info("AuthMe no encontrado: el mensaje de entrada se muestra al conectarse");
            return;
        }
        try {
            for (String name : AUTHME_EVENTS) {
                hookAuthMeEvent(Class.forName(name, true, authMe.getClass().getClassLoader()).asSubclass(Event.class));
            }
            authMeHooked = true;
            plugin.getLogger().info("Conectado a AuthMe: el mensaje de entrada se muestra al iniciar sesión");
        } catch (ReflectiveOperationException | ClassCastException e) {
            plugin.getLogger().warning("No se pudo conectar con AuthMe (" + e + "); el mensaje de entrada se muestra al conectarse");
        }
    }

    private void hookAuthMeEvent(Class<? extends Event> eventClass) throws NoSuchMethodException {
        Method getPlayer = eventClass.getMethod("getPlayer");
        Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR, (listener, event) -> {
            if (!eventClass.isInstance(event)) {
                return;
            }
            try {
                announceJoin((Player) getPlayer.invoke(event));
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().warning("Error leyendo evento de AuthMe: " + e);
            }
        }, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if (authMeHooked) {
            event.joinMessage(null);
        } else {
            event.joinMessage(null);
            announceJoin(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        boolean wasAnnounced = announced.remove(event.getPlayer().getUniqueId());
        event.quitMessage(wasAnnounced ? format("quit-message", event.getPlayer()) : null);
    }

    private void announceJoin(Player player) {
        if (player == null || !announced.add(player.getUniqueId())) {
            return;
        }
        plugin.daily().onReady(player);
        Component message = format("join-message", player);
        if (message == null) {
            return;
        }
        // Lo ven todos menos el que entró (y la consola, para el log).
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId())) {
                other.sendMessage(message);
            }
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private Component format(String key, Player player) {
        String template = plugin.getConfig().getString(key, "");
        if (template.isEmpty()) {
            return null;
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(template.replace("{player}", player.getName()));
    }
}
