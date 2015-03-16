package app.net;

import app.core.Connection;
import app.core.Connector;
import app.core.ServerContext;
import app.core.Session;

public interface AppHandler extends ServerContext {
	/** 创建新的Session */
	@Override
	AppSession createSession(Connection conn, Object sid);

	/** 获取指定Session */
	@Override
	AppSession getSession(String sessionId);

	/** 注销服务过滤器 */
	boolean removeFilter(AppFilter filter);

	/** 添加过滤器到第一个 */
	void addFirstFilter(AppFilter filter);

	/** 注册服务过滤器 */
	boolean addLastFilter(AppFilter filter);

	@Override
	public Connector<Connection, Session> getConnector();

	AppMessageFactory getMessageFactory();

	void setMessageFactory(AppMessageFactory factory);

	boolean addListener(AppListener listener);

	boolean removeListener(AppListener listener);

	void setDispatcher(AppRequestDispatcher disptcher);

	AppRequestDispatcher getDispatcher();

	void setMessageFlow(boolean messageFlow);

	boolean isMessageFlow();
}
