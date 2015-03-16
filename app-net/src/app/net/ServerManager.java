package app.net;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class ServerManager {
	public static final String APP_ATTR_NAME = "__SERVER_MANAGER__";
	protected Logger log = Logger.getLogger(ServerManager.class);
	private static ServerManager instance = new ServerManager();

	public static ServerManager getInstance() {
		return instance;
	}

	protected Map<String, AppServer> servers = new HashMap<String, AppServer>();
	protected boolean runing;

	public void destroy(){
		if(runing)
			throw new IllegalStateException("servers is runing.");
		servers.clear();
	}
	
	public Map<String, AppServer> getServers() {
		return servers;
	}

	public AppServer getServer(String name) {
		return servers.get(name);
	}

	public void addServer(AppServer server) {
		if (runing)
			throw new IllegalStateException(
					"Servers is Runing, Can't add server.");
		servers.put(server.getName(), server);
		server.getHandler().setAttribute(APP_ATTR_NAME, this);
	}

	public void startup() {
		if (runing)
			return;
		runing = true;
		for (AppServer server : servers.values())
			server.start();
			
	}

	public void shutdown() {
		if (runing == false)
			return;
		runing = false;
		for (AppServer server : servers.values())
			server.stop();
			
	}
}
