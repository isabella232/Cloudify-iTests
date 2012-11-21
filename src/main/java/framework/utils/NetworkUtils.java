package framework.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtils {
	
	public static String[] resolveIpsToHostNames(String[] ips) {
		
		String[] machinesHostNames = new String[ips.length];
		for (int i = 0 ; i < ips.length ; i++) {
			String host = ips[i];
			machinesHostNames[i] = resolveIpToHostName(host);
		}
		return machinesHostNames;
	}
	
	public static String resolveIpToHostName(String ip) {

		String[] chars = ip.split("\\.");

		byte[] add = new byte[chars.length];
		for (int j = 0 ; j < chars.length ; j++) {
			add[j] =(byte) ((int) Integer.valueOf(chars[j]));
		}

		try {
			InetAddress byAddress = InetAddress.getByAddress(add);
			String hostName = byAddress.getHostName();
			LogUtils.log("IP address " + ip + " was resolved to " + hostName);
			return hostName;
		} catch (UnknownHostException e) {
			throw new IllegalStateException("could not resolve host name of ip " + ip);
		}
		
	}
}
