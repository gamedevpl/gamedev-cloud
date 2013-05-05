(function(global) {
	function Connection(socketURL) {
		this.socketURL = socketURL;

		this.on(/^(host|client)$/, function(header, body) {
			if (header == 'host')
				this.isHost = true;
			if (header == 'client')
				this.clientID = body;
		}.bind(this));

		if (window.socketOffline)
			this.closeSocket();
		else
			try {
				this.socket = new WebSocket(this.socketURL);

				this.socket.onopen = function() {

				}.bind(this);

				this.socket.onerror = this.socket.onclose = this.closeSocket
						.bind(this);

				this.socket.onmessage = function(event) {
					var header = event.data.substring(0, event.data
							.indexOf(':'));
					var body = event.data
							.substring(event.data.indexOf(':') + 1);
					this.receive(header, body, event.data);
				}.bind(this);
			} catch (e) {
				this.closeSocket();
			}
	}

	Connection.prototype.closeSocket = function() {
		this.socket = null;
		if (!this.clientID) { // init local host mode
			this.log('Server connection failed, entering offline mode');
			setTimeout(function() {
				this.receive("client", "0");
				this.receive("host", "true");
			}.bind(this), 0);
		}
	};
	
	Connection.prototype.log = function() {
	}	

	Connection.prototype.onListeners = [],

	Connection.prototype.receive = function(header, body, data) {
		this.onListeners.every(function(listener) {
			if (header.match(listener.filter))
				listener.callback(header, body, data);
			return true;
		})
	};

	Connection.prototype.on = function(filter, callback) {

		this.onListeners.push({
			filter : filter,
			callback : callback
		});
	};

	Connection.prototype.hon = function(filter, callback) {
		this.onListeners.push({
			filter : /^(\d+)$/,
			callback : function(header, body, data) {
				var clientID = header;
				var header = body.substring(0, body.indexOf(':'));
				if (!header.match(filter))
					return;
				var body = body.substring(body.indexOf(':') + 1);
				callback(header, body, data, clientID);
			}
		});
	};

	Connection.prototype.broadcast = function(header, body, callback) {
		if (this.isHost) {
			if (this.socket)
				this.socket.send("*:" + header + ":"
						+ (body != null ? body : ''));
			setTimeout(function() {
				this.receive(header, body);
			}.bind(this), 0)
		}
	};

	Connection.prototype.toHost = function(header, body, callback) {
		if (this.isHost)
			setTimeout(function() {
				this.receive(this.clientID, header + ":"
						+ (body != null ? body : ''));
			}.bind(this), 0);
		else if (this.socket)

			this.socket.send("host:" + header + ":"
					+ (body != null ? body : ''));

	};

	Connection.prototype.toClient = function(clientID, header, body, callback) {
		if (this.isHost)
			if (clientID == this.clientID)
				setTimeout(function() {
					this.receive(header, body);
				}.bind(this), 0);
			else if (this.socket)
				this.socket.send(clientID + ":" + header + ":"
						+ (body != null ? body : ''));

	};
	
	global["Connection"] = Connection;
})(this)