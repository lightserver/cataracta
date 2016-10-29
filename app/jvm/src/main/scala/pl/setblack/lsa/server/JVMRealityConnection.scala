package pl.setblack.lsa.server

import akka.actor.ActorSystem
import pl.setblack.lsa.concurrency.AkkaConcurrencySystem
import pl.setblack.lsa.io.{FileStorage, FileStore}
import pl.setblack.lsa.os.{Reality, SimpleReality}
import pl.setblack.lsa.resources.JVMResources
import pl.setblack.lsa.security.SecurityProvider

import scala.concurrent.{ExecutionContext, Future}

object JVMRealityConnection {

  def create(baseSecurityProvider : Future[SecurityProvider])(implicit system: ActorSystem, executionContext : ExecutionContext ) : Reality = {
    new SimpleReality(createFileStorage(system),
      new AkkaConcurrencySystem(system),
      createSecurityProvider(baseSecurityProvider),
      new JVMResources

    )
  }


  private def createFileStorage(system: ActorSystem) (implicit  executionContext: ExecutionContext)= {
    val fileStorePath = system.settings.config.getString("app.file.filesDir")
    new FileStorage(fileStorePath)
  }

  private def createSecurityProvider( baseSecurityProvider  : Future[SecurityProvider]) : Future[SecurityProvider]  = {
  /* TODO add root key - remove baseSec if possible
    baseSecurityProvider.flatMap(_.registerSigner(
      rootKey.author, rootKey.exported, rootCertificate)
    ) */
    baseSecurityProvider
  }
}
