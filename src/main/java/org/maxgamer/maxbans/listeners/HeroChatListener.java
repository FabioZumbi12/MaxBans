package org.maxgamer.maxbans.listeners;

import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.Chatter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.maxgamer.maxbans.MaxBans;
import org.maxgamer.maxbans.banmanager.Mute;
import org.maxgamer.maxbans.banmanager.TempMute;
import org.maxgamer.maxbans.util.Util;

public class HeroChatListener implements Listener {
    private final MaxBans plugin;

    public HeroChatListener(final MaxBans plugin) {
        super();
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onHeroChat(final ChannelChatEvent e) {
        final Player p = e.getSender().getPlayer();
        final Mute mute = this.plugin.getBanManager().getMute(p.getName());

        if (mute != null) {
            if (this.plugin.getBanManager().hasImmunity(p.getName())) {
                return;
            }

            if (mute instanceof TempMute) {
                final TempMute tMute = (TempMute) mute;
                p.sendMessage(ChatColor.RED + "You're muted for another " + Util.getTimeUntil(tMute.getExpires()));
            } else {
                p.sendMessage(ChatColor.RED + "You're muted!");
            }

            e.setResult(Chatter.Result.FAIL);
        }
    }
}
