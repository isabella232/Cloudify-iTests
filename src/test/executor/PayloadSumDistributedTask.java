package test.executor;

import com.gigaspaces.async.AsyncResult;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;
import test.data.Stock;

import java.util.List;


public class PayloadSumDistributedTask implements DistributedTask<Long, Long> {
    private static final long serialVersionUID = 1L;
    private Stock template;
    @TaskGigaSpace
    private transient GigaSpace gigaSpace;
    private long partitionedRate;
    private long globalRate;

    public PayloadSumDistributedTask() {
    }

    public PayloadSumDistributedTask(Stock template) {
        this.template = template;
    }

    public Long execute() throws Exception {
        Stock[] stocks = gigaSpace.readMultiple(template);
        for (Stock s : stocks) {
            partitionedRate += s.getRate();
        }
        return partitionedRate;
    }


    public Long reduce(List<AsyncResult<Long>> asyncResults) throws Exception {
        for (AsyncResult<Long> res : asyncResults) {
            globalRate += res.getResult();
        }
        return globalRate;
    }
}
