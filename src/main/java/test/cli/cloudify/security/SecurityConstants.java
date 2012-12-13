package test.cli.cloudify.security;

import framework.tools.SGTestHelper;

public class SecurityConstants {
	
	public static final String ALL_ROLES_USER_PWD = "Superuser";
	public static final String CLOUD_ADMIN_USER_PWD = "Amanda";
	public static final String CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD = "Dana";
	public static final String APP_MANAGER_USER_PWD = "Dan";
	public static final String APP_MANAGER_AND_VIEWER_USER_PWD = "Don";
	public static final String VIEWER_USER_PWD = "John";
	public static final String NO_ROLE_USER_PWD = "Jane";

	public static final String ALL_ROLES_DESCRIPTIN = ALL_ROLES_USER_PWD + " (all roles)";
	public static final String CLOUD_ADMIN_DESCRIPTIN = CLOUD_ADMIN_USER_PWD + " (cloud admin)";
	public static final String CLOUD_ADMIN_AND_APP_MANAGER_DESCRIPTION = CLOUD_ADMIN_AND_APP_MANAGER_USER_PWD + " (cloud admin and app manager)";
	public static final String APP_MANAGER_DESCRIPTIN = APP_MANAGER_USER_PWD + " (app manager)";
	public static final String APP_MANAGER_AND_VIEWER_DESCRIPTIN = APP_MANAGER_AND_VIEWER_USER_PWD + " (app manager and viewer)";
	public static final String VIEWER_DESCRIPTIN = VIEWER_USER_PWD + " (viewer)";
	public static final String NO_ROLE_DESCRIPTIN = NO_ROLE_USER_PWD + " (no roles)";
	
	public static final String GE_GROUP = "GE";
	public static final String BEZEQ_GROUP = "Bezeq";
	public static final String CELLCOM_GROUP = "Cellcom";
	
	public static final String CLOUDADMINS_GROUP = "ROLE_CLOUDADMINS";
	public static final String APPMANAGERS_GROUP = "ROLE_APPMANAGERS";
	public static final String VIEWERS_GROUP = "ROLE_VIEWERS";

	public static final String SGTEST_ROOT_DIR = SGTestHelper.getSGTestRootDir().replace('\\', '/');
	public static final String BUILD_SECURITY_FILE_PATH = SGTestHelper.getBuildDir().replace('\\', '/') + "/config/security/spring-security.xml";
	public static final String BUILD_SECURITY_BACKUP_FILE_PATH = SGTestHelper.getBuildDir().replace('\\', '/') + "/config/security/spring-security.xml.backup";
	public static final String DEFAULT_KEYSTORE_FILE_PATH = SGTestHelper.getSGTestRootDir().replace('\\', '/') + "/src/main/config/security/keystore";
	public static final String DEFAULT_KEYSTORE_PASSWORD = "sgtest";
	public static final String LDAP_SECURITY_FILE_PATH = SGTEST_ROOT_DIR + "/src/main/config/security/ldap-spring-security.xml";
}
