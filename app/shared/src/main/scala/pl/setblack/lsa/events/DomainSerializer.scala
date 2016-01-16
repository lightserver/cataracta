package pl.setblack.lsa.events

abstract  trait DomainSerializer[O] {
  def write(domain: O) : String
  def read(serialized : String) : O
}
