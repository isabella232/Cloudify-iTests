
service {
    def context = Class.forName("com.j_spaces.kernel.PlatformVersion")
    def absolutePath = context.getResource('/' + context.name.replace(".", "/")+ ".class").getPath()
    def absolutePath1 = absolutePath.substring(0, absolutePath.lastIndexOf("!"))
    def absolutePath2 = absolutePath1.substring(0, absolutePath1.lastIndexOf("/"))
    if(System.getProperty("os.name").toLowerCase().startsWith("win")){
        def absolutePath3 =  absolutePath2.substring("file:/".length(), absolutePath2.length())+"/../../examples/travel/cassandra"
    }else{
        def absolutePath3 =  absolutePath2.substring("file:".length(), absolutePath2.length())+"/../../examples/travel/cassandra"
    }

    extend absolutePath3
    name "cassandra-extend"
}


