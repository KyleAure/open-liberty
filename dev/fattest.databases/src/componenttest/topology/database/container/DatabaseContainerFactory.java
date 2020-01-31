/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database.container;

import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Consumer;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a factory class that creates database test-containers.
 * The test container returned will be based on the {fat.bucket.db.type}
 * system property. </br>
 *
 * The {fat.bucket.db.type} property is set to different databases
 * by our test infrastructure when a fat-suite is enlisted in
 * database rotation by setting the property {fat.test.databases} to true.</br>
 *
 * <br> Container Information: <br>
 * 
 * DB2: Uses <a href="https://hub.docker.com/r/ibmcom/db2">Offical DB2 Container</a> <br> 
 * Derby: Uses a derby no-op test container <br>
 * DerbyClient: Uses a derby no-op test container <br>
 * Oracle: TODO replace this container with the official oracle-xe container if/when it is available without a license. <br>
 * Postgres: Uses <a href="https://hub.docker.com/_/postgres">Offical Postgres Container</a> <br>
 * SQLServer: Uses <a href="https://hub.docker.com/_/microsoft-mssql-server">Offical Microsoft SQL Container</a> <br>
 *
 * @see DatabaseContainerType
 */
public class DatabaseContainerFactory {
    private static final Class<DatabaseContainerFactory> c = DatabaseContainerFactory.class;
    
    private boolean derbyOnDisk = false;
    private DatabaseContainerType defaultType = DatabaseContainerType.Derby;
    private LibertyServer serv;
    
    /**
     * For both derby embedded and derby client:<br>
     * 
     * By default derby database will be created using "memory:test" as the database name.
     * This will create an inMemory database that will not persist between server restarts. <br>
     * 
     * To enable a persistent database call this method, and we will instead use database name
     * ${shared.resource.dir}/data/testdb as the save location.  The database will persist between 
     * server restarts, but will be cleaned up after test suite has finished running. <br>
     * 
     * @param serv - LibertyServer instance so we know where to save, look for, and clean up this database
     * 
     * @return this
     */
    public DatabaseContainerFactory withDerbyOnDisk(LibertyServer serv) {
    	Log.info(c, "withDerbyOnDisk", "KJA1017 serv " + serv);
    	derbyOnDisk = true;
    	this.serv = serv;
    	return this;
    }
    
    /**
     * When creating a database container if the system property {fat.bucket.db.type} is 
     * not set we will default to Derby Embedded.  If you do not want this to be the case,
     * use this method to tell us what type we should default to. <br>
     * 
     * Most useful if you want to test against DerbyClient by default.  <br>
     * 
     * @param defaultType - test container type
     * 
     * @return this
     */
    public DatabaseContainerFactory withDefaultDatabaseType(DatabaseContainerType defaultType) {
    	this.defaultType = defaultType;
    	return this;
    }

    /**
     * Used for <b>database rotation testing</b>.
     *
     * Reads the {fat.bucket.db.type} system property and returns a container based on that property.
     * [DB2, DerbyEmbedded, DerbyClient, Oracle, Postgres, SQLServer] <br>
     *
     * If {fat.bucket.db.type} is not set with a value, default to Derby Embedded. 
     * To change the default option use {@link #withDefaultDatabaseType(DatabaseContainerType)} <br>
     *
     * @return JdbcDatabaseContainer - The test container.
     *
     * @throws IllegalArgumentException - if database rotation {fat.test.databases} is not set or is false,
     *                                      or database type {fat.bucket.db.type} is unsupported.
     */
    public JdbcDatabaseContainer<?> create() throws IllegalArgumentException {
        String dbRotation = System.getProperty("fat.test.databases");
        String dbProperty = System.getProperty("fat.bucket.db.type", defaultType.name());

        Log.info(c, "create", "System property: fat.test.databases is " + dbRotation);
        Log.info(c, "create", "System property: fat.bucket.db.type is " + dbProperty);

        if (!"true".equals(dbRotation)) {
            throw new IllegalArgumentException("To use a generic database, the FAT must be opted into database rotation by setting 'fat.test.databases: true' in the FAT project's bnd.bnd file");
        }

        DatabaseContainerType type = null;
        try {
            type = DatabaseContainerType.valueOf(dbProperty);
            Log.info(c, "create", "FOUND: database testcontainer type: " + type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("No database testcontainer supported for " + dbProperty, e);
        }

        return initContainer(type);
    }

    //Private Method: used to initialize test container.
    private JdbcDatabaseContainer<?> initContainer(DatabaseContainerType dbContainerType) {
        //Check to see if JDBC Driver is available.
        isJdbcDriverAvailable(dbContainerType);

        //Create container
        JdbcDatabaseContainer<?> cont = null;
        Class<?> clazz = dbContainerType.getContainerClass();

        try {
	        switch (dbContainerType) {
	            case DB2:
	                cont = (JdbcDatabaseContainer<?>) clazz.getConstructor().newInstance();
	                //Accept License agreement
	            	Method acceptDB2License = cont.getClass().getMethod("acceptLicense");
	            	acceptDB2License.invoke(cont);
	            	//Add startup timeout since DB2 tends to take longer than the default 3 minutes on build machines. 
	            	Method withStartupTimeout = cont.getClass().getMethod("withStartupTimeout", Duration.class);
	            	withStartupTimeout.invoke(cont, Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 5 : 15));
	                break;
	            case Derby:
	            	cont = (JdbcDatabaseContainer<?>) clazz.getConstructor().newInstance();
	            	if (derbyOnDisk) {
	            		Method setOnDiskLocation = cont.getClass().getMethod("setOnDiskLocation", LibertyServer.class);
	            		setOnDiskLocation.invoke(cont, serv);
	            	}
	                break;
				case DerbyClient:
					cont = (JdbcDatabaseContainer<?>) clazz.getConstructor().newInstance();
	            	if (derbyOnDisk) {
	            		Method setOnDiskLocation = cont.getClass().getMethod("setOnDiskLocation", LibertyServer.class);
	            		setOnDiskLocation.invoke(cont, serv);
	            	}
					break;
	            case Oracle:          	
	            	cont = (JdbcDatabaseContainer<?>) clazz.getConstructor(String.class).newInstance("oracleinanutshell/oracle-xe-11g");
	                break;
	            case Postgres:
	            	cont = (JdbcDatabaseContainer<?>) clazz.getConstructor().newInstance();
	            	//This allows postgres by default to participate in XA transactions (2PC). 
	            	//Documentation on the Prepare Transaction action in postgres: https://www.postgresql.org/docs/9.3/sql-prepare-transaction.html
	            	
	            	//If a test is failing that is using XA connections check to see if postgres is failing due to:
	            	// ERROR: prepared transaction with identifier "XXX" does not exist STATEMENT: ROLLBACK PREPARED 'XXX'
	            	// then this value may need to be increased. 
	            	Method withCommand = cont.getClass().getMethod("withCommand", String.class);
	            	withCommand.invoke(cont, "postgres -c max_prepared_transactions=5");
	                break;
	            case SQLServer:
	            	cont = (JdbcDatabaseContainer<?>) clazz.getConstructor().newInstance();
	            	//Accept license agreement
	            	Method acceptSQLServerLicense = cont.getClass().getMethod("acceptLicense");
	            	acceptSQLServerLicense.invoke(cont);
	            	//Init Script
	            	Method initScript = cont.getClass().getMethod("withInitScript", String.class);
	            	initScript.invoke(cont, "resources/init-sqlserver.sql");
	                break;
				default:
					break;
	        }
	        
	        //Allow each container to log to output.txt
	        Method withLogConsumer = cont.getClass().getMethod("withLogConsumer", Consumer.class);
	        withLogConsumer.invoke(cont, (Consumer<OutputFrame>) dbContainerType::log);
        
        } catch (Exception e) {
        	throw new RuntimeException("Unable to create a " + dbContainerType.name() + " TestContainer instance", e);
        }
        
        return cont;
    }

    /**
     * Check to see if the JDBC driver necessary for this test-container is in the location
     * where the server expects to find it. <br>
     *
     * JDBC drivers are not publicly available for some databases. In those cases the
     * driver will need to be provided by the user to run this test-container. <br>
     *
     * @return boolean - true if and only if driver exists. Otherwise, false.
     */
    private boolean isJdbcDriverAvailable(DatabaseContainerType type) {
        File temp = new File("publish/shared/resources/jdbc/" + type.getDriverName());
        boolean result = temp.exists();

        if (result) {
            Log.info(c, "isJdbcDriverAvailable", "FOUND: " + type + " JDBC driver in location: " + temp.getAbsolutePath());
        } else {
            Log.warning(c, "MISSING: " + type + " JDBC driver not in location: " + temp.getAbsolutePath());
        }

        return result;
    }
}
