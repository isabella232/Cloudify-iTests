// for implementing a startDetector
import org.cloudifysource.utilitydomain.context.ServiceContextFactory
def context = ServiceContextFactory.getServiceContext()
new File(context.serviceDirectory + "/marker.log").createNewFile();

sleep(Long.MAX_VALUE)
