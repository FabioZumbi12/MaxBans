package org.maxgamer.maxbans.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.maxgamer.maxbans.util.Formatter;

import java.util.Arrays;

public class MBCommand extends CmdSkeleton {
    public MBCommand() {
        super("mb", null);
        this.namePos = -1;
    }

    public boolean run(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        sender.sendMessage(Formatter.primary + "MaxBans Commands:");
        final CmdSkeleton[] commands = CmdSkeleton.getCommands();
        Arrays.sort(commands);
        CmdSkeleton[] array;

        for (int length = (array = commands).length, i = 0; i < length; ++i) {
            final CmdSkeleton skelly = array[i];

            if (skelly.hasPermission(sender)) {
                sender.sendMessage(String.valueOf(skelly.getUsage()) + " - " + Formatter.primary + skelly.getDescription());
            }
        }
        return true;
    }
}
