package app.net;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 
 * @author yiyongpeng
 * 
 */
public interface AppResponse {

	short STATUS_SUCCESS = 200, STATUS_PERMISSION_DENIED = 403,
			STATUS_NOTFOUND = 404, STATUS_INNER_ERROR = 500;

	/**
	 * 
	 * @param status
	 */
	void setStatus(short status);

	/**
	 * 
	 * @param msg
	 * @return
	 * @throws IOException
	 */
	void setData(ByteBuffer msg);

	/**
	 * 是否有效
	 * 
	 * @return
	 */
	boolean isValid();

	void flush();
}
