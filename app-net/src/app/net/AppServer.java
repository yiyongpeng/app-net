package app.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import app.core.AccessException;
import app.core.Connection;
import app.core.Connector;
import app.core.Session;
import app.core.impl.DefaultConnector;
import app.core.impl.DefaultMessageReader;
import app.core.impl.DefaultMessageWriter;
import app.core.impl.DefaultNotifier;
import app.util.AppThreadFactory;

public class AppServer extends DefaultConnector<Connection, Session> {
	protected final Logger log = Logger.getLogger(getClass());

	protected DefaultAppHandler handler;

	protected String hostname = "0.0.0.0";
	protected int port = 0;

	private int executorPoolSize;
	private int readerPoolSize;
	private int writerPoolSize;

	private ServerSocketChannel ssc;

	@Override
	public String toString() {
		return getPort() > 0 ? String.format("{%s  %s:%d}", getName(),
				getHostname(), getPort()) : String.format("{ %s }", getName());
	}

	public AppServer() {
		this(new DefaultAppHandler());
	}

	public AppServer(DefaultAppHandler handler) {
		this.handler = handler;
		this.notifier = new DefaultNotifier<Connection, Session>();
		this.notifier.addHandler(handler);
	}

	public AppServer(DefaultAppHandler handler, int executorPoolSize, int readerPoolSize, int writerPoolSize) throws IOException{
		this(null, null, null);

		setHandler(handler);
		
		this.executorPoolSize = executorPoolSize;
		this.readerPoolSize = readerPoolSize;
		this.writerPoolSize = writerPoolSize;
		
	}
	
	public AppServer(ExecutorService executor, Executor reader, Executor writer)
			throws IOException {
		super(executor, new DefaultNotifier<Connection, Session>(),
				new DefaultMessageReader<Connection, Session>(reader),
				new DefaultMessageWriter<Connection, Session>(writer));
		this.handler = new DefaultAppHandler();
		this.notifier = new DefaultNotifier<Connection, Session>();
		this.notifier.addHandler(this.handler);
	}

	public ExecutorService newFixedThreadPool(final Connector<Connection, Session> connector, final String name, int size) {
		if(size>0){
			return Executors.newFixedThreadPool(size, new AppThreadFactory(){
				public String getNamePrefix() {
					return connector.getName()+"-"+name+"-";
				}
			});
		}else{
			return newCachedThreadPool(connector, name);
		}
	}
	
	public void setHandler(DefaultAppHandler handler) {
		if (isRuning())
			throw new IllegalStateException("The Server is Runing.");
		if (this.handler != null)
			this.notifier.removeHandler(this.handler);
		this.notifier.addHandler(handler);
		this.handler = handler;
	}

	public DefaultAppSession getHandle(ByteChannel sc) {
		return handler.getHandle(sc);
	}

	public DefaultAppHandler getHandler() {
		return this.handler;
	}

	public void registor2wait(ByteChannel sc, Connection conn) {
		registor((SelectableChannel) sc, conn);
		for (int i = 0; i < 1000 && getHandle(sc) == null; i++)
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	public void listen(String host, int port) throws IOException {
		if (port == 0)
			return;
		InetSocketAddress address = new InetSocketAddress(host, port);
		ssc = ServerSocketChannel.open();
		ssc.socket().setPerformancePreferences(0, 2, 1);
		ssc.socket().setReceiveBufferSize(512);
		ssc.socket().bind(address);
		super.registor(ssc);
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public int getExecutorPoolSize() {
		return executorPoolSize;
	}

	public void setExecutorPoolSize(int executorPoolSize) {
		this.executorPoolSize = executorPoolSize;
	}

	public int getReaderPoolSize() {
		return readerPoolSize;
	}

	public void setReaderPoolSize(int readerPoolSize) {
		this.readerPoolSize = readerPoolSize;
	}

	public int getWriterPoolSize() {
		return writerPoolSize;
	}

	public void setWriterPoolSize(int writerPoolSize) {
		this.writerPoolSize = writerPoolSize;
	}

	@Override
	public void onStart() {
		super.onStart();
		this.executorService = newFixedThreadPool(this, "executor", executorPoolSize);
		((DefaultMessageReader<Connection, Session>)this.reader).setExecutor(newFixedThreadPool(this, "reader", readerPoolSize));
		((DefaultMessageWriter<Connection, Session>)this.writer).setExecutor(newFixedThreadPool(this, "writer", writerPoolSize));
		try {
			listen(hostname, port);
		} catch (IOException e) {
			throw new AccessException("listen "+hostname+":"+port,e);
		}
	}
	@Override
	protected void init() {
		super.init();
		log.info(String.format("Listen to %s:%d",  hostname, port));
	}
	@Override
	protected void onStop() {
		super.onStop();
		if(ssc!=null){
			try {
				ssc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			ssc =null;
		}
	}

	public void start(String hostName, int port) throws IOException {
		this.hostname = hostName;
		this.port = port;
		start();
	}

	public void start(int port) throws IOException {
		start("0.0.0.0", port);
	}
}
