package org.maxgamer.maxbans.commands;

import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ToggleChat extends CmdSkeleton implements Listener {
    public static List<String> disable;
    
    static {
        ToggleChat.disable = new ArrayList<>();
    }
    
    public ToggleChat() {
        super("togglechat", "MaxBans.togglechat");
        this.namePos = -1;
    }
    
    public boolean run(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (sender instanceof Player) {
            final Player p = (Player)sender;

            if (ToggleChat.disable.contains(p.getName())) {
                ToggleChat.disable.remove(p.getName());
                p.sendMessage(ChatColor.RED + "Player chat is now unmuted!");
            }
            else {
                ToggleChat.disable.add(p.getName());
                p.sendMessage(ChatColor.GREEN + "All player chat has now been muted!");
            }
        }

        return true;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncChat(final AsyncPlayerChatEvent e) {
        final List<Player> rem = new ArrayList<>();

        for (final Player p : e.getRecipients()) {
            if (ToggleChat.disable.contains(p.getName())) {
                rem.add(p);
            }
        }

        for (final Player p : rem) {
            e.getRecipients().remove(p);
        }
    }
}
