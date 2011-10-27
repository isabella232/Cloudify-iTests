package com.gigaspaces.ps.hibernateimpl.loader;

import com.gigaspaces.ps.hibernateimpl.common.Account;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MyDataLoader {
    private ExecutorService executorService;
    private LoaderTask loaderTask;
    private int instanceId;
    private int startIdFrom;

    @GigaSpaceContext(name = "hibernateLocalCacheSpace")
    private GigaSpace gigaSpace;
    private int accounts;
    private long delay;

    @PostConstruct
    public void construct() {
        System.out.println("--- STARTING FEEDER ");
        if (instanceId != 0) {
            // have a range of ids based on the instance id of the processing unit
            startIdFrom = instanceId * 100000000;
        }
        executorService = Executors.newSingleThreadExecutor();
        loaderTask = new LoaderTask();
        executorService.execute(loaderTask);
    }

    public void setInstanceId(int instanceId) {
        this.instanceId = instanceId;
    }

    public void setAccounts(int accounts) {
        this.accounts = accounts;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }


    public class LoaderTask implements Runnable {
        int i = 0;
        public void run() {
            try {
                for (i = 0; i < accounts; i++) {
                    Account account = new Account();
                    int index = startIdFrom + i;
                    account.setId(index);
                    account.setFirstName("FirstName" + index);
                    account.setLastName("LastName" + index);
                    gigaSpace.write(account);
                    System.out.println(this.getClass().getSimpleName() + " Wrote Account " + account);
                    Thread.sleep(delay);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

         public int getCounter() {
            return i;
        }
    }

    public int getLoaderCount() {
        return loaderTask.getCounter();
    }

}