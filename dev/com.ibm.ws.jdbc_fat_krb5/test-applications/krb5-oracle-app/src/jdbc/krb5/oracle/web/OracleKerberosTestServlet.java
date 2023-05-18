/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.krb5.oracle.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/OracleKerberosTestServlet")
public class OracleKerberosTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/nokrb5")
    DataSource noKrb5;

    @Resource(lookup = "jdbc/krb/basic")
    DataSource krb5DataSource;

    @Resource(lookup = "jdbc/krb/userpass")
    DataSource krb5UPDataSource;

    @Resource(lookup = "jdbc/krb/xa")
    DataSource krb5XADataSource;

    @Resource(lookup = "jdbc/krb/invalidPrincipal")
    DataSource invalidPrincipalDs;

    @Resource(lookup = "jdbc/krb/DataSource")
    DataSource krb5RegularDs;

    @Resource(name = "jdbc/krb/xaRecovery/rc", lookup = "jdbc/krb/xaRecovery", shareable = false)
    DataSource krb5xaRecoveryRC;

    @Resource(name = "jdbc/krb/xaRecovery/ser", lookup = "jdbc/krb/xaRecovery", shareable = false)
    DataSource krb5xaRecoverySer;

    @Resource
    UserTransaction tran;

    /**
     * Getting a connection too soon after the initial ticket is obtained can cause intermittent
     * issues where we getConnection() fails with: Oracle Error ORA-12631
     * These timing issues can be reproduced in a standalone JDBC program, which indicates that
     * we aren't doing anything wrong in the Liberty code, and instead this is due a Oracle driver
     * or DB issue which would require an Oracle support contract to investigate further
     */
    private static Connection getConnectionWithRetry(DataSource ds) throws SQLException {
        SQLException firstEx = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return ds.getConnection();
            } catch (Throwable e) {
                //Get the underlying SQLException
                SQLException found = null;
                for (Throwable t = e; t.getCause() != null; t = t.getCause()) {
                    if (t instanceof SQLException && t.getClass() == SQLException.class) {
                        found = (SQLException) t;
                        break;
                    }
                }
                //If nested SQLException was not found throw original
                if (found == null)
                    throw e;
                //Keep track of the first SQLException we found
                if (firstEx == null)
                    firstEx = found;
                //Check to see if we failed with ORA-12631 if so attempt again
                if (found.getMessage() != null && found.getMessage().contains("ORA-12631")) {
                    System.out.println("getConnection attempt " + attempt + " failed with ORA-12631");
                    waitFor(3000);
                    continue;
                } else {
                    throw e;
                }
            }
        }
        //After 5 attempts, throw the first SQLException we stored
        throw firstEx;
    }

    private static void waitFor(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // see getConnectionWithRetry for reasoning behind wait
        waitFor(750);
        super.doGet(request, response);
    }

    @Test
    @AllowedFFDC
    public void testNonKerberosConnection() throws Exception {
        try (Connection con = getConnectionWithRetry(noKrb5)) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
        }
    }

    /**
     * Get a connection from a javax.sql.ConnectionPoolDataSource
     */
    @Test
    @AllowedFFDC
    public void testKerberosBasicConnection() throws Exception {
        try (Connection con = getConnectionWithRetry(krb5DataSource)) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
        }
    }

    /**
     * Get a connection using a password in server.xml
     * Config updates are done in OracleKerberosTest.java
     */
    public void testKerberosUsingPassword() throws Exception {

        try (Connection con = getConnectionWithRetry(krb5UPDataSource)) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
        }
    }

    /**
     * Get a connection from a javax.sql.XADataSource
     */
    @Test
    @AllowedFFDC
    public void testKerberosXAConnection() throws Exception {
        try (Connection con = getConnectionWithRetry(krb5XADataSource)) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
        }
    }

    /**
     * Get a connection from a javax.sql.DataSource
     */
    @Test
    @AllowedFFDC
    public void testKerberosRegularConnection() throws Exception {
        try (Connection con = getConnectionWithRetry(krb5RegularDs)) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
        }
    }

    @Test
    @AllowedFFDC
    public void testInvalidPrincipal() throws Exception {
        try (Connection con = invalidPrincipalDs.getConnection()) {
            con.createStatement().execute("SELECT 1 FROM DUAL");
            fail("Should not be able to get a connection using an invalid principal");
        } catch (SQLException expected) {
            Throwable cause = expected.getCause();
            assertNotNull("Expected SQLException to have a cause", cause);
            assertEquals("javax.resource.ResourceException", cause.getClass().getCanonicalName());

            cause = cause.getCause();
            assertNotNull("Expected ResourceException to have a cause", cause);
            assertTrue("Expected cause to be instanceof LoginException but was: " + cause.getClass().getCanonicalName(),
                       cause instanceof LoginException);
        }
    }

    /**
     * Get two connection handles from the same datasource.
     * Ensure that both connection handles share the same managed connection (i.e. physical connection)
     * to prove that Subject reuse is working
     */
    @Test
    @AllowedFFDC
    public void testConnectionReuse() throws Exception {
        String managedConn1 = null;
        String managedConn2 = null;

        try (Connection conn = getConnectionWithRetry(krb5DataSource)) {
            managedConn1 = getManagedConnectionID(conn);
            System.out.println("Managed connection 1 is: " + managedConn1);
        }

        try (Connection conn = getConnectionWithRetry(krb5DataSource)) {
            managedConn2 = getManagedConnectionID(conn);
            System.out.println("Managed connection 2 is: " + managedConn2);
        }

        assertEquals("Expected two connections from the same datasource to share the same underlying managed connection",
                     managedConn1, managedConn2);
    }

    /**
     * Cause an in-doubt transaction and verify that XA recovery resolves it.
     * The recoveryAuthData should be used for recovery.
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "javax.transaction.xa.XAException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
    public void testXARecovery() throws Throwable {
        initTable(krb5xaRecoveryRC);
        Connection[] cons = new Connection[3];
        tran.begin();
        try {
            // Use unsharable connections, so that they all get their own XA resources
            cons[0] = getConnectionWithRetry(krb5xaRecoverySer);
            cons[1] = getConnectionWithRetry(krb5xaRecoverySer);
            cons[2] = getConnectionWithRetry(krb5xaRecoverySer);

//            assertEquals("Isolation level must be 2 (READ_COMMITTED) for participation in distributed transaction in Oracle",
//                         Connection.TRANSACTION_READ_COMMITTED, cons[0].getTransactionIsolation());

            String dbProductName = cons[0].getMetaData().getDatabaseProductName().toUpperCase();
            System.out.println("Product Name is " + dbProductName);

            try (PreparedStatement pstmt = cons[0].prepareStatement("insert into cities values (?, ?, ?)")) {
                pstmt.setString(1, "Edina");
                pstmt.setInt(2, 47941);
                pstmt.setString(3, "Hennepin");
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = cons[1].prepareStatement("insert into cities values (?, ?, ?)")) {
                pstmt.setString(1, "St. Louis Park");
                pstmt.setInt(2, 45250);
                pstmt.setString(3, "Hennepin");
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = cons[2].prepareStatement("insert into cities values (?, ?, ?)")) {
                pstmt.setString(1, "Moorhead");
                pstmt.setInt(2, 38065);
                pstmt.setString(3, "Clay");
                pstmt.executeUpdate();
            }

            System.out.println("Intentionally causing in-doubt transaction");
            TestXAResource.assignSuccessLimit(1, cons);
            try {
                tran.commit();
                throw new Exception("Commit should not have succeeded because the test infrastructure is supposed to cause an in-doubt transaction.");
            } catch (HeuristicMixedException x) {
                TestXAResource.removeSuccessLimit(cons);
                System.out.println("Caught expected HeuristicMixedException: " + x.getMessage());
            }
        } catch (Throwable x) {
            TestXAResource.removeSuccessLimit(cons);
            try {
                tran.rollback();
            } catch (Throwable t) {
            }
            throw x;
        } finally {
            for (Connection con : cons)
                if (con != null)
                    try {
                        con.close();
                    } catch (Throwable x) {
                    }
        }

        // At this point, the transaction is in-doubt.
        // We won't be able to access the data until the transaction manager recovers
        // the transaction and resolves it.
        //
        // A connection configured with TRANSACTION_SERIALIZABLE is necessary in
        // order to allow the recovery to kick in before using the connection.

        System.out.println("attempting to access data (only possible after recovery)");
        try (Connection con = getConnectionWithRetry(krb5xaRecoverySer)) {
            assertEquals("Isolation level must be 8 (TRANSACTION_SERIALIZABLE) for XA recovery",
                         Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());

            PreparedStatement pstmt = con.prepareStatement("select name, population, county from cities where name = ?");

            /*
             * Poll for results once a second for up to 2 minutes.
             * Most databases will have XA recovery done by this point
             *
             */
            List<String> cities = new ArrayList<>();
            for (int count = 0; cities.size() < 3 && count < 120; Thread.sleep(1000)) {
                if (!cities.contains("Edina")) {
                    pstmt.setString(1, "Edina");
                    if (pstmt.executeQuery().next())
                        cities.add("Edina");
                }

                if (!cities.contains("St. Louis Park")) {
                    pstmt.setString(1, "St. Louis Park");
                    if (pstmt.executeQuery().next())
                        cities.add("St. Louis Park");
                }

                if (!cities.contains("Moorhead")) {
                    pstmt.setString(1, "Moorhead");
                    if (pstmt.executeQuery().next())
                        cities.add("Moorhead");
                }
                count++;
                System.out.println("Attempt " + count + " to retrieve recovered XA data. Current status: " + cities);
                if (cities.size() == 3)
                    break; // success
            }

            if (cities.size() < 3)
                throw new Exception("Missing entry in database. Results: " + cities);
            else
                System.out.println("successfully accessed the data");
        }
    }

    /**
     * clears table of all data to ensure fresh start for this test.
     *
     * @param datasource the data source to clear the table for
     */
    private void initTable(DataSource datasource) throws Exception {
        try (Connection con = getConnectionWithRetry(datasource); Statement st = con.createStatement();) {
            st.execute("drop table cities");
        } catch (SQLException e) {
            //assume table didn't exist
        }

        try (Connection con = getConnectionWithRetry(datasource); Statement st = con.createStatement();) {
            st.execute("create table cities (name varchar(50) not null primary key, population int, county varchar(30))");
        }

        // End the current LTC and get a new one, so that test methods start from the correct place
        tran.begin();
        tran.commit();
    }

    /**
     * Get the managed connection ID of a given Connection
     * The managed connection ID is an implementation detail of Liberty that a real app would never care
     * about, but it's a simple way for us to verify that the underlying managed connections are being
     * reused.
     */
    private String getManagedConnectionID(Connection conn1) {
        for (Class<?> clazz = conn1.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                Field f1 = clazz.getDeclaredField("managedConn");
                f1.setAccessible(true);
                String mc1 = String.valueOf(f1.get(conn1));
                f1.setAccessible(false);
                return mc1;
            } catch (Exception ignore) {
            }
        }
        throw new RuntimeException("Did not find field 'managedConn' on " + conn1.getClass());
    }

}
