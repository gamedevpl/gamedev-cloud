Gamedev Cloud
=============

noBackend solution for HTML5 games

Example game, Dupocracy:
http://gtanczyk.warsztat.io/Dupocracy/game/index.html

API
===

- GamedevCloud (Class)
> gamedev.pl/api/ connector

- GamedevCloud.prototype.getConnection(channel) (Deferred)
> connect to any public proxy server on channel (optional)

- Connection
> WebSocket connection wrapper

- Connection.prototype.on(filter, callback)
> receive message as a client, filter can be either String or RegExp, callback = function(header, body, data)

- Connection.prototype.hon(filter, callback)
> receive message as a host, filter can be either String or RegExp, callback = function(header, body, data, clientID)

- Connection.prototype.toHost(header, body)
> as a client, send message to host

- Connection.prototype.toClient(clientID, header, body)
> as a host, send message to client

- Connection.prototype.broadcast(header, body)
> as a host, broadcast message to all clients

How it works
============

One of clients is a host.

Connection.on and Connection.hon are exclusive, it means that message received as a client(on) will not be received as a host(hon)

Everything sent thru Connection.toClient or Connection.broadcast will be received by client with Connection.on callback.

Messages sent thru Connection.toHost are received with Connection.hon callback.

Example
==========

    var sendMessage;
    
    var gc = new GamedevCloud("http://www.gamedev.pl/api/"); 
    gc.getConnection('consolechat').then(function(connection) {
      connection.hon('msg', function(header, body, data, clientID) {
        // do stuff as host
        connection.broadcast('msg', body);
      });
      connection.on('msg', function(header, body, data) {
        // do stuff as a client
        console.log(body);
      });  
      
      // ask host to broadcast your message
      sendMessage = function(message) {
        connection.toHost('msg', message);
      }      
      
      sendMessage('hello world!')
    });
