package com.arepalandia.core;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Formato del chat: cabeza del jugador + prefijo de LuckPerms + nombre + mensaje.
 * El formato se define en config.yml (chat-format) con MiniMessage.
 */
final class ChatListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ArepaCore plugin;
    private final LuckPerms luckPerms;

    ChatListener(ArepaCore plugin) {
        this.plugin = plugin;
        LuckPerms lp = null;
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            lp = Bukkit.getServicesManager().load(LuckPerms.class);
        }
        this.luckPerms = lp;
        if (lp == null) {
            plugin.getLogger().info("LuckPerms no encontrado: el chat se muestra sin prefijos");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String format = plugin.getConfig().getString("chat-format", "<head> <prefix><player><gray>:</gray> <message>");
        Component head = Component.object(ObjectContents.playerHead(player));
        Component prefix = prefixOf(player);

        event.renderer((source, sourceDisplayName, message, viewer) -> MM.deserialize(format, TagResolver.resolver(
                Placeholder.component("head", head),
                Placeholder.component("prefix", prefix),
                Placeholder.unparsed("player", source.getName()),
                Placeholder.component("message", message)
        )));
    }

    /** Prefijo del jugador en LuckPerms, con un espacio al final; vacío si no tiene. */
    private Component prefixOf(Player player) {
        if (luckPerms == null) {
            return Component.empty();
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        String raw = user == null ? null : user.getCachedData().getMetaData().getPrefix();
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        // Acepta prefijos en MiniMessage (<red>[Admin]) o con códigos & (&c[Admin]).
        Component parsed = raw.indexOf('&') >= 0 || raw.indexOf('§') >= 0
                ? LegacyComponentSerializer.legacyAmpersand().deserialize(raw.replace('§', '&'))
                : MM.deserialize(raw);
        return parsed.append(Component.space());
    }
}
