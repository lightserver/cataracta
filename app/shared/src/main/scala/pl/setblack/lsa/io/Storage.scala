package pl.setblack.lsa.io

trait Storage {
  def save(value : String, path : Seq[String]) : Unit

  def load( path : Seq[String]) : Option[String]

}
