package test.scripts;

import static org.testng.AssertJUnit.assertEquals;
import static test.utils.LogUtils.log;

import java.util.Iterator;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import test.AbstractTestSuite;
import test.utils.CliUtils;
import test.utils.ScriptUtils;

/***
 * 
 * This test gets its data input from test/scripts/resources/script-patterns.xml
 * Each script is denoted by a script tag with the 'name' attribute being the actual 
 * script (+ arguments) tested.
 * The 'timeout' attribute tell the test when to break and kill the running script 
 * (and maybe its descendants)
 * 
 * Each pattern tag inside script tags represents a regualr expression ('regex' attribute)
 * that is to be matched against the script output and is expected to appear a certain number
 * of times ('expected-amount' attribute)
 * 
 * @author Dan Kilman
 *
 */
public class ScriptsTest extends AbstractTestSuite {
    
    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1", dataProvider="dataProvider")
    public void test(String scriptName, Object[] patterns, long timeout) throws Exception {
        
        log("Waiting " + timeout + "ms for " + scriptName +
                ScriptUtils.getScriptSuffix() + " to reach a stable state");

        //Here we run the script
        String scriptOutput = ScriptUtils.runScript(scriptName, timeout);

        
        for (int i=0; i < patterns.length; i++) {
            String[] patternPair = (String[]) patterns[i];
            String regex = patternPair[0];
            int expectedAmount = 0;
            
            try {
                expectedAmount = Integer.parseInt(patternPair[1]);
            } catch (NumberFormatException e) {
                Assert.fail("Error parsing xml file", e);
            }
            
            log("Testing against '" + regex + "' and expecting it to appear " + expectedAmount + " time(s)");
            int actualAmount = CliUtils.patternCounter(regex, scriptOutput);
            String failureOutput = "When testing: " + regex;
            if (expectedAmount == 0 && actualAmount > 0) {
                failureOutput += " , found: " + CliUtils.getLastPatternMatch();
            }
            
            assertEquals(failureOutput, expectedAmount, actualAmount);
        }
    
    }
    
    @DataProvider(name = "dataProvider") 
    public Iterator<Object[]> dataProvider() throws Exception {
        return new ScriptUtils.ScriptPatternIterator("test/scripts/resources/script-patterns.xml");
    }
    
}
