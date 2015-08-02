package app.net;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import app.core.AccessException;
import app.core.Connection;
import app.core.impl.DefaultSession;

public class DefaultAppSession extends DefaultSession implements AppSession {
	public static final byte HEAD_RESPONSE = 0;

	// private static final int CAPACITY_TIMEOUT_QUEUE = 50;

	@Override
	public String toString() {
		return new StringBuilder("(").append(getInetAddress()).append("@")
				.append(getSessionId()).append(")").toString();
	}

	@Override
	public int getSid() {
		int index = this.sessionId.lastIndexOf('@');
		if (index != -1) {
			return Integer.parseInt(this.sessionId.substring(index + 1));
		}
		return 0;
	}

	@Override
	public AppHandler getServerHandler() {
		return (AppHandler) super.getServerHandler();
	}

	@Override
	public String getSessionId(Integer sid) {
		return conn.getSession().getSessionId() + "@" + sid;
	}

	@Override
	public AppSession getSession(Integer sid) {
		return getServerHandler().getSession(getSessionId(sid));
	}

	@Override
	public boolean hasSessionId(Integer sid) {
		String sessionid = getSessionId(sid);
		return getServerHandler().hasSessionId(sessionid);
	}

	@Override
	public void closeSession(int sid) {
		DefaultAppSession session = (DefaultAppSession) getSession(sid);
		if (session != null) {
			session.close();
			// log.debug(conn.getSession().getInetAddress() + "(" +
			// getSessionId()
			// + ")  closeSession(" + sid + ")");
		}
	}

	@Override
	public void buildSession(int sid) {
		if (!hasSessionId(sid)) {
			createSession(getSessionId(sid));
		}
	}

	@Override
	public AppSession createSession(Integer sid) {
		if (sid == null || sid.intValue() == 0)
			throw new IllegalArgumentException(String.valueOf(sid));
		String sessionid = getSessionId(sid);
		AppSession newSession = createSession(sessionid);
		return newSession;
	}

	@Override
	public synchronized AppSession getOrCreateSession(Integer sid) {
		AppSession session = hasSessionId(sid) ? ((DefaultAppSession) getSession(sid))
				.onAccpeted() : createSession(sid);
		return session;
	}

	@Override
	public synchronized AppSession createSession(String sessionId) {
		AppSession session = getServerHandler().createSession(conn, sessionId);
		((DefaultAppSession) session).parent = this;
		return session;
	}

	@Override
	public AppSession getParent() {
		return parent;
	}

	public AppSession onAccpeted() {
		setClosed(false);
		updateLastTime();
		return this;
	}

	public void onClosed() {
		setClosed(true);
		clear4waiting();
	}

	public void clear4waiting() {
		if (respPool == null)
			return;
		if (respPool.size() > 0) {
			for (Entry<Integer, AppCallResponse> entry : respPool.entrySet()) {
				AppCallResponse resp = entry.getValue();
				synchronized (resp) {
					log.debug(String.format(
							"%s  Clear waiting call-response: (%s)%s",
							this.getInetAddress(), entry.getKey(), resp));
					resp.notify();
				}
			}
			respPool.clear();
		}
	}

	/**
	 * 
	 * @param time
	 * @return true 销毁断开连接的
	 */
	public boolean check4timeout(long time) {
		long timeout = getSessionTimeout();
		if (timeout > 0 && time - getLastTime() > timeout) {
			return onTimeout();
		}
		return false;
	}

	public long getSessionTimeout() {
		return (Long) getCoverAttributeOfUser(SESSION_KEPLIVE_TIMEOUT,
				DEFAULT_KEEPLIVE_TIMEOUT);
	}

	private long getResponseTimeout() {
		return (Long) getCoverAttributeOfUser(SESSION_RECV_TIMEOUT,
				DEFAULT_RECV_TIMEOUT);
	}

	protected boolean onTimeout() {
		if (!isClosed()) {
			updateLastTime();
			close();
			return false;
		} else {
			return true;
		}
	}

	private Object registorSync(int mode, AppCallResponse response) {
		registor(mode, response);
		return response;
	}

	private void registor(int mode, AppCallResponse response) {
		respPool.put(mode, response);
	}

	private AppCallResponse unregistorSync(int mode) {
		return unregistor(mode);
	}

	private AppCallResponse unregistor(int mode) {
		return respPool.remove(mode);
	}


	@Override
	public void send(Object message) {
		updateLastTime();
		super.send(message);
	}

	@Override
	public boolean send(Object message, AppCallResponse response) {
		return send(message, response, false);
	}

	@Override
	public boolean send(Object message, AppCallResponse response, boolean sync) {
		return send(message, response, sync, getResponseTimeout());
	}

	@Override
	public boolean send(Object message, AppCallResponse response, boolean sync,
			long timeout) {
		int mode = getMessageMode(message);
		ByteBuffer msg = toByteBuffer(message);
		if (mode == 0 || msg == null) {
			throw new AccessException("Message convert failt: mode=" + mode
					+ "  buffer=" + msg + ",  from message: " + message);
		}
		return send(mode, msg, response, sync, timeout);
	}

	protected ByteBuffer toByteBuffer(Object message) {
		IAppMessageConverter converter = (IAppMessageConverter) getCoverAttributeOfUser(
				SESSION_MESSAGE_CONVERTER, null);
		if (converter != null) {
			return converter.toByteBuffer(message);
		}
		return null;
	}

	protected int getMessageMode(Object message) {
		IAppMessageConverter converter = (IAppMessageConverter) getCoverAttributeOfUser(
				SESSION_MESSAGE_CONVERTER, null);
		if (converter != null) {
			return converter.toMessageMode(message);
		}
		return 0;
	}

	@Override
	public boolean send(int mode, ByteBuffer msg) {
		return send(mode, 0, msg);
	}

	@Override
	public boolean send(int mode, ByteBuffer msg, AppCallResponse response) {
		return send(mode, msg, response, false);
	}

	@Override
	public boolean send(int mode, ByteBuffer msg, AppCallResponse response,
			boolean sync) {
		return send(mode, msg, response, sync, getResponseTimeout());
	}

	@Override
	public boolean send(int mid, ByteBuffer msg, AppCallResponse response,
			boolean sync, long timeout) {
		if(getServerHandler()==null)
			return false;
		if (response.getTimeout() != 0){
			timeout = response.getTimeout();
			//System.err.println(this.getSessionId()+"  send for response-wait: "+timeout+", mid:"+mid);
		}
		int sendId = nextSendId();
		if (sync) {
			try {
				Object lock = registorSync(sendId, response);// 注册到响应等待池
				synchronized (lock) {
					do {
						if (send(mid, sendId, msg)) {
							if (timeout == -1) {
								// ========永久等待对方响应=======
								lock.wait();
								return true;
							} else {
								// ======按超时时间等待对方响应======
								long time = System.nanoTime();
								lock.wait(timeout);
								time = System.nanoTime() - time;
								if (time / 1000000 < timeout) {
									if (respPool.containsKey(sendId) == false) {
										return true;// 响应成功
									} else {
										throw new IllegalStateException(
												"Connection is closed");// 被其他线程中断了响应等待。
									}
								} else {
									// 等待同步响应超时，是否继续重发消息
									if (response.onTimeout(this, msg, true,
											timeout)) {
										log.warn(getInetAddress()
												+ " Retry send call message,  sid:"
												+ sendId + "  msg:" + msg);
										// 唤醒连接器，继续处理
										getServerHandler().getConnector()
												.wakeup();
									} else {
										break;// 停止继续重发消息
									}
								}
							}
						} else {
							break;
						}
					} while (true);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			unregistorSync(sendId);// 注销等待响应
			throw new AccessException(getConnection()
					+ ", Response timeout: "+timeout+", sendId: " + sendId);
		} else {
			registor(sendId, response);
			if (timeout != -1) {
				// 启用响应超时
				if (timeout < 100)
					timeout = 100;
				TimeoutTask t = newTimeoutTask(mid, sendId, msg, (int) timeout);
				response.setTimeoutTask(t);
				ScheduledFuture<?> future = getServerHandler().schedule(t,
						timeout);
				response.setFuture(future);
			}
			return send(mid, sendId, msg);
		}
	}

	public int nextSendId() {
		int respId;
		for(;(respId = respIdNext.incrementAndGet())==0;);
		return respId;
	}

	/**
	 * 处理到达的响应消息
	 */
	final void onResponse(AppMessage message) {
		int mode = message.getResponseId();
		AppCallResponse response = unregistor(mode);
		if (response == null) {
			log.warn(this.getInetAddress() + "@" + this.getSessionId()
					+ " Response is handle missed: " + message);
			return;
		}
		// 取消异步超时检测任务
		ScheduledFuture<?> s = response.getFuture();
		if (s != null) {
			response.setFuture(null);
			if (!s.cancel(false)) {
//				System.err.println("cancel the response timeout task.");
				return;
			} else {
				TimeoutTask t = (TimeoutTask) response.getTimeoutTask();
				if (t != null) {
					response.setTimeoutTask(null);
					t.finalize();
				}
			}
		}

		// 处理响应
		Object lock = response;
		synchronized (lock) {
			try {
				// if (getRespCallWaitingPool().remove(mode) == null) {
				// log.warn("Ignore the response of the timeout,  session: "
				// + this
				// + " mode: "
				// + mode
				// + "  resp: "
				// + response);
				// return;
				// }
				handleResponse(message, response);
			} finally {
				lock.notify();
			}
		}
	}

	private void handleResponse(AppMessage message, AppCallResponse response) {
		ByteBuffer msg = message.getByteBuffer();
		int status = msg.getShort();
		if (status == AppResponse.STATUS_SUCCESS) {
			response.onSuccess(msg);
		} else {
			response.onFailed(status, msg);
		}
	}

	public TimeoutTask newTimeoutTask(int mid, int sendId, ByteBuffer msg,
			int timeout) {
		TimeoutTask t = queueTimeoutTask.poll();
		if (t == null)
			t = new TimeoutTask();

		t.mid = mid;
		t.sendId = sendId;
		t.msg = msg;
		t.timeout = timeout;

		return t;
	}

	private final Queue<TimeoutTask> queueTimeoutTask = new LinkedBlockingQueue<TimeoutTask>();

	private class TimeoutTask implements Runnable {
		int mid;
		int sendId;
		ByteBuffer msg;
		int timeout;

		@Override
		public void run() {
			AppCallResponse resp = unregistor(sendId);
			if (!isClosed() && resp != null) {
				log.error(String.format(
						"Async-Timeout: sessionId: %s  CallResponse: (%s)%s ",
						getSessionId(), sendId, resp));
				// 异步响应超时
				if (resp.onTimeout(DefaultAppSession.this, msg, false, timeout)) {
					registor(sendId, resp);
					resp.setTimeoutTask(this);
					ScheduledFuture<?> future = getServerHandler().schedule(
							this, timeout);
					resp.setFuture(future);
					send(mid, sendId, msg);
					return;// 继续重发消息
				}
			}
			finalize();
		}

		@Override
		protected void finalize() {
			// 自动回收
			sendId = 0;
			msg = null;
			timeout = 0;
			queueTimeoutTask.offer(this);
		}
	}

	/**
	 * 发送消息(type(byte)[,mode(int)],data(byte[...]))
	 * 
	 * @param mid
	 *            请求消息编号
	 * @param sendId
	 *            消息序号（0非阻塞，其他阻塞）
	 * @param data
	 *            消息内容
	 * @return
	 */
	public boolean send(int mid, int sendId, ByteBuffer data) {
		AppMessageFactory f = getServerHandler().getMessageFactory();
		if (f == null) {
			throw new IllegalStateException("Not found message-factory.");
		}
		int pos = data.position();
		ByteBuffer packet = f.encode(this, mid, sendId, data);
		super.send(packet);
		data.position(pos);
		return true;
	}

	@Override
	public void updateLastTime() {
		lastTime = System.currentTimeMillis();
	}

	public long getLastTime() {
		return lastTime;
	}

	public DefaultAppHandler getHandler() {
		return (DefaultAppHandler) getServerHandler();
	}

	public DefaultAppSession(Connection conn, String sessionId) {
		this(sessionId);
		init(conn);
	}

	public DefaultAppSession(String sessionId) {
		super(sessionId);
	}

	/* begin members */

	private AppSession parent;
	private AtomicInteger respIdNext = new AtomicInteger();
	private volatile long lastTime;
	private Map<Integer, AppCallResponse> respPool = new ConcurrentHashMap<Integer, AppCallResponse>(16);

	/* end members */

	public static long DEFAULT_RECV_TIMEOUT = -1L;
	public static long DEFAULT_KEEPLIVE_TIMEOUT = 1200000L;

	private static final Logger log = Logger.getLogger(DefaultAppSession.class);

}
