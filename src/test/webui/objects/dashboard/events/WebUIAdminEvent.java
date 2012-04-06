package test.webui.objects.dashboard.events;

public class WebUIAdminEvent {
	
	public static String DESC_ATTEMPTING_RECOVERY = "attempting recovery";
	public static String DESC_ATTEMPTING_DEPLOYMENT = "attempting deployment";
	public static String DESC_SUCCESSFULLY_INSTALLED = "successfully installed";
	public static String DESC_SUCCESSFULLY_UNINSTALLED = "successfully uninstalled";
	public static String DESC_SUCCESSFULLY_DEPLOYED = "deployed";
	public static String DESC_SERVICE_RESTARTED = "service restarted";
	public static String DESC_SUCCESSFULLY_REMOVED = "successfully removed";
	public static String DESC_FAILED_DEPLOYING = "failed deploying";
	public static String DESC_SERVICE_NON_RESPONSIVE = "service non-responsive";
	public static String DESC_FAILURE_SUSPECTED = "failure suspected";
	public static String DESC_CONTAINER_NA = "no container available";
	
	public enum EventStatus {
		
		OK,
		
		WARN,
		
		FAILED,
		
		IN_PROGRESS,		
	}
	
	private String title;
	private String description;
	private long time;
	
	public WebUIAdminEvent(String title, String description, long time) {
		this.description = description;
		this.time = time;
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public long getTime() {
		return time;
	}
	
	public void setTime(long time) {
		this.time = time;
	}
	
	public EventStatus getStatus() {
		return null;
	}
	
	@Override
	public String toString() {
		return this.title + " | " + this.description + " | " + this.time;
	}
}
