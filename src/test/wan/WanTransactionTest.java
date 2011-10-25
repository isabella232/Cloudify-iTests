package test.wan;

import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.transaction.manager.DistributedJiniTxManagerConfigurer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testng.annotations.Test;

import test.AbstractTest;
import test.data.Data;

import com.j_spaces.core.IJSpace;

public class WanTransactionTest extends AbstractWanTest {

	private PlatformTransactionManager ptm1;
	private PlatformTransactionManager ptm2;

	@Override
	protected GigaSpace createGigaSpace1() {
		final IJSpace space = this.sp1.getGigaSpace().getSpace();

		try {
			this.ptm1 = new DistributedJiniTxManagerConfigurer().transactionManager();
			return new GigaSpaceConfigurer(space)
					.transactionManager(ptm1)
					.gigaSpace().getClustered();
		} catch (final Exception e) {
			throw new IllegalStateException("Failed to create Txn Manager", e);
		}
	}

	@Override
	protected GigaSpace createGigaSpace2() {
		final IJSpace space = this.sp2.getGigaSpace().getSpace();

		try {
			this.ptm2 = new DistributedJiniTxManagerConfigurer().transactionManager();
			return new GigaSpaceConfigurer(space)
					.transactionManager(ptm2)
					.gigaSpace().getClustered();
		} catch (final Exception e) {
			throw new IllegalStateException("Failed to create Txn Manager", e);
		}

	}

	@Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "2")
	public void test() throws Exception {

		final Data template = new Data(1);
		template.setId("1");
		
		final DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setTimeout(30000);
		final TransactionStatus status = ptm1.getTransaction(definition);

		final int bulkSize = 10;
		writeDataEntriesToSpace(bulkSize, 0, "Wan Transaction Test");

		// txn not committed yet.
		// first make sure that nothing was written to the other site.
		// here we can check using a blocking read to a single partition!
		final Data resultBeforeCommit = gigaSpace2.read(template, 10000);
		assertTrue("Entries written over the wan before commit", resultBeforeCommit == null);
		ptm1.commit(status);

		final Data data = gigaSpace2.read(template, 15000);
		AbstractTest.assertNotNull(data);
		final int remoteCount = gigaSpace2.count(new Data());
		AbstractTest.assertEquals(remoteCount, bulkSize);

	}

	@Override
	protected ProcessingUnitDeployment createPUDeploymentForMirror1(final String ip1, final String ip2) {
		return super.createPUDeploymentForMirror1(ip1, ip2)
				.setContextProperty("operationGrouping", "group-by-space-transaction");
	}

	@Override
	protected ProcessingUnitDeployment createPUDeploymentForMirror2(final String ip1, final String ip2) {
		return super.createPUDeploymentForMirror2(ip1, ip2)
				.setContextProperty("operationGrouping", "group-by-space-transaction");
	}

}
