package com.github.albardoo02;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class SimpleReplace extends JavaPlugin {

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        double currentVersion = getConfig().getDouble("configVersion", 1.0);
        double saveVersion = getSavedVersion();

        if (currentVersion > saveVersion) {
            getLogger().info("Configに新しいバージョン(" + currentVersion + ")が見つかったため、config.ymlを更新しています...");
            moveOldConfig();
            saveNewConfig();
            saveVersion(currentVersion);
        } else {
            getLogger().info("config.ymlは最新です");
        }

        this.getCommand("simplereplace").setExecutor(new ReplaceCommand(this));
        this.getServer().getPluginManager().registerEvents(new WandListener(this), this);
    }

    private void moveOldConfig() {
        moveFileToOld("config.yml");
    }

    private void moveFileToOld(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) return;
        File oldFolder = new File(getDataFolder(), "old");
        if (!oldFolder.exists()) {
            oldFolder.mkdir();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backupFile = new File(oldFolder, fileName.replace(".yml", "_" + timestamp + ".yml"));

        if (file.renameTo(backupFile)) {
            getLogger().info(fileName + " をoldフォルダに移動しました: " + backupFile.getName());
        } else {
            getLogger().warning(fileName + " の移動に失敗しました");
        }
    }

    private void saveNewConfig() {
        saveResource("config.yml", true);
    }

    private double getSavedVersion() {
        File versionFile = new File(getDataFolder(), "version.yml");
        if (!versionFile.exists()) return 1.0;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(versionFile);
        return config.getDouble("configVersion", 1.0);
    }

    private void saveVersion(double version) {
        File versionFile = new File(getDataFolder(), "version.yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("configVersion", version);
        try {
            config.save(versionFile);
        } catch (IOException e) {
            getLogger().severe("バージョン情報の保存に失敗しました: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        saveConfig();
    }
}
