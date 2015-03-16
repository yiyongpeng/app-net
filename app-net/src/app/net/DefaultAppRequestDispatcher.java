package app.net;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import org.apache.log4j.Logger;

public class DefaultAppRequestDispatcher implements AppRequestDispatcher {
	private Logger log = Logger.getLogger(getClass());

	private TIntObjectMap<AppServlet> servlets = new TIntObjectHashMap<AppServlet>();

	@Override
	public void init(AppHandler handler) {
		for (AppServlet servlet : servlets.valueCollection()) {
			servlet.init(handler);
		}
	}

	@Override
	public void dispatch(AppRequest request, AppResponse response)
			throws Exception {
		int mode = getMode(request);
		AppServlet servlet = servlets.get(mode);
		if (servlet != null) {
			servlet.service(request, response);
		} else {
			log.warn(String.format("Not found app-servlet: (%d)", mode));
		}
	}

	protected int getMode(AppRequest request) {
		return request.getMode();
	}

	@Override
	public void destroy() {
		for (AppServlet servlet : servlets.valueCollection()) {
			servlet.destroy();
		}
	}

	@Override
	public boolean hasServlet(int mode) {
		return servlets.containsKey(mode);
	}

	@Override
	public void addServlet(int mode, AppServlet servlet) {
		servlets.put(mode, servlet);
	}

	@Override
	public AppServlet removeServlet(int mode) {
		return servlets.remove(mode);
	}

	@SuppressWarnings("unchecked")
	@Override
	public AppServlet addServlet(String mode, String servletClass) throws ClassNotFoundException {
		Class<?> clazz0 = Thread.currentThread().getContextClassLoader().loadClass(servletClass);
		try{
			if (mode != null && !mode.trim().equals("")) {
				AppServlet servlet = (AppServlet) clazz0.newInstance();
				servlets.put(Integer.parseInt(mode), servlet);
				return servlet;
			} else {
				Class<AppServlet> clazz = (Class<AppServlet>) Thread.currentThread().getContextClassLoader().loadClass(servletClass);
				Code code = clazz.getAnnotation(Code.class);
				if (code != null) {
					AppServlet servlet = clazz.newInstance();
					addServlet(code.value(), servlet);
					return servlet;
				} else {
					throw new IllegalArgumentException("no service id : " + servletClass);
				}
			}
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
