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

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.Machine;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a Derby database test container that is returned when attempting to
 * test against derby embedded.
 *
 * This test container overrides the start and stop methods to prevent the
 * creation of a docker container since Derby just runs locally on your system.
 * 
 * This container can be used to create a database connection:
 *      derbyContainer.createConnection("")
 *
 */
class DerbyContainer<SELF extends DerbyContainer<SELF>> extends JdbcDatabaseContainer<SELF> {
	// Database name and location
	static final String dbName = "testdb";
	static final String inMemoryLocation = "memory:" + dbName;
	
	boolean createDatabase = true;

	// Server used to set save location if using onDisk database
	LibertyServer serv = null;
	String onDiskLocation = null;
	
	private boolean onDisk() {
		return serv == null ? false : true;
	}
	
	public String createDatabase() {
		return createDatabase ? "create" : "false";
	}
	
	public void setCreateDatabase(boolean create) {
		this.createDatabase = create;
	}
	
	public void setOnDiskLocation(LibertyServer serv) {
		this.serv = serv;
		onDiskLocation = serv.getServerSharedPath() + "resources/data/" + dbName;
	}

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
		logger().info("Running " + this.getClass().getName() + " onDisk? " + onDisk());

		if (onDisk()) {
			logger().info(this.getClass().getName() + " saving database on disk in location: " + onDiskLocation);
		}
	}

	@Override
	protected void doStart() {
		// DO NOTHING
	}

	@Override
	public void stop() {
		logger().info("Stop called on " + this.getClass().getName());

		if (onDisk()) {
			logger().info("Attempting to delete saved database data on disk in location: " + onDiskLocation);

			// For each server that has connected to this database
			// Remove the locally saved database data in the server's install root
			Machine machine = serv.getMachine();
			try {
				LibertyFileManager.deleteLibertyDirectoryAndContents(machine, onDiskLocation);
			} catch (Exception e) {
				// Do nothing
			}
		}
	}

	@Override
	public void close() {
		// DO NOTHING
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public String getDatabaseName() {
		if (onDisk())
			return onDiskLocation;
		else
			return inMemoryLocation;
	}

	@Override
	public String getJdbcUrl() {
		return createDatabase ? "jdbc:derby:" + getDatabaseName() + ";create=true" : "jdbc:derby:" + getDatabaseName();
	}

	@Override
	public String getUsername() {
		return "test";
	}

	@Override
	public String getPassword() {
		return "test";
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

	@Override
	protected String getTestQueryString() {
		return "";
	}
}
