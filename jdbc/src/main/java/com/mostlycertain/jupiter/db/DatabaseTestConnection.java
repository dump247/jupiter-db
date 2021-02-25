package com.mostlycertain.jupiter.db;

import java.sql.Connection;

public interface DatabaseTestConnection {
    String getName();

    DatabaseConnectionConfig getConfig();

    Connection getConnection();
}
