import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import java.util.concurrent.TimeUnit
import org.cloudifysource.dsl.internal.CloudifyConstants
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.admin.Admin;
import org.cloudifysource.utilitydomain.context.blockstorage.ServiceVolume;
import org.cloudifysource.utilitydomain.context.blockstorage.VolumeState;

final int MANAGEMENT_SPACE_LOOKUP_TIMEOUT = 10;

def context = ServiceContextFactory.getServiceContext()
final Admin admin = context.getAdmin()

final Space space = admin.getSpaces().waitFor(CloudifyConstants.MANAGEMENT_SPACE_NAME,
					MANAGEMENT_SPACE_LOOKUP_TIMEOUT, TimeUnit.SECONDS);
final GigaSpace managementSpace = space.getGigaSpace();

if (managementSpace == null) {
	 println "Space not Found"
	 return
}

// not passing the device name in template!
final ServiceVolume serviceVolume = new ServiceVolume()
serviceVolume.setApplicationName(context.getApplicationName())
serviceVolume.setServiceName(context.getServiceName())
serviceVolume.setIp(context.getBindAddress())
final ServiceVolume[] readMultiple = managementSpace.readMultiple(serviceVolume)
// expecting to find 2 space objects for 2 attached volumes.
if (readMultiple.length != 2) {
	 println "Expecting 2 volume instance records in space. Found: " + readMultiple.length
	 return
}

for (ServiceVolume volume : readMultiple) {
	println "Found volume with device: " + volume.getDevice() + " and state: " + volume.getState().toString()
}
return

