package apps.cloudify.recipes.sgvalidator

import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import org.openspaces.admin.AdminFactory
import java.util.concurrent.TimeUnit
context=ServiceContextFactory.serviceContext
gridname = context.attributes.thisInstance["gridname"]
xaplocators = context.attributes.thisInstance["xaplocators"]
admin = new AdminFactory().useDaemonThreads(true).addLocators(xaplocators).createAdmin();
pus = admin.getProcessingUnits().waitFor(gridname, 30, TimeUnit.SECONDS);
println admin.groups
println admin.locators
if (pus == null) println "Unable to find ${gridname}, please make sure it is deployed."
if (!pus.waitFor(1)) println "Unable to find instances of ${gridname}, please make sure it is deployed."

gigaSpace = admin.getProcessingUnits().getProcessingUnit(gridname).getSpace().getGigaSpace();

gigaSpace.clear(new Object());

print "Ended"
