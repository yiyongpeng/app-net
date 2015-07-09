package app.net;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledFuture;

import app.core.AccessException;
import app.core.Connection;
import app.core.Connector;
import app.core.ServerContext;
import app.core.Session;
import app.core.SessionFactory;
import app.core.impl.DefaultConnection;
import app.core.impl.DefaultServerHandler;
import app.core.impl.DefaultSession;
import app.filter.IAcceptFilter;
import app.filter.IAcceptorFilter;
import app.filter.IClosedFilter;
import app.filter.IErrFilter;
import app.filter.IFilterChain;
import app.filter.IMessageFilter;
import app.filter.IFilterChain.FilterChain;
import app.filter.IFilterChain.INextFilter;
import app.filter.IFilterChain.IPrevFilter;
import app.util.ServerMode;

public class DefaultAppHandler extends DefaultServerHandler implements
		AppHandler, IAcceptFilter, IAcceptorFilter, IMessageFilter,
		IClosedFilter, IErrFilter, AppSessionListener {

	private static final int CACHE_TASK_MAX = 1024;

	private List<AppSessionListener> sessionListeners;

	private AppProtocFilter protocFilter;
	private ScheduledFuture<?> checkFuture;

	public AppRequestDispatcher dispatcher;

	public DefaultAppHandler() {
		sessionListeners = new ArrayList<AppSessionListener>();
		dispatcher = new DefaultAppRequestDispatcher();
		setProtocFilter(instanceProtocolFilter());
	}

	@Override
	public void init(Connector<Connection, Session> connector) {
		super.init(connector);

		IFilterChain chain = getFilterChain();
		chain.addLastFilter(IFilterChain.FILTER_ACCEPT, this);
		chain.addLastFilter(IFilterChain.FILTER_ACCEPTOR, this);
		chain.addLastFilter(IFilterChain.FILTER_MESSAGE, this);
		chain.addLastFilter(IFilterChain.FILTER_CLOSED, this);
		chain.addLastFilter(IFilterChain.FILTER_ERROR, this);

		chain.addFirstFilter(IFilterChain.FILTER_PROTOCOL_ENCODE, protocFilter);
		chain.addLastFilter(IFilterChain.FILTER_PROTOCOL_DECODE, protocFilter);

		dispatcher.init(this);

		int length = filters.size();
		for (int i = 0; i < length; i++) {
			filters.get(i).init(this);
		}

		addListener(this);

		for (int i = 0, size = sessionListeners.size(); i < size; i++) {
			sessionListeners.get(i).initializeListener();
		}

		startKeepLiveChecker();

	}

	@Override
	public void destory() {

		stopKeepLiveChecker();

		for (int i = 0, size = sessionListeners.size(); i < size; i++) {
			sessionListeners.get(i).destroyListener();
		}

		removeListener(this);

		int length = filters.size();
		for (int i = 0; i < length; i++) {
			filters.get(i).destroy();
		}

		dispatcher.destroy();

		IFilterChain chain = getFilterChain();
		chain.removeFilter(IFilterChain.FILTER_ACCEPT, this);
		chain.removeFilter(IFilterChain.FILTER_ACCEPTOR, this);
		chain.removeFilter(IFilterChain.FILTER_MESSAGE, this);
		chain.removeFilter(IFilterChain.FILTER_CLOSED, this);
		chain.removeFilter(IFilterChain.FILTER_ERROR, this);
		chain.removeFilter(IFilterChain.FILTER_PROTOCOL_ENCODE, protocFilter);
		chain.removeFilter(IFilterChain.FILTER_PROTOCOL_DECODE, protocFilter);

		super.destory();
	}

	private void stopKeepLiveChecker() {
		if (checkFuture != null) {
			checkFuture.cancel(false);
			checkFuture = null;
		}
	}

	/** 注册响应超时检查周期任务 **/
	protected void startKeepLiveChecker() {
		stopKeepLiveChecker();
		checkFuture = schedule(new CheckKeepLiveTask(), 1000, 1000);
	}

	private class CheckKeepLiveTask implements Runnable {
		@Override
		public void run() {
			long time = System.currentTimeMillis();
			// for (Connection conn : connections.values())
			// ((DefaultMsgSession) conn.getSession()).check4timeout(time);
			List<Session> list = new ArrayList<Session>(getSessionAll());
			Session session = null;
			int size = sessionListeners.size();
			for (int i = 0, count = list.size(); i < count; i++)
				try {
					// String key = entry.getKey();
					session = list.get(i);
					for (int j = 0; j < size; j++)
						if (!sessionListeners.get(j).onSessionScan(session,
								time))
							break;

				} catch (Throwable e) {
					getNotifier().fireOnError(session, e);
				}
		}
	}

	public DefaultAppSession getHandle(ByteChannel sc) {
		Connection conn = getConnection(sc);
		if (conn != null)
			return (DefaultAppSession) conn.getSession();
		return null;
	}

	@Override
	public void serverAccept(ServerContext serverHandler,
			FilterChain<IAcceptFilter> filterChain) throws Exception {
		if (filterChain.hasNext()) {
			filterChain.nextFilter().serverAccept(serverHandler,
					filterChain.getNext());
		}
	}

	@Override
	public Connection sessionAccept(ServerContext serverHandler,
			SelectableChannel socket, FilterChain<IAcceptorFilter> filterChain)
			throws Exception {
		Connection conn = null;
		if (filterChain.hasNext()) {
			conn = filterChain.nextFilter().sessionAccept(serverHandler,
					socket, filterChain.getNext());
			if (conn != null
					&& !(conn.getSession() instanceof DefaultAppSession)) {
				return conn;
			}
		}

		Session session = null;
		if (conn == null) {
			conn = createConnection(socket);
			session = createSession(conn, conn.hashCode());
			conn.setSession(session);// 设置连接的默认Session
		} else {
			session = conn.getSession();
		}

		log.debug(new StringBuilder("Connected: ").append(session)
				.append("  =>  ").append(getConnector()).toString());

		return session.getConnection();
	}

	protected Connection createConnection(SelectableChannel sc) {
		if (sc instanceof ByteChannel) {
			DefaultConnection conn = new DefaultConnection();
			conn.init((ByteChannel) sc);
			return conn;
		}
		throw new IllegalArgumentException("Unsupport Channel:" + sc);
	}

	@Override
	public Connection sessionOpened(ServerContext serverHandler,
			Connection conn, FilterChain<IAcceptorFilter> filterChain)
			throws Exception {
		if (conn == null)
			return null;

		onSessionOpened(conn.getSession());

		if (filterChain.hasNext()) {
			conn = filterChain.nextFilter().sessionOpened(serverHandler, conn,
					filterChain.getNext());
		}
		return conn;
	}

	@Override
	protected void onSessionOpened(Session session) {
		if (session instanceof DefaultAppSession)
			synchronized (session) {
				DefaultAppSession s = (DefaultAppSession) session;
				s.init(this);
				s.onAccpeted();
			}
		super.onSessionOpened(session);
	}

	@Override
	public void messageReceived(Session session0, Object message0,
			FilterChain<IMessageFilter> chain) {
		if (session0 instanceof DefaultAppSession
				&& message0 instanceof AppMessage) {
			AppMessage message = (AppMessage) message0;
			DefaultAppSession session = message.getSession();
			if (message.isResponse()) {
				// ==========响应消息到达 Begin========
				try {
					session.onResponse(message);
				} catch (Throwable e) {
					getNotifier().fireOnError(session, e);
				} finally {
					message.destory();// 销毁响应消息
				}
				// ==========响应消息到达 End========
			} else {
				// ==========请求消息到达 Begin========
				try {
					takeFilterChain().doFilter(message, message);// 过滤请求消息
				} catch (Throwable e) {
					message.setStatus(AppResponse.STATUS_INNER_ERROR);// 内部未知错误
					getNotifier().fireOnError(session, e);
				} finally {
					message.flush();// 发送响应消息
					message.destory();// 销毁请求消息
				}
				// ==========请求消息到达 End========
			}
		} else if (chain.hasNext()) {
			chain.nextFilter().messageReceived(session0, message0,
					chain.getNext());
		}
	}

	@Override
	public void serverExcept(Session session, Throwable e,
			FilterChain<IErrFilter> filterChain) {
		// StackTraceElement[] stack = e.getStackTrace();
		// if (stack.length < 4
		// || (!stack[stack.length - 4].getClassName().startsWith(
		// DefaultMessageReader.class.getCanonicalName()) && !stack[stack.length
		// - 4]
		// .getClassName().startsWith(
		// DefaultMessageWriter.class.getCanonicalName()))) {
		e.printStackTrace();
		// }

		if (filterChain.hasNext()) {
			filterChain.nextFilter().serverExcept(session, e,
					filterChain.getNext());
		}

		if (e instanceof IOException && session != null && session.isDefault()
				&& session.getConnection().isClosed() == false) {
			session.getConnection().close();
		}
	}

	@Override
	public void sessionClosed(Connection conn,
			FilterChain<IClosedFilter> filterChain) {
		Session session = conn.getSession();

		if (ServerMode.isDebug()) {
			log.debug("Closed: " + conn + (session.getMessageInputQueue().size() > 0 ? "  msg-in: " + session.getMessageInputQueue().size() : "")
					+ (session.getMessageOutputQueue().size() > 0 ? "  msg-out: " + session.getMessageOutputQueue().size() : ""));
		}
		if (filterChain.hasNext()) {
			filterChain.nextFilter().sessionClosed(conn, filterChain.getNext());
		}

		if (session instanceof DefaultAppSession) {
			DefaultAppSession session0 = (DefaultAppSession) session;
			session0.onClosed();
		}

		if (conn instanceof DefaultConnection)
			((DefaultConnection) conn).onClosed();
	}

	private AppFilterChain takeFilterChain() {
		app.net.AppFilterChain chain = recycle4filter.poll();
		if (chain == null)
			chain = new FilterChainCache();
		return chain;
	}

	private class FilterChainCache implements app.net.AppFilterChain {
		private int pos;

		@Override
		public void doFilter(AppRequest request, AppResponse response)
				throws Exception {
			if (pos < filters.size()) {
				// Filter
				filters.get(pos++).doFilter(request, response, this);
				pos--;// 回溯
			} else if (dispatcher != null) {
				// Dispatch Request
				dispatcher.dispatch(request, response);
			}
			if (pos == 0) {
				// Recycle
				recycle4filter.offer(this);
			}
		}
	}

	@Override
	public AppSession createSession(Connection conn, Object sessionId) {
		AppSession session = (AppSession) super.createSession(conn, sessionId);
		for (int i = 0, size = sessionListeners.size(); i < size; i++) {
			if (!sessionListeners.get(i).onSessionCreate(session))
				throw new AccessException(sessionListeners.get(i)
						+ " SessionListener not allow sreate Session: "
						+ sessionId + "  Connection: " + conn.getInetAddress());
		}
		return session;
	}

	@Override
	public AppSession removeSession(String sessionId) {
		AppSession session = getSession(sessionId);
		if (session == null)
			return null;
		for (int i = 0, size = sessionListeners.size(); i < size; i++) {
			Connection conn = session.getConnection();
			if (!sessionListeners.get(i).onSessionDestroy(session))
				throw new AccessException(sessionListeners.get(i)
						+ " SessionListener not allow close Session: "
						+ sessionId + "  Connection: " + conn.getInetAddress());
		}
		super.removeSession(sessionId);
		return session;
	}

	@Override
	public AppSession getSession(String sessionId) {
		return (AppSession) super.getSession(sessionId);
	}

	@Override
	public boolean removeFilter(AppFilter filter) {
		log.debug(String.format("Remove filter:(%d) %s",
				filters.indexOf(filter), filter));
		return filters.remove(filter);
	}

	@Override
	public boolean addLastFilter(AppFilter filter) {
		log.debug(String.format("Add last-filter:(%d) %s", filters.size(),
				filter));
		return filters.add(filter);
	}

	@Override
	public void addFirstFilter(AppFilter filter) {
		log.debug(String.format("Add first-filter:(%d) %s", filters.size(),
				filter));
		filters.add(0, filter);
	}

	/** 过滤器列表 */
	private List<AppFilter> filters = new ArrayList<AppFilter>();
	private final Queue<app.net.AppFilterChain> recycle4filter = new ArrayBlockingQueue<app.net.AppFilterChain>(
			CACHE_TASK_MAX);

	@Override
	public void onAdded(IFilterChain filterChain, IPrevFilter prevFilter) {
	}

	@Override
	public void onRemoved(IFilterChain filterChain, IPrevFilter prevFilter) {
	}

	@Override
	public boolean onPrevFilterAdd(IFilterChain filterChain,
			IPrevFilter prevFilter) {
		return true;
	}

	@Override
	public boolean onNextFilterAdd(IFilterChain filterChain,
			INextFilter nextFilter) {
		return true;
	}

	@Override
	public boolean onPrevFilterRemove(IFilterChain filterChain,
			IPrevFilter prevFilter) {
		return true;
	}

	@Override
	public boolean onNextFilterRemove(IFilterChain filterChain,
			INextFilter nextFilter) {
		return true;
	}

	@Override
	public void initializeListener() {
	}

	@Override
	public boolean onSessionCreate(Session session) {
		return true;
	}

	@Override
	public boolean onSessionScan(Session session, long time) {
		// log.debug("Scan Session:" + session.getSessionId());
		if (session instanceof DefaultAppSession)
			if (((DefaultAppSession) session).check4timeout(time))
				try {
					log.debug("Session idle timeout : " + session);
					removeSession(session.getSessionId());
					((DefaultSession)session).destory();
				} catch (Throwable e) {
					getNotifier().fireOnError(session, e);
				}
		return true;
	}

	@Override
	public boolean onSessionDestroy(Session session) {
		return true;
	}

	@Override
	public void destroyListener() {
	}

	public AppMessageFactory instanceMessageFactory() {
		return new DefaultAppMessageFactory();
	}

	public AppProtocFilter instanceProtocolFilter() {
		return new DefaultAppProtocolFilter();
	}

	@Override
	public SessionFactory instanceSessionFactory() {
		return new DefaultAppSessionFactory();
	}

	public void setProtocFilter(AppProtocFilter protocFilter) {
		if (protocFilter.getMessageFactory() == null)
			protocFilter.setMessageFactory(instanceMessageFactory());
		this.protocFilter = protocFilter;
	}

	public AppProtocFilter getProtocFilter() {
		return protocFilter;
	}

	@Override
	public AppMessageFactory getMessageFactory() {
		return protocFilter.getMessageFactory();
	}

	@Override
	public void setMessageFactory(AppMessageFactory factory) {
		this.protocFilter.setMessageFactory(factory);
	}

	@Override
	public boolean addListener(AppListener listener) {
		if (listener instanceof AppSessionListener) {
			return sessionListeners.add((AppSessionListener) listener);
		}
		return false;
	}

	@Override
	public AppRequestDispatcher getDispatcher() {
		return dispatcher;
	}

	@Override
	public void setDispatcher(AppRequestDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	public boolean removeListener(AppListener listener) {
		return sessionListeners.remove(listener);
	}
}
