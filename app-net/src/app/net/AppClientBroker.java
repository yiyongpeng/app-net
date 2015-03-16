package app.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import app.core.Connection;

/**
 * Proto客户端工具类
 * 
 * @author yiyongpeng
 * 
 */
public class AppClientBroker {
	private static final Logger log = Logger.getLogger(AppClientBroker.class);
	private static AppServer connector = new AppServer();

	private static class ClientConnectionFactory extends
			DefaultAppSessionFactory {
		@Override
		public DefaultAppSession create(Connection conn, Object sessionId) {
			return new ClientAppSession(conn, String.valueOf(sessionId));
		}
	}

	private static class ClientAppSession extends DefaultAppSession {
		private static final ByteBuffer empty = ByteBuffer.allocate(0);

		@Override
		protected boolean onTimeout() {
			log.debug(">> Ping >> " + this.getConnection());

			send(empty);// ping

			return false;
		}

		public ClientAppSession(Connection conn, String sessionId) {
			super(conn, sessionId);

			// 20s ping
			setAttribute(SESSION_KEPLIVE_TIMEOUT, 20000L);
		}

	}

	static {
//		protoConnector.start();
		connector.getHandler().setSessionFactory(
				new ClientConnectionFactory());
	}

	public static void startup() {
		connector.start();
	}

	public static void shutdown() {
		connector.stop();
	}

	public static AppHandler getHandler() {
		return connector.getHandler();
	}

	/**
	 * 创建Proto连接会话
	 * 
	 * @param address
	 *            地址
	 * @param port
	 *            端口
	 * @param retry
	 *            0无限尝试； >=1尝试次数
	 * @return
	 * @throws IOException
	 */
	public static AppSession createConnection(String address, int port,
			int retry) throws IOException {
		connector.start();

		// Search online Connection
		for (Connection conn : connector.getHandler().getConnections()) {
			if (conn.getRemoteAddress().equals(address)
					&& conn.getRemotePort() == port && conn.isClosed() == false) {
				log.debug("Find the Connection: " + address + ":" + port);
				return (AppSession) conn.getSession();
			} else {
				// log.debug(new StringBuilder("Search miss Connection: ")
				// .append(address).append(":").append(port)
				// .append("  >>  ").append(conn.getRemoteAddress())
				// .append(":").append(conn.getRemotePort())
				// .append("  closed:").append(conn.isClosed()));
			}
		}

		// Create new Connection
		AppSession session = null;
		IOException excep = null;
		InetSocketAddress inetAddress = new InetSocketAddress(address, port);
		for (int k = 0; (retry == 0 || k < retry); k++)
			try {
				System.out.println("Connect "+address+":"+port+"  ...");
				SocketChannel socketChannel = SocketChannel.open(inetAddress);
				connector.registor(socketChannel);

				for (int i = 0; (retry == 0 || i < retry)
						&& (session = connector.getHandle(socketChannel)) == null; i++) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (session != null)
					break;
			} catch (IOException e) {
				excep = e;
				log.warn("Connect " + inetAddress + " ...");
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}

		if (session == null) {
			throw excep;
		}
		return session;
	}

}
