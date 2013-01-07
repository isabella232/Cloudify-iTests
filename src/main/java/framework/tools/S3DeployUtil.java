package framework.tools;

import com.google.inject.Module;
import framework.utils.LogUtils;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.s3.S3Client;
import org.jclouds.s3.domain.AccessControlList;
import org.jclouds.s3.domain.CannedAccessPolicy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class S3DeployUtil {

    public static void uploadLogFile(File source, String buildNumber, String suiteName, String testName){
        try {
            Properties props = getS3Properties();
            String container =  props.getProperty("container");
            String user =  props.getProperty("user");
            String key =  props.getProperty("key");
            String target = buildNumber + "/" + suiteName + "/" + testName;
            BlobStoreContext context;
            Set<Module> wiring = new HashSet<Module>();
            context = new BlobStoreContextFactory().createContext("aws-s3", user, key, wiring, new Properties());
            S3Client client = S3Client.class.cast(context.getProviderSpecificContext().getApi());
            BlobStore store = context.getBlobStore();

            uploadLogFile(source, target, container, client, store);
            context.close();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    private static void uploadLogFile(File source, String target, String container, S3Client client, BlobStore store){
        if (source.isDirectory()){
            for (File f : source.listFiles()){
                if(f.getName().endsWith(".log"))
                    uploadLogFile(new File(source.getPath() + "/" + f.getName()), target + "/" + f.getName(), container, client, store);
            }
        }
        else{
            LogUtils.log("Processing " + source + ", upload size is: " + (source).length() + ". Target: " + target);
            store.putBlob(container, store.blobBuilder(target)
                    .payload(source)
                    .build());
            LogUtils.log("Upload of " + source + " was ended successfully");

                String ownerId = client.getObjectACL(container, target).getOwner().getId();
                client.putObjectACL(container, target,
                        AccessControlList.fromCannedAccessPolicy(CannedAccessPolicy.PUBLIC_READ, ownerId));
        }
    }


    private static Properties getS3Properties() {
        Properties properties = new Properties();
        InputStream is = S3DeployUtil.class.getClassLoader().getResourceAsStream("s3.properties");
        if ( is != null ) {
            try {
                properties.load( is );
            } catch (IOException e) {
                throw new RuntimeException("failed to read s3.properties file - " + e, e);
            }
        }else{
            throw new RuntimeException("failed to find s3.properties file - ");
        }

        return properties;
    }

    public static void main(String[] args) {
        uploadLogFile(new File("D:\\opt\\9.5.0\\gigaspaces-xap-premium-9.5.0-m3\\logs"), "1111", "SG" ,"test");
    }
}
