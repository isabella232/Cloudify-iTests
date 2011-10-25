package test.wan;

import org.openspaces.core.GigaSpace;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.AbstractTest;

import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.metadata.SpaceTypeDescriptorBuilder;

public class WanDocumentBasicTest extends AbstractWanTest {

	@Override
	@AfterMethod
	public void afterTest() {
		super.afterTest();
	}

	@Override
	@BeforeMethod
	public void beforeTest() {

		super.beforeTest();
	}

	private void registerProductType(final GigaSpace gigaspace) {
		// Create type descriptor:
		final SpaceTypeDescriptor typeDescriptor = new SpaceTypeDescriptorBuilder("Product")
				.idProperty("CatalogNumber")
				.routingProperty("Category")

				.create();
		// Register type:
		gigaspace.getTypeManager().registerTypeDescriptor(typeDescriptor);
	}

	/**
	 * @throws Exception
	 */
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {
		try {
			registerProductType(this.gigaSpace1);
			
			final SpaceDocument doc = new SpaceDocument("Product");
			doc.setProperty("CatalogNumber", 1);
			doc.setProperty("Category", "Test");
			
			// Write the document			
			gigaSpace1.write(doc);
			
			// wait until the object arrives at the target site
			// A document read would not work here, since Product
			// has not been introduced yet.			
			waitForObject(30000);

			// Read the document from the other site
			final SpaceDocument template = new SpaceDocument("Product");
			template.setProperty("Category", "Test");
			SpaceDocument docRes = gigaSpace2.read(template, 20000);

			AbstractTest.assertNotNull("Could not find document on remote site", docRes);

			// Write Test successful

			// Test Update
			// Add a dynamic property to the document			
			doc.setProperty("Modified", true);
			gigaSpace1.write(doc);

			// read the modified document from the other site
			template.setProperty("Modified", true);
			docRes = gigaSpace2.read(template, 20000);				
			AbstractTest.assertNotNull("Could not find data object in remote site", docRes);
			AbstractTest.assertEquals("New property is missing in remote site", doc.getProperty("Modified"),
					docRes.getProperty("Modified"));
			// Update Test Successful

			// Test take
			final SpaceDocument temp = gigaSpace1.take(doc);
			AbstractTest.assertNotNull("Could not remove data object from source site", temp);
			Thread.sleep(10000);
			final int count = gigaSpace2.count(doc);
			AbstractTest.assertEquals("Expected 0 entries in remote site, found: " + count, count, 0);
			// Take test successful
		} catch (final Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
			throw e;
		}

	}

	private void waitForObject(long millis) {
		long end = System.currentTimeMillis() + millis;
		while(System.currentTimeMillis() <= end) {
			final int count = gigaSpace2.count(new Object());
			if(count == 1) {
				return;
			}
			
			try {
				Thread.sleep(1000);
			} catch(InterruptedException ie) {
				// ignore
			}
		}
		
		
	}

}
