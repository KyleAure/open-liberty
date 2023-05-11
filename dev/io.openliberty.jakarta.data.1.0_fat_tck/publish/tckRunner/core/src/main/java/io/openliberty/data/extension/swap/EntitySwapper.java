package io.openliberty.data.extension.swap;

import java.util.List;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.impl.base.path.BasicPath;

public class EntitySwapper implements ApplicationArchiveProcessor {

    /**
     * Description: List of test classes that deploy application(s) that you need to be customized
     * Example:     EntityTest.class
     */
    List<String> testClasses = List.of("ee.jakarta.tck.data.core.persistence.example.PersistenceEntityTests");
    
    /**
     * Description: Map of paths to entities that need to be replaced within the application
     * Example:     new BasicPath("ee/jakarta/tck/data/standalone/persistence/example/Product.class")
     */
    List<BasicPath> replacedEntities = List.of(new BasicPath("ee/jakarta/tck/data/core/persistence/example/Product.class"));

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if(! testClasses.contains(testClass.getClass().getCanonicalName())){
            return;
        }
        
        for(BasicPath p : replacedEntities) {
            if(! applicationArchive.getContent().containsKey(p)) {
                continue;
            }
            applicationArchive.delete(p);
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                ((ClassContainer<?>) applicationArchive).addClass(loader.loadClass(basicPathToClassPath(p)));
            } catch (IllegalArgumentException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static String basicPathToClassPath(BasicPath path) {
        String classpath = path.get();
        classpath.replace("/", ".");
        classpath.replace(".class", "");
        return classpath;
    }

}
