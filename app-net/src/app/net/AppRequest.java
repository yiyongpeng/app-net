package app.net;

import java.nio.ByteBuffer;

import app.core.Context;

/**
 * 
 * @author yiyongpeng
 * 
 */
public interface AppRequest extends Context {

	AppSession getSession();

	String getRemoteAddress();

	int getRemotePort();

	ByteBuffer getByteBuffer();

	int getMode();
}
