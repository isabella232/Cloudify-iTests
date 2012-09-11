service {
	name "multi-simple"
	type "WEB_SERVER"

	lifecycle {
		start (["Win.*":"run.bat", "Linux":"run.sh"])
		locator {
			def winPids = ServiceUtils.ProcessUtils.getPidsWithMainClass("simplejavaprocess.jar")
			def linuxPids = ServiceUtils.ProcessUtils.getPidsWithName("nc")
			
			List<Long> merge = []
			merge.addAll(winPids)
			merge.addAll(linuxPids)
			
			return merge
		}

		startDetection {
			return ServiceUtils.isPortOccupied(7777) && ServiceUtils.isPortOccupied(7778)
		}
	}
}