package app.net;

import java.nio.ByteBuffer;

import app.core.Session;

public interface AppSession extends Session {
	/**
	 * Session自定义接收超时时间毫秒(Long)属性，-1表示永久。
	 */
	String SESSION_RECV_TIMEOUT = "__SESSION_RECV_TIMEOUT__";

	/**
	 * Session自定义保持在线超时时间毫秒(Long)属性，-1表示永久。
	 */
	String SESSION_KEPLIVE_TIMEOUT = "__SESSION_KEPLIVE_TIMEOUT__";

	/**
	 * Session消息序列化转换器
	 */
	String SESSION_MESSAGE_CONVERTER = "__MESSAGE_CONVERTER__";
	
	/**
	 * 发送异步消息
	 * 
	 * @param msg
	 * @return
	 */
	boolean send(int mode, ByteBuffer msg);

	/**
	 * 发送异步阻塞消息
	 * 
	 * @param msg
	 *            请求消息
	 * @param response
	 *            消息响应
	 */
	boolean send(Object message, AppCallResponse response);

	/**
	 * 发送同步阻塞消息
	 * 
	 * @param msg
	 *            请求消息
	 * @param response
	 *            消息响应
	 */
	boolean send(Object message, AppCallResponse response, boolean sync);

	/**
	 * 发送特定阻塞消息
	 * 
	 * @param msg
	 *            请求消息
	 * @param response
	 *            消息响应
	 */
	boolean send(Object message, AppCallResponse response, boolean sync,
			long timeout);

	/**
	 * 发送异步阻塞消息
	 * 
	 * @param msg
	 *            请求消息
	 * @param response
	 *            消息响应
	 */
	boolean send(int mode, ByteBuffer msg, AppCallResponse response);

	/**
	 * 发送阻塞消息,使用默认超时
	 * 
	 * @param msg
	 *            请求消息
	 * @param response
	 *            消息响应
	 * @param sync
	 *            是否同步
	 */
	boolean send(int mode, ByteBuffer msg, AppCallResponse response,
			boolean sync);

	/**
	 * 发送阻塞消息
	 * 
	 * @param msg
	 *            请求消息
	 * @param response
	 *            消息响应
	 * @param sync
	 *            是否同步
	 * @param timout
	 *            超时毫秒， -1表示无限等待
	 */
	boolean send(int mode, ByteBuffer msg, AppCallResponse response,
			boolean sync, long timout);

	/** 更新最后活动事件 */
	void updateLastTime();

	/** 创建自定义Session */
	AppSession createSession(String sessionId);

	/** 创建服务器同步Session */
	AppSession getSession(Integer sid);

	/** 创建服务器同步Session */
	AppSession createSession(Integer sid);

	AppSession getParent();

	/** 生成会话 */
	void buildSession(int sid);

	/** 删除会话 */
	void closeSession(int sid);

	/** 是否存在Session */
	boolean hasSessionId(Integer sid);

	/** 获取或创建会话 */
	AppSession getOrCreateSession(Integer sid);

	/** 获取非默认Session的Sid，0为默认Session */
	int getSid();

	/** 获取ChildSession的编号 */
	String getSessionId(Integer sid);
	
	/** 获取服务端处理器 */
	@Override
	AppHandler getServerHandler();
}
