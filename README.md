# Cataracta

*Cataracta*  is ScalaJS event based framework enabling Light Server Approach.

## LSA - Light Server Approach
LSA It is a architectural pattern where Browser (Web) and Server roles are not strictly defined.
They are simply Nodes.
Decisions which business components are deployed in Browser(s) or on Server(s) can be delayed or easily adapted during development.
(and sorry I've created the term because I needed some name - maybe there is already something better there.)


Because:
> A good architecture maximizes the number of decisions NOT made.
Robert C. Martin

### Why LSA - top reasons
1. *Forget Server* -  develop as You would do in html / standalone demo and get all server functionality for free.
2. *Akka but also in JS* - Actors are great tool for creating  safe and scalable distributed systems. And now You can have exactly the same actors working in Browser and Server. They can transparently communicate with each other no matter where they are.
3. *Events everywhere* Thanks to events and websockets clients may react on any events that happen anywhere in the system.
4. *Event sourcing* -  And because it is all events...  persistence is for free.
5. *All your nodes are belong to us* - every client that connects to your page is not a problem anymore - but just another node in your cluster :-).
6. *Backup everywhere* - each client may be have a backup of your data.
7. *Offline* - clients can work offline for longer periods of time - events between server and clients will be synchronized once it is possible.

##  Basics of Cataracta
1. It uses *Scala* as one language for server and client thanks to great ScalaJS).
2. Every connected browser or server is a *Node*.
3. Each Node can host multiple *Domains* - domains are sth like Actors and Aggregates from DDD.
4. Domains react on events and change state or send events to other domains.
5. Server *Nodes* run on JVM (with Akka , Akka-HTTP), browser nodes run simply in Web/Javascript.
5. Domains (Web or JVM) may have Persistent store. Store  saves events and allows to restore Domains.
6. Domains may have Listeners - this is default way to bind any UI Framework (like ReactJS or Angular).

## Examples
# Chat system
[MB137](https://github.com/lightserver/mb137)



## Problems
 - this is way before alpha stage, do not use it on production unless you want to help me in development,
 - yes, I need help,
 - very naive event routing system,
 - performance is not a goal for first version,
