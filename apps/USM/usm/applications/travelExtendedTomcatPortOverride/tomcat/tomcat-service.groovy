service {
	extend "../../../tomcatHttpLivenessDetectorPlugin"
	
	lifecycle{
		init "tomcat_install.groovy"
	}
}