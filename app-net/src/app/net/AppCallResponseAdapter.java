package app.net;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;

public abstract class AppCallResponseAdapter implements AppCallResponse {
	protected final Logger log = Logger.getLogger(getClass());

	@Override
	public void onFailed(int status, ByteBuffer msg) {
		log.warn("The server response failure.   status: " + status + "  msg:"
				+ msg);
	}

	@Override
	public boolean onTimeout(AppSession session, ByteBuffer msg, boolean sync,
			long timeout) {
		log.warn("Waiting for response timeout.  Session:" + session + "  msg:"
				+ msg + "  sync:" + sync + "   timeout:" + timeout);
		return onTimeout();
	}

	@Override
	public int getTimeout() {
		return 0;
	}

	protected boolean onTimeout() {
		return false;
	}

	@Override
	public void setFuture(ScheduledFuture<?> future) {
		this.future = future;
	}

	@Override
	public void setTimeoutTask(Object t) {
		this.task = t;
	}

	@Override
	public Object getTimeoutTask() {
		return task;
	}

	@Override
	public ScheduledFuture<?> getFuture() {
		return future;
	}

	private Object task;
	private ScheduledFuture<?> future;

}
