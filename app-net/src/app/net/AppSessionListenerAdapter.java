package app.net;

import app.core.Session;

public class AppSessionListenerAdapter implements AppSessionListener {

	@Override
	public void initializeListener() {
	}

	@Override
	public void destroyListener() {
	}

	@Override
	public boolean onSessionCreate(Session session) {
		return true;
	}

	@Override
	public boolean onSessionScan(Session session, long time) {
		return true;
	}

	@Override
	public boolean onSessionDestroy(Session session) {
		return true;
	}

}
