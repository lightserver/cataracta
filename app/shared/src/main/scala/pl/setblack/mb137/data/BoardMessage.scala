package pl.setblack.mb137.data

case class BoardMessage(val author: String,
                        val txt: String,
                        timestamp : Long,
                         uuid : String = "07")
