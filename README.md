# Multi-server-Network

##Introduction

This project is to implement a prototype of ditributed servers on which clients can register, login and broadcast objects to all the other clients.

##Team

This project has 4 collaborators including: @dongjize, @mason1002, @Lo1nt, @GatoY.

##Get started
Our servers' architecture is a binary tree. A single server will connect to another server as a child connection and get connections by another 2 servers as a parent connection. So make sure do not connect more than 2 servers to a single server as child connections, it will throw system failure.

Clients can log in on any available servers whatever the load may be, because the server will redirect the login request to another server when this server has to many loads to handle.

##Usage
Specific arguments is described in help functions of this project.



##Files
###bugsRecord.md
During the implementation of this project, we have faced many bugs and some bugs are fatal and hard to detect and handled, so we recorded the most annoying bugs here and how the triggers are and how we solved them.

###src.zip
It has the source code of this project. Server.java  has the main method for server. Client.java has the main method for client.
###testcase.md
This file records the main test cases. Of course, we have tested other circumstances like very complicated structure but we didn't put the details here.
###testExp.txt
This file records the common command and arguments to use both in Server and Client. Feel free to use these to have a try.
##Improvement
We can improve this project in some ways.
###LOCK_ALLOWED
We can improve 'server.control.onLockAllowed' by make a node to send 'LOCK_ALLOWED' after it gets 'LOCK_ALLOWED' either from parent connection or all the active child connections. Use this logic, we don't have to send as many 'LOCK_ALLOWED' message as we did.
###LOCK_DENIED
We can improve 'server.control.onLockDenied' by make a node to send 'LOCK_DENIED' immediately only to where it received 'LOCK_REQUEST' and not transfer the 'LOCK_REQUEST' when this node find the username has been registered already.
###Architecture
Our architecture in this project is a binary tree. We can make more than 2 child nodes to connect.