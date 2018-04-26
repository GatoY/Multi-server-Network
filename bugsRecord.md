## \#01
###Issue
Launch server on 3780, then server on 4000, we can register on 4000, not 3780.
###Response
In serverIdList, confused lChildConnection with rChildConnection
- By yu

## \#02
###Issue
Register same username twice, no fail message.

throw ConcurrentModificationException.
###Response
trigger: thread not safe
Change Collections.synchronizedList to CopyOnWriteArrayList
- By yu

## \#03
###Issue
Register and login 2 users, servers will close connections and the user will close connection.

###Response
Issue lied in Redirect part. We should send rediret to client other than server. Besides, the message should include another server's ip and port. Last but not least, socket.getInetAddress().getHostName() is the right one to get IP. Oh god.

