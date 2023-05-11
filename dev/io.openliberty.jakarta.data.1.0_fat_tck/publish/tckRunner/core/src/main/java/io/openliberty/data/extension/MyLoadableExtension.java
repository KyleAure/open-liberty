package io.openliberty.data.extension;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

import io.openliberty.data.extension.swap.EntitySwapper;
import io.openliberty.data.extension.append.ResourceAppender;

public class MyLoadableExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        extensionBuilder.service(ApplicationArchiveProcessor.class, EntitySwapper.class);
        extensionBuilder.service(ApplicationArchiveProcessor.class, ResourceAppender.class);
    }

}
