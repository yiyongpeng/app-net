package app.util;

public interface IMethodHandler {

	void handleServiceMethod(int code, Object instance);

	void handleEventMethod(int eventType, Object instance);

}
