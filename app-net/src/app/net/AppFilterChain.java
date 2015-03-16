package app.net;

public interface AppFilterChain {

	void doFilter(AppRequest request, AppResponse response) throws Exception;

}
