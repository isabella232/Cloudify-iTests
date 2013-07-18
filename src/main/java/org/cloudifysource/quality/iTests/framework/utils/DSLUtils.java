package org.cloudifysource.quality.iTests.framework.utils;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.Service;

public class DSLUtils {
	
	/**
	 * This method is intended to write applications to a -application.groovy file
	 * inside the destination folder. it should be used to generate application files on the fly using dsl POJOS.
	 * eliminating the need to maintain so many application dsl's within SGTest
	 * @param application
	 * @param destFolder
	 * @throws IOException 
	 */
	public static void writeSimpleApplicationToFolder(final Application application, final File destFolder) throws IOException {
		
		final File applicationGroovy = new File(destFolder,application.getName() + "-application.groovy");
		
		StringBuilder applicationBuilder = new StringBuilder();
		applicationBuilder.append("application").append(" ").append("{").append("\n");
		applicationBuilder.append("name = ").append('"' + application.getName() + '"').append("\n");
		
		for (final Service service : application.getServices()) {
			applicationBuilder.append("service").append(" ").append("{").append("\n");
			applicationBuilder.append("name = ").append('"' + service.getName() + '"').append("\n");
			applicationBuilder.append("dependsOn = ").append(listToGroovyArray(service.getDependsOn())).append("\n");
			applicationBuilder.append("}\n");		
		}
		applicationBuilder.append("}");
		FileUtils.writeStringToFile(applicationGroovy, applicationBuilder.toString());	
	}
	
	public static String listToGroovyArray(final List<String> list) {
		
		final List<String> copy = new ArrayList<String>();
		for (String s : list) {
			copy.add('"' + s + '"');
		}
		
		Binding binding = new Binding();
		binding.setVariable("list", copy);
		GroovyShell shell = new GroovyShell(binding);
		Object evaluate = shell.evaluate("list as String");
		return evaluate.toString();	
	}
}
