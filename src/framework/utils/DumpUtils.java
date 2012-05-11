package framework.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.j_spaces.kernel.PlatformVersion;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.openspaces.admin.Admin;
import org.openspaces.admin.dump.DumpResult;

import com.gigaspaces.internal.dump.heap.HeapDumpProcessor;
import com.gigaspaces.internal.dump.log.LogDumpProcessor;
import com.gigaspaces.internal.dump.pu.ProcessingUnitsDumpProcessor;
import com.gigaspaces.internal.dump.summary.SummaryDumpProcessor;
import com.gigaspaces.internal.dump.thread.ThreadDumpProcessor;

import framework.tools.SGTestHelper;


public class DumpUtils {

    private static File testFolder;
    private static File zipFile;

    public static void dumpALL(Admin admin) {
        dump(admin, null, getAllDumpOptions());
    }

    public static void dumpLogs(Admin admin) {
        dump(admin, null, LogDumpProcessor.NAME);
    }

    public static void dumpThreads(Admin admin) {
        dump(admin, null, ThreadDumpProcessor.NAME);
    }

    public static void dumpHeap(Admin admin) {
        dump(admin, null, HeapDumpProcessor.NAME);
    }

    public static void dumpProcessingUnit(Admin admin) {
        dump(admin, null, ProcessingUnitsDumpProcessor.NAME);
    }

    public static void dump(Admin admin, String cause, String... dumpOptions) {
        try {
            DumpResult result = admin.generateDump(cause, null, dumpOptions);
            Date date = new Date();
            DateFormat date1 = new SimpleDateFormat("dd-MM-yyyy");
            DateFormat hour = new SimpleDateFormat("HH-mm-ss-SSS");
            zipFile = new File(getTestFolder().getAbsolutePath() + "/" + date1.format(date) + "_" + hour.format(date) + "_dump.zip");
            result.download(zipFile, null);
            LogUtils.log("> Logs: " + zipFile.getAbsolutePath() + "\n");
        } catch (Exception e) {
            LogUtils.log("Dump Failed", e);
        }
    }

    public static void dump(URL url) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url.toURI());
        try {

            String dumpOutput = client.execute(get, new BasicResponseHandler());
            List<String> dumpByteArray = new ArrayList<String>();
            Map<String, String> mapJson = new ObjectMapper().readValue(
                    dumpOutput, TypeFactory.mapType(HashMap.class, String.class, String.class));
            //dump of specific machine
            if (mapJson.containsKey("response")) {
                dumpByteArray.add(mapJson.get("response"));
            } else {
                for (String key : mapJson.keySet()) {
                    if (!key.equals("status")) {
                        dumpByteArray.add(mapJson.get(key));
                    }
                }
            }
            DateFormat date1 = new SimpleDateFormat("dd-MM-yyyy");
            DateFormat hour = new SimpleDateFormat("HH-mm-ss-SSS");
            for (String dumpByte : dumpByteArray) {
                Date date = new Date();
                zipFile = new File(getTestFolder().getAbsolutePath() + "/" + date1.format(date) + "_" + hour.format(date) + "_dump.zip");
                byte[] decodedBytes = Base64.decodeBase64(dumpByte);
                FileUtils.writeByteArrayToFile(zipFile, decodedBytes);
                LogUtils.log("> Logs: " + zipFile.getAbsolutePath() + "\n");
            }
        } catch (Exception e) {
            LogUtils.log("Dump Failed", e);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    private static String[] getAllDumpOptions() {
        List<String> dumpOptionsList = new ArrayList<String>(5);
        dumpOptionsList.add(SummaryDumpProcessor.NAME);
        dumpOptionsList.add(ThreadDumpProcessor.NAME);
        dumpOptionsList.add(LogDumpProcessor.NAME);
        dumpOptionsList.add(ProcessingUnitsDumpProcessor.NAME);
        dumpOptionsList.add(HeapDumpProcessor.NAME);
        String[] dumpOptionsArray = new String[dumpOptionsList.size()];
        dumpOptionsList.toArray(dumpOptionsArray);
        return dumpOptionsArray;
    }

    public static File createTestFolder(String testName, String suiteName) {
        String buildNumber = PlatformVersion.getBuildNumber();
        if (buildNumber == null) {
            return null;
        }

        File buildFolder = new File(SGTestHelper.getSGTestRootDir() + "/deploy/local-builds/build_" + buildNumber);

        if (!buildFolder.exists())
            buildFolder.mkdir();
        testFolder = new File(buildFolder.getAbsolutePath() + "/" + suiteName + "/" + testName);
        if (!testFolder.exists())
            testFolder.mkdirs();

        return testFolder;
    }

    public static File getTestFolder() {
        if (testFolder == null) {
            File buildFolder = new File(SGTestHelper.getSGTestRootDir() + "/deploy/local-builds/build_" +
                    PlatformVersion.getBuildNumber());
            testFolder = new File(buildFolder.getAbsolutePath());
            testFolder.mkdirs();
        }
        return testFolder;
    }

    public static File getDumpFile() {
        return zipFile;
    }

    public static void unzipDump() {
        ZipUtils.unzipArchive(zipFile, testFolder);
    }

    public static void copyBeforeConfigurationsLogToTestDir(String testName, String suiteName) {

        String buildNumber = PlatformVersion.getBuildNumber();
        File buildFolder = new File(SGTestHelper.getSGTestRootDir() + "/deploy/local-builds/build_" + buildNumber);
        File beforeConfigurationsLogDir = new File(buildFolder.getAbsolutePath() + "/" + suiteName + "/" + testName);
        if (beforeConfigurationsLogDir.exists()) {
            try {
                FileUtils.copyDirectory(beforeConfigurationsLogDir, testFolder);
                FileUtils.deleteDirectory(beforeConfigurationsLogDir);
            } catch (IOException e) {
                LogUtils.log("Failed to copy before configurations to test dir : " + testFolder.getAbsolutePath(), e);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        DateFormat date1 = new SimpleDateFormat("dd-MM-yyyy");
        DateFormat hour = new SimpleDateFormat("HH-mm-ss-SSS");
        Date date = new Date();
        zipFile = new File(getTestFolder().getAbsolutePath() + "/" + date1.format(date) + "_" + hour.format(date) + "_dump.zip");
        zipFile.createNewFile();

    }
}
