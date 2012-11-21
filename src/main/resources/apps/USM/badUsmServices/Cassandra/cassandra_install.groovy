import java.util.concurrent.TimeUnit

config = new ConfigSlurper().parse(new File("cassandra.properties").toURL())
throw new Exception("Some runtime exception")
new AntBuilder().sequential {
	mkdir(dir:config.installDir)
	get(src:config.downloadPath, dest:"${config.installDir}/${config.zipName}", skipexisting:true)
	untar(src:"c\2.txt", dest:config.installDir, compression:"gzip")
	chmod(dir:"${config.home}/bin", perm:'+x', excludes:"*.bat")
}	

