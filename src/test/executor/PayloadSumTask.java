package test.executor;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.Task;
import org.openspaces.core.executor.TaskGigaSpace;
import test.data.Stock;


public class PayloadSumTask implements Task<Long> {
    private static final long serialVersionUID = 1L;
    private Stock template;
    @TaskGigaSpace
    private transient GigaSpace gigaSpace;
    private long partitionedRate;

    public PayloadSumTask(Stock stock) {
        this.template = stock;
    }

    public Long execute() throws Exception {
        Stock[] stocks = gigaSpace.readMultiple(template);
        for (Stock s : stocks) {
            partitionedRate += s.getRate();
        }
        return partitionedRate;
    }
}
