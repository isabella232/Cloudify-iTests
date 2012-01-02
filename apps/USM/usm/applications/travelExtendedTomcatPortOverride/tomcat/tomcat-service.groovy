service {
    def context = Class.forName("com.j_spaces.kernel.PlatformVersion")
    def absolutePath = context.getResource('/' + context.name.replace(".", "/")+ ".class").getPath()
    def absolutePath1 = absolutePath.substring(0, absolutePath.lastIndexOf("!"))
    def absolutePath2 = absolutePath1.substring(0, absolutePath1.lastIndexOf("/"))
    //def absolutePath3 =  absolutePath2.substring("file:/".length(), absolutePath2.length())+"/../../examples/travel/tomcat"
    if(System.getProperty("os.name").toLowerCase().startsWith("win")){
        def absolutePath3 =  absolutePath2.substring("file:/".length(), absolutePath2.length())+"/../../recipes/tomcat"
    }else{
        def absolutePath3 =  absolutePath2.substring("file:".length(), absolutePath2.length())+"/../../recipes/tomcat"
    }
    extend absolutePath3

    lifecycle{

        startDetection {
            !ServiceUtils.isPortsFree([9876, 8009] )
        }
    }

    network {
        port = 9876
        protocolDescription ="HTTP"
    }
}