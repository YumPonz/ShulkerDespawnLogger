package net.shin19.shulkerdespawnlogger;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class ShulkerDespawnLogger extends JavaPlugin {
    public static final String CMD_PERMISSION = "shulkerdespawnlogger.command";
    public static final String CMD_ADMIN_PERMISSION = "shulkerdespawnlogger.admin";
    public static final String PREFIX = "[Shulker]";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ShulkerDespawnEvent(this), this);
        Objects.requireNonNull(getCommand("shulker")).setExecutor(new ShulkerDespawnCommand(this));
        Objects.requireNonNull(getCommand("shulker")).setTabCompleter(new ShulkerDespawnTabCompleter());

        final String sql = "CREATE TABLE IF NOT EXISTS `ShulkerDespawn` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT," +
                "`time` INTEGER," +
                "`name` TEXT," +
                "`world` TEXT," +
                "`x` INTEGER," +
                "`y` INTEGER," +
                "`z` INTEGER," +
                "`data` TEXT," +
                "`rollback` INTEGER DEFAULT '0'" +
                ");";

        Connection con = getConnection();
        try {
            Statement statement = Objects.requireNonNull(con).createStatement();
            statement.execute(sql);
            statement.close();
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

    @Override
    public void onDisable() { }

    public Connection getConnection() {
        File dbFile = new File(getDataFolder(), "shulker_log.db");
        dbFile.getParentFile().mkdirs();
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String encodeInv(Inventory inv) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
            boos.writeInt(inv.getSize());

            for (int i = 0; i < inv.getSize(); i++) {
                boos.writeObject(inv.getItem(i));
            }

            boos.close();
            return Base64Coder.encodeLines(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Inventory decodeInv(String base64) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
            BukkitObjectInputStream bois = new BukkitObjectInputStream(bais);
            Inventory inv = Bukkit.getServer().createInventory(null, bois.readInt());

            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, (ItemStack) bois.readObject());
            }

            bois.close();
            return inv;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
