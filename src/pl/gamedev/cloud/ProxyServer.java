package pl.gamedev.cloud;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

class Room extends Thread {
	ConcurrentHashMap<Long, WebSocket> connections = new ConcurrentHashMap<Long, WebSocket>() {
		public WebSocket put(Long key, WebSocket value) {
			members.add(key);
			return super.put(key, value);
		};

		public WebSocket remove(Object key) {
			members.remove(key);
			return super.remove(key);
		};
	};

	ConcurrentLinkedQueue<Long> members = new ConcurrentLinkedQueue<Long>() {
		@Override
		public boolean add(Long key) {
			lastPong.put(key, System.currentTimeMillis());
			return super.add(key);
		}

		public boolean remove(Object key) {
			lastPong.remove(key);
			return super.remove(key);
		};
	};

	ConcurrentHashMap<Long, Long> lastPong = new ConcurrentHashMap<Long, Long>();

	AtomicLong hostID = new AtomicLong(-1);

	String alias;

	public Room(String alias) {
		this.alias = alias;

		System.out.println("[" + alias + "] created");
	}

	private void changeHost(long clientID) {
		hostID.set(clientID);
		connections.get(clientID).send("host:");
		System.out.println("[" + alias + "] host change");
	}

	private void disconnectClient(Long clientID) {
		if (!connections.containsKey(clientID))
			return;

		System.out.println("[" + clientID + "]" + connections.get(clientID).getRemoteSocketAddress() + " connected");

		connections.remove(clientID);

		if (clientID == hostID.get() && !isEmpty())
			changeHost(members.peek());

	}

	@Override
	public void run() {
		while (true) {
			for (Long clientID : members)
				if (System.currentTimeMillis() - lastPong.get(clientID) > 5000) {
					disconnectClient(clientID);
				}

			if (!members.contains(hostID.get()) || System.currentTimeMillis() - lastPong.get(hostID.get()) > 3000)
				if (!members.isEmpty())
					changeHost(members.peek());

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				System.out.println("[" + alias + "] interrupted");
				break;
			}
		}
	}

	public void addMember(long clientID, WebSocket conn) {
		conn.send("client:" + clientID);
		connections.put(clientID, conn);
		if (hostID.get() == -1)
			changeHost(clientID);
	}

	public void handleMessage(Long clientID, String message) {

		lastPong.put(clientID, System.currentTimeMillis());

		boolean isHost = hostID.get() == clientID;

		WebSocket host = connections.get(hostID.get());
		String header = message.substring(0, message.indexOf(':'));
		String body = message.substring(message.indexOf(':') + 1);
		// System.out.println(header + ", " + body);
		if (header.equals("host") && connections.containsKey(clientID))
			if (connections.containsKey(hostID.get()))
				connections.get(hostID.get()).send(clientID + ":" + body);
		if (isHost && header.matches("^([0-9]+)$")) {
			long targetID = Long.parseLong(header);
			if (connections.containsKey(targetID))
				connections.get(targetID).send(body);
		} else if (isHost && header.equals("*")) {
			for (WebSocket client : connections.values()) {
				if (client != host)
					client.send(body);
			}
		}
	}

	public void handleError(long clientID, Exception ex) {
		disconnectClient(clientID);
	}

	public void handleLeave(long id) {
		disconnectClient(id);
		if (connections.contains(hostID.get()))
			connections.get(hostID.get()).send(id + ":leave");
	}

	public boolean isEmpty() {
		return connections.isEmpty();
	}
}

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