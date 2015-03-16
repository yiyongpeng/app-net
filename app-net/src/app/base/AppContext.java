package app.base;

import java.util.List;

import org.dom4j.Document;

import app.core.Context;
import app.net.ServerManager;

public interface AppContext extends Context{
	String APP_ATTR_NAME = "__APP_CONTEXT__";
	 
	void init(String fileName)throws AppContextException;
	
	void startup()throws AppContextException;
	
	void shutdown();
	
	void destroy();
	
	ServerManager getServerManager();

	List<IPlugin> getPlugins();
	
	String getManifestPath();
	
	Document getDocument();
	
}
