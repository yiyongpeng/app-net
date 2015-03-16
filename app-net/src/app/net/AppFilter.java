package app.net;

public interface AppFilter {

	void doFilter(AppRequest request, AppResponse response,
			AppFilterChain filterChain) throws Exception;

	void init(AppHandler handler);

	void destroy();

}
