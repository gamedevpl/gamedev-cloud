package pl.gamedev.cloud;

import static org.junit.Assert.*;

import org.java_websocket.WebSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class RoomTest {

	Room room, roomSpy;

	@Before
	public void setUp() {
		room = new Room("testroom");
		roomSpy = Mockito.spy(room);
	}

	@After
	public void teardown() {

	}

	@Test
	public void testChangeHost() {
		WebSocket socket1 = Mockito.mock(WebSocket.class), socket2 = Mockito
				.mock(WebSocket.class);

		roomSpy.addMember(1, socket1);
		roomSpy.addMember(2, socket2);
		// member1 should be the host
		Mockito.verify(socket1).send("client:1");
		Mockito.verify(socket1).send("host:");
		Mockito.verify(socket2).send("client:2");
		
		// change host
		room.changeHost(2);
		// member1 should be informed about host change
		Mockito.verify(socket1).send("client:");
		// member2 is the new host
		Mockito.verify(socket2).send("host:");
	}

	@Test
	public void testRun() {
		fail("Not yet implemented");
	}

	@Test
	public void testRoom() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddMember() {
		fail("Not yet implemented");
	}

	@Test
	public void testHandleMessage() {
		fail("Not yet implemented");
	}

	@Test
	public void testHandleError() {
		fail("Not yet implemented");
	}

	@Test
	public void testHandleLeave() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsEmpty() {
		fail("Not yet implemented");
	}

}
