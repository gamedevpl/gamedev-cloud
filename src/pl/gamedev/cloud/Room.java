package pl.gamedev.cloud;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.java_websocket.WebSocket;

public class Room extends Thread {
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

	public void changeHost(long clientID) {
		hostID.set(clientID);
		sendMessage(clientID, "host:");
		System.out.println("[" + alias + "] host change");
	}
	
	public void sendMessage(long clientID, String message) {
		connections.get(clientID).send(message);
	}

	public void disconnectClient(Long clientID) {
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
				if (System.currentTimeMillis() - lastPong.get(clientID) > 5000)
					handleLeave(clientID);

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
		if (connections.containsKey(hostID.get()))
			connections.get(hostID.get()).send(id + ":leave:" + id);
	}

	public boolean isEmpty() {
		return connections.isEmpty();
	}
}