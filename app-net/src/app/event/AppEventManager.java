package app.event;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * 事件管理器
 * 
 * @author yiyongpeng
 * 
 */
public class AppEventManager {

	/** 发出事件 */
	public void fireEvent(AppEvent event) {
		int eventType = event.getType();
		if (listeners.containsKey(eventType)) {
			List<AppEventListener> list = listeners.get(eventType);
			for (int i = 0, size = list.size(); i < size; i++) {
				AppEventListener listener = list.get(i);
				listener.onAppEvent(event);
			}
		}
	}

	public void removeListener(int eventType, AppEventListener listener){
		if (listeners.containsKey(eventType)) {
			List<AppEventListener> list = listeners.get(eventType);
			list.remove(listener);
		}
	}
	
	public void addListener(int eventType, AppEventListener listener) {
		if (listeners.containsKey(eventType)) {
			List<AppEventListener> list = listeners.get(eventType);
			if (list.contains(listener) == false) {
				list.add(listener);
			}
		} else {
			List<AppEventListener> list = new ArrayList<AppEventListener>();
			list.add(listener);
			listeners.put(eventType, list);
		}
	}

	private TIntObjectMap<List<AppEventListener>> listeners = new TIntObjectHashMap<List<AppEventListener>>();

	private static AppEventManager instance = new AppEventManager();

	public static AppEventManager getInstance() {
		return instance;
	}

}
