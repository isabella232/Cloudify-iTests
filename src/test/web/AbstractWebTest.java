package test.web;

import static org.testng.AssertJUnit.fail;

import java.io.IOException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import test.AbstractTest;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class AbstractWebTest extends AbstractTest {
    
    protected WebClient webClient;

    @Override
    @BeforeMethod
    public void beforeTest() {
        super.beforeTest();
        webClient = new WebClient(BrowserVersion.FIREFOX_3);
    }

    protected void assertPageNotExists(String url) {
        try {
            webClient.getPage(url);
            fail("IOException should be thrown");
        } catch (IOException ce) {
            System.out.println("Expected Exception");
        }
    }

    protected void assertPageNotFound(String url) {
        HtmlPage page = null;
        try {
            page = webClient.getPage(url);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals(404, page.getWebResponse().getStatusCode());
    }

    protected HtmlPage assertPageExists(String url) {
        HtmlPage page = null;
        try {
            page = webClient.getPage(url);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals("OK", page.getWebResponse().getStatusMessage());
        return page;
    }

    protected HtmlPage getPage(String url) {
        
        HtmlPage page = null;
        try {
            page = webClient.getPage(url);
        } catch (Exception e) {
            return null;
        }
        
        if (page.getWebResponse().getStatusMessage().equals("OK")) {
            return page;
        } else {
            return null;
        }
        
    }
    
    protected void assertHtmlContainsText(HtmlPage page,String text) {
        assertHtmlContainsText(page, new String[] { text });
    }
    
    /***
     * 
     * Assert that at least one of the strings in 'texts' appears in page
     * 
     * @param page The page to check
     * @param texts Assert that at least one string in 'texts' appears in page
     * 
     */
    public void assertHtmlContainsText(HtmlPage page,String[] texts) {
        Iterable<HtmlElement> iter = page.getHtmlElementDescendants();
        boolean contains=false;
        
        outerloop:
        for (HtmlElement htmlElement : iter) {
            for (String text : texts) {
                if(htmlElement.asText() != null && 
                        htmlElement.asText().contains(text)) {
                    contains=true;
                    break outerloop;
                }
            }
        }
        
        assertEquals(true, contains);
    }
    
    @Override
    @AfterMethod
    public void afterTest() {
        webClient.closeAllWindows();
        webClient = null;
        super.afterTest();
    }
}
