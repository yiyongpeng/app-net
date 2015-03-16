package app.util;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import app.base.IPlugin;
import app.event.AppEvent;
import app.event.AppEventListener;
import app.event.AppEventManager;
import app.event.OPEvent;

public class PluginProxyKit implements IPluginProxyKit, TIntObjectProcedure<List<AppEventListener>>{
	private static final Logger log = Logger.getLogger(PluginProxyKit.class);
	private ClassPool pool;
	
	public PluginProxyKit() {
		pool = getClassPool();
	}
	
	protected ClassPool getClassPool() {
		Object appPool = ThreadContext.contains()&&ThreadContext.contains("ClassPool")?ThreadContext.getAttribute("ClassPool"):null;
		ClassPool pool = appPool!=null&&(appPool instanceof ClassPool)?(ClassPool)appPool:ClassPool.getDefault();
		return pool;
	}

	public IPlugin createPlugin(String className) throws Exception {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Class<?> clazz = loader.loadClass(className);
		// Code => Methods
		Map<Integer, List<Method>> eventMethods = new HashMap<Integer, List<Method>>();
		for (Method method : clazz.getDeclaredMethods())
			try {
				// OPEvent Method
				OPEvent opEvent = method.getAnnotation(OPEvent.class);
				if (opEvent != null) {
					Class<?>[] params = method.getParameterTypes();
					if (params.length > 1
							|| (params.length == 1 && !AppEvent.class
									.isAssignableFrom(params[0]))) {
						throw new IllegalStateException(method
								+ "  Parameters Error!");
					}
					int code = opEvent.value();
					List<Method> methodList = eventMethods.get(code);
					if (methodList == null) {
						methodList = new ArrayList<Method>();
						eventMethods.put(code, methodList);
					}
					methodList.add(method);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		if (eventMethods.isEmpty())
			return (IPlugin) clazz.newInstance();

		// GenProxy
//		ClassClassPath cp = new ClassClassPath(clazz);
//		pool.insertClassPath(cp);
		CtClass oldClass = pool.get(clazz.getName());
		String proxyClassName = clazz.getName() + "$Proxy"+clazz.hashCode();
		proxyClassName = proxyClassName.replace("-", "_");
		CtClass newClass = null;
		try{
			clazz = loader.loadClass(proxyClassName);
		}catch (ClassNotFoundException e) {
			try{
				newClass = pool.get(proxyClassName);
			}catch (NotFoundException e1) {
				newClass = pool.makeClass(proxyClassName, oldClass);
				// AppEventListener Interface
				if (eventMethods.isEmpty() == false) {
					CtClass anInterface = pool
							.get(AppEventListener.class.getName());
					newClass.addInterface(anInterface);
					StringBuilder sb = new StringBuilder(
							"public void onAppEvent(app.event.AppEvent event) {\n");
					sb.append("  int code = event.getType();\n");
					sb.append("  switch (code) {\n");
					for (Entry<Integer, List<Method>> entry : eventMethods
							.entrySet()) {
						int code = entry.getKey();
						List<Method> methods = entry.getValue();
						sb.append(String.format("    case %d :\n", code));
						for (Method method : methods) {
							log.debug(String.format("[%d]%s", code, method));
	
							Class<?>[] paramsClass = method.getParameterTypes();
							String invokeCode = "";
	
							if (paramsClass.length == 1)
								invokeCode = String.format("      %s(%s event);\n",
										method.getName(),
										paramsClass[0] == AppEvent.class ? "" : "("
												+ paramsClass[0].getName() + ")");
							else
								invokeCode = String.format("      %s();\n",
										method.getName());
	
							sb.append(invokeCode);
						}
						sb.append("      break;\n");
					}
					sb.append("  }\n");
					sb.append("}\n");
					log.debug(new StringBuilder("[Proxy] ").append(clazz.getName()).append("(pool-loader:").append(pool.getClass().getClassLoader()).append(",class-loader:").append(loader)
							.append(")\n").append(sb));
	
					newClass.addMethod(CtMethod.make(sb.toString(), newClass));
					sb.setLength(0);
				}
			}
			clazz = newClass.toClass(loader, null);
		}

		Object instance = clazz.newInstance();
//		pool.removeClassPath(cp);
		// OPEvent Instance
		for (Integer eventType : eventMethods.keySet()) {
			AppEventListener listener = (AppEventListener) instance;
			AppEventManager.getInstance().addListener(eventType,
					listener);
			registListener(eventType, listener);
			log.debug("[Plugin] addListener: " + eventType
					+ "  class:" + instance.getClass().getName());
		}
		return (IPlugin) instance;
	}
	
	private void registListener(int eventType, AppEventListener listener) {
		List<AppEventListener> list = null;
		if(listeners.containsKey(eventType)){
			list = listeners.get(eventType);
		}else{
			list = new ArrayList<AppEventListener>();
			listeners.put(eventType, list);
		}
		list.add(listener);
	}

	public void init(){
		listeners = new TIntObjectHashMap<List<AppEventListener>>();
	}
	
	@Override
	public boolean execute(int eventType, List<AppEventListener> list) {
		for (AppEventListener listener : list) {
			AppEventManager.getInstance().removeListener(eventType, listener);
		}
		return true;
	}
	
	public void destroy(){
		listeners.forEachEntry(this);
		listeners.clear();
		listeners = null;
	}
	
	private TIntObjectMap<List<AppEventListener>> listeners;
}
