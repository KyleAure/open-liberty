/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database.container;

/**
 * This is a Derby Client database test container that is returned
 * when attempting to test against derby client.
 *
 * This test container overrides the start and stop methods
 * to prevent the creation of a docker container since Derby
 * just runs locally on your system.
 * 
 * This container can be used to create a database connection:
 * 		derbyContainer.createConnection("")
 *
 */
class DerbyClientContainer<SELF extends DerbyClientContainer<SELF>> extends DerbyContainer<SELF> {

	@Override
	public String getJdbcUrl() {
		return createDatabase ? "jdbc:derby://" + getContainerIpAddress() + ":" + getFirstMappedPort() + "/" + getDatabaseName() + ";create=true"
				: "jdbc:derby://" + getContainerIpAddress() + ":" + getFirstMappedPort() + "/" + getDatabaseName();
	}

	@Override
	public Integer getFirstMappedPort() {
		return 1527;
	}

	@Override
	public String getContainerIpAddress() {
		return "localhost";
	}

	@Override
	public String getDriverClassName() {
		return "org.apache.derby.jdbc.ClientDriver";
	}
}
