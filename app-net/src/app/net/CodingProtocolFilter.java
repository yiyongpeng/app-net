package app.net;

import java.nio.ByteBuffer;

import app.core.Connection;
import app.core.MessageOutput;
import app.core.Session;
import app.filter.FilterAdapter;
import app.filter.IProtocolDecodeFilter;
import app.filter.IProtocolEncodeFilter;
import app.filter.IFilterChain.FilterChain;
import app.net.AppHandler;
import app.net.AppRequest;
import app.net.AppSession;
import app.util.CodingKit;

/**
 * 消息加密编码过滤器
 * 
 * @author yiyongpeng
 * 
 */
public class CodingProtocolFilter extends FilterAdapter implements
		IProtocolDecodeFilter, IProtocolEncodeFilter {
	public static final String SESSION_ATTR_CODEKEY = "codekey";
	protected  byte[] codekey = "hotgame".getBytes();

	protected CodingKit codingKit;
	protected boolean client;

	public CodingProtocolFilter() {
		codingKit = new CodingKit();
	}
	
	public CodingProtocolFilter(boolean client) {
		this.client = client;
	}

	public void setClient(boolean client) {
		this.client = client;
	}

	@Override
	public void messageEncode(Connection conn, Object message,
			MessageOutput out, FilterChain<IProtocolEncodeFilter> chain)
			throws Exception {

		// coding message
		if (client) {
			if (message instanceof ByteBuffer) {
				ByteBuffer buffer = (ByteBuffer) message;
				Session session = conn.getSession();
				byte[] key = (byte[]) session.getCoverAttributeOfUser(
						SESSION_ATTR_CODEKEY, codekey);
				codingKit.coding(buffer, key);
				message = buffer;
			}
		}

		// Next filter
		if (chain.hasNext()) {
			chain.nextFilter().messageEncode(conn, message, out,
					chain.getNext());
		} else {
			// Can't at last filter
			throw new IllegalStateException();
		}
	}

	@Override
	public boolean messageDecode(Connection conn, ByteBuffer buffer,
			MessageOutput out, FilterChain<IProtocolDecodeFilter> chain) {

		// coding message
		if (!client) {
			Session session = conn.getSession();
			byte[] key = (byte[]) session.getCoverAttributeOfUser(
					SESSION_ATTR_CODEKEY, codekey);
			codingKit.coding(buffer, key);
		}

		if (chain.hasNext()) {
			// Next Filter
			return chain.nextFilter().messageDecode(conn, buffer, out,
					chain.getNext());
		} else if (conn.getSession() instanceof AppSession) {
			// Last filter, Create AppRequest
			AppSession session = (AppSession) conn.getSession();
			AppHandler handler = session.getServerHandler();
			AppRequest message = handler.getMessageFactory().create(session,
					buffer);
			out.putLast(message);
			return true;
		} else {
			// Last filter, Other message
			throw new IllegalStateException();
		}
	}
}
