import org.cloudifysource.dsl.utils.ServiceUtils

service {
    def context = Class.forName("com.j_spaces.kernel.PlatformVersion")
    def absolutePath = context.getResource('/' + context.name.replace(".", "/")+ ".class").getPath()
    def absolutePath1 = absolutePath.substring(0, absolutePath.lastIndexOf("!"))
    def absolutePath2 = absolutePath1.substring(0, absolutePath1.lastIndexOf("/"))
    //def absolutePath3 =  absolutePath2.substring("file:/".length(), absolutePath2.length())+"/../../examples/travel/tomcat"
    def absolutePath3
    if(System.getProperty("os.name").toLowerCase().startsWith("win")){
       absolutePath3 =  absolutePath2.substring("file:/".length(), absolutePath2.length())+"/../../recipes/services/tomcat"
    }else{
        absolutePath3 =  absolutePath2.substring("file:".length(), absolutePath2.length())+"/../../recipes/services/tomcat"
    }
    extend absolutePath3

    lifecycle{

        startDetection {
            !ServiceUtils.arePortsFree([9876, 8009] )
        }
    }

    network {
        port = 9876
        protocolDescription ="HTTP"
    }
}