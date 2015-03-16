package app.net;

import app.base.AppContext;

@Code(9)
public class ShutdownServlet extends AppServletAdapter {

	@Override
	public void service(AppRequest request, AppResponse response)
			throws ServletException {
		AppHandler handler = request.getSession().getServerHandler();
		AppContext app = (AppContext) handler.getAttribute(AppContext.APP_ATTR_NAME);

		onShutdownBefore(app, request);
		onShutdownDoing(app, request);
		onShutdownAfter(app, request);
		onShutdownExit(app, request);
		
	}

	protected void onShutdownExit(AppContext app, AppRequest request) {
		System.exit(0);
	}

	protected void onShutdownAfter(AppContext app, AppRequest request) {
		app.destroy();
	}

	protected void onShutdownDoing(AppContext app, AppRequest request) {
		app.shutdown();
	}

	protected void onShutdownBefore(AppContext app, AppRequest request) {
	}

}
