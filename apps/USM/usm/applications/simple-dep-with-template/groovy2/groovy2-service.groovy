import java.util.concurrent.TimeUnit


service {
	name "groovy2"
	type "WEB_SERVER"
	elastic true
	lifecycle { start "run.groovy" }

	compute {
		template "HA_PROXY"
	}
}