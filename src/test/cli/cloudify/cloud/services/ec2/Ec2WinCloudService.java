package test.cli.cloudify.cloud.services.ec2;

public class Ec2WinCloudService extends Ec2CloudService {
	
	private String cloudName = "ec2-win";
	
	@Override
	public String getCloudName() {
		return cloudName;
	}

}
