package com.arepalandia.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/** Un archivo YAML dentro de plugins/arepaCore/data/ para guardar datos persistentes. */
final class DataFile {

    private final ArepaCore plugin;
    private final File file;
    private YamlConfiguration yaml;
    private boolean dirty;

    DataFile(ArepaCore plugin, String name) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "data");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        this.file = new File(dir, name);
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    YamlConfiguration yaml() {
        return yaml;
    }

    /** Marca cambios; se guardan en el próximo autoguardado o al apagar. */
    void markDirty() {
        dirty = true;
    }

    /** Guarda ya mismo. */
    void save() {
        try {
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar " + file.getName() + ": " + e.getMessage());
        }
    }

    void saveIfDirty() {
        if (dirty) {
            save();
        }
    }
}
