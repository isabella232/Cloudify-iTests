/*
 * Copyright 200005 GigaSpaces Technologies Ltd. All rights reserved.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS," WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED INCLUDING BUT NOT LIMITED TO WARRANTIES OF MERCHANTABILITY AND 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT. GIGASPACES WILL NOT 
 * BE LIABLE FOR ANY DAMAGE OR LOSS IN CONNECTION WITH THE SOFTWARE.
 */
/**
 * Title:        Caching/Space Topologies Example
 * Description:  /docs/ExampleDesc_Space_Topologies.htm
 * 
 * This examples writes and reads entries in a loop so someone can see
 * performance differences depending on the used topology. Space or Map-API 
 * is used.
 * 
 */

package com.gigaspaces.ps.hibernateimpl.loader;

import java.text.DecimalFormat;
import java.util.Random;

import net.jini.core.lease.Lease;

import com.gigaspaces.ps.hibernateimpl.common.Account;
import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.SpaceFinder;
import com.j_spaces.map.IMap;

public class PopulateCache {
	static int maxObj = 10000;
	static int objectSize = 128;
	static int sampleSize = 500000;
	public static String operation = null;
	static final String OPERATION_READ = "read";
	static final String OPERATION_WRITE = "write";
	static final String OPERATION_BOTH = "both";
	static DecimalFormat df = new DecimalFormat("#0.00");
	public static boolean isMap = false;
	IJSpace space = null;
	IMap map = null;

	public PopulateCache(String url) throws Exception {
		Lease leases[] = new Lease[5000];

		Random ran1 = new Random();
		Random ran2 = new Random();
		
		System.out.println("\nConnect to " +  url );
		space = (IJSpace) SpaceFinder.find(url);
		if (space == null) {
			System.out.println("Space not found: " + url);
			System.exit(-1);
		}
		System.out.println("Connected successfully ! ");

		try {
			
			long iterationStartTime =0l;
			long startTime =0l;
			long endTime = 0l;
			System.out.println("Writing 1 thru 5000 entries to space...");

			iterationStartTime = System.currentTimeMillis();
			for (int i = 0; i < 5000; i++) {
				Account obj = new Account();
				

	            long curRandom = System.nanoTime()%1000;
	            obj.setId((int)curRandom);
	            obj.setFirstName("FirstName"+curRandom);
	            obj.setLastName("LastName"+curRandom);
	            
				startTime = System.currentTimeMillis();
				leases [i] = space.write(obj, null, Lease.FOREVER);
				endTime = System.currentTimeMillis();
				System.out.println("Writing entry '" + i + "' ...Done in " + (endTime - startTime) + " milliseconds");
			}
			System.out.println("Written 1 thru 5000 entries to space in " + (endTime - iterationStartTime ) + " milliseconds");
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}

	public static void main(String[] args) throws Exception {
	/*	if (args.length != 1) {
			System.out.println("Usage: <spaceURL>");
			System.out.println("<protocol_name>://host:port/container-name/space-name");
			
			System.exit(1);
		}*/
		PopulateCache populatecache = new PopulateCache(args[0]);
	}
}
