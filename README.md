Gamedev Cloud
=============

noBackend solution for HTML5 games

How to use
==========

    var gc = new GamedevCloud("http://www.gamedev.pl/api/"); 
    gc.getConnection.then(function(connection) {
      connection.hon('ping', function(header, body, data, clientID) {
        // do stuff as host
        connection.toClient(clientID, 'pong');
      });
      connection.on('ping', function(header, body, data) {
        // do stuff as a client
        connection.toHost('pong');
      });  
      connection.toHost('ping');
    });
