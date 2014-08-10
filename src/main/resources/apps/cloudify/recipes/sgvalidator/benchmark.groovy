import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder
import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import org.openspaces.admin.AdminFactory
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer
import org.openspaces.core.util.MemoryUnit

import java.util.concurrent.TimeUnit
println "Hello world"
context=ServiceContextFactory.serviceContext
gridname = context.attributes.thisInstance["gridname"]
xaplocators = context.attributes.thisInstance["xaplocators"]
admin = new AdminFactory().useDaemonThreads(true).addLocators(xaplocators).createAdmin();
pus = admin.getProcessingUnits().waitFor(gridname, 1, TimeUnit.MINUTES);
println admin.groups
println admin.locators
if (pus == null) {
    println "Unable to find ${gridname}, please make sure it is deployed."
    admin.close()
    return
}
if (!pus.waitFor(1)) {
    println "Unable to find instances of ${gridname}, please make sure it is deployed."
    admin.close()
    return
}
gigaSpace = admin.getProcessingUnits().getProcessingUnit(gridname).getSpace().getGigaSpace();

spaceTypeDescriptor = new SpaceTypeDescriptorBuilder("MyDocument").idProperty("id").supportsDynamicProperties(true).addFixedProperty("id", Integer.class).addFixedProperty("name", String.class).addFixedProperty("age", String.class).create();
gigaSpace.getTypeManager().registerTypeDescriptor(spaceTypeDescriptor);


runNumber = context.attributes.thisInstance["runNumber"]
if (runNumber == null)
    runNumber = 0

runNumber = runNumber + 1

context.attributes.thisInstance["runNumber"] = runNumber

witerations = context.attributes.thisInstance["witerations"]
wobjectsPerIteration = context.attributes.thisInstance["wobjectsPerIteration"]
int it= (witerations == null ? 100 : Integer.valueOf(witerations));
int objsPerIt= (wobjectsPerIteration == null ? 250000 : Integer.valueOf(wobjectsPerIteration))
for (int j = 0; j < it; j++) {
    spaceDocuments = new SpaceDocument[objsPerIt];
    for (int i = 0; i < objsPerIt; i++) {
        spaceDocument = new SpaceDocument("MyDocument");
        spaceDocument.setProperty("id", (objsPerIt * j*10 + i)*1000 + runNumber);
        spaceDocument.setProperty("name", "MyDocument Object (" + i + ","+j+")");
        spaceDocument.setProperty("company", "GigaSpaces");
        spaceDocument.setProperty("age", runNumber);
        spaceDocuments[i] = spaceDocument;
    }
    gigaSpace.writeMultiple(spaceDocuments);
}

admin.close()
print "Ended"

