package org.maxgamer.maxbans.commands.bridge;

import org.maxgamer.maxbans.MaxBans;
import org.maxgamer.maxbans.database.Database;

import java.sql.SQLException;

public class MySQLBridge implements Bridge {
    private final Database db;

    public MySQLBridge(final Database db) {
        super();
        this.db = db;
    }

    public void export() throws SQLException {
        MaxBans.instance.getDB().copyTo(this.db);
    }

    public void load() throws SQLException {
        this.db.copyTo(MaxBans.instance.getDB());
    }
}
