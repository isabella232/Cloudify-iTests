package test.wan;

import net.jini.core.lease.Lease;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.data.Data;

import com.gigaspaces.client.ReadByIdsResult;
import com.j_spaces.core.client.UpdateModifiers;

/**
 * Test WAN using two clusters of size 2 & 3.
 *  
 * @author idan
 * @since 8.0.1
 *
 */
public class WanAsymmetricClustersTest extends AbstractWanTest {

	private static final int OBJECTS = 12;
	private static final long WAIT = 10000;
	
	@Override
	@AfterMethod
	public void afterTest() {
		super.afterTest();
	}

	@Override
	@BeforeMethod
	public void beforeTest() {
		site1NumberOfInstances = 2;
		site2NumberOfInstances = 3;
		super.beforeTest();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		try {
			// test write
			Data[] data = new Data[OBJECTS];
			for (int i = 0; i < data.length; i++) {
				data[i] = new Data();
				data[i].setId(String.valueOf(i));
				data[i].setData("ABCDEFGHIJKLMNOPQRSTUVXYZ" + data[i].getId());
				data[i].setType(i);
			}
			gigaSpace1.writeMultiple(data);
			
			Thread.sleep(WAIT);
			
			// verify write
			Data[] result = gigaSpace2.readMultiple(new Data(), Integer.MAX_VALUE);
			assertEquals(OBJECTS, result.length);		
			
			
			// test update
			Object[] ids = { data[0].getId(), data[1].getId(), data[2].getId() };
			ReadByIdsResult<Data> readByIdsResult = gigaSpace2.readByIds(Data.class, ids);
			result = readByIdsResult.getResultsArray();
			assertEquals(3, result.length);
			for (int i = 0; i < result.length; i++) {
				result[i].setData("A");
			}
			gigaSpace1.writeMultiple(result, Lease.FOREVER, UpdateModifiers.UPDATE_ONLY);
			
			Thread.sleep(WAIT);
			
			readByIdsResult = gigaSpace2.readByIds(Data.class, ids);
			assertEquals(3, readByIdsResult.getResultsArray().length);
			for (Data d : readByIdsResult) {
				assertEquals("A", d.getData());
			}

			// test take
			ids = new Object[OBJECTS / 2];
			for (int i = 0; i < ids.length; i++) {
				ids[i] = data[i*2].getId();
			}
			gigaSpace1.takeByIds(Data.class, ids);
			
			Thread.sleep(WAIT);
			
			int count = gigaSpace2.count(new Data());
			assertEquals(ids.length, count);
			
		} catch(Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
			throw e;
		}

	}
	
}
