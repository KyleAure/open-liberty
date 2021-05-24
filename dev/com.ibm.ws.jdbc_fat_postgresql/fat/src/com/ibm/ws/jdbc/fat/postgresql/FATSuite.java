/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.postgresql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.PostgreSQLContainer;

@RunWith(Suite.class)
@SuiteClasses({
                PostgreSQLTest.class,
                PostgreSQLSSLTest.class
})
public class FATSuite {

    private static final int RETRY_COUNT = FATRunner.FAT_TEST_LOCALRUN ? 2 : 5;

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
    }

    public static void setupDatabase(PostgreSQLContainer postgre) throws SQLException {
        try (Connection conn = getConnectionWithRetry(postgre); Statement stmt = conn.createStatement();) {
            stmt.execute("CREATE TABLE people( id integer UNIQUE NOT NULL, name VARCHAR (50) );");
            stmt.execute("CREATE SCHEMA premadeschema");
        }

    }

    public static Connection getConnectionWithRetry(PostgreSQLContainer postgre) throws SQLException {
        Connection conn = null;
        SQLException sqle = null;
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                conn = postgre.createConnection("");
                break;
            } catch (SQLException x) {
                sqle = sqle == null ? x : sqle;
            }
        }

        if (conn == null) {
            if (sqle != null) {
                throw sqle;
            } else {
                throw new RuntimeException("Not able to get connection to postgre database.");
            }
        } else {
            return conn;
        }
    }
}
