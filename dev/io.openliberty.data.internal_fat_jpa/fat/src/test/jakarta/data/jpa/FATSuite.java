/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                DataJPATest.class,
                DataJPATestCheckpoint.class
})
public class FATSuite extends TestContainerSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatWithJPA32())
                    .andWith(new RepeatWithJPA32Hibernate().fullFATOnly());

    public static String repeatPhase = "";

    static class RepeatWithJPA32 extends EmptyAction {
        public static final String ID = "eclipselink";

        public static boolean isActive() {
            return RepeatTestFilter.isRepeatActionActive(ID);
        }

        @Override
        public String getID() {
            return ID;
        }

        @Override
        public String toString() {
            return "ID: " + ID + " Features: data-1.0 + persistence-3.2";
        }

        @Override
        public void setup() {
            super.setup();
            FATSuite.repeatPhase = "jpa32-cfg.xml";
        }
    }

    static class RepeatWithJPA32Hibernate extends EmptyAction {
        public static final String ID = "hibernate";

        public static boolean isActive() {
            return RepeatTestFilter.isRepeatActionActive(ID);
        }

        @Override
        public String getID() {
            return ID;
        }

        @Override
        public String toString() {
            return "ID: " + ID + " Features: persistenceContainer-3.2";
        }

        @Override
        public void setup() {
            super.setup();
            FATSuite.repeatPhase = "hibernate32-cfg.xml";
        }
    }
}
