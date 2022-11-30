/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.connectors.schedule;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import web.ScheduleTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ScheduleTest {
	
	@Server
	@TestServlet(servlet = ScheduleTestServlet.class, path="application/ScheduleTestServlet")
	public static LibertyServer server;
	
	@BeforeClass
	public static void setup() throws Exception {
		ShrinkHelper.defaultDropinApp(server, "application", "web");
		server.startServer();
	}
	
	@AfterClass
	public static void tearDown() throws Exception {
		server.stopServer();
	}

}