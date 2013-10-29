package pl.gamedev.cloud;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class ProxyServer {

	public static void main(String[] args) throws Exception {
		String serverHost = args != null && args.length > 0 && args[0] != null ? args[0] : "localhost";
		int serverSocketPort = args != null && args.length > 1 && args[1] != null ? Integer.parseInt(args[1]) : 1750;

		final ConcurrentLinkedQueue<WebSocket> clients = new ConcurrentLinkedQueue<WebSocket>();
		final ConcurrentHashMap<Long, WebSocket> clientIDRev = new ConcurrentHashMap<Long, WebSocket>();
		final ConcurrentHashMap<WebSocket, Long> clientID = new ConcurrentHashMap<WebSocket, Long>() {
			public Long put(WebSocket key, Long value) {
				super.put(key, value);
				clientIDRev.put(value, key);
				return value;
			};
		};

		final AtomicLong clientIDSequence = new AtomicLong(System.currentTimeMillis());

		final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<String, Room>();

		final ConcurrentHashMap<WebSocket, Room> connections = new ConcurrentHashMap<WebSocket, Room>();

		WebSocketServer socketServer = new WebSocketServer(new InetSocketAddress(serverHost, serverSocketPort)) {

			@Override
			public void onOpen(WebSocket conn, ClientHandshake handshake) {
				String roomAlias = handshake.getResourceDescriptor();

				if (!rooms.containsKey(roomAlias) || rooms.get(roomAlias).isEmpty()) {
					if (rooms.containsKey(roomAlias)) {
						rooms.get(roomAlias).interrupt();
					}
					rooms.put(roomAlias, new Room(roomAlias));
					rooms.get(roomAlias).start();
				}

				Room room = rooms.get(roomAlias);

				connections.put(conn, room);

				long id;
				synchronized (clientIDSequence) {
					id = System.currentTimeMillis();
				}
				System.out.println("[" + id + "]" + conn.getRemoteSocketAddress() + " connected");

				clients.add(conn);
				clientID.put(conn, id);

				room.addMember(id, conn);
			}

			@Override
			public void onMessage(WebSocket conn, String message) {
				try {
					long id = clientID.get(conn);
					Room room = connections.get(conn);
					if (room != null)
						room.handleMessage(id, message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onError(WebSocket conn, Exception ex) {
				System.out.println(conn.getRemoteSocketAddress() + " error:");
				try {
					long id = clientID.get(conn);
					System.out.println("[" + id + "]" + conn.getRemoteSocketAddress() + " error");
					clients.remove(conn);

					Room room = connections.get(conn);
					if (room != null)
						room.handleError(id, ex);

					ex.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onClose(WebSocket conn, int code, String reason, boolean remote) {

				try {
					long id = clientID.get(conn);

					System.out.println("[" + id + "]" + conn.getRemoteSocketAddress() + " disconnected");
					clients.remove(conn);

					Room room = connections.get(conn);
					if (room != null)
						room.handleLeave(id);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		socketServer.start();
	}
}