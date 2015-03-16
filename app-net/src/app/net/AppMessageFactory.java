package app.net;

import java.nio.ByteBuffer;

public interface AppMessageFactory {

	AppRequest create(AppSession session, ByteBuffer msg);

	ByteBuffer encode(AppSession session, int type, int mode, ByteBuffer data);

	void recycle(AppMessage message);

}
