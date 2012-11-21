package test.webui.recipes.applications.recipespanel;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import test.webui.recipes.applications.AbstractSeleniumApplicationRecipeTest;

import com.gigaspaces.webuitf.LoginPage;
import com.gigaspaces.webuitf.topology.TopologyTab;
import com.gigaspaces.webuitf.topology.applicationmap.ApplicationMap;
import com.gigaspaces.webuitf.topology.recipes.RecipesPanel;
import com.gigaspaces.webuitf.topology.recipes.RecipesViewPanel;
import com.gigaspaces.webuitf.topology.recipes.selectionpanel.RecipeFileNode;
import com.gigaspaces.webuitf.topology.recipes.selectionpanel.RecipeFolderNode;
import com.gigaspaces.webuitf.topology.recipes.selectionpanel.RecipesSelectionPanel;

import framework.utils.LogUtils;
import framework.utils.ScriptUtils;

public class SimpleRecipeViewerTest extends AbstractSeleniumApplicationRecipeTest {
	
	private static final String TRAVEL_APPLICATION_NAME = "travel";
	private static final String CASSANDRA_SERVICE_NAME = "cassandra";
	private static final String CASSANDRA_SERVICE_FILE_NAME = "cassandra_install.groovy";
	private static final String CASSANDRA_SERVICE_FULL_NAME = ServiceUtils.getAbsolutePUName(TRAVEL_APPLICATION_NAME, CASSANDRA_SERVICE_NAME);
	
	@Override
	@BeforeMethod
	public void install() throws IOException, InterruptedException {
		setCurrentApplication(TRAVEL_APPLICATION_NAME);
		super.install();
	}
	
	@Test(timeOut = DEFAULT_TEST_TIMEOUT, enabled = true)
	public void recipeViewerTest() throws IOException, InterruptedException {
		
		LoginPage loginPage = getLoginPage();
		TopologyTab topologyTab = loginPage.login().switchToTopology();
		RecipesPanel recipesPanel = topologyTab.getTopologySubPanel().switchToRecipes();
		
		admin.getApplications().waitFor(TRAVEL_APPLICATION_NAME, waitingTime, TimeUnit.SECONDS);
		topologyTab.selectApplication(TRAVEL_APPLICATION_NAME);
		
		ApplicationMap applicationMap = topologyTab.getApplicationMap();
		applicationMap.deselectAllNodes();

		// 1. get selection tree, get editor
		RecipesSelectionPanel selectionPanel = recipesPanel.getSelectionPanel();
		RecipesViewPanel viewPanel = recipesPanel.getViewPanel();

		// 2. expand a folder node
		RecipeFolderNode recipeFolderNode = selectionPanel.getRecipeFolderNode(CASSANDRA_SERVICE_FULL_NAME);
		recipeFolderNode.expand();
		
		// 3. check editor empty and / or masked
		LogUtils.log("> checking recipes viewer is empty before loading a file...");
		String content = viewPanel.getContent();
		assertTrue("Recipes view panel is not empty", content == null || content.trim().isEmpty());

		// 4. get service file content
		String servicePath = ScriptUtils.getBuildRecipesServicesPath()
				+ File.separator + CASSANDRA_SERVICE_NAME;
		File serviceFile = new File(servicePath + File.separator
				+ CASSANDRA_SERVICE_FILE_NAME);
		String fileContent = FileUtils.readFileToString(serviceFile);
		
		// 5. select a file node
		RecipeFileNode recipeFileNode = selectionPanel.getRecipeFileNode(CASSANDRA_SERVICE_FULL_NAME, CASSANDRA_SERVICE_FILE_NAME);
		recipeFileNode.select();
		
		// 6. check editor contains the expected text
		content = viewPanel.getContent();
		
		LogUtils.log("> checking file contents from the build and from the recipes viewer match...");
		assertTrue(
				"Service file content does not match the text inside the recipes viewer",
				content != null && content.equals(fileContent));

		// 7. (optional, for a more complex text):
		//		repeat steps 5 and 6 for several files: test supported extensions 
		// 		(.groovy, .properties, .ini, .py, .rb, .sh, 
		// 		common markdown extensions: .markdown, .mdown, .mkdn, .md, .mkd, .mdwn, .mdtxt, .mdtext, .text)

		
		// clear environment
		uninstallApplication(TRAVEL_APPLICATION_NAME, true);
	}

}
