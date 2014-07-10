package io.liveoak.container.tenancy;

import io.liveoak.container.tenancy.service.ApplicationExtensionRemovalService;
import io.liveoak.spi.LiveOak;
import io.liveoak.spi.extension.Extension;
import io.liveoak.spi.resource.async.Resource;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * @author Bob McWhirter
 * @author Ken Finnigan
 */
public class InternalApplicationExtension implements Consumer<Exception> {

    private static Logger log = Logger.getLogger(InternalApplicationExtension.class);

    public InternalApplicationExtension(ServiceRegistry registry, InternalApplication app, String extensionId, String resourceId) {
        this.registry = registry;
        this.app = app;
        this.extensionId = extensionId;
        this.resourceId = resourceId;
    }

    public InternalApplication application() {
        return this.app;
    }

    public String extensionId() {
        return this.extensionId;
    }

    public String resourceId() {
        return this.resourceId;
    }

    public void remove() {
        // unmount them first
        if (this.adminResourceController != null) {
            this.adminResourceController.setMode(ServiceController.Mode.REMOVE);
        }
        if (this.publicResourceController != null) {
            this.publicResourceController.setMode(ServiceController.Mode.REMOVE);
        }

        String appId = this.app.id();
        ServiceController<InternalApplicationExtension> extController = (ServiceController<InternalApplicationExtension>) this.registry.getService(LiveOak.applicationExtension(appId, this.resourceId));

        ApplicationExtensionRemovalService removal = new ApplicationExtensionRemovalService(extController);

        ServiceTarget target = extController.getServiceContainer().subTarget();

        CountDownLatch latch = new CountDownLatch( 1 );

        target.addListener( new AbstractServiceListener<Object>() {
            @Override
            public void transition(ServiceController<?> controller, ServiceController.Transition transition) {
                if ( transition.getAfter().equals(ServiceController.Substate.REMOVED ) ) {
                    latch.countDown();
                }
            }
        });

        target.addService(extController.getName().append("remove"), removal)
                .addDependency(LiveOak.extension(this.extensionId), Extension.class, removal.extensionInjector())
                .install();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void adminResourceController(ServiceController<? extends Resource> controller) {
        this.adminResourceController = controller;
    }

    public void publicResourceController(ServiceController<? extends Resource> controller) {
        this.publicResourceController = controller;
    }

    public Resource adminResource() throws InterruptedException {
        return this.adminResourceController.awaitValue();
    }

    public Resource publicResource() throws InterruptedException {
        return this.publicResourceController.awaitValue();
    }

    @Override
    public void accept(Exception e) {
        this.exception = e;
        log.error("Exception during application initialization (/" + app.id() + "/" + resourceId + "): ", e);
    }

    public Exception exception() {
        return this.exception;
    }

    private final String extensionId;
    private final String resourceId;

    private ServiceRegistry registry;
    private InternalApplication app;

    private ServiceController<? extends Resource> adminResourceController;
    private ServiceController<? extends Resource> publicResourceController;

    private Exception exception;

}
