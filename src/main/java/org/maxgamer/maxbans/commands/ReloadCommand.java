package org.maxgamer.maxbans.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.maxgamer.maxbans.util.Formatter;

public class ReloadCommand extends CmdSkeleton {
    public ReloadCommand() {
        super("mbreload", "maxbans.reload");
        this.namePos = -1;
    }

    public boolean run(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        sender.sendMessage(Formatter.secondary + "Reloading MaxBans");
        Bukkit.getPluginManager().disablePlugin(this.plugin);
        Bukkit.getPluginManager().enablePlugin(this.plugin);
        sender.sendMessage(ChatColor.GREEN + "Reload Complete");
        return true;
    }
}
