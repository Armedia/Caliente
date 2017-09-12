import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import oracle.jcr.OracleRepository;
import oracle.jcr.OracleRepositoryFactory;
import oracle.stellent.jcr.IdcRepositoryConfiguration;

public class JCRTest {
	public JCRTest() {
		super();
	}

	public static void main(String[] args) throws Throwable {

		// Get a repository configuration object to set the connection parameters on.
		IdcRepositoryConfiguration configuration = new IdcRepositoryConfiguration();

		// Set the connection type. This example uses socket as the type, so only the host and
		// port are needed.
		configuration.setConfigurationProperty(IdcRepositoryConfiguration.CIS_CONFIG_SOCKET_TYPE, "socket");
		configuration.setConfigurationProperty(IdcRepositoryConfiguration.SERVER_CONFIG_HOST,
			"armdec6aapp06.dev.armedia.com");
		configuration.setConfigurationProperty(IdcRepositoryConfiguration.SERVER_CONFIG_PORT, "4444");

		// Create a repository object to use for opening a session.
		OracleRepository repository = OracleRepositoryFactory.createOracleRepository(configuration);

		// Open the session using the username/password. The repository login method with
		// authenticate the user.
		Session session = repository.login(new SimpleCredentials("weblogic", "system01".toCharArray()));
		Node rootNode = session.getRootNode();

		NodeIterator ni = rootNode.getNodes();
		String indent = " ";

		while (ni.hasNext()) {
			Node currNode = ni.nextNode();
			System.out.println(indent + currNode.getName());
		}
	}

}