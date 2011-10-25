
String url
String destinationFolder
String destinationFile

public wget(url){
	destinationFolder = System.properties("user.dir")
	wget(url, destinationFolder)
}

public wget(url, destinationFolder){
	String [] urlParse = url.split("/")
	fileName = urlParse[urlParse.length - 1]
	wget(url, destinationFolder, fileName)
}


public wget(url, destinationFolder, destinationFile){
	new AntBuilder().sequential {
		mkdir(dir:destinationFolder)
		get(src:url, dest:destinationFolder + destinationFile, skipexisting:true)
	}
}