service {
	extend "../../../services/mongodb/mongod"
	numInstances 1
	compute {
		template "MEDIUM_WIN"
	}
}