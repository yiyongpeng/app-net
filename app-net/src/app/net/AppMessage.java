package app.net;

import java.nio.ByteBuffer;

import app.core.impl.DefaultContext;
import app.util.ByteBufferUtils;

public class AppMessage extends DefaultContext implements AppRequest,
		AppResponse {

	private DefaultAppSession session;

	protected int mode;
	private int responseId;

	private short status;

	protected ByteBuffer requestData;
	private ByteBuffer responseData;

	@Override
	public String toString() {
		return new StringBuilder("{").append(" mode=").append(mode)
				.append("  responseId=").append(responseId).append("  data=")
				.append(requestData).append(" }").toString();
	}

	public void init(DefaultAppSession session) {
		this.status = STATUS_SUCCESS;
		this.session = session;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public void setByteBuffer(ByteBuffer data) {
		this.requestData = data;
	}

	@Override
	public DefaultAppSession getSession() {
		return session;
	}

	@Override
	public String getRemoteAddress() {
		return session.getRemoteAddress();
	}

	@Override
	public int getRemotePort() {
		return session.getRemotePort();
	}

	@Override
	public ByteBuffer getByteBuffer() {
		return requestData;
	}

	public boolean isResponse() {
		return mode == DefaultAppSession.HEAD_RESPONSE;
	}

	public int length() {
		return requestData != null ? requestData.remaining() : 0;
	}

	@Override
	public boolean isValid() {
		return responseId != 0;
	}

	@Override
	public int getMode() {
		return mode;
	}

	@Override
	public void setStatus(short status) {
		this.status = status;
	}

	@Override
	public void setData(ByteBuffer msg) {
		this.responseData = msg;
	}

	/**
	 * 发送响应消息
	 */
	@Override
	public void flush() {
		if (responseId == 0)
			return;

		boolean has = responseData != null;

		if (has && responseData.capacity() - responseData.limit() >= 2) {
			ByteBufferUtils.offset(responseData, 2);
			responseData.limit(responseData.limit() + 2);
			responseData.putShort(0, status);
		} else {
			ByteBuffer resp = ByteBufferUtils.create(2 + (has ? responseData
					.remaining() : 0));
			resp.putShort(status);// 响应状态
			if (has) {
				resp.put(responseData);// 响应数据
			}
			resp.flip();
			responseData = resp;
		}

		session.send(DefaultAppSession.HEAD_RESPONSE, responseId, responseData);

		responseId = 0;
		responseData = null;
	}

	public void destory() {
		AppMessageFactory f = null;
		if (session != null)
			f = session.getServerHandler().getMessageFactory();

		this.mode = 0;
		this.responseId = 0;
		this.status = 0;
		this.session = null;
		this.requestData = null;
		this.clear();

		if (f != null)
			f.recycle(this);
	}

	public void setResponseId(int responseId) {
		this.responseId = responseId;
	}

	public int getResponseId() {
		return responseId;
	}

	protected AppMessage() {
	}
}
