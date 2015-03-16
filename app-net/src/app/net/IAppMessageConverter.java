package app.net;

import java.nio.ByteBuffer;

/**
 * 消息对象序列化转换器
 * 
 * @author yiyongpeng
 * 
 */
public interface IAppMessageConverter {

	ByteBuffer toByteBuffer(Object message);

	int toMessageMode(Object message);

}
