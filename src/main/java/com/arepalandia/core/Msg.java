package com.arepalandia.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/** Utilidades de mensajes: acepta texto con códigos & o MiniMessage. */
final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static ArepaCore plugin;

    private Msg() {
    }

    static void init(ArepaCore p) {
        plugin = p;
    }

    /** Convierte texto a Component: si tiene '&' usa códigos clásicos, si no MiniMessage. */
    static Component parse(String text) {
        if (text == null) {
            return Component.empty();
        }
        if (text.indexOf('&') >= 0 || text.indexOf('§') >= 0) {
            return LEGACY.deserialize(text.replace('§', '&'));
        }
        return MM.deserialize(text);
    }

    static Component mini(String text) {
        return MM.deserialize(text);
    }

    static String prefix() {
        return plugin.getConfig().getString("prefijo", "&6[Arepa] &r");
    }

    /** Envía un mensaje con el prefijo del server. */
    static void send(CommandSender to, String text) {
        to.sendMessage(parse(prefix() + text));
    }

    static void broadcast(String text) {
        Component c = parse(text);
        Bukkit.getServer().sendMessage(c);
    }

    /** Escapa un texto de jugador para meterlo en MiniMessage sin que interprete etiquetas. */
    static String esc(String s) {
        return MM.escapeTags(s);
    }
}
