package app.net;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;

/**
 * 
 * @author yiyongpeng
 * 
 */
public interface AppCallResponse {

	/**
	 * 
	 * @param status
	 * @param code
	 * @param msg
	 */
	void onFailed(int status, ByteBuffer msg);

	/**
	 * 
	 * @param msg
	 */
	void onSuccess(ByteBuffer msg);

	void setFuture(ScheduledFuture<?> future);

	ScheduledFuture<?> getFuture();

	/**
	 * 响应超时
	 * 
	 * @param mode
	 * @param timeout
	 * @param sync
	 * @param msg
	 * 
	 * @return 是否重发
	 */
	boolean onTimeout(AppSession session, ByteBuffer msg, boolean sync,
			long timeout);

	void setTimeoutTask(Object t);

	Object getTimeoutTask();

	/**
	 * 获取响应超时时间，0表示使用系统默认值，-1无限等待
	 * 
	 * @return
	 */
	int getTimeout();

}
