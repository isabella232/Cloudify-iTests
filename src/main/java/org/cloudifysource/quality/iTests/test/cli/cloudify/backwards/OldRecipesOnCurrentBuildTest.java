package org.cloudifysource.quality.iTests.test.cli.cloudify.backwards;

import com.google.common.io.Resources;
import org.cloudifysource.quality.iTests.test.cli.cloudify.AbstractLocalCloudTest;
import org.testng.annotations.Test;

/**
 *
 * @author Eli Polonsky
 */
public class OldRecipesOnCurrentBuildTest extends AbstractLocalCloudTest {

   @Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
   public void testBadImportInServiceFile() throws Exception {
      String path = Resources.getResource("apps/cloudify/recipes/backwards/in-service-file").getPath();
      installServiceAndWait(path, "simple-backwards", false);
   }

   @Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
   public void testBadImportInExternalScript() throws Exception {
      String path = Resources.getResource("apps/cloudify/recipes/backwards/in-external-script").getPath();
      installServiceAndWait(path, "simple-backwards", false);
   }
}
