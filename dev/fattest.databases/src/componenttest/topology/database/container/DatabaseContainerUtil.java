/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database.container;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public final class DatabaseContainerUtil {
    //Logging Constants
    private static final Class<DatabaseContainerUtil> c = DatabaseContainerUtil.class;
    
    private DatabaseContainerUtil() {
    	//No objects should be created from this class
    }
    
    /**
     * See: {@link #configureForDatabase(LibertyServer, JdbcDatabaseContainer)}
     * This allows you to specifiy if you want to use generic or database specific properties. 
     */
    public static void configureForDatabase(LibertyServer serv, JdbcDatabaseContainer<?> cont, boolean useGenericProps) throws CloneNotSupportedException, Exception {
    	//Get server config
    	ServerConfiguration cloneConfig = serv.getServerConfiguration().clone();
    	//Get datasources to be changed
    	List<DataSource> datasources = getDataSources(serv, cloneConfig);
    	//Modify those datasources
    	if (useGenericProps)
    		modifyDataSourcePropsGeneric(datasources, cloneConfig, serv, cont);	
    	else 
    		modifyDataSourcePropsForDatabase(datasources, cloneConfig, serv, cont);	
    }

    /**
     * For use when attempting to use <b>database rotation</b>. <br>
     *
     * Retrieves database specific properties from the provided JdbcDatabaseContainer, such as;
     * username, password, etc. <br>
     *
     * Using the ServerConfiguration API. Retrieves all &lt;dataSource&gt; elements and modifies
     * those that have the <b>fat.modify=true</b> attribute set. <br>
     *
     * This will inject the datasource with generic &lt;properties... &gt; 
     * for the provided JdbcDatabaseContainer. <br>
     *
     * @see com.ibm.websphere.simplicity.config.ServerConfiguration
     *
     * @param serv - LibertyServer server instance being used for this FAT suite.
     * @param cont - JdbcDatabaseContainer instance being used for database connectivity.
     *
     * @throws Exception
     * @throws CloneNotSupportedException
     */
    public static void configureForDatabase(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws CloneNotSupportedException, Exception {    	
    	//Get server config
    	ServerConfiguration cloneConfig = serv.getServerConfiguration().clone();
    	//Get datasources to be changed
    	List<DataSource> datasources = getDataSources(serv, cloneConfig);
    	//Modify those datasources
    	modifyDataSourcePropsGeneric(datasources, cloneConfig, serv, cont);	
    }
    
    /**
     * This utility method will allow you to provide a SQL Statement in the form of 
     *  "CREATE TABLE xxx (col DATATYPE ...)" and this statement will be run in the database
     *  to setup a table resource to be used for testing.
     *  
     * @param cont - database container where SQL Command will be run
     * @param sqlCreateTable - SQL statement to create table
     * @throws SQLException - if connection or statement execution fails
     */
    public static void createTableResource(JdbcDatabaseContainer<?> cont, String sqlCreateTable) throws SQLException {
    	if(!sqlCreateTable.toUpperCase().contains("CREATE TABLE"))
    		throw new IllegalArgumentException("A SQL Statement was provided that ");
    	
    	try (Connection con = cont.createConnection(""); Statement stmt = con.createStatement()) {
    		stmt.executeUpdate(sqlCreateTable);
    	}
    }
    
    private static List<DataSource> getDataSources(LibertyServer serv, ServerConfiguration cloneConfig) {
        //Get a list of datasources that need to be updated
        List<DataSource> datasources = new ArrayList<>();
        
        //Get general datasources
        for (DataSource ds : cloneConfig.getDataSources()) {
            if (ds.getFatModify() != null && ds.getFatModify().equals("true")) {
                datasources.add(ds);
            }
        }

        //Get datasources that are nested under databasestores
        for (DatabaseStore dbs : cloneConfig.getDatabaseStores())
            for (DataSource ds : dbs.getDataSources())
                if (ds.getFatModify() != null && ds.getFatModify().equals("true"))
                    datasources.add(ds);
        
        return datasources;
    }

    /*
     * Creates generic properties for each database
     */
    private static void modifyDataSourcePropsGeneric(List<DataSource> datasources, ServerConfiguration cloneConfig, LibertyServer serv,
                                              JdbcDatabaseContainer<?> cont) throws Exception {
    	//Get database type
    	DatabaseContainerType type = DatabaseContainerType.valueOf(cont);

        //Create general properties
        DataSourceProperties props = new Properties();
        props.setUser(cont.getUsername());
        props.setPassword(cont.getPassword());
        try { props.setServerName(cont.getContainerIpAddress());} 
        	catch (UnsupportedOperationException e) {}
    	try { props.setPortNumber(Integer.toString(cont.getFirstMappedPort()));}
    		catch (UnsupportedOperationException e) {}
    	try { props.setDatabaseName(cont.getDatabaseName());} 
    		catch (UnsupportedOperationException e) {}

        //TODO this should not be required even when using general datasource properties
        // investigating here: https://github.com/OpenLiberty/open-liberty/issues/10066
        if (type.equals(DatabaseContainerType.DB2)) {
            props.setExtraAttribute("driverType", "4");
        }
        
        if (type.equals(DatabaseContainerType.Oracle)) {
        	Class<?> clazz = type.getContainerClass();
        	Method getSid = clazz.getMethod("getSid");
        	props.setDatabaseName((String) getSid.invoke(cont));
        }
        
        if (type.equals(DatabaseContainerType.Derby) || type.equals(DatabaseContainerType.DerbyClient)) {
        	Class<?> clazz = type.getContainerClass();
        	Method createDatabase = clazz.getMethod("createDatabase");
        	props.setExtraAttribute("createDatabase", (String) createDatabase.invoke(cont));
        }
    	
    	for(DataSource ds : datasources) {
            Log.info(c, "setupDataSourceProperties", "FOUND: DataSource to be enlisted in database rotation.  ID: " + ds.getId());            
            //Replace derby properties
            ds.replaceDatasourceProperties(props);	
    	}

        //Update config
        serv.updateServerConfiguration(cloneConfig);
    }
    
    /*
     * Creates properties for specific database
     */
    private static void modifyDataSourcePropsForDatabase(List<DataSource> datasources, ServerConfiguration cloneConfig, LibertyServer serv,
            JdbcDatabaseContainer<?> cont) throws Exception {
    	//Get database type
    	DatabaseContainerType type = DatabaseContainerType.valueOf(cont);
    	
    	//Create properties based on type
    	DataSourceProperties props = type.getDataSourceProps();
        props.setUser(cont.getUsername());
        props.setPassword(cont.getPassword());
        try { props.setServerName(cont.getContainerIpAddress());} 
    		catch (UnsupportedOperationException e) {}
        try { props.setPortNumber(Integer.toString(cont.getFirstMappedPort()));}
			catch (UnsupportedOperationException e) {}
        try { props.setDatabaseName(cont.getDatabaseName());} 
			catch (UnsupportedOperationException e) {}
    	
        if (type.equals(DatabaseContainerType.Oracle)) {
        	Class<?> clazz = type.getContainerClass();
        	Method getSid = clazz.getMethod("getSid");
        	props.setDatabaseName((String) getSid.invoke(cont));
        }
        
        if (type.equals(DatabaseContainerType.Derby) || type.equals(DatabaseContainerType.DerbyClient)) {
        	Class<?> clazz = type.getContainerClass();
        	Method createDatabase = clazz.getMethod("createDatabase");
        	props.setExtraAttribute("createDatabase", (String) createDatabase.invoke(cont));
        }

    	for(DataSource ds : datasources) {
            Log.info(c, "setupDataSourceProperties", "FOUND: DataSource to be enlisted in database rotation.  ID: " + ds.getId());
            //Replace derby properties
            ds.replaceDatasourceProperties(props);
    	}

        //Update config
        serv.updateServerConfiguration(cloneConfig);
    }
}
