package io.liveoak.container.tenancy.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.liveoak.container.extension.ApplicationExtensionContextImpl;
import io.liveoak.container.tenancy.InternalApplication;
import io.liveoak.container.tenancy.InternalApplicationExtension;
import io.liveoak.spi.LiveOak;
import io.liveoak.spi.extension.Extension;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.*;
import org.jboss.msc.value.InjectedValue;

import java.util.Properties;

/**
 * @author Bob McWhirter
 */
public class ApplicationExtensionService implements Service<InternalApplicationExtension> {


    public ApplicationExtensionService(String extensionId, String resourceId, ObjectNode configuration, boolean boottime) {
        this.extensionId = extensionId;
        this.resourceId = resourceId;
        this.configuration = configuration;
        this.boottime = boottime;
    }

    @Override
    public void start(StartContext context) throws StartException {

        log.debug( "** Activate " + this.extensionId + " as " + resourceId + " for " + this.applicationInjector.getValue().id() );

        ServiceTarget target = context.getChildTarget();

        /*
        target.addListener( new AbstractServiceListener<Object>() {
            @Override
            public void transition(ServiceController<?> controller, ServiceController.Transition transition) {
                System.err.println( controller.getName() + " :: " + transition );
                if ( transition.enters(ServiceController.State.START_FAILED )) {
                    System.err.println( controller.getImmediateUnavailableDependencies() );
                }
                if ( controller.getStartException() != null ) {
                    controller.getStartException().printStackTrace();
                }
            }
        });
        */

        this.appExtension = new InternalApplicationExtension(
                this.serviceRegistryInjector.getValue(),
                this.applicationInjector.getValue(),
                this.extensionId,
                this.resourceId
        );


        String appId = this.appExtension.application().id();

        this.extensionContext = new ApplicationExtensionContextImpl(
                target,
                this.appExtension,
                LiveOak.applicationContext(appId),
                LiveOak.applicationAdminResource(appId).append( "extensions" ),
                this.configuration,
                this.boottime);

        try {
            this.extensionInjector.getValue().extend(extensionContext);

            if (this.appExtension.exception() != null) {
                throw this.appExtension.exception();
            }
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public InternalApplicationExtension getValue() throws IllegalStateException, IllegalArgumentException {
        return this.appExtension;
    }

    public Injector<InternalApplication> applicationInjector() {
        return this.applicationInjector;
    }

    public Injector<ServiceRegistry> serviceRegistryInjector() {
        return this.serviceRegistryInjector;
    }

    public Injector<Extension> extensionInjector() {
        return this.extensionInjector;
    }

    protected Properties properties() {
        InternalApplication app = this.applicationInjector.getValue();

        Properties props = new Properties();
        props.put( "application.name", app.name() );
        props.put( "application.id", app.id() );
        props.put( "application.url",  "/" + app.id() );

        props.put( "application.dir", app.directory().getAbsolutePath() );

        return props;
    }


    private final String extensionId;
    private final String resourceId;


    private ApplicationExtensionContextImpl extensionContext;

    private InjectedValue<InternalApplication> applicationInjector = new InjectedValue<>();
    private InjectedValue<ServiceRegistry> serviceRegistryInjector = new InjectedValue<>();
    private InjectedValue<Extension> extensionInjector = new InjectedValue<>();

    private InternalApplicationExtension appExtension;
    private final ObjectNode configuration;
    private boolean boottime;

    private static final Logger log = Logger.getLogger(ApplicationExtensionService.class);
}
