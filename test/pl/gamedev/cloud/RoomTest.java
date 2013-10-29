package pl.gamedev.cloud;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class RoomTest {
	
	@Mock
	Room room;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@After 
	public void teardown() {
		
	}
	
	@Test
	public void testChangeHost() {
		room.changeHost(0);
		Mockito.verify(room).sendMessage(0, "host:");
		Mockito.verify(room, Mockito.times(1));
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
