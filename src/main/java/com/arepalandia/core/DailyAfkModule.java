package com.arepalandia.core;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * - Bienvenida y recompensa diaria al iniciar sesión.
 * - Contador de días del mundo (/dia) y aviso cada amanecer.
 * - AFK: avisa cuando alguien lleva un rato sin moverse.
 * - Anuncios automáticos.
 */
final class DailyAfkModule implements TabExecutor, Listener {

    private final ArepaCore plugin;
    private final DataFile data;
    private final Map<UUID, Long> lastActive = new HashMap<>();
    private final Set<UUID> afk = new HashSet<>();
    private long lastDay = -1;
    private int announceIndex;
    private int announceTask = -1;

    DailyAfkModule(ArepaCore plugin) {
        this.plugin = plugin;
        this.data = new DataFile(plugin, "diario.yml");
    }

    void register() {
        plugin.getCommand("dia").setExecutor(this);
        plugin.getCommand("afk").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 200L, 200L);
        restartAnnouncer();
    }

    void restartAnnouncer() {
        if (announceTask != -1) {
            Bukkit.getScheduler().cancelTask(announceTask);
            announceTask = -1;
        }
        long minutes = plugin.getConfig().getLong("anuncios.cada-minutos", 15);
        if (minutes <= 0) {
            return;
        }
        announceTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<String> list = plugin.getConfig().getStringList("anuncios.mensajes");
            if (list.isEmpty() || Bukkit.getOnlinePlayers().isEmpty()) {
                return;
            }
            announceIndex = announceIndex % list.size();
            Msg.broadcast(list.get(announceIndex++));
        }, minutes * 1200L, minutes * 1200L).getTaskId();
    }

    /** Lo llama JoinQuitListener cuando el jugador ya inició sesión (o entró, sin AuthMe). */
    void onReady(Player p) {
        markActive(p);
        for (String line : plugin.getConfig().getStringList("bienvenida")) {
            p.sendMessage(Msg.parse(line.replace("{player}", p.getName()).replace("{dia}", String.valueOf(plugin.worldDay()))));
        }
        if (!plugin.getConfig().getBoolean("recompensa-diaria.activado", true)) {
            return;
        }
        String today = LocalDate.now(plugin.zone()).toString();
        String path = p.getUniqueId().toString();
        if (today.equals(data.yaml().getString(path))) {
            return;
        }
        data.yaml().set(path, today);
        data.save();
        // Un poco después, para que no se mezcle con los mensajes de login.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) {
                return;
            }
            for (String spec : plugin.getConfig().getStringList("recompensa-diaria.items")) {
                ItemStack item = parseItem(spec);
                if (item != null) {
                    p.getInventory().addItem(item).values().forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
                }
            }
            int xp = plugin.getConfig().getInt("recompensa-diaria.experiencia", 0);
            if (xp > 0) {
                p.giveExp(xp);
            }
            Msg.send(p, plugin.getConfig().getString("recompensa-diaria.mensaje", "&a¡Recompensa diaria recibida!"));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.3f);
        }, 60L);
    }

    private ItemStack parseItem(String spec) {
        String[] parts = spec.split(":");
        Material m = Material.matchMaterial(parts[0].trim());
        if (m == null) {
            plugin.getLogger().warning("Recompensa diaria: material desconocido '" + parts[0] + "'");
            return null;
        }
        int amount = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 1;
        return new ItemStack(m, amount);
    }

    private void tick() {
        // Nuevo día en el mundo.
        long day = plugin.worldDay();
        if (lastDay != -1 && day > lastDay && plugin.getConfig().getBoolean("dias.anunciar-amanecer", true)) {
            Msg.broadcast(plugin.getConfig().getString("dias.mensaje", "&6☀ &eAmanece el día &6{dia} &een el mundo.").replace("{dia}", String.valueOf(day)));
        }
        lastDay = day;

        // AFK.
        long limit = plugin.getConfig().getLong("afk.minutos", 5) * 60_000L;
        if (limit <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            long last = lastActive.getOrDefault(p.getUniqueId(), now);
            if (!afk.contains(p.getUniqueId()) && now - last >= limit) {
                setAfk(p, true);
            }
        }
    }

    private void setAfk(Player p, boolean on) {
        if (on ? !afk.add(p.getUniqueId()) : !afk.remove(p.getUniqueId())) {
            return;
        }
        Msg.broadcast(on ? "&7" + p.getName() + " está AFK." : "&7" + p.getName() + " ya no está AFK.");
        p.playerListName(on ? Msg.parse("&7" + p.getName() + " &8[AFK]") : null);
    }

    private void markActive(Player p) {
        lastActive.put(p.getUniqueId(), System.currentTimeMillis());
        if (afk.contains(p.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> setAfk(p, false));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.hasChangedOrientation() || event.hasChangedBlock()) {
            markActive(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncChatEvent event) {
        lastActive.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        if (afk.contains(event.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> setAfk(event.getPlayer(), false));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!event.getMessage().toLowerCase(Locale.ROOT).startsWith("/afk")) {
            markActive(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        afk.remove(event.getPlayer().getUniqueId());
        lastActive.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("dia")) {
            Msg.send(sender, "&eEstamos en el día &6" + plugin.worldDay() + " &edel mundo.");
            return true;
        }
        if (sender instanceof Player p) {
            if (afk.contains(p.getUniqueId())) {
                lastActive.put(p.getUniqueId(), System.currentTimeMillis());
                setAfk(p, false);
            } else {
                setAfk(p, true);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return List.of();
    }
}
