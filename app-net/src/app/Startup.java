package app;

import app.base.AppContext;
import app.base.FileSystemXmlAppContext;

public class Startup {
	
	public static void main(String[] args) throws Exception {
		String manifestFileName = System.getProperty("Manifest", System.getProperty("config","./")+"Manifest.xml");
		
		final AppContext context = FileSystemXmlAppContext.getDefault();
		context.init(manifestFileName);
		context.startup();
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				context.shutdown();
				context.destroy();
			}
		});
	}

}
