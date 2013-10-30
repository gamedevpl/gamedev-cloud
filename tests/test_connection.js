test("connection", function(fail, pass) {
	var conn = new Connection(new WebSocket("ws://localhost:1750"));
	conn.syncTime.then(function(roomTime) {
		console.log(roomTime);
		pass();
	});
});
