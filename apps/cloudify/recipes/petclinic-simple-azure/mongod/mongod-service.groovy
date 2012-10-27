import com.mongodb.CommandResult;
import com.mongodb.Mongo;
import com.mongodb.DB;

service {
	extend "../../../services/mongodb/mongod"
	numInstances 1
	lifecycle {
		install "mongod_install.groovy"
		start "mongod_start.groovy"
		startDetectionTimeoutSecs 480
		startDetection {
			ServiceUtils.isPortOccupied(context.attributes.thisInstance["port"])
		}

		monitors{
			try {
				port  = context.attributes.thisInstance["port"] as int
				mongo = new Mongo("127.0.0.1", port)
				db = mongo.getDB("mydb")
														
				result = db.command("serverStatus")
				
														
				return [
					"Active Read Clients":result.globalLock.activeClients.readers,
					"Active Write Clients":result.globalLock.activeClients.writers,
					"Read Clients Waiting":result.globalLock.currentQueue.readers,
					"Write Clients Waiting":result.globalLock.currentQueue.writers,
					"Current Active Connections":result.connections.current,
					"Open Cursors":result.cursors.totalOpen
				]
			}
			finally {
				if (null!=mongo) mongo.close()
			}
			
			
		}
	}
}