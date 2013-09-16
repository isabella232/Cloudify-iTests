import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import java.util.concurrent.TimeUnit;
import java.util.Arrays

final def context = ServiceContextFactory.getServiceContext()

context.getAdmin().getGridServiceAgents().waitFor(1, 20, TimeUnit.SECONDS)
final Set<String> agentIdSet = context.getAdmin().getGridServiceAgents().getUids().keySet()

println(Arrays.toString(agentIdSet.toArray(new String[1])))