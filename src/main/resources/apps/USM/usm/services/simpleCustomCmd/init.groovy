import org.cloudifysource.domain.context.ServiceContext
import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import com.gigaspaces.internal.sigar.SigarHolder; 


println "This is the init event"
ServiceContext context = ServiceContextFactory.getServiceContext()
println "Service Name: " + context.getServiceName()
println "pid <" + SigarHolder.getSigar().getPid() + ">"
sleep 20000
