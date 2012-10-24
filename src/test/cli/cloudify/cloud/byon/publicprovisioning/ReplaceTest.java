package test.cli.cloudify.cloud.byon.publicprovisioning;

import org.testng.annotations.Test;

public class ReplaceTest {
	
	@Test
	public void test() {
		
		String a = "cloudifyUrl \"sdfsdfsdf\"";
		System.out.println(a);
		String regex = "cloudifyUrl \".+\"";
		String replaceAll = a.replaceAll(regex, "hello");
		System.out.println(replaceAll);
		
	}

}
