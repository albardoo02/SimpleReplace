package com.github.albardoo02;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.UUID;


public class WandListener implements Listener {

    private String prefix;
    private final HashMap<UUID, int[]> pos1Map = new HashMap<>();

    public WandListener(SimpleReplace plugin) {
        String prefixConfig = plugin.getConfig().getString("Prefix");
        if (prefixConfig == null) {
            this.prefix = "§e[§aSimpleReplace§e]§r ";
        } else {
            this.prefix = ChatColor.translateAlternateColorCodes('&',prefixConfig) + " ";
        }
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', message));
    }


    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (!ReplaceCommand.selectModePlayers.contains(playerId)) {
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.STONE_AXE) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            int x = event.getClickedBlock().getX();
            int y = event.getClickedBlock().getY();
            int z = event.getClickedBlock().getZ();

            ReplaceCommand.Selection selection = ReplaceCommand.selections.getOrDefault(playerId,
                    new ReplaceCommand.Selection(x, y, z, x, y, z));
            selection.x1 = x;
            selection.y1 = y;
            selection.z1 = z;

            ReplaceCommand.selections.put(playerId, selection);

            sendMessage(player, "&bPos1を選択しました (X:" + x + ", Y:" + y + ", Z:" + z + ")");
            event.setCancelled(true);
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            int x = event.getClickedBlock().getX();
            int y = event.getClickedBlock().getY();
            int z = event.getClickedBlock().getZ();

            ReplaceCommand.Selection selection = ReplaceCommand.selections.getOrDefault(playerId,
                    new ReplaceCommand.Selection(x, y, z, x, y, z));
            selection.x2 = x;
            selection.y2 = y;
            selection.z2 = z;

            ReplaceCommand.selections.put(playerId, selection);

            sendMessage(player, "&bPos2を選択しました (X:" + x + ", Y:" + y + ", Z:" + z + ")");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwing(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (player.getInventory().getItemInMainHand().getType() != Material.STONE_AXE) return;
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        if (!player.hasPermission("simplereplace.selection")) return;

        boolean isSelecting = ReplaceCommand.selectModePlayers.contains(playerId);
        String status = isSelecting ? "選択モード: " + ChatColor.GREEN + "ON" : "選択モード: " + ChatColor.RED +  "OFF";

        ReplaceCommand.Selection selection = ReplaceCommand.selections.get(playerId);
        String posInfo = "";
        if (selection != null) {
            posInfo = ChatColor.WHITE + " | " +
                    "Pos1: " + selection.x1 + "," + selection.y1 + "," + selection.z1 + " | " +
                    "Pos2: " + selection.x2 + "," + selection.y2 + "," + selection.z2;
        }

        String msg = status + posInfo;
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ReplaceCommand.selectModePlayers.remove(playerId);
        ReplaceCommand.selections.remove(playerId);
    }
}