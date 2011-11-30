package test.webui.objects.topology;

public class MetricType {

	private String type;
	private String name;

	public MetricType(String type, String name) {
		this.name = name;
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}
}
