import org.cloudifysource.dsl.utils.ServiceUtils

service {
    
    
    extend "../../recipes/services/tomcat"

    lifecycle{

        startDetection {
            !ServiceUtils.arePortsFree([9876, 8009] )
        }
    }

    network {
        port = 9876
        protocolDescription ="HTTP"
    }
}