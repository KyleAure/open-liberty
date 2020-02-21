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
 * This is a Derby Client database testcontainer that is returned
 * when attempting to test against derby client.
 *
 * This testcontainer overrides the start and stop methods
 * to prevent the creation of a docker container since Derby
 * just runs locally on your system.
 * 
 * This container can be used to create a database connection:
 * 		derbyClientContainer.createConnection("")
 *
 */
class DerbyClientContainer<SELF extends DerbyClientContainer<SELF>> extends DerbyContainer<SELF> {
	
    /**
     * @see DerbyClientContainer
     */
    public DerbyClientContainer() {
        super();
    }
	
	@Override
	public String getJdbcUrl() {
		return "jdbc:derby://" + getContainerIpAddress() + ":" + getFirstMappedPort() + "/" + getDatabaseName();
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
