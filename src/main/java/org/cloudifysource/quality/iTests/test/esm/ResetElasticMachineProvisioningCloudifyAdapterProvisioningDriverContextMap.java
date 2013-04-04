
/**
 * Created with IntelliJ IDEA.
 * User: boris
 * Date: 04/04/13
 * Time: 15:37
 * This class is created in order to change a private static field PROVISIONING_DRIVER_CONTEXT_PER_DRIVER_CLASSNAME
 * in ElasticMachineProvisioningCloudifyAdapter to clear the "cached" map of byonDeployer nodes.
 * This code is based on: http://stackoverflow.com/questions/3301635/change-private-static-final-field-using-java-reflection
 * reviewed by Itai.
 */
package org.cloudifysource.quality.iTests.test.esm;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.ElasticMachineProvisioningCloudifyAdapter;

public class ResetElasticMachineProvisioningCloudifyAdapterProvisioningDriverContextMap {

    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }

    public static void ResetProvisioningDriverContextPerDriver() throws Exception {
        setFinalStatic(ElasticMachineProvisioningCloudifyAdapter.class.getDeclaredField("PROVISIONING_DRIVER_CONTEXT_PER_DRIVER_CLASSNAME")
                ,new HashMap<String, ProvisioningDriverClassContext>());
    }
}
