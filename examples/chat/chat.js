var gc = new GamedevCloud("http://www.gamedev.pl/api/");
gc.getProxyServers().then(function(servers) {
	servers.some(function(server) {
		var conn = new Connection(server.socketURL);
		
		return true;
	});
})