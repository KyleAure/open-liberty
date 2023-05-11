package io.openliberty.data.extension.append;

import java.util.List;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ClassContainer;

import io.openliberty.data.entity.Entity;

public class ResourceAppender implements ApplicationArchiveProcessor {
    
    //List of test classes that deploy application that you need to customize
    List<String> testClasses = List.of("ee.jakarta.tck.data.core.persistence.example.PersistenceEntityTests");

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        System.out.println("KJA1017 ResourceAppender Entry " + testClass.getName());
        if(testClasses.contains(testClass.getClass().getCanonicalName())){
            System.out.println("KJA1017 Adding entity package");
            ((ClassContainer) archive).addPackage(Entity.class.getPackage());
        }
    }
}
