package org.openspaces.example.data.ejb;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;

import com.j_spaces.core.IJSpace;

public class SpaceSingleton {

	private GigaSpace gigaSpace = null;

	private SpaceSingleton() {
		IJSpace space = new UrlSpaceConfigurer("jini://*/*/space").space();
		gigaSpace = new GigaSpaceConfigurer(space).gigaSpace();
	}

	private static class SingletonHolder {
		private final static SpaceSingleton instance = new SpaceSingleton();
	}

	public static SpaceSingleton getInstance() {
		return SingletonHolder.instance;
	}

	public GigaSpace getSpace() {
		return gigaSpace;
	}
}