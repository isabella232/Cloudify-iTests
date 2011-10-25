package com.gigaspaces.gateway;

import java.text.DateFormat;
import java.util.Calendar;

import com.gatewayPUs.common.MessageGW;
import com.gatewayPUs.common.Stock;
import com.gigaspaces.cluster.replication.gateway.conflict.*;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Sagi Bernstein
 * @since 8.0.4
 *
 */
public class TxnConflictResolver extends ConflictResolver {
	private int numOfTxn;
	private Stock [] stocks;
	private int numOfEntries; //the number of expected entries



	@Autowired
	protected GigaSpace gigaSpace;

	public TxnConflictResolver() {
		numOfTxn = 0;

	}

	@Override
	public void onDataConflict(String sourceGatewayName, DataConflict conflict) {
		numOfTxn++;
		System.out.println("**************************DATA CONFLICT**************************");
		

		stocks = new Stock[conflict.getOperations().length];
		for(int i = 0; i < conflict.getOperations().length; i++){
			stocks[i] = (Stock) conflict.getOperations()[i].getOperationEntry();
			System.out.println("stock " + i + " id: " + stocks[i].getStockId());
		}
		System.out.println(stocks.length);
		printCurrentTime();
		System.out.println("\n");
		
		boolean okay = (stocks.length == numOfEntries);
		
		//check that all operations are ordered by the partition number
		//@pre = all operations in original txn are stocks with id 1 incremental
		for(int i = 0; i < stocks.length && okay; i++){
			Integer partitionNum = stocks[i].getData();
			for(int j = i + 1; j < stocks.length && okay; j++){
				if(stocks[j].getData() == partitionNum)
					okay = (stocks[i].getStockId() < stocks[j].getStockId());
			}
		}
		if (okay){
			MessageGW template = new MessageGW(numOfTxn);
			template.setInfo("txn");
			gigaSpace.write(template);
		}
		conflict.overrideAll();

	}

	@Override
	public void onRegisterTypeDescriptorConflict(String sourceGatewayName,
			RegisterTypeDescriptorConflict conflict) {
	}

	@Override
	public void onAddIndexConflict(String sourceGatewayName,
			AddIndexConflict conflict) {
	}

	/**
	 * @param numOfEntries the numOfEntries to set
	 */
	public void setNumOfEntries(int numOfEntries) {
		this.numOfEntries = numOfEntries;
	}
	
	private void printCurrentTime(){
		Calendar cal = Calendar.getInstance();
		DateFormat df = DateFormat.getTimeInstance(DateFormat.FULL);
		System.out.println(df.format(cal.getTime()));
	}
}
