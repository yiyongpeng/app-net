package app.net;

import app.core.Connection;
import app.core.impl.DefaultSessionFactory;

public class DefaultAppSessionFactory extends DefaultSessionFactory {

	@Override
	public DefaultAppSession create(Connection conn, Object sessionId) {
		return new DefaultAppSession(conn, String.valueOf(sessionId));
	}

}
