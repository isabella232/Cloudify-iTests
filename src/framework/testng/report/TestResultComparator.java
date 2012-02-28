package framework.testng.report;

import java.util.Comparator;

import org.testng.ITestResult;

/**
* Comparator for sorting TestNG test results alphabetically by method name.
* @author moran
*/
class TestResultComparator implements Comparator<ITestResult>
{
    public int compare(ITestResult result1, ITestResult result2)
    {
        return result1.getName().compareTo(result2.getName());
    }
}
