import org.cloudifysource.dsl.context.ServiceContext
import org.cloudifysource.dsl.context.ServiceContextFactory


ServiceContext context = ServiceContextFactory.getServiceContext()

new Thread(new Runnable() {

	public void run() {

		ServerSocket serverSocket = new ServerSocket();
		serverSocket.bind(new InetSocketAddress("127.0.0.1", 9000 + context.instanceId))

		while (true) {
			try {
				Socket clientSocket = serverSocket.accept();
				println "Connection accepted"
				clientSocket.close();
			} catch(Exception e) {
				// ignore
			}
		}
	}
}).start()

sleep(Long.MAX_VALUE)