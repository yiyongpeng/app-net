package app.base;

import org.dom4j.Element;

import app.filter.IFilter;
import app.net.AppFilter;
import app.net.AppHandler;
import app.net.AppRequestDispatcher;
import app.net.AppServer;
import app.net.AppServlet;

public class PluginAdapter implements IPlugin {
	protected AppContext context;
	protected Element pluginElement;
	
	@Override
	public void init(AppContext context, Element pluginElement) {
		this.context = context;
		this.pluginElement = pluginElement;
	}

	@Override
	public void startup() throws Exception {
	}

	@Override
	public void shutdown() {
	}
	
	@Override
	public void parsedServer(AppServer server, Element serverElement) {
	}

	@Override
	public void parseHandler(AppServer server, Element serverElement,
			AppHandler handler, Element handlerElement) {
	}

	@Override
	public void parseAppFilter(AppServer server, Element serverElement,
			AppFilter appFilter, Element filterElement) {
	}

	@Override
	public void parseFilter(AppServer server, Element serverElement,
			IFilter filter, Element filterElement) {
	}

	@Override
	public void parseDispatcher(AppServer server, Element serverElement,
			AppRequestDispatcher disptcher, Element dispatcherElement) {
	}

	@Override
	public void parsedServlet(AppServer server, AppServlet servlet,
			Element servletElement) {
	}

	@Override
	public void destroy() {
	}
}
