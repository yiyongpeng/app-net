package app;

import java.nio.ByteBuffer;

import app.net.AppCallResponseAdapter;
import app.net.AppSession;
import app.net.AppClientBroker;

public class Shutdown {

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err
					.println("error: Invalid args.\ne.g. java app.Shutdown hostname port");
			return;
		}
		String address = args[0];
		int port = Integer.parseInt(args[1]);
		try {
			AppSession session = AppClientBroker.createConnection(address, port,
					3);
			session.send(9, ByteBuffer.allocate(0),
					new AppCallResponseAdapter() {
						@Override
						public void onSuccess(ByteBuffer msg) {
						}
					}, true, 60000);
			System.exit(0);
		} catch (Exception e) {
			System.err.println(e);
			System.exit(1);
		}
		System.exit(2);
	}
}
