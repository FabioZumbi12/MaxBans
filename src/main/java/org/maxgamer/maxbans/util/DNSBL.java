package org.maxgamer.maxbans.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.maxgamer.maxbans.MaxBans;
import org.maxgamer.maxbans.Msg;
import org.maxgamer.maxbans.database.Database;
import org.maxgamer.maxbans.sync.Packet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DNSBL {
    private static long cache_timeout;

    static {
        DNSBL.cache_timeout = 604800000L;
    }

    private final Map<String, CacheRecord> history = new HashMap<>();
    private final List<String> servers = new ArrayList<>();
    private MaxBans plugin;
    private boolean kick;
    private boolean notify;

    public DNSBL(final MaxBans plugin) {
        super();
        this.kick = false;
        this.notify = true;
        this.plugin = plugin;
        final Database db = plugin.getDB();
        this.kick = plugin.getConfig().getBoolean("dnsbl.kick");
        this.notify = plugin.getConfig().getBoolean("dnsbl.notify");
        final List<String> cfgServers = this.plugin.getConfig().getStringList("dnsbl.servers");

        if (cfgServers != null) {
            this.servers.addAll(cfgServers);
        }

        plugin.getLogger().info("Loading proxys...");

        try {
            db.getConnection().close();
            final PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM proxys WHERE created < ?");
            ps.setLong(1, System.currentTimeMillis() - DNSBL.cache_timeout);
            ps.execute();
            final ResultSet rs = db.getConnection().prepareStatement("SELECT * FROM proxys").executeQuery();

            while (rs.next()) {
                final String ip = rs.getString("ip");
                final String statusString = rs.getString("status");
                final long created = rs.getLong("created");
                final DNSStatus status = DNSStatus.valueOf(statusString);

                if (status == null) {
                    plugin.getLogger().info("Invalid proxy status found: " + statusString);
                    db.execute("DELETE FROM proxys WHERE ip = ?", ip);
                } else {
                    final CacheRecord r = new CacheRecord(status, created);
                    this.history.put(ip, r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().info("Could not load proxys...");
        }
    }

    public Map<String, CacheRecord> getHistory() {
        return this.history;
    }

    public void handle(final PlayerLoginEvent event) {
        if (event.getAddress() == null) {
            return;
        }

        this.handle(event.getPlayer(), event.getAddress().getHostAddress());
    }

    public void handle(final Player p, final String address) {
        final CacheRecord r = this.getRecord(address);

        if (r == null) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                public void run() {
                    final CacheRecord r = DNSBL.this.reload(address);

                    if (DNSBL.this.plugin.getSyncer() != null) {
                        final Packet packet = new Packet("dnsbl").put("ip", address);
                        packet.put("status", r.getStatus().toString()).put("created", r.getCreated());
                        DNSBL.this.plugin.getSyncer().broadcast(packet);
                    }

                    if (r.getStatus() == DNSStatus.DENIED) {
                        Bukkit.getScheduler().runTask(DNSBL.this.plugin, new Runnable() {
                            public void run() {
                                if (DNSBL.this.kick && p.isOnline()) {
                                    final String msg = Msg.get("disconnection.you-are-proxied", "ip", address);
                                    p.kickPlayer(msg);
                                }

                                if (DNSBL.this.notify) {
                                    final String msg = Formatter.secondary + p.getName() + Formatter.primary + " (" + Formatter.secondary + address + Formatter.primary + ") is joining from a proxy IP!";

                                    for (final Player p : Bukkit.getOnlinePlayers()) {
                                        if (p.hasPermission("maxbans.notify")) {
                                            p.sendMessage(msg);
                                        }
                                    }
                                }
                                Bukkit.getLogger().info(String.valueOf(p.getName()) + " is using a proxy IP!");
                            }
                        });
                    }
                }
            });
        } else if (r.getStatus() == DNSStatus.DENIED) {
            if (this.notify) {
                final String msg = Formatter.secondary + p.getName() + Formatter.primary + " (" + Formatter.secondary + address + Formatter.primary + ") is joining from a proxy IP!";

                for (final Player pl : Bukkit.getOnlinePlayers()) {
                    if (pl.hasPermission("maxbans.notify")) {
                        pl.sendMessage(msg);
                    }
                }
            }

            Bukkit.getLogger().info(String.valueOf(p.getName()) + " is using a proxy IP!");

            if (this.kick) {
                final String msg = Msg.get("disconnection.you-are-proxied", "ip", address);
                p.kickPlayer(msg);
            }
        }
    }

    public List<String> getServers() {
        return this.servers;
    }

    public CacheRecord getRecord(final String ip) {
        final CacheRecord r = this.history.get(ip);

        if (r == null) {
            return null;
        }

        if (r.hasExpired()) {
            this.plugin.getDB().execute("DELETE FROM proxys WHERE ip = ?", ip);
            return null;
        }

        return r;
    }

    public CacheRecord reload(final String ip) {
        final String[] parts = ip.split("\\.");
        final StringBuilder buffer = new StringBuilder();

        for (String part : parts) {
            buffer.insert(0, '.');
            buffer.insert(0, part);
        }

        final String reverse = buffer.toString();
        CacheRecord r = new CacheRecord(DNSStatus.ALLOWED);

        for (final String server : this.servers) {
            try {
                if (InetAddress.getByName(String.valueOf(reverse) + server) != null) {
                    r = new CacheRecord(DNSStatus.DENIED);
                    break;
                }
            } catch (UnknownHostException ignored) {
            }
        }

        this.setRecord(ip, r);
        return r;
    }

    public void setRecord(final String ip, final CacheRecord r) {
        if (this.getRecord(ip) == null) {
            this.plugin.getDB().execute("INSERT INTO proxys (ip, status, created) VALUES (?, ?, ?)", ip, r.getStatus().toString(), r.getCreated());
        } else {
            this.plugin.getDB().execute("UPDATE proxys SET status = ?, created = ? WHERE ip = ?", r.getStatus().toString(), r.getCreated(), ip);
        }

        this.history.put(ip, r);
    }

    public enum DNSStatus {
        ALLOWED,
        DENIED,
        UNKNOWN
    }

    public static class CacheRecord {
        private final DNSStatus status;
        private final long created;

        public CacheRecord(final DNSStatus status, final long created) {
            super();
            this.status = status;
            this.created = created;
        }

        public CacheRecord(final DNSStatus status) {
            this(status, System.currentTimeMillis());
        }

        public DNSStatus getStatus() {
            return this.status;
        }

        public long getCreated() {
            return this.created;
        }

        public long getExpires() {
            return this.getCreated() + DNSBL.cache_timeout;
        }

        public boolean hasExpired() {
            return System.currentTimeMillis() > this.getExpires();
        }

        public String toString() {
            return this.status.toString();
        }
    }
}
