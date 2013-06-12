package org.cloudifysource.quality.iTests.test.esm.stateless.manual.memory;

import iTests.framework.tools.SGTestHelper;
import iTests.framework.utils.DeploymentUtils;
import iTests.framework.utils.IOUtils;
import iTests.framework.utils.LogUtils;
import iTests.framework.utils.AssertUtils.RepetitiveConditionProvider;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.esc.driver.provisioning.byon.ByonProvisioningDriver;
import org.cloudifysource.quality.iTests.test.esm.AbstractFromXenToByonGSMTest;
import org.openspaces.admin.esm.ElasticServiceManager;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntryMatcher;
import com.gigaspaces.log.LogEntryMatchers;

public class AbstractStatelessManualByonCleanupTest extends AbstractFromXenToByonGSMTest {

    protected void repetitiveAssertOnServiceUninstalledInvoked(final String expectedLogStatement) {
    	final ElasticServiceManager esm = admin.getElasticServiceManagers().waitForAtLeastOne(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        LogUtils.log("Waiting for 1 "+ expectedLogStatement + " log entry before undeploying PU");
        final LogEntryMatcher logMatcher = LogEntryMatchers.containsString(expectedLogStatement);
        repetitiveAssertTrue("Expected " + expectedLogStatement +" log", new RepetitiveConditionProvider() {
            
            @Override
            public boolean getCondition() {

                final LogEntries logEntries = esm.logEntries(logMatcher);
                final int count = logEntries.logEntries().size();
                LogUtils.log("Exepcted at least one "+ expectedLogStatement + " log entries. Actual :" + count);
                return count > 0;
            }
        } , OPERATION_TIMEOUT);
    }

    @Override
    public void beforeBootstrap() throws IOException {
        String s = System.getProperty("file.separator");
        String repoQualityItests = DeploymentUtils.getQualityItestsPath(s);
        // copy custom location aware driver to cloudify-overrides
        File locationAwareDriver = new File (repoQualityItests +s+"location-aware-provisioning-byon"+s+"1.1.1-SNAPSHOT"+s+"location-aware-provisioning-byon-1.1.1-SNAPSHOT.jar");
        File uploadOverrides =
                new File(getService().getPathToCloudFolder() + "/upload/cloudify-overrides/");
        if (!uploadOverrides.exists()) {
            uploadOverrides.mkdir();
        }

        File uploadEsmDir = new File(uploadOverrides.getAbsoluteFile() + "/lib/platform/esm");
        File localEsmFolder = new File(SGTestHelper.getBuildDir() + "/lib/platform/esm");

        FileUtils.copyFileToDirectory(locationAwareDriver, uploadEsmDir, true);
        FileUtils.copyFileToDirectory(locationAwareDriver, localEsmFolder, false);

        final Map<String, String> propsToReplace = new HashMap<String, String>();

        final String oldCloudDriverClazz = ByonProvisioningDriver.class.getName();
        String newCloudDriverClazz = "org.cloudifysource.quality.iTests.OnServiceUninstalledByonProvisioningDriver" ;

        propsToReplace.put(toClassName(oldCloudDriverClazz),toClassName(newCloudDriverClazz));
        IOUtils.replaceTextInFile(getService().getPathToCloudGroovy(), propsToReplace);
    }

    public String toClassName(String className) {
        return "className \""+className+"\"";
    }
}
