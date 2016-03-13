# Cataracta

*Cataracta*  is ScalaJS event based framework enabling Light Server Approach.

## LSA - Light Server Approach
LSA It is a architectural pattern where Browser (Web) and Server roles are not strictly defined.
They are simply Nodes.
Decisions which business components are deployed in Browser or on Server can be delayed or easily adapted during development.

> A good architecture maximizes the number of decisions NOT made.
Robert C. Martin

### Why LSA - top reasons
1. *Forget Server* -  develop as You would do html / standalone demo and get all server functionalities for free.
2. *Akka but also in JS* - Actors are great tool for creating  safe and scalable distributed systems. And now You can have same actors in Browser and Server and You can transparently communicate with them no matter where they are.
3. *Events everywehere* Thanks to events and websocket client can react on any event that happens anywhere in the system.
4. *Event sourcing* -  And becuase it is all events... you get persistence for free.
5. *All your nodes are belong to us* - every client that connects to your page is not a problem anymore - but another Node in your cluster.
6. *Backup everywhere* - each client may be a Backup for all  or part of Your data.
7. *Offline* - clients can work offline for longer periods of time - events will between server and clients will be synchronized when  it is possible.


##  Basics of Cataracta
1. It uses *Scala* as one language for server and client thanks to great ScalaJS).
2. Every connected browser or server is a *Node*.
3. Each Node can host multiple *Domains* - domains are sth like Actors and Aggreagates from DDD.
4. Domains react on events and change state or send events to other domains.
5. Server *Nodes* run on JVM (with Akka , Akka-HTTP), browser nodes run simply in Web/Javascript.
5. Domains (Web or JVM) may have Persistent store. Store  saves events and allows to restore Domains.
6. Domains may have Listeners - this is default way to bind any UI Framework (like ReactJS or Angular).

## Examples
# Chat system









Testing ScalaJS and LightServer architecture

 - very naive event routing system (ScalaJS compatible)


