package com.github.albardoo02;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class ReplaceCommand implements CommandExecutor {

    private String prefix;
    private Selection currentSelection;
    private final FileConfiguration config;
    private final SimpleReplace plugin;
    public ReplaceCommand(SimpleReplace plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        String prefixConfig = plugin.getConfig().getString("Prefix");
        if (prefixConfig == null) {
            this.prefix = "§e[§aSimpleReplace§e]§r ";
        } else {
            this.prefix = ChatColor.translateAlternateColorCodes('&',prefixConfig) + " ";
        }
    }
    public static final HashMap<UUID, Selection> selections = new HashMap<>();
    public static final HashSet<UUID> selectModePlayers = new HashSet<>();
    private static final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("help message");
            if (player.hasPermission("simplereplace.admin")) {
                sendMessage(player, "/simplereplace <Command>");
            } else {
                sendMessage(player, "&aSimpleReplace &fv" + plugin.getDescription().getVersion());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',"&3サブコマンドを実行する権限がありません"));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("wand")) {
            if (player.hasPermission("simplereplace.admin")) {
                giveWand(player);
            }
            else {
                sendMessage(player, "&c権限がありません");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("select")) {
            if (player.hasPermission("simplereplace.admin")) {
                toggleSelectMode(player);
            } else {
                sendMessage(player, "&c権限がありません");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("register")) {
            if (player.hasPermission("simplereplace.admin")) {
                if (args.length == 1) {
                    sendMessage(player, "&6/sr register <範囲名>");
                    return true;
                }
                saveRange(player, args[1]);
            } else {
                sendMessage(player, "&c権限がありません");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("execute")) {
            if (player.hasPermission("simplereplace.admin")) {
                if (args.length == 1) {
                    sendMessage(player, "&6/sr execute <範囲名> [ブロックID]");
                    return true;
                }
                String blockName = (args.length >= 3) ? args[2] : "BEDROCK";
                setBlock(player, args[1], blockName);
            } else {
                sendMessage(player, "&c権限がありません");
            }
            return true;
        }
        else {
            sendMessage(player, "&c" + args[0] + "というコマンドはありません");
        }
        return true;
    }

    private void giveWand(Player player) {
        ItemStack wand = new ItemStack(Material.STONE_AXE);
        player.getInventory().addItem(wand);
        sendMessage(player, "&dLeft click: select pos #1; Right click: select pos #2");
    }

    private void toggleSelectMode(Player player) {
        UUID playerId = player.getUniqueId();
        if (selectModePlayers.contains(playerId)) {
            selectModePlayers.remove(playerId);
            sendMessage(player, "&f選択モードを&c無効&fにしました");
        } else {
            selectModePlayers.add(playerId);
            sendMessage(player, "&f選択モードを&a有効&fにしました");
            sendMessage(player, "&f石の斧で範囲を選択してください");
        }
    }

    private void saveRange(Player player, String rangeName) {

        UUID playerId = player.getUniqueId();

        if (!selections.containsKey(playerId)) {
            sendMessage(player, ChatColor.RED + "設定範囲を先に選択してください");
            return;
        }

        Selection sel = selections.get(playerId);
        String path = "ranges." + rangeName;

        config.set(path + ".world", player.getWorld().getName());
        config.set(path + ".pos1.x", sel.x1);
        config.set(path + ".pos1.y", sel.y1);
        config.set(path + ".pos1.z", sel.z1);
        config.set(path + ".pos2.x", sel.x2);
        config.set(path + ".pos2.y", sel.y2);
        config.set(path + ".pos2.z", sel.z2);

        plugin.saveConfig();
        selections.remove(playerId);
        selections.clear();
        sendMessage(player, "&f選択範囲を&b" + rangeName + "&fとして保存しました");
    }

    private void setBlock(Player player, String rangeName, String blockName) {

        String path = plugin.getConfig().getString("ranges." + rangeName);

        if (!config.contains(path)) {
            sendMessage(player, "&c" + rangeName + "という範囲名はありません");
            return;
        }

        String worldName = config.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            sendMessage(player, "&c" + worldName + "が見つかりません");
            return;
        }

        int x1 = config.getInt(path + ".pos1.x");
        int y1 = config.getInt(path + ".pos1.y");
        int z1 = config.getInt(path + ".pos1.z");
        int x2 = config.getInt(path + ".pos2.x");
        int y2 = config.getInt(path + ".pos2.y");
        int z2 = config.getInt(path + ".pos2.z");

        Selection selection = new Selection(x1, y1, z1, x2, y2, z2);

        Material blockType;
        try {
            blockType = Material.valueOf(blockName.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendMessage(player, "&c" + blockName + "というブロックIDは見つかりません");
            return;
        }
        sendMessage(player, "&b" + rangeName + "&fの範囲内のブロックを" + blockType.name() + "に置き換えています...");

        new BlockFillTask(plugin, player, world, selection, blockType).runTaskTimer(plugin, 0L, 1L);
    }

    public static class Selection {
        public int x1, y1, z1;
        public int x2, y2, z2;

        public Selection(int x1, int y1, int z1, int x2, int y2, int z2) {
            this.x1 = Math.min(x1, x2);
            this.y1 = Math.min(y1, y2);
            this.z1 = Math.min(z1, z2);
            this.x2 = Math.max(x1, x2);
            this.y2 = Math.max(y1, y2);
            this.z2 = Math.max(z1, z2);
        }
    }
}