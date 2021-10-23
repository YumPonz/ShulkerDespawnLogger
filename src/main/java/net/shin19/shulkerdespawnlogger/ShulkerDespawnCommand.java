package net.shin19.shulkerdespawnlogger;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ShulkerDespawnCommand implements CommandExecutor {
    private final ShulkerDespawnLogger plugin;

    public ShulkerDespawnCommand(ShulkerDespawnLogger plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(ShulkerDespawnLogger.CMD_PERMISSION)) {
            sender.sendMessage(ShulkerDespawnLogger.PREFIX + " 権限がありません");
            return false;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ShulkerDespawnLogger.PREFIX + " プレイヤーが実行してください");
            return false;
        }
        if (args.length < 1) {
            sender.sendMessage(ShulkerDespawnLogger.PREFIX + " Usage: /shulker help");
            return false;
        }

        Player player = (Player) sender;
        switch (args[0]) {
            case "respawn":
                if (!player.hasPermission(ShulkerDespawnLogger.CMD_ADMIN_PERMISSION)) {
                    player.sendMessage(ShulkerDespawnLogger.PREFIX + " 権限がありません");
                    return false;
                }
                if (args.length < 2) {
                    player.sendMessage(ShulkerDespawnLogger.PREFIX + " Usage: /shulker respawn <id>");
                    return false;
                } else {
                    int id;
                    try {
                        id = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ShulkerDespawnLogger.PREFIX + " idには整数のみ入力してください");
                        return false;
                    }

                    if (id < 1) {
                        return false;
                    }
                    boolean isForceRespawn = false;
                    if (args.length > 2) {
                        if (args[2].equals("force")) {
                            isForceRespawn = true;
                        }
                    }

                    String sql = "SELECT * FROM ShulkerDespawn WHERE id = ? LIMIT 1";
                    Connection con = plugin.getConnection();
                    ResultSet rs;
                    try {
                        PreparedStatement st = Objects.requireNonNull(con).prepareStatement(sql);
                        st.setInt(1, id);

                        rs = st.executeQuery();
                        if (!rs.next()) {
                            player.sendMessage(ShulkerDespawnLogger.PREFIX + " #" + id + " のシュルカーボックスが見つかりませんでした");
                            return false;
                        }

                        if (rs.getInt("rollback") == 1 && !isForceRespawn) {
                            player.sendMessage(ShulkerDespawnLogger.PREFIX + " #" + rs.getInt("id") + " のシュルカーボックスは既に復元済みです\n" +
                                    "再度復元する場合はコマンドの後ろにforceをつけてください");
                            return false;
                        }

                        ItemStack itemStack = new ItemStack(Objects.requireNonNull(Material.getMaterial(rs.getString("name"))));
                        ShulkerBox shulker = (ShulkerBox)((BlockStateMeta) Objects.requireNonNull(itemStack.getItemMeta())).getBlockState();
                        String data = rs.getString("data");
                        Inventory inventory = ShulkerDespawnLogger.decodeInv(data);
                        shulker.getInventory().setContents(Objects.requireNonNull(inventory).getContents());

                        BlockStateMeta meta = (BlockStateMeta) itemStack.getItemMeta();
                        meta.setBlockState(shulker);
                        itemStack.setItemMeta(meta);

                        Objects.requireNonNull(Bukkit.getWorld(rs.getString("world"))).dropItemNaturally(new Location(Bukkit.getWorld(rs.getString("world")), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")), itemStack);
                        TextComponent mainText = new TextComponent(String.format("%s #%d のシュルカーボックスを" + (isForceRespawn ? "強制的に" : "")  + "復元しました ", ShulkerDespawnLogger.PREFIX, rs.getInt("id"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));

                        TextComponent locText = new TextComponent("(x" + rs.getInt("x") + " y" + rs.getInt("y") + " z" + rs.getInt("z") + ")");
                        locText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("#" + rs.getInt("id") + "にテレポート")));
                        locText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + rs.getInt("x") + " " + rs.getInt("y") + " " + rs.getInt("z")));
                        locText.setColor(ChatColor.GRAY);

                        player.spigot().sendMessage(mainText, locText);
                        if (rs.getInt("rollback") != 1) {
                            sql = "UPDATE ShulkerDespawn SET rollback = 1 WHERE id = ?";
                            st = con.prepareStatement(sql);
                            st.setInt(1, id);
                            st.executeUpdate();
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    } finally {
                        try {
                            if (con != null) {
                                con.close();
                            }
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    }
                }
                return true;

            case "view":
                if (args.length < 2) {
                    player.sendMessage(ShulkerDespawnLogger.PREFIX + " Usage: /shulker view <id>");
                    return false;
                } else {
                    int id;
                    try {
                        id = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ShulkerDespawnLogger.PREFIX + " idには整数のみ入力してください");
                        return false;
                    }

                    if (id < 1) {
                        player.sendMessage(ShulkerDespawnLogger.PREFIX + " id " + id + " は存在しません");
                        return false;
                    }

                    String sql = "SELECT * FROM ShulkerDespawn WHERE id = ? LIMIT 1";
                    Connection con = plugin.getConnection();
                    ResultSet rs;
                    try {
                        PreparedStatement st = Objects.requireNonNull(con).prepareStatement(sql);
                        st.setInt(1, id);

                        rs = st.executeQuery();
                        if (!rs.next()) {
                            player.sendMessage(ShulkerDespawnLogger.PREFIX + " #" + id + " のシュルカーボックスが見つかりませんでした");
                            return false;
                        }

                        String data = rs.getString("data");
                        Inventory inventory = ShulkerDespawnLogger.decodeInv(data);

                        Inventory viewerInv = Bukkit.createInventory(null, 27, "Shulker Box Item #" + rs.getInt("id"));
                        viewerInv.setContents(Objects.requireNonNull(inventory).getContents());
                        player.openInventory(viewerInv);
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    } finally {
                        try {
                            if (con != null) {
                                con.close();
                            }
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    }
                }
                return true;

            case "search":
                int radius = 0;
                if (args.length >= 2) {
                    try {
                        radius = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ShulkerDespawnLogger.PREFIX + " Usage: /shulker search <radius>");
                        return false;
                    }

                    if (radius < 0) {
                        player.sendMessage(ShulkerDespawnLogger.PREFIX + " 半径は0以上の数値を入力してください");
                        return false;
                    }
                }

                Connection con = plugin.getConnection();
                ResultSet rs = null;
                if (radius == 0) {
                    String sql = "SELECT * FROM ShulkerDespawn WHERE world = ? ORDER BY id DESC LIMIT 15";
                    PreparedStatement st;
                    try {
                        st = Objects.requireNonNull(con).prepareStatement(sql);
                        st.setString(1, Objects.requireNonNull(player.getLocation().getWorld()).getName());

                        rs = st.executeQuery();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                } else {
                    int senderX = player.getLocation().getBlockX();
                    int senderZ = player.getLocation().getBlockZ();

                    int radiusHalf = (int)Math.ceil((double)radius / 2);

                    int searchX_low = senderX - radiusHalf;
                    int searchX_high = senderX + radiusHalf;

                    int searchZ_low = senderZ - radiusHalf;
                    int searchZ_high = senderZ + radiusHalf;

                    String sql = "SELECT * FROM ShulkerDespawn WHERE (x BETWEEN ? AND ?) AND (z BETWEEN ? AND ?) AND (world = ?) ORDER BY id DESC LIMIT 15";
                    PreparedStatement st;
                    try {
                        st = Objects.requireNonNull(con).prepareStatement(sql);
                        st.setInt(1, searchX_low);
                        st.setInt(2, searchX_high);
                        st.setInt(3, searchZ_low);
                        st.setInt(4, searchZ_high);
                        st.setString(5, Objects.requireNonNull(player.getLocation().getWorld()).getName());

                        rs = st.executeQuery();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
                if (rs != null) {
                    String worldName = player.getLocation().getWorld().getName();
                    try {
                        if (!rs.next()) {
                            player.sendMessage(ShulkerDespawnLogger.PREFIX + " " + ((radius == 0) ? worldName : "r=" + radius) + " 内に消滅したシュルカーボックスは見つかりませんでした");
                            return true;
                        }
                        player.sendMessage(ShulkerDespawnLogger.PREFIX + " --- ShulkerLog at " + worldName + ((radius == 0) ? "" : " (r=" + radius + ")") + " ---");
                        do {
                            TextComponent numberText = new TextComponent("#" + rs.getInt("id"));
                            numberText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("#" + rs.getInt("id") + "のインベントリを開く")));
                            numberText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shulker view " + rs.getInt("id")));
                            numberText.setColor(ChatColor.AQUA);

                            TextComponent separateText = new TextComponent(" : ");
                            TextComponent spaceText = new TextComponent(" ");

                            TextComponent materialText = new TextComponent(rs.getString("name"));

                            LocalDateTime dateTime
                                    = Instant.ofEpochSecond(rs.getInt("time")).atZone(ZoneId.systemDefault()).toLocalDateTime();
                            TextComponent dateText = new TextComponent("[" + dateTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) + "]");
                            dateText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))));

                            TextComponent locText = new TextComponent("(x" + rs.getInt("x") + " y" + rs.getInt("y") + " z" + rs.getInt("z") + ")");
                            locText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("#" + rs.getInt("id") + "にテレポート")));
                            locText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + rs.getInt("x") + " " + rs.getInt("y") + " " + rs.getInt("z")));
                            locText.setColor(ChatColor.GRAY);

                            if (rs.getInt("rollback") == 1) {
                                numberText.setStrikethrough(true);
                                separateText.setStrikethrough(true);
                                spaceText.setStrikethrough(true);
                                materialText.setStrikethrough(true);
                                dateText.setStrikethrough(true);
                                locText.setStrikethrough(true);
                            }

                            player.spigot().sendMessage(numberText, separateText, dateText, spaceText, materialText, spaceText, locText);
                        } while (rs.next());
                        player.sendMessage(ShulkerDespawnLogger.PREFIX + " --- ShulkerDespawnLog ここまで ---");
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
                try {
                    if (con != null) {
                        con.close();
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                return true;

            case "help":
            default:
                player.sendMessage(ShulkerDespawnLogger.PREFIX + " ----- ShulkerDespawnLogger help -----\n" +
                    "/shulker help : これ\n" +
                    "/shulker search <radius> : 自身から<radius>の半径で消失したシュルカーボックスを検索する\n" +
                    "/shulker view <id> : <id>のシュルカーボックスの中身を確認する\n" +
                    ((player.hasPermission(ShulkerDespawnLogger.CMD_ADMIN_PERMISSION) ?
                            "/shulker respawn <id> : <id>のシュルカーボックスを消滅位置に復元する\n" +
                            "/shulker respawn <id> force : すでに復元したシュルカーボックスを強制的に復元する" : "")));
                break;
        }
        return true;
    }
}
