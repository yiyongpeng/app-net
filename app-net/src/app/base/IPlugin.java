package app.base;

import org.dom4j.Element;

import app.filter.IFilter;
import app.net.AppFilter;
import app.net.AppHandler;
import app.net.AppRequestDispatcher;
import app.net.AppServer;
import app.net.AppServlet;

public interface IPlugin {

	void startup() throws Exception;

	void shutdown();
	
	void init(AppContext context, Element pluginElement);

	void destroy();

	void parsedServer(AppServer server, Element serverElement);

	void parseHandler(AppServer server, Element serverElement,
			AppHandler handler, Element handlerElement);

	void parseAppFilter(AppServer server, Element serverElement,
			AppFilter appFilter, Element filterElement);

	void parseFilter(AppServer server, Element serverElement, IFilter filter,
			Element filterElement);

	void parseDispatcher(AppServer server, Element serverElement,
			AppRequestDispatcher disptcher, Element dispatcherElement);

	void parsedServlet(AppServer server, AppServlet servlet,
			Element servletElement);
}
