/**
 * This service validate DataGrid deployment
 */


import com.gigaspaces.document.SpaceDocument
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder
import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.Admin
import org.openspaces.admin.machine.Machine
import org.openspaces.admin.vm.VirtualMachine

import java.util.concurrent.TimeUnit;
import util

import java.util.concurrent.atomic.AtomicInteger

service {
    name "sg-validator"
    type "APP_SERVER"
    icon "icon.png"
    elastic false
    numInstances 1
    minAllowedInstances 1
    maxAllowedInstances 1

    compute {
        template "SMALL_LINUX"
    }

    lifecycle{
    }

    customCommands ([
            "get-datagrid-instances" : {gridname,lookuplocators,waitfor->
                Admin admin = new AdminFactory().useDaemonThreads(true).addLocators(lookuplocators).createAdmin();
                admin.getProcessingUnits().waitFor(gridname,5, TimeUnit.MINUTES).waitFor(Integer.valueOf(waitfor));
                res = admin.getProcessingUnits().getProcessingUnit(gridname).getInstances().length;
                admin.close()
                return res
            },
            "get-datagrid-partitions" : {gridname,lookuplocators,waitfor->
                Admin admin = new AdminFactory().useDaemonThreads(true).addLocators(lookuplocators).createAdmin();
                admin.getProcessingUnits().waitFor(gridname,5, TimeUnit.MINUTES).waitFor(Integer.valueOf(waitfor));
                res = admin.getProcessingUnits().getProcessingUnit(gridname).getPartitions().length;
                admin.close()
                return res
            },
            "get-datagrid-backups" : {gridname,lookuplocators,waitfor->
                Admin admin = new AdminFactory().useDaemonThreads(true).addLocators(lookuplocators).createAdmin();
                admin.getProcessingUnits().waitFor(gridname,5, TimeUnit.MINUTES).waitFor(Integer.valueOf(waitfor));
                res = admin.getProcessingUnits().getProcessingUnit(gridname).getNumberOfBackups();
                admin.close()
                return res
            },
            "get-datagrid-deploymentstatus" : {gridname,lookuplocators,waitfor->
                Admin admin = new AdminFactory().useDaemonThreads(true).addLocators(lookuplocators).createAdmin();
                admin.getProcessingUnits().waitFor(gridname,5, TimeUnit.MINUTES).waitFor(Integer.valueOf(waitfor));
                res = admin.getProcessingUnits().getProcessingUnit(gridname).getStatus().toString();
                admin.close()
                return res
            },
            "get-datagrid-maxinstancespermachine" : {gridname,lookuplocators,waitfor->
                Admin admin = new AdminFactory().useDaemonThreads(true).addLocators(lookuplocators).createAdmin();
                admin.getProcessingUnits().waitFor(gridname,5, TimeUnit.MINUTES).waitFor(Integer.valueOf(waitfor));
                res = admin.getProcessingUnits().getProcessingUnit(gridname).getMaxInstancesPerMachine();
                admin.close()
                return res
            },
            "get-datagrid-maxinstancespervm" : {gridname,lookuplocators,waitfor->
                Admin admin = new AdminFactory().useDaemonThreads(true).addLocators(lookuplocators).createAdmin();
                admin.getProcessingUnits().waitFor(gridname,5, TimeUnit.MINUTES).waitFor(Integer.valueOf(waitfor));
                res = admin.getProcessingUnits().getProcessingUnit(gridname).getMaxInstancesPerVM();
                admin.close()
                return res
            },
            "getMachinesCount" : { gridname, xaplocators ->
                admin = new AdminFactory().useDaemonThreads(true).addLocators(xaplocators).createAdmin();
                pus = admin.getProcessingUnits().waitFor(gridname, 1, TimeUnit.MINUTES);
                println admin.groups
                println admin.locators
                if (pus == null) {
                    println "Unable to find ${gridname}, please make sure it is deployed."
                    admin.close()
                    return null;
                }
                if (!pus.waitFor(1)) {
                    println "Unable to find instances of ${gridname}, please make sure it is deployed."
                    admin.close()
                    return null;
                }

                gigaSpace = admin.getProcessingUnits().getProcessingUnit(gridname).getSpace().getGigaSpace();

                datagridInstances = admin.getProcessingUnits().getProcessingUnit(gridname).getInstances()
                machines = new HashSet<Machine>();

                for (int i=0 ; i<datagridInstances.size(); i++) {
                    instance = datagridInstances[i]
                    machines.add(instance.getMachine())
                }

                admin.close()
                return machines.size()
            },
            "getSpaceMachines" : { gridname, xaplocators ->
                admin = new AdminFactory().useDaemonThreads(true).addLocators(xaplocators).createAdmin();
                pus = admin.getProcessingUnits().waitFor(gridname, 1, TimeUnit.MINUTES);
                println admin.groups
                println admin.locators
                if (pus == null) {
                    println "Unable to find ${gridname}, please make sure it is deployed."
                    admin.close()
                    return null;
                }
                if (!pus.waitFor(1)) {
                    println "Unable to find instances of ${gridname}, please make sure it is deployed."
                    admin.close()
                    return null;
                }

                gigaSpace = admin.getProcessingUnits().getProcessingUnit(gridname).getSpace().getGigaSpace();

                datagridInstances = admin.getProcessingUnits().getProcessingUnit(gridname).getInstances()
                machines = new HashMap<String, AtomicInteger>();

                for (int i=0 ; i<datagridInstances.size(); i++) {
                    instanceMachine = datagridInstances[i].getMachine().getHostAddress()
                    if (machines.containsKey(instanceMachine)) {
                        machines.get(instanceMachine).incrementAndGet()
                    } else {
                        machines.put(instanceMachine, new AtomicInteger(1));
                    }
                }

                sb = new StringBuilder()
                iterator = machines.keySet().iterator()
                while (iterator.hasNext()) {
                    String key = iterator.next()
                    sb.append(key+":"+machines.get(key).get())
                    if (iterator.hasNext()) {
                        sb.append(";")
                    }
                }
                admin.close()
                return sb.toString()
            },
            "write" : { gridname, xaplocators, witerations, wobjectsPerIteration ->
                return util.invokeLocal(context,"_benchmark", [
                        "gridname":gridname,
                        "xaplocators":xaplocators,
                        "witerations": witerations,
                        "wobjectsPerIteration":wobjectsPerIteration
                ])
            },
            //witerations and wobjectsPerIteration default values for benchmark is defined in benchmark.groovy
            "benchmark" : { gridname, xaplocators ->
                return util.invokeLocal(context,"_benchmark", [
                        "gridname":gridname,
                        "xaplocators":xaplocators,
                        "witerations": 100,
                        "wobjectsPerIteration":25000
                ])
            },
            "_benchmark"	: "benchmark.groovy",

            "clearSpace" : { gridname, xaplocators ->
                return util.invokeLocal(context,"_clearSpace", [
                        "gridname":gridname,
                        "xaplocators":xaplocators
                ])
            },
            "_clearSpace"	: "clearspace.groovy"

    ])
}