/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package test.jakarta.data.jpa;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.jpa.web.DataJPATestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataJPATest extends FATServletClient {
    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @Server("io.openliberty.data.internal.fat.jpa")
    @TestServlet(servlet = DataJPATestServlet.class, contextRoot = "DataJPATestApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Get driver type
        DatabaseContainerType type = DatabaseContainerType.valueOf(testContainer);
        server.addEnvVar("DB_DRIVER", type.getDriverName());
        server.addEnvVar("REPEAT_PHASE", FATSuite.repeatPhase);

        // Set up server DataSource properties
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, testContainer);

        WebArchive war = ShrinkHelper.buildDefaultApp("DataJPATestApp", "test.jakarta.data.jpa.web");

        if (FATSuite.RepeatWithJPA32.isActive()) {
            //Generated classes that depend on hibernate
            //No easy way to automate the removal of generated classes when running against eclipselink
            //beacuse @Generated is not a runtime annotation.
            war.deleteClass(test.jakarta.data.jpa.web.Manufacturers_.class);
            war.deleteClass(test.jakarta.data.jpa.web.WorkAddresses_.class);
            war.deleteClass(test.jakarta.data.jpa.web.Models_.class);
        }

        if (FATSuite.RepeatWithJPA32Hibernate.isActive()) {
            //TODO the DataContainer feature should configure @Repository as a bean defining annotation, once created remove this.
            String beansContent = """
                            <?xml version="1.0" encoding="UTF-8"?>
                            <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_1.xsd"
                              version="4.1"
                              bean-discovery-mode="all">
                            </beans>
                            """;

            war.addAsWebInfResource(new StringAsset(beansContent), "beans.xml");

            //Create a persistence unit for Hibernate
            String persistenceContent = """
                            <?xml version="1.0" encoding="UTF-8"?>
                            <persistence
                              xmlns="https://jakarta.ee/xml/ns/persistence"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence
                              https://jakarta.ee/xml/ns/persistence/persistence_3_2.xsd"
                              version="3.2">

                              <persistence-unit name="DEFAULT_PU_HIBERNATE">
                                <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
                                <jta-data-source>java:comp/DefaultDataSource</jta-data-source>
                                <properties>
                                      <property name="jakarta.persistence.schema-generation.database.action" value="drop-and-create" />
                                </properties>
                              </persistence-unit>

                              <persistence-unit name="DSD_PU_HIBERNATE">
                                <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
                                <jta-data-source>java:module/jdbc/RepositoryDataStore</jta-data-source>
                                <properties>
                                      <property name="jakarta.persistence.schema-generation.database.action" value="drop-and-create" />
                                </properties>
                              </persistence-unit>
                            </persistence>
                            """;
            war.addAsManifestResource(new StringAsset(persistenceContent), "persistence.xml");
        }

        ShrinkHelper.exportAppToServer(server, war, DeployOptions.OVERWRITE);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // TODO if we decide to add the ability to put Jakarta Data properties onto DataSourceDefinition properties,
        // then an update will be needed to com.ibm.ws.jdbc.internal.JDBCDriverService.create to ignore them for the data source:
        // W DSRA8020E: Warning: The property 'data.createTables' does not exist on the DataSource class ...
        server.stopServer("DSRA8020E.*data.createTables",
                          "DSRA8020E.*data.dropTables",
                          "DSRA8020E.*data.tablePrefix");
    }
}
