var messagesNode = document.createElement('div');
document.body.appendChild(messagesNode);

var inputNode = document.createElement('input');
document.body.appendChild(inputNode);

var gc = new GamedevCloud("http://www.gamedev.pl/api/");
gc.getProxyServers().then(function(servers) {
	servers.some(function(server) {
		var connection = new Connection('ws://' + server.host + ':' + server.port);

		// debug
		connection.on(/(.*)/, function() {
			console.log("debug:", arguments)
		});
		
		// client stuff
				
		inputNode.addEventListener('keydown', function(event) {
			if(event.keyCode == 13) {
				connection.toHost('msg', inputNode.value);
				inputNode.value = '';
			}			
		});
		
		connection.on("msg", function(header, body, data) {
			var line = document.createElement('input');
			line.value = body;
			messagesNode.appendChild(line);
		});
				
		// host stuff

		connection.hon("msg", function(header, body, data, clientID) {
			connection.broadcast("msg", body);
		});


		return true;
	});
})