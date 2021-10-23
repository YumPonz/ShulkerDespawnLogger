package net.shin19.shulkerdespawnlogger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShulkerDespawnTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList("help", "search", "view"));
            if (sender.hasPermission(ShulkerDespawnLogger.CMD_ADMIN_PERMISSION)) {
                list.add("respawn");
            }
            return list;
        } else {
            if (args[0].equalsIgnoreCase("respawn")) {
                if (args.length == 3) {
                    return Collections.singletonList("force");
                }
            }
            return Collections.emptyList();
        }
    }
}
