AndroidTCPSourceApp
============

Simple and performant Android library that extracts an application's basic information,
given a Socket object.

## What is it ##

This library is particularly useful for Android developers who are working on proxies.
The typical scenario consists in accepting the incoming connections with the 
ServerSocket.accept() method, which returns a Socket object. Since there is no easy way
for the developer to know to which application that Socket belongs (i.e. which application
initiated the connection), the AndroidTCPSourceApp is the way to go.

## How does it work ##

It is based on the fact that each socket is mapped as an entry on a file, called 
/proc/net/tcp (or /proc/net/tcp6 for IPv6-based connections).
This means that, for each entry, we can read its port and its associated PID.

Basically, the main method of the library performs the following steps:

   1. Receives a Socket object or the associated port (Socket.getPort())
   2. Parses the /proc/net/tcp (or /proc/net/tcp6) file, it looks for an entry with the given port
   3. If an entry is found, the method extracts the PID from it
   4. By calling the Android PackageManager.getPackagesFromPid() method with the newly found PID, 
   we can obtain the unique information about the source application, such as package name and application version

## How to use it ##

After importing the AndroidTCPSourceApp into your Android project, all you have to do is invoke the static method

```
TCPSourceApp.getApplicationInfo(Context, Socket)
```

Alternatively, you can call the overloaded method passing directly the Socket's port as an int value

```
TCPSourceApp.getApplicationInfo(Context, int)
```

The result of this method is an AppDescriptor object, which is just a container for the unique identifiers 
of an Android application: the package name (e.g. com.megadevs.tcpsourceapp) and the application version (e.g. 1.0).

How does it perform
-------------------

Typically, the /proc/net/tcp (or tcp6) file is no longer than 50 entries, so the parsing is extremely fast, 
adding almost no overhead to the whole procedure.

For the hardcore optimisers, there is a convenience method called 

```
TCPSourceApp.setCheckConnectedIfaces(boolean)
```
   
which basically tells the library to check if there are IPv6 addresses (global, not link-local) available for at least
one of the connected network interfaces, therefore avoiding useless parsing of the tcp6 file when no IPv6 connection
is available.

License
=======

Copyright (c) 2013, Sebastiano Gottardo
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the MegaDevs nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL SEBASTIANO GOTTARDO BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
