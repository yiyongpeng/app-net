package app.net;

import org.apache.log4j.Logger;

public abstract class AppServletAdapter implements AppServlet {
	protected final Logger log = Logger.getLogger(getClass());
	
	@Override
	public void service(AppRequest request, AppResponse response)
			throws ServletException {
	}

	@Override
	public void init(AppHandler handler) {
	}

	@Override
	public void destroy() {
	}

}
