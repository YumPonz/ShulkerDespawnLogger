package net.shin19.shulkerdespawnlogger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

public class ShulkerDespawnEvent implements Listener {

    private final ShulkerDespawnLogger plugin;
    public ShulkerDespawnEvent(ShulkerDespawnLogger plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        if (isShulkerBox(event.getEntity().getItemStack().getType())) {
            ItemStack item = event.getEntity().getItemStack();
            if(item.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta blockStateMeta = (BlockStateMeta)item.getItemMeta();
                if(blockStateMeta.getBlockState() instanceof ShulkerBox) {
                    ShulkerBox shulker = (ShulkerBox)blockStateMeta.getBlockState();
                    Location loc = event.getLocation();
                    if (loc.getWorld() == null) {
                        return;
                    }
                    String blockName = item.getType().name();
                    String worldName = loc.getWorld().getName();
                    int x = loc.getBlockX();
                    int y = loc.getBlockY();
                    int z = loc.getBlockZ();
                    LocalDateTime ldt = LocalDateTime.now();
                    ZonedDateTime zdt = ldt.atZone(ZoneOffset.ofHours(+9));
                    long epochSecond = zdt.toEpochSecond();
                    String inventory = ShulkerDespawnLogger.encodeInv(shulker.getInventory());

                    String sql = "INSERT INTO ShulkerDespawn(time, name, world, x, y, z, data) VALUES(?, ?, ?, ?, ?, ?, ?)";
                    Connection con = plugin.getConnection();
                    try {
                        PreparedStatement statement = Objects.requireNonNull(con).prepareStatement(sql);
                        statement.setInt(1, (int)epochSecond);
                        statement.setString(2, blockName);
                        statement.setString(3, worldName);
                        statement.setInt(4, x);
                        statement.setInt(5, y);
                        statement.setInt(6, z);
                        statement.setString(7, inventory);

                        statement.execute();
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
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryInteract(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Shulker Box Item #")) {
            event.setCancelled(true);
        }
    }

    private boolean isShulkerBox(Material material) {
        switch(material) {
            case SHULKER_BOX:
            case RED_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case WHITE_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
                return true;
            default:
                return false;
        }
    }
}
