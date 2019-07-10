package org.maxgamer.maxbans.commands.bridge;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.maxgamer.maxbans.MaxBans;
import org.maxgamer.maxbans.banmanager.Ban;
import org.maxgamer.maxbans.banmanager.IPBan;
import org.maxgamer.maxbans.banmanager.TempBan;
import org.maxgamer.maxbans.banmanager.TempIPBan;

import java.util.Map;

public class VanillaBridge implements Bridge {

    public void export() {
        System.out.println("Exporting to Vanilla bans...");
        final MaxBans plugin = MaxBans.instance;

        for (final Map.Entry<String, Ban> entry : plugin.getBanManager().getBans().entrySet()) {
            if (entry.getValue() instanceof TempBan) {
                continue;
            }

            BanList ban = Bukkit.getServer().getBanList(BanList.Type.NAME);

            if (ban.isBanned(entry.getKey())) {
                continue;
            }

            ban.addBan(entry.getKey(), entry.getValue().getReason(), null, null);
        }

        for (final Map.Entry<String, IPBan> entry2 : plugin.getBanManager().getIPBans().entrySet()) {
            if (entry2.getValue() instanceof TempIPBan) {
                continue;
            }

            BanList ban = Bukkit.getServer().getBanList(BanList.Type.IP);
            ban.addBan(entry2.getKey(), entry2.getValue().getReason(), null, null);
        }
    }

    public void load() {
        System.out.println("Importing from Vanilla bans...");
        final MaxBans plugin = MaxBans.instance;

        for (final BanEntry b : Bukkit.getServer().getBanList(BanList.Type.NAME).getBanEntries()) {
            plugin.getBanManager().ban(b.getTarget(), b.getReason(), b.getSource());
        }

        for (final BanEntry b : Bukkit.getServer().getBanList(BanList.Type.IP).getBanEntries()) {
            plugin.getBanManager().ipban(b.getTarget(), b.getReason(), b.getSource());
        }
    }
}
