package app.net;

import app.core.Session;

public interface AppSessionListener extends AppListener {

	boolean onSessionCreate(Session session);

	boolean onSessionScan(Session session, long time);

	boolean onSessionDestroy(Session session);

}
