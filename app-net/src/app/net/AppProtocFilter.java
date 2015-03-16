package app.net;

import app.filter.IProtocolDecodeFilter;
import app.filter.IProtocolEncodeFilter;

public interface AppProtocFilter extends IProtocolDecodeFilter,
		IProtocolEncodeFilter {

	void setMessageFactory(AppMessageFactory factory);

	AppMessageFactory getMessageFactory();

}
