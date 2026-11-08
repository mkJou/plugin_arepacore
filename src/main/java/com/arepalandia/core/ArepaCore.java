package com.arepalandia.core;

import org.bukkit.plugin.java.JavaPlugin;

public final class ArepaCore extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        new JoinQuitListener(this).register();
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getLogger().info("arepaCore activado");
    }
}
