package app.net;

import java.nio.ByteBuffer;

import app.core.Connection;
import app.core.MessageOutput;
import app.core.Session;
import app.filter.FilterAdapter;
import app.filter.IProtocolDecodeFilter;
import app.filter.IProtocolEncodeFilter;
import app.filter.IFilterChain.FilterChain;
import app.util.ByteBufferUtils;
import app.util.ServerMode;

public class DefaultAppProtocolFilter extends FilterAdapter implements
		AppProtocFilter {
	private AppMessageFactory messageFactory;
	private int maxLenth = 2 * 1024 * 1024;// 2M

	public void setMaxLenth(int maxLenth) {
		this.maxLenth = maxLenth;
	}

	public int getMaxLenth() {
		return maxLenth;
	}

	@Override
	public void setMessageFactory(AppMessageFactory messageFactory) {
		this.messageFactory = messageFactory;
	}

	@Override
	public void messageEncode(Connection conn, Object message,
			MessageOutput out, FilterChain<IProtocolEncodeFilter> chain)
			throws Exception {
		Session session = conn.getSession();

		// session diy protocol
		IProtocolEncodeFilter protocol = (IProtocolEncodeFilter) session
				.getAttribute(Session.IO_PROTOCOL_ENCODE);
		if (protocol != null) {
			protocol.messageEncode(conn, message, out, chain);
			return;
		}
		// encode app-protocol
		if (message instanceof ByteBuffer) {
			ByteBuffer msg = (ByteBuffer) message;
			if (msg.capacity() - msg.limit() >= 4) {
				// 复用4个剩余空间字节
				ByteBufferUtils.offset(msg, 4);
				msg.limit(msg.limit() + 4);
				int length = msg.remaining();
				msg.putInt(msg.position(), length);// 头部
				message = msg;
			} else {
				int length = 4 + msg.remaining();
				ByteBuffer packet = ByteBufferUtils.create(length);
				packet.putInt(length).put(msg).flip();// 数据包长+数据包内容+校验长度
				message = packet;
			}
		}

		if (chain.hasNext()) {
			chain.nextFilter().messageEncode(conn, message, out,
					chain.getNext());
		} else if (message instanceof ByteBuffer) {
			out.putLast(message);
		} else {
			log.error("Miss send message: " + message);
		}
	}

	@Override
	public boolean messageDecode(Connection conn, ByteBuffer in,
			MessageOutput out, FilterChain<IProtocolDecodeFilter> chain) {
		// limit length
		if (in.remaining() >= maxLenth) {
			log.fatal(String.format("Receive buffer overflow: %s  -  %d bytes",
					conn, in.remaining()));
			in.clear();
			conn.close();
			return false;
		}
		// update active last time
		Session session = conn.getSession();
		if (session instanceof DefaultAppSession) {
			((DefaultAppSession) session).updateLastTime();
		}
		// session diy protocol
		IProtocolDecodeFilter protocol = (IProtocolDecodeFilter) session
				.getAttribute(Session.IO_PROTOCOL_DECODE);
		if (protocol != null) {
			return protocol.messageDecode(conn, in, out, chain);
		}
		// decode app-protocol
		boolean suc = false;
		for (; in.remaining() >= 4
				&& in.getInt(in.position()) <= in.remaining();) {

			// 消息解包
			int length = in.getInt();/* 消息长度 */
			if (length > 4) {
				suc = true;
				ByteBuffer msg = ByteBufferUtils.create(length - 4);/* 消息內容包 */
				in.get(msg.array());/* 读取包内容 */
				// Next Filter
				if (chain.hasNext() == false
						|| !chain.nextFilter().messageDecode(conn, msg, out,
								chain.getNext())) {
					out.putLast(createRequest((AppSession) session, msg));
					if (out.isFulled())
						return true;
				}
			} else if (length == 4) {
				suc = false;
				if (ServerMode.isDebug())
					log.debug(new StringBuilder(" << ping << ").append(conn));
				onPing(conn);
			} else {
				// other protocol, go back
				in.position(in.position() - 4);
				suc = false;
				break;
			}
		}
		if (suc == false) {
			if (chain.hasNext()) {
				return chain.nextFilter().messageDecode(conn, in, out,
						chain.getNext());
			}
		}
		return suc;
	}

	protected void onPing(Connection conn) {
	}

	private AppRequest createRequest(AppSession session, ByteBuffer msg) {
		return messageFactory.create(session, msg);
	}

	@Override
	public AppMessageFactory getMessageFactory() {
		return messageFactory;
	}
}
