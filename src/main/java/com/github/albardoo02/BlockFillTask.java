package com.github.albardoo02;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BlockFillTask extends BukkitRunnable {

    private final SimpleReplace plugin;
    private final Player player;
    private final World world;
    private final ReplaceCommand.Selection selection;
    private final Material blockType;

    private int x, y, z;
    private final int x1, y1, z1;
    private final int x2, y2, z2;

    private final int totalBlocks;
    private int changedBlocks;

    public BlockFillTask(SimpleReplace plugin, Player player, World world, ReplaceCommand.Selection selection, Material blockType) {
        this.plugin = plugin;
        this.player = player;
        this.world = world;
        this.selection = selection;
        this.blockType = blockType;

        this.x1 = selection.x1;
        this.y1 = selection.y1;
        this.z1 = selection.z1;
        this.x2 = selection.x2;
        this.y2 = selection.y2;
        this.z2 = selection.z2;

        this.x = x1;
        this.y = y1;
        this.z = z1;

        this.totalBlocks = (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
        this.changedBlocks = 0;
    }

    @Override
    public void run() {
        int count = 0;

        int blocksPerTick = plugin.getConfig().getInt("blocksPerTick", 500);
        while (count < blocksPerTick) {
            Block block = world.getBlockAt(x, y, z);
            block.setType(blockType, false);

            changedBlocks++;
            count++;

            z++;
            if (z > z2) {
                z = z1;
                y++;
                if (y > y2) {
                    y = y1;
                    x++;
                    if (x > x2) {
                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                new TextComponent("§a処理が完了しました"));
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&e[&aSimpleReplace&e] &a" + changedBlocks + "ブロックの置き換えが完了しました"));
                        this.cancel();
                        return;
                    }
                }
            }
        }

        double percent = ((double) changedBlocks / totalBlocks) * 100;
        String progressBar = createProgressBar(percent, 20);

        player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(
                        "§cブロックを置き換えています... §f[" + progressBar + "§f]  §e" + changedBlocks + " §f/ §a" + totalBlocks + " §3(" +String.format("%.2f", percent) + "%)"
                )
        );
    }

    private String createProgressBar(double percent, int length) {
        int filledLength = (int) (percent / 100 * length);
        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < length; i++) {
            if (i < filledLength) {
                bar.append("§a■");
            } else {
                bar.append("§7□");
            }
        }
        return bar.toString();
    }
}