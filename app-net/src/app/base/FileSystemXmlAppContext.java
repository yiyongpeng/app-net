package app.base;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import app.core.impl.DefaultContext;
import app.filter.IFilter;
import app.net.AppFilter;
import app.net.AppHandler;
import app.net.AppRequestDispatcher;
import app.net.AppServer;
import app.net.AppServlet;
import app.net.DefaultAppHandler;
import app.net.ServerManager;
import app.util.IPluginProxyKit;
import app.util.PluginProxyKit;
import app.util.ThreadContext;

public class FileSystemXmlAppContext extends DefaultContext implements AppContext {
	protected final Logger log = Logger.getLogger(getClass());

	private static final AppContext context = new FileSystemXmlAppContext();

	public static AppContext getDefault() {
		return context;
	}

	protected String manifestPath;
	protected Document doc;
	protected List<IPlugin> plugins;
	protected ServerManager serverManager;

	private IPluginProxyKit proxyKit;

	public FileSystemXmlAppContext() {
		proxyKit = new PluginProxyKit();
	}

	public FileSystemXmlAppContext(IPluginProxyKit proxyKit) {
		this.proxyKit = proxyKit;
	}

	public FileSystemXmlAppContext(String filePath) throws AppContextException {
		init(filePath);
		startup();
	}

	@SuppressWarnings("unchecked")
	public void init(String fileName) throws AppContextException {
		this.manifestPath = fileName;
		File manifestFile = new File(manifestPath);
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		proxyKit.init();
		boolean ttinited = ThreadContext.contains();
		if (ttinited == false) {
			ThreadContext.init();
		}
		ThreadContext.setAttribute(APP_ATTR_NAME, this);
		try {
			SAXReader xml = new SAXReader();
			doc = xml.read(manifestFile);

			Element root = doc.getRootElement();

			// Load plugins
			List<Element> pluginsEle = root.elements("plugin");
			plugins = new ArrayList<IPlugin>();

			if (pluginsEle != null)
				for (Element element : pluginsEle) {
					String className = element.attributeValue("class");
					IPlugin plugin = proxyKit.createPlugin(className);
					plugin.init(this, element);

					log.info("[Load-plugin] " + plugin);

					plugins.add(plugin);
				}

			// Load Servers
			serverManager = new ServerManager();
			List<Element> serverNodes = root.elements("server");
			for (Element serverElement : serverNodes) {
				// Server
				String serverClass = serverElement.attributeValue("class", AppServer.class.getName());
				AppServer server = (AppServer) loader.loadClass(serverClass).newInstance();
				server.setExecutorPoolSize(Integer.valueOf(serverElement.attributeValue("executorPoolSize", "0")));
				server.setReaderPoolSize(Integer.valueOf(serverElement.attributeValue("readPoolSize", "0")));
				server.setWriterPoolSize(Integer.valueOf(serverElement.attributeValue("writePoolSize", "0")));
				server.getHandler().setAttribute(APP_ATTR_NAME, this);
				server.setName(serverElement.attributeValue("name", server.getName()));
				log.info(String.format("[Create] Server(%s) : %s", server.getName(), serverClass));

				// Handler
				Element handlerElement = serverElement.element("handler");
				if (handlerElement != null) {
					String handlerClass = handlerElement.attributeValue("class");
					DefaultAppHandler handler = (DefaultAppHandler) loader.loadClass(handlerClass).newInstance();
					handler.setAttribute(APP_ATTR_NAME, this);
					for (IPlugin plugin : plugins)
						plugin.parseHandler(server, serverElement, handler, handlerElement);
					server.setHandler(handler);

					log.info(String.format("[Create] Handler : %s", handlerClass));

				}
				AppHandler handler = server.getHandler();

				// Filter
				// Name: accept acceptor encode decode message error closed app
				List<Element> filterNodes = serverElement.elements("filter");
				for (Element filterElement : filterNodes) {
					String name = filterElement.attributeValue("name").toLowerCase();
					String filterClass = filterElement.attributeValue("class").trim();
					if (name.equals("app")) {

						log.info(String.format("[Create] AppFilter : %s", filterClass));

						AppFilter appFilter = (AppFilter) loader.loadClass(filterClass).newInstance();
						for (IPlugin plugin : plugins)
							plugin.parseAppFilter(server, serverElement, appFilter, filterElement);
						handler.addLastFilter(appFilter);
					} else {

						log.info(String.format("[Create] Filter : (%s) %s", name, filterClass));

						IFilter filter = (IFilter) loader.loadClass(filterClass).newInstance();
						for (IPlugin plugin : plugins)
							plugin.parseFilter(server, serverElement, filter, filterElement);
						handler.getFilterChain().addLastFilter(name, filter);
					}
				}

				// Dispatcher
				Element dispatcherElement = serverElement.element("dispatcher");
				if (dispatcherElement != null) {
					String dispatcherClass = dispatcherElement.attributeValue("class");

					log.info(String.format("[Create] Dispatcher : %s", dispatcherClass));

					AppRequestDispatcher disptcher = (AppRequestDispatcher) loader.loadClass(dispatcherClass).newInstance();
					for (IPlugin plugin : plugins)
						plugin.parseDispatcher(server, serverElement, disptcher, dispatcherElement);
					handler.setDispatcher(disptcher);

				}

				// Servlet
				List<Element> servletNodes = serverElement.elements("servlet");
				for (Element servletElement : servletNodes) {
					String id = servletElement.attributeValue("id");
					String servletClass = servletElement.attributeValue("class");

					log.info(String.format("[Create] Servlet : %s %s", id != null ? " (" + id + ")" : "", servletClass));

					AppServlet servlet = handler.getDispatcher().addServlet(id, servletClass);
					for (IPlugin plugin : plugins)
						plugin.parsedServlet(server, servlet, servletElement);
				}

				// Config address port
				String hostName = serverElement.attributeValue("address", "0.0.0.0");
				int port = Integer.parseInt(serverElement.attributeValue("port"));
				server.setHostname(hostName);
				server.setPort(port);

				for (IPlugin plugin : plugins)
					plugin.parsedServer(server, serverElement);

				// Add Server
				serverManager.addServer(server);

				log.info(String.format("[Create] Server(%s) finished.", server.getName()));

			}
		} catch (Exception e1) {
			throw new AppContextException("init failed. manifestPath: " + manifestFile, e1);
		} finally {
			if (ttinited == false) {
				ThreadContext.destory();
			}
		}

	}

	public void destroy() {
		for (IPlugin plugin : plugins) {
			plugin.destroy();
		}
		plugins.clear();

		serverManager.destroy();
		proxyKit.destroy();

	}

	public void startup() throws AppContextException {
		// Startup all plugins
		for (IPlugin plugin : plugins)
			try {
				plugin.startup();
			} catch (Exception e) {
				throw new AppContextException("startup failed : " + plugin, e);
			}

		// Startup all Servers
		try {
			serverManager.startup();
		} catch (Exception e) {
			throw new AppContextException("startup failed!!!", e);
		}
	}

	public void shutdown() {
		// Shutdown all Servers
		serverManager.shutdown();

		// Startup all plugins
		for (IPlugin plugin : plugins)
			plugin.shutdown();

	}

	public List<IPlugin> getPlugins() {
		return plugins;
	}

	public ServerManager getServerManager() {
		return serverManager;
	}

	public String getManifestPath() {
		return manifestPath;
	}

	public Document getDocument() {
		return doc;
	}

}
