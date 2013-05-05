test("gamedevcloud", function(fail, pass) {
	var gc = new GamedevCloud("http://www.gamedev.pl/api/");

	gc.getProxyServers().then(function(servers) {
		console.log(servers);
		pass();
	})
});
