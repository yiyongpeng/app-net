package app.util;

import app.base.IPlugin;

public interface IPluginProxyKit {

	IPlugin createPlugin(String className) throws Exception;

	void destroy();

	void init();

}
