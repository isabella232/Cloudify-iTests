package framework.utils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.j_spaces.kernel.PlatformVersion;
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
            DateFormat hour = new SimpleDateFormat("HH-mm");
            zipFile = new File(getTestFolder().getAbsolutePath() + "/" + date1.format(date) + "_" + hour.format(date) + "_dump.zip");
            result.download(zipFile, null);
            LogUtils.log("> Logs: " + zipFile.getAbsolutePath() + "\n");
        } catch (Exception e) {
            LogUtils.log("Dump Failed", e);
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

    public static File createTestFolder(String testName) {
        String buildNumber = PlatformVersion.getBuildNumber();
        if(buildNumber == null){
            return null;
        }

        File buildFolder = new File(SGTestHelper.getSGTestRootDir() + "/deploy/local-builds/build_" + buildNumber);

        if (!buildFolder.exists())
            buildFolder.mkdir();
        testFolder = new File(buildFolder.getAbsolutePath() + "/" + testName);
        if (!testFolder.exists())
            testFolder.mkdir();

        return testFolder;
    }

    public static File getTestFolder() {
        return testFolder;
    }

    public static File getDumpFile() {
        return zipFile;
    }

    public static void unzipDump() {
        ZipUtils.unzipArchive(zipFile, testFolder);
    }

}
