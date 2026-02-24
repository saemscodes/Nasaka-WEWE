package org.briarproject.bramble.db;

import org.briarproject.bramble.api.db.DbException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.db.JdbcUtils.tryToClose;

class Migration50_51 implements Migration<Connection> {

    private static final Logger LOG = getLogger(Migration50_51.class.getName());

    @Override
    public int getStartVersion() {
        return 50;
    }

    @Override
    public int getEndVersion() {
        return 51;
    }

    @Override
    public void migrate(Connection txn) throws DbException {
        Statement s = null;
        try {
            s = txn.createStatement();
            // Add role column to localAuthors
            s.execute("ALTER TABLE localAuthors"
                    + " ADD COLUMN role INT NOT NULL DEFAULT 0");
            // Add role column to contacts
            s.execute("ALTER TABLE contacts"
                    + " ADD COLUMN role INT NOT NULL DEFAULT 0");
            s.close();
        } catch (SQLException e) {
            tryToClose(s, LOG, WARNING);
            throw new DbException(e);
        }
    }
}
