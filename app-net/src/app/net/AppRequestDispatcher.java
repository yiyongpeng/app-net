package app.net;

public interface AppRequestDispatcher {
	void init(AppHandler handler);

	boolean hasServlet(int mode);

	void addServlet(int mode, AppServlet servlet);

	AppServlet addServlet(String mode, String servletClass)	throws ClassNotFoundException;

	AppServlet removeServlet(int mode);

	void dispatch(AppRequest request, AppResponse response) throws Exception;

	void destroy();

}
