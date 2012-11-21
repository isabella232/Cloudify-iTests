service {
	extend "../../../services/mongodb/mongod"
	numInstances 1
	lifecycle {
		startDetectionTimeoutSecs 480
	}
}