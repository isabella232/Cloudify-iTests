package framework.utils;

import org.testng.ITestResult;

public class TestNGUtils {
	
	public static String constructTestMethodName(ITestResult iTestResult) {
		String parameters = extractParameters(iTestResult);
		int currentInvocationCount = iTestResult.getMethod().getCurrentInvocationCount();
		return iTestResult.getMethod().toString().split("\\(|\\)")[0] + "(" + parameters + ")";
		
	}
	
	/**
	 * @param iTestResult
	 * @return a string  of the test's invoked parameters separated by a comma (',')
	 */
	public static String extractParameters(ITestResult iTestResult) {
		String parameters = "";
        Object[] params = iTestResult.getParameters();
        if (params.length != 0) {
        	parameters = params[0].toString();
            for (int i = 1 ; i < params.length ; i++) {
            	parameters += parameters + "_";
            }
        }
        return parameters;	
	}

}
