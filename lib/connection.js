(function(global) {
	function Connection(socket, server) {
		this.socketURL = socket.url;
		this.server = server;
		
		this.onListeners = [];
	
		this.on(/^(host|client)$/, function(header, body) {
			if (header == 'host')
				this.isHost = true;
			if (header == 'client')
				this.clientID = body;
		}.bind(this));
		
		this.hon('ping', function(header, body, data, clientID) {
			setTimeout(this.toClient.bind(this, clientID, 'pong'), 1000);
		}.bind(this));
		
		this.on(/^(.*)$/, function(header, body) {
			setInterval(this.toHost.bind(this, 'ping'), 1000);
		}.bind(this), { single: true });
		
		this.syncTime = new Deferred();
		this.on('@time', function(header, body) {
			var t = new Date().getTime();
			this.on('@@@time', function(header, body) {
				this.roomSyncTime = new Date().getTime();
				var dt = (this.roomSyncTime - t) / 2;
				this.roomTime = parseInt(body) - dt;
				this.syncTime.resolve(this.roomTime);
			}.bind(this), { single: true });			
			this.toHost('@@time')
		}.bind(this), { single: true });
		
		if(this.socketURL=='loopback')
			this.loopback();
		else
			try {
				this.socket = socket;
				
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
	
	Connection.prototype.getRoomTime = function() {
		if(!this.roomTime)
			throw "roomTime was not synced, call connection.syncTime.then(function() { })"
		return this.roomTime + new Date().getTime() - this.roomSyncTime;
	}
	
	Connection.prototype.loopback = function() {
		this.receive("client", "0");
		this.receive("host", "true");
		setTimeout(this.receive.bind(this, "host", "true"));
	}	

	Connection.prototype.closeSocket = function() {
		this.receive("disconnected");
		this.socket = null;		
		this.log('Server connection failed');
	};
	
	Connection.prototype.log = function() {
	}	

	Connection.prototype.receive = function(header, body, data) {
		this.onListeners = this.onListeners.filter(function(listener) {
			if (header.match(listener.filter)) {				
				setTimeout(listener.callback.bind(null, header, body, data));
				return !listener.opts.single;
			} else
				return true;
		})
	};

	Connection.prototype.on = function(filter, callback, opts) {
		var listener = {
			filter : filter,
			callback : callback,
			opts: opts || {},
			remove: function() {
				this.onListeners.splice(this.onListeners.indexOf(listener), 1)
			}.bind(this)			
		};
		this.onListeners.push(listener);
		return listener;
	};

	Connection.prototype.hon = function(filter, callback, opts) {
		var listener = {
			filter : /^(\d+)$/,
			opts: opts || {},
			callback : function(header, body, data) {
				var clientID = header;
				var header = body.substring(0, body.indexOf(':'));
				if (!header.match(filter))
					return;
				var body = body.substring(body.indexOf(':') + 1);
				callback(header, body, data, clientID);
			},
			remove: function() {
				this.onListeners.splice(this.onListeners.indexOf(listener), 1)
			}.bind(this)	
		};
		this.onListeners.push(listener);
		return listener;
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
		if (this.isHost && header != 'ping')
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
