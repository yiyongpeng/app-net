package app.util;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

/**
 * 编码解码工具
 * 
 * @author yiyongpeng
 * 
 */
public class CodingKit {

	protected final Logger log = Logger.getLogger(this.getClass());

	/** 用指定的字符串进行编码 */
	public void coding(byte[] bytes, int pos, int len, String code) {
		byte[] bytesCode = code.getBytes();
		coding(bytes, pos, len, bytesCode, 0);
	}

	/** 用指定的字节数组进行编码 */
	public void coding(byte[] bytes, byte[] code) {
		coding(bytes, 0, bytes.length, code, 0);
	}

	/** 用指定的字节数组的偏移位置进行编码 */
	public void coding(byte[] bytes, byte[] code, int offset) {
		coding(bytes, 0, bytes.length, code, offset);
	}

	/**
	 * 指定位置和长度的部分， 用指定的字节数组进行滚动编码
	 */
	public void coding(byte[] bytes, int pos, int len, byte[] code) {
		coding(bytes, pos, len, code, 0);
	}

	/**
	 * 指定位置和长度的部分， 用指定的字节数组的偏移位置进行滚动编码
	 */
	public void coding(byte[] bytes, int pos, int len, byte[] code, int offset) {
		if (pos < 0 || pos >= bytes.length)
			return;
		if (offset < 0 || offset >= code.length)
			return;
		if (len <= 0)
			return;
		if (pos + len > bytes.length)
			len = bytes.length - pos;
		int l = code.length - offset;
		int t = (len < l) ? offset + len : code.length;
		for (int j = offset; j < t; j++)
			bytes[pos++] ^= code[j];
		len -= l;
		if (len <= 0)
			return;
		int c = len / code.length;
		for (int i = 0; i < c; i++) {
			for (int j = 0; j < code.length; j++)
				bytes[pos++] ^= code[j];
		}
		l = len % code.length;
		for (int j = 0; j < l; j++)
			bytes[pos++] ^= code[j];
	}

	/**
	 * 用指定字节数组滚动编码一个ByteBuffer
	 * 
	 * @param buffer
	 * @param codekey
	 */
	public void coding(ByteBuffer buffer, byte[] codekey) {
		byte[] bytes = buffer.array();
		int pos = buffer.position();
		int len = buffer.limit() - buffer.position();
		coding(bytes, pos, len, codekey);
	}

}
