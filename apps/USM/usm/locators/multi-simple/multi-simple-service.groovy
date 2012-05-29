service {
	name "multi-simple"
	icon "icon.png"
	type "WEB_SERVER"

	lifecycle {
		start (["Win.*":"run.bat", "Linux":"run.sh"])
		locator {
			return ServiceUtils.ProcessUtils.getPidsWithMainClass("simplejavaprocess.jar")
		}

		startDetection {
			return ServiceUtils.isPortOccupied(7777) && ServiceUtils.isPortOccupied(7778)
		}
	}
}