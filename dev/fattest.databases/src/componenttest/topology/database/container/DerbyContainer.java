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

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

import java.util.Properties;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.Machine;

import componenttest.common.apiservices.Bootstrap;
import componenttest.topology.impl.LibertyFileManager;

/**
 * This is a Derby database testcontainer that is returned when attempting to
 * test against derby embedded.
 *
 * This testcontainer overrides the start and stop methods to prevent the
 * creation of a docker container since Derby just runs locally on your system.
 * 
 * This container can be used to create a database connection:
 *      derbyContainer.createConnection("")
 *
 */
class DerbyContainer<SELF extends DerbyContainer<SELF>> extends JdbcDatabaseContainer<SELF> {
	static final String memoryLocation = "memory:testdb";
	static final String dirLocation = "publish/shared/resources/data/testdb";
	
	//Database name and location
	String dbName = memoryLocation;	
	String username = "test";
	String password = "test";

    /**
     * @see DerbyContainer
     */
    public DerbyContainer() {
        super("");
    }

    @Override
    public String getDockerImageName() {
        return "";
    }

    @Override
    public void start() {
    	//DO NOTHING
    }

    @Override
    protected void doStart() {
    	//DO NOTHING
    }

    @Override
    public void stop() {
        final Properties info = new Properties();
        final Driver jdbcDriverInstance = getJdbcDriverInstance();
        
        info.put("user", this.getUsername());
        info.put("password", this.getPassword());
        info.put("drop", "true");

        logger().info("Attempting to drop derby database with name: " + dbName);
        try (Connection conn = jdbcDriverInstance.connect(getJdbcUrl(), info)){
        	//Do nothing this should throw SQLException no matter what
		} catch (SQLException e) {			
			//Successfully dropped derby inMemory
			if(e.getSQLState().equals("08006"))
				logger().info("Successfully dropped derby database with name: " + dbName);
			
			//This is a persistent database
			if(e.getSQLState().equals("XBM0I")) {
				info.remove("drop");
				info.put("shutdown", "true");
				logger().info("Attempting to shutdown and delete derby database with name: " + dbName);
				
				try (Connection conn = jdbcDriverInstance.connect(getJdbcUrl(), info)) {
					//Do nothing this should throw SQLException no matter what
				} catch (SQLException e1) {
					try {
						deleteFolder(new File(dbName));
					} catch (Exception e2) {
						logger().error("Unable to delete file database contents", e2);
					}
				}	
			}
		}
    }

    @Override
    public void close() {
        this.stop();
    }
    
    @Override
	public boolean isRunning() {
		return true;
	}
    
    public SELF withPersistentDerby() {
    	dbName = new File(dirLocation).getAbsolutePath();
    	return self();
    }
    
    @Override
    public SELF withUsername(String username) {
    	this.username = username;
		return self();
    }
    
    @Override
    public SELF withPassword(String password) {
    	this.password = password;
		return self();
    }

	@Override
	public String getJdbcUrl() {
		return "jdbc:derby:" + getDatabaseName();
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}
	
	@Override
	public String getDatabaseName() {
		return dbName;
	}

	@Override
	public Integer getFirstMappedPort() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getContainerIpAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDriverClassName() {
		return "org.apache.derby.jdbc.EmbeddedDriver";
	}
    
    void deleteFolder(File folder) throws Exception {
        Bootstrap bootstrap = Bootstrap.getInstance(new File("bootstrapping.properties"));
        Machine machine = Machine.getLocalMachine();
        String installRoot = LibertyFileManager.getInstallPath(bootstrap);
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/testdb");
    }

	@Override
	protected String getTestQueryString() {
		return "";
	}
}
