import java.util.concurrent.TimeUnit;

import org.cloudifysource.utilitydomain.context.ServiceContextImpl;

service {
	name "simple"
	icon "${iconName}.png"
	type "UNDEFINED"
    url urlProp

    lifecycle {
        init initService
        start {println "This is the shutdown event"}
        shutdown {println "This is the shutdown event"}
    }

}