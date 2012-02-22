package test.webui.objects.topology.physicalpanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import test.webui.resources.WebConstants;
import framework.utils.AssertUtils;
import framework.utils.LogUtils;
import framework.utils.WebUiUtils;

public class PuIBoxes {

	private List<PuIBox> puis;

	public PuIBoxes(String name, WebDriver driver) {
		List<PuIBox> puBoxes = new ArrayList<PuIBox>();
		List<WebElement> rectsAll = null;
		List<WebElement> textsAll = null;	

		List<WebElement> rects = new ArrayList<WebElement>();
		List<WebElement> texts = new ArrayList<WebElement>();

		List<String> textsString = null;
		List<String> puNames = null;
		List<String> colors = null;

		double seconds = 0;
		while (seconds < WebUiUtils.ajaxWaitingTime) {
			try {
				String id = WebConstants.ID.getHostId(name);
				WebElement hostElement = driver.findElement(By.id(id));
				rectsAll = hostElement.findElement(By.className("x-grid3-td-processing_unit_instances")).findElements(By.tagName("rect"));
				textsAll = hostElement.findElement(By.className("x-grid3-td-processing_unit_instances")).findElements(By.tagName("text"));

				for (int i = 0 ; i < rectsAll.size() ; i++) {
					WebElement eRect = rectsAll.get(i);
					WebElement eText = textsAll.get(i);
					if ((eRect.getAttribute("class") != null) && (eText.getAttribute("class") != null)) {
						rects.add(eRect);
						texts.add(eText);
					}
				}


				sortByPosition(rects);
				sortByPosition(texts);

				textsString = new ArrayList<String>();
				puNames = new ArrayList<String>();
				colors = new ArrayList<String>();

				for (int i = 0 ; i < rects.size() ; i++) {
					textsString.add(texts.get(i).getText());
					puNames.add(rects.get(i).getAttribute("class").trim().split(" ")[0].substring(13));
					colors.add(rects.get(i).getAttribute("class").split(" ")[1].substring(13));
				}

				break;
			}
			catch (StaleElementReferenceException e) {
				LogUtils.log("Failed to discover element due to statistics update, retyring...");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				seconds = seconds + 0.1;
			}
		}
		AssertUtils.assertTrue("Test failed because it was unable to discover the element",seconds != WebUiUtils.ajaxWaitingTime);

		for (int i = 0 ; i < rects.size() ; i++) {
			puBoxes.add(new PuIBox(Integer.parseInt(textsString.get(i)), i + 1, puNames.get(i), colors.get(i), driver));
		}
		this.puis = puBoxes;

	}

	public List<PuIBox> getBoxes() {
		return puis;
	}

	public List<PuIBox> getPuIBoxesOfAdifferentApplication() {

		List<PuIBox> result = new ArrayList<PuIBox>();
		for (PuIBox p : puis) {
			if (p.getColor().equals("null")) result.add(p);
		}
		return result;

	}

	public List<PuIBox> getPuIBoxesOfCurrentApplication() {

		List<PuIBox> result = new ArrayList<PuIBox>();
		for (PuIBox p : puis) {
			if (!p.getColor().equals("null")) result.add(p);
		}
		return result;
	}
	
	private void sortByPosition(List<WebElement> rects) {

		WebElement[] array = new WebElement[rects.size()];
		rects.toArray(array);

		for (int i = 0 ; i < rects.size() ; i++) {
			Arrays.sort(array, new Comparator<WebElement>() {

				@Override
				public int compare(WebElement arg0, WebElement arg1) {
					double x0 = Double.parseDouble(arg0.getAttribute("x"));
					double x1 = Double.parseDouble(arg1.getAttribute("x"));
					if (x0 == x1) return 0;
					if (x0 < x1) return -1;
					return 1;
				}
			});
		}

		rects.clear();
		for (WebElement e : array) {
			rects.add(e);
		}
	}
}
