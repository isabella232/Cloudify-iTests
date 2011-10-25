package framework.utils;

import static framework.utils.ProcessingUnitUtils.getProcessingUnitInstanceName;

import java.util.Map;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.zone.Zone;

public class ToStringUtils {
	
	/** returns host/address */
	public static String machineToString(Machine machine) {
		return machine.getHostName()+"/"+machine.getHostAddress();
	}
	
	/** returns GSM-agentId[pid]@host <zones>*/
	public static String gsmToString(GridServiceManager gsm) {
		if (gsm == null) return "GSM is null" ;
		
		String toString = "GSM-"+gsm.getAgentId()+"["+gsm.getVirtualMachine().getDetails().getPid()+"]@" + gsm.getMachine().getHostName();
		Map<String, Zone> zones = gsm.getZones();
		if (!zones.isEmpty()) {
			toString += " " + zones.keySet();
		}
		return toString;
	}
	
	/** returns GSC-agentId[pid]@host <zones>*/
	public static String gscToString(GridServiceContainer gsc) {
		if (gsc == null) return "GSC is null" ;
		
		String toString = "GSC-"+gsc.getAgentId()+"["+gsc.getVirtualMachine().getDetails().getPid()+"]@" + gsc.getMachine().getHostName();
		Map<String, Zone> zones = gsc.getZones();
		if (!zones.isEmpty()) {
			toString += " " + zones.keySet();
		}
		return toString;
	}
	
	/** returns PUname SpaceMode */
	public static String puInstanceToString(ProcessingUnitInstance pu) {
		if (pu == null) return "PU is null" ;
		
		String toString = getProcessingUnitInstanceName(pu);
		if (pu.getSpaceInstance()!=null)
			toString += " " + pu.getSpaceInstance().getMode();
		return toString;
	}
	
	/** returns presentation name of space - instance id and backup id like shown in UI*/
	public static String spaceInstanceToString(SpaceInstance spaceInstance) {
		if (spaceInstance == null) return "space instacne is null";
		String name = spaceInstance.getSpace().getName();
		Integer id = spaceInstance.getInstanceId();
		if (spaceInstance.getSpace().getNumberOfBackups() > 0) {
			Integer bid = spaceInstance.getBackupId();
			if (bid == null) {
				bid = Integer.valueOf(0);
			}
			name += "."+id+" ["+(bid+1)+"]";
		} else {
			name += " ["+id+"]";
		}
		
		return name;
	}
}
