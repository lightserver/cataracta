package pl.setblack.lsa.events

class NodeConnection(
                       val targetId:Long,
                       val protocol:Protocol) {
                          def send ( msg:NodeMessage): Unit = {
                            println("nc sending to: " + targetId)
                            protocol.send(msg)
                          }

                          def knows (id: Long) : Boolean = {
                            id == targetId
                          }
                        }


