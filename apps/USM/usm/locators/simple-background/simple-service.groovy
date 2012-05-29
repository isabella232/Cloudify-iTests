service {
	name "simple-background"
	icon "icon.png"
	type "WEB_SERVER"

	lifecycle {


		start (["Win.*":"run.bat", "Linux":"run.sh"])

		locator {
			return ServiceUtils.ProcessUtils.getPidsWithMainClass("simplejavaprocess.jar")
		}
		
		startDetection {
			return ServiceUtils.isPortOccupied(7777)
		}
	}

}