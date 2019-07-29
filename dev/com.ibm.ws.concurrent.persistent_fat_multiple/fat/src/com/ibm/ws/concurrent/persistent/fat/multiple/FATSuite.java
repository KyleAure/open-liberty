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
package com.ibm.ws.concurrent.persistent.fat.multiple;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import com.ibm.websphere.simplicity.Machine;

import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({ MultiplePersistentExecutorsEmbeddedTest.class, MultiplePersistentExecutorsContainerTest.class})
public class FATSuite {
    static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat.multiple");
    static enum DB {DERBY, POSTGRE, DB2, ORACLE};
    static DB database;

    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Delete the Derby-only database that is used by the persistent scheduled executor
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/persistmultidb");
        
        switch(System.getProperty("fat.bucket.db.type", "Derby")) {
        case "Postgre":
        	database = DB.POSTGRE;
        	break;
        case "DB2":
        	database = DB.DB2;
        	break;
        case "Oracle":
        	database = DB.ORACLE;
        	break;
        case "Derby":
        	System.setProperty("fat.bucket.db.type", "Derby");
        	database = DB.DERBY;
        	break;
        default: //If not an approved database
        	System.setProperty("fat.bucket.db.type", "Derby");
        	database = DB.DERBY;
        }

    }
    
	public static GenericContainer<?> getDatabaseContainer(){
    	switch(database) {
		case DB2:
			return new Db2Container();
		case POSTGRE:
			return new PostgreSQLContainer<>();
		case DERBY:
		default:
			throw new IllegalStateException("Fat should not attempt to get a database test container when using a derby embedded database.");
    	}
    }
}