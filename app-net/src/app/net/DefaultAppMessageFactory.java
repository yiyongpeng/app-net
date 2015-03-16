package app.net;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import app.util.ByteBufferUtils;

public class DefaultAppMessageFactory implements AppMessageFactory {
	private static final int CAPACITY = 50;

	private BlockingQueue<AppMessage> queue = new ArrayBlockingQueue<AppMessage>(
			CAPACITY);

	@Override
	public void recycle(AppMessage message) {
		queue.offer(message);// 回收资源
	}

	public AppMessage newInstance() {
		AppMessage instance = popInstance();
		if (instance == null) {
			instance = newMessage();
		}
		return instance;
	}

	protected AppMessage newMessage() {
		return new AppMessage();
	}

	public AppMessage popInstance() {
		return queue.poll();
	}

	@Override
	public AppRequest create(AppSession session, ByteBuffer packet) {
		AppMessage message = newInstance();
		message.init((DefaultAppSession) session);

		// Read Message from ByteBuffer
		message.setMode(packet.getShort());
		message.setResponseId(packet.getInt());
		message.setByteBuffer(packet.asReadOnlyBuffer());

		return message;
	}

	@Override
	public ByteBuffer encode(AppSession session, int mid, int sendId,
			ByteBuffer msg) {
		ByteBuffer packet = onSendBefore(session, msg);

		packet.putShort((short) mid); // ID
		packet.putInt(sendId); // sendId
		onSendAfter(session, packet, msg);// Data

		packet.flip();
		return packet;
	}

	protected void onSendAfter(AppSession session, ByteBuffer packet,
			ByteBuffer msg) {
		packet.put(msg);// 数据内容
	}

	protected ByteBuffer onSendBefore(AppSession session, ByteBuffer data) {
		int length = 6 + data.remaining();// ID + SendId + Data
		data = ByteBufferUtils.create(length);
		return data;
	}
}
