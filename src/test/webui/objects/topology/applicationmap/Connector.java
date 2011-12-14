package test.webui.objects.topology.applicationmap;

public class Connector {
	
	private ApplicationNode source;
	private ApplicationNode target;
	private String status;
	
	public Connector(ApplicationNode source, ApplicationNode target, String status) {
		this.source = source;
		this.target = target;
		this.status = status;
	}
	
	public ApplicationNode getSource() {
		return source;
	}
	public ApplicationNode getTarget() {
		return target;
	}
	
	public String getStatus() {
		return status;
	}
	
}
