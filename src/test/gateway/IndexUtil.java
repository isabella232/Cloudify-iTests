package test.gateway;


import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.index.AddTypeIndexesResult;
import com.gigaspaces.metadata.index.SpaceIndex;
import com.gigaspaces.metadata.index.SpaceIndexFactory;
import com.gigaspaces.metadata.index.SpaceIndexType;
import org.openspaces.core.GigaSpace;
import test.utils.AssertUtils;
import test.utils.TestUtils;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class IndexUtil {

    public static void addDynamicIndex(GigaSpace gigaSpace, String propertyName, String type, SpaceIndexType indexType) throws Exception {
        System.out.print("Adding dynamic index for type [" + type + "] index name =" + propertyName + "... \n");
        AsyncFuture<AddTypeIndexesResult> future = gigaSpace.getTypeManager().asyncAddIndex(type,
                SpaceIndexFactory.createPropertyIndex(propertyName, indexType));

        AddTypeIndexesResult result = future.get();
        AssertUtils.assertNotNull(result);

    }

    public static void assertIndex(final GigaSpace gigaSpace, final String dataType, final String indexedField, final SpaceIndexType indexType) throws RemoteException {
        TestUtils.repetitive(new Runnable() {
            public void run() {
                Map<String, SpaceIndexType> indexes = new HashMap<String, SpaceIndexType>();
                SpaceTypeDescriptor typeDesc = gigaSpace.getTypeManager().getTypeDescriptor(dataType);
                for (SpaceIndex spaceIndex : typeDesc.getIndexes().values()) {
                    indexes.put(spaceIndex.getName(), spaceIndex.getIndexType());
                }
                AssertUtils.assertEquals("Index doesn't match for type [" + dataType + "] index name=" + indexedField, indexType, indexes.get(indexedField));

            }
        }, 900000);
    }

}
