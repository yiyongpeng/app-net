package app.net;

public interface AppServlet {

	void service(AppRequest request, AppResponse response)
			throws ServletException;

	void init(AppHandler handler);

	void destroy();

}
