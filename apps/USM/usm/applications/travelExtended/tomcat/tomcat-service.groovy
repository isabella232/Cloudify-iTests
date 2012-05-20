service {
    def context = Class.forName("com.j_spaces.kernel.PlatformVersion")
    def absolutePath = context.getResource('/' + context.name.replace(".", "/")+ ".class").getPath()
    def absolutePath1 = absolutePath.substring(0, absolutePath.lastIndexOf("!"))
    def absolutePath2 = absolutePath1.substring(0, absolutePath1.lastIndexOf("/"))
    def absolutePath3
    if(System.getProperty("os.name").toLowerCase().startsWith("win")){
        absolutePath3 =  absolutePath2.substring("file:/".length(), absolutePath2.length())+"/../../recipes/apps/travel/tomcat"
    }else{
        absolutePath3 =  absolutePath2.substring("file:".length(), absolutePath2.length())+"/../../recipes/apps/travel/tomcat"
    }
    //def absolutePath3 =  absolutePath2.substring("file:/".length(), absolutePath2.length())+"/../../recipes/tomcat"
    extend absolutePath3
    name "tomcat-extend"
}