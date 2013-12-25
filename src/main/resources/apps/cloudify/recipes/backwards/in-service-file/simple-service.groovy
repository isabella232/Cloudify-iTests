import org.cloudifysource.dsl.context.ServiceContextFactory

service {
    
	name "simple-backwards"
	type "UNDEFINED"
	
	lifecycle {

		start (["Win.*":"run.bat", "Linux":"run.sh"])
        
	}
}