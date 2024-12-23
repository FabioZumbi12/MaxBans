package org.maxgamer.maxbans;

import org.maxgamer.maxbans.commands.UnbanRangeCommand;
import org.maxgamer.maxbans.commands.TempRangeBanCommand;
import org.maxgamer.maxbans.commands.RangeBanCommand;
import org.maxgamer.maxbans.commands.ImmuneCommand;
import org.maxgamer.maxbans.commands.WhitelistCommand;
import org.maxgamer.maxbans.commands.ReloadCommand;
import org.maxgamer.maxbans.commands.MBDebugCommand;
import org.maxgamer.maxbans.commands.MBExportCommand;
import org.maxgamer.maxbans.commands.MBImportCommand;
import org.maxgamer.maxbans.commands.HistoryCommand;
import org.maxgamer.maxbans.commands.MBCommand;
import org.maxgamer.maxbans.commands.ForceSpawnCommand;
import org.maxgamer.maxbans.commands.KickCommand;
import org.maxgamer.maxbans.commands.LockdownCommand;
import org.maxgamer.maxbans.commands.ClearWarningsCommand;
import org.maxgamer.maxbans.commands.UnWarnCommand;
import org.maxgamer.maxbans.commands.WarnCommand;
import org.maxgamer.maxbans.commands.DupeIPCommand;
import org.maxgamer.maxbans.commands.CheckBanCommand;
import org.maxgamer.maxbans.commands.CheckIPCommand;
import org.maxgamer.maxbans.commands.UUID;
import org.maxgamer.maxbans.commands.UnMuteCommand;
import org.maxgamer.maxbans.commands.UnbanCommand;
import org.maxgamer.maxbans.commands.TempMuteCommand;
import org.maxgamer.maxbans.commands.TempIPBanCommand;
import org.maxgamer.maxbans.commands.TempBanCommand;
import org.maxgamer.maxbans.commands.MuteCommand;
import org.maxgamer.maxbans.commands.IPBanCommand;
import org.maxgamer.maxbans.commands.BanCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.maxgamer.maxbans.bungee.BungeeListener;
import org.maxgamer.maxbans.commands.ToggleChat;
import org.maxgamer.maxbans.banmanager.SyncBanManager;

import java.io.*;

import org.maxgamer.maxbans.database.DatabaseCore;
import org.maxgamer.maxbans.database.SQLiteCore;
import org.maxgamer.maxbans.database.MySQLCore;
import org.maxgamer.maxbans.metrics.Metrics;
import org.maxgamer.maxbans.util.Formatter;
import org.bukkit.Bukkit;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.maxgamer.maxbans.database.Database;
import org.maxgamer.maxbans.listeners.ChatCommandListener;
import org.maxgamer.maxbans.listeners.ChatListener;
import org.maxgamer.maxbans.listeners.HeroChatListener;
import org.maxgamer.maxbans.listeners.JoinListener;
import org.maxgamer.maxbans.geoip.GeoIPDatabase;
import org.maxgamer.maxbans.sync.SyncServer;
import org.maxgamer.maxbans.sync.Syncer;
import org.maxgamer.maxbans.banmanager.BanManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MaxBans extends JavaPlugin {
    public static final String BUNGEE_CHANNEL = "BungeeCord";
    private BanManager banManager;
    private Syncer syncer;
    private SyncServer syncServer;
    private GeoIPDatabase geoIPDB;
    private JoinListener joinListener;
    private HeroChatListener herochatListener;
    private ChatListener chatListener;
    private ChatCommandListener chatCommandListener;
    private Database db;
    public boolean filter_names;
    public static MaxBans instance;
    
    public GeoIPDatabase getGeoDB() {
        return this.geoIPDB;
    }
    
    public void onEnable() {
        MaxBans.instance = this;

        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }

        final File configFile = new File(this.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            this.saveResource("config.yml", false);
        }

        this.reloadConfig();
        Msg.reload();
        this.getConfig().options().copyDefaults();
        final File geoCSV = new File(this.getDataFolder(), "GeoIPCountryWhois.csv");

        if (!geoCSV.exists()) {
            final Runnable download = new Runnable() {
                public void run() {
                    MaxBans.this.getLogger().info("Downloading geoIPDatabase...");

                    try {
                        File zipLocal = new File(MaxBans.this.getDataFolder(), "GeoIPCountryCSV.zip");
                        URL url = new URL("http://geolite.maxmind.com/download/geoip/database/GeoIPCountryCSV.zip");
                        URLConnection conn = url.openConnection();
                        InputStream in = conn.getInputStream();
                        FileOutputStream out = new FileOutputStream(zipLocal);
                        byte[] b = new byte[1024];
                        int count;
                        while ((count = in.read(b)) >= 0) {
                            out.write(b, 0, count);
                        }
                        out.flush(); out.close(); in.close();

                        MaxBans.this.getLogger().info("Download complete.");

                        //unzip
                        File zipFile = new File(MaxBans.this.getDataFolder(), "GeoIPCountryCSV.zip");
                        unZipIt(zipFile.getAbsolutePath());

                        //delete zip
                        zipLocal.delete();

                        MaxBans.access$0(MaxBans.this, new GeoIPDatabase(geoCSV));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Failed to download MaxBans GeoIPDatabase");
                    }
                }
            };
            Bukkit.getScheduler().runTaskAsynchronously(this, download);
        }
        else {
            this.geoIPDB = new GeoIPDatabase(geoCSV);
        }

        this.filter_names = this.getConfig().getBoolean("filter-names");
        Formatter.load(this);
        final ConfigurationSection dbConfig = this.getConfig().getConfigurationSection("database");
        DatabaseCore dbCore;

        if (this.getConfig().getBoolean("database.mysql", false)) {
            this.getLogger().info("Using MySQL");
            final String user = dbConfig.getString("user");
            final String pass = dbConfig.getString("pass");
            final String host = dbConfig.getString("host");
            final String name = dbConfig.getString("name");
            final String port = dbConfig.getString("port");
            dbCore = new MySQLCore(host, user, pass, name, port);
        }
        else {
            this.getLogger().info("Using SQLite");
            dbCore = new SQLiteCore(new File(this.getDataFolder(), "bans.db"));
        }

        final boolean readOnly = dbConfig.getBoolean("read-only", false);

        try {
            this.db = new Database(dbCore) {
                public void execute(final String query, final Object... objs) {
                    if (readOnly) {
                        return;
                    }

                    super.execute(query, objs);
                }
            };
        }
        catch (Database.ConnectionException e1) {
            e1.printStackTrace();
            System.out.println("Failed to create connection to database. Disabling MaxBans :(");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        final ConfigurationSection syncConfig = this.getConfig().getConfigurationSection("sync");

        if (syncConfig.getBoolean("use", false)) {
            this.getLogger().info("Using Sync.");
            final String host = syncConfig.getString("host");
            final int port2 = syncConfig.getInt("port");
            final String pass2 = syncConfig.getString("pass");

            if (syncConfig.getBoolean("server", false)) {
                try {
                    (this.syncServer = new SyncServer(port2, pass2)).start();
                }
                catch (IOException e2) {
                    e2.printStackTrace();
                    this.getLogger().info("Could not start sync server!");
                }
            }

            (this.syncer = new Syncer(host, port2, pass2)).start();
            this.banManager = new SyncBanManager(this);
        }
        else {
            this.banManager = new BanManager(this);
        }

        this.registerCommands();
        Bukkit.getServer().getPluginManager().registerEvents(new ToggleChat(), this);

        if (Bukkit.getPluginManager().getPlugin("Herochat") != null) {
            this.getLogger().info("Found Herochat... Hooking!");
            this.herochatListener = new HeroChatListener(this);
            Bukkit.getServer().getPluginManager().registerEvents(this.herochatListener, this);
        }
        else {
            this.chatListener = new ChatListener(this);
            Bukkit.getServer().getPluginManager().registerEvents(this.chatListener, this);
        }

        this.joinListener = new JoinListener();
        this.chatCommandListener = new ChatCommandListener();
        Bukkit.getServer().getPluginManager().registerEvents(this.joinListener, this);
        Bukkit.getServer().getPluginManager().registerEvents(this.chatCommandListener, this);

        if (this.isBungee()) {
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeListener());
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }

        // Metrics
        try {
            Metrics metrics = new Metrics(this, 24206);

            var bans = new HashMap<String, Integer>();
            bans.put("Bans", MaxBans.this.getBanManager().getBans().size());
            bans.put("IP_Bans", MaxBans.this.getBanManager().getIPBans().size());
            bans.put("Range_Bans", MaxBans.this.getBanManager().getRangeBans().size());
            metrics.addCustomChart(new Metrics.MultiLineChart("Bans", () -> bans));

            var tempBans = new HashMap<String, Integer>();
            tempBans.put("Temp_Bans", MaxBans.this.getBanManager().getTempBans().size());
            tempBans.put("Temp_IP_Bans", MaxBans.this.getBanManager().getTempIPBans().size());
            metrics.addCustomChart(new Metrics.MultiLineChart("Temp_Bans", () -> tempBans));

            var mutes = new HashMap<String, Integer>();
            mutes.put("Mutes", MaxBans.this.getBanManager().getMutes().size());
            mutes.put("Temp_Mutes", MaxBans.this.getBanManager().getTempMutes().size());
            metrics.addCustomChart(new Metrics.MultiLineChart("Mutes", () -> mutes));

            if (metrics.isEnabled())
                this.getLogger().info("Metrics enabled! See our stats here: https://bstats.org/plugin/bukkit/MaxBans");
        } catch (Exception ex) {
            this.getLogger().info("Metrics not enabled due errors: " + ex.getLocalizedMessage());
        }
    }
    
    public boolean isBungee() {
        return MaxBans.instance.getConfig().getBoolean("bungee");
    }
    
    public void onDisable() {
        this.getLogger().info("Disabling Maxbans...");

        if (this.syncer != null) {
            this.syncer.stop();
            this.syncer = null;
        }

        if (this.syncServer != null) {
            this.syncServer.stop();
            this.syncServer = null;
        }

        this.getLogger().info("Clearing buffer...");
        this.db.close();
        this.getLogger().info("Cleared buffer...");
        MaxBans.instance = null;
    }
    
    public BanManager getBanManager() {
        return this.banManager;
    }
    
    public Database getDB() {
        return this.db;
    }
    
    public void registerCommands() {
        new BanCommand();
        new IPBanCommand();
        new MuteCommand();
        new TempBanCommand();
        new TempIPBanCommand();
        new TempMuteCommand();
        new UnbanCommand();
        new UnMuteCommand();
        new UUID();
        new CheckIPCommand();
        new CheckBanCommand();
        new DupeIPCommand();
        new WarnCommand();
        new UnWarnCommand();
        new ClearWarningsCommand();
        new LockdownCommand();
        new KickCommand();
        new ForceSpawnCommand();
        new MBCommand();
        new HistoryCommand();
        new MBImportCommand();
        new MBExportCommand();
        new MBDebugCommand();
        new ReloadCommand();
        new WhitelistCommand();
        new ImmuneCommand();
        new RangeBanCommand();
        new TempRangeBanCommand();
        new UnbanRangeCommand();
    }

    public Syncer getSyncer() {
        return this.syncer;
    }
    
    static /* synthetic */ void access$0(final MaxBans maxBans, final GeoIPDatabase geoIPDB) {
        maxBans.geoIPDB = geoIPDB;
    }

    /**
     * Unzip it
     * @param zipFile input zip file
     */
    public void unZipIt(String zipFile){

        byte[] buffer = new byte[1024];

        try{
            //get the zip file content
            ZipInputStream zis =
                    new ZipInputStream(new FileInputStream(zipFile));
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while(ze!=null){

                String fileName = ze.getName();
                File newFile = new File(this.getDataFolder(), File.separator + fileName);

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
}
