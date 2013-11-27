/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.quality.iTests.test.cli.cloudify.cloud.templates;

public class EC2TemplateDetails extends TemplateDetails {
	private String imageId;
	private String locationId;
	private String hardwareId;
	private String keyFile;
	private String keyPair;
	private int machineMemoryMB;
	private String remoteDirectory;
	private String username;

	public final String getImageId() {
		return imageId;
	}

	public final void setImageId(final String imageId) {
		this.imageId = imageId;
	}

	public final String getLocationId() {
		return locationId;
	}

	public final void setLocationId(final String locationId) {
		this.locationId = locationId;
	}

	public final String getHardwareId() {
		return hardwareId;
	}

	public final void setHardwareId(final String hardwareId) {
		this.hardwareId = hardwareId;
	}

	public final String getKeyFile() {
		return keyFile;
	}

	public final void setKeyFile(final String keyFile) {
		this.keyFile = keyFile;
	}

	public final String getKeyPair() {
		return keyPair;
	}

	public final void setKeyPair(final String keyPair) {
		this.keyPair = keyPair;
	}

	public int getMachineMemoryMB() {
		return machineMemoryMB;
	}

	public void setMachineMemoryMB(final int machineMemoryMB) {
		this.machineMemoryMB = machineMemoryMB;
	}

	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setRemoteDirectory(final String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}
}
