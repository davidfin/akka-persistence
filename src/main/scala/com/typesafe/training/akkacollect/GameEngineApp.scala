/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

import akka.actor.{ ActorRef, ActorSystem }
import scala.annotation.tailrec
import scala.io.StdIn

object GameEngineApp extends BaseApp with Terminal {

  override def initialize(system: ActorSystem, settings: Settings): Unit = {
    system.actorOf(SharedJournalSetter.props, "shared-journal-setter")
    PlayerSharding(system).startProxy()
  }


  override protected val parser: CommandParser.Parser[Command] =
    CommandParser.shutdown

  override def createTop(system: ActorSystem, settings: Settings): ActorRef = {
    import settings.gameEngine._
    val scoresRepository = system.actorOf(ScoresRepository.props, ScoresRepository.name)
    system.actorOf(GameEngine.props(tournamentInterval), GameEngine.name)
  }

  @tailrec
  override protected def commandLoop(system: ActorSystem, settings: Settings, top: ActorRef): Unit = {
    Command(StdIn.readLine()) match {
      case Command.Shutdown =>
        system.terminate()
      case Command.Unknown(command, message) =>
        system.log.warning("Unknown command {} ({})!", command, message)
        commandLoop(system, settings, top)
      case other =>
        system.log.warning("Not responsible for command {}!", other)
        commandLoop(system, settings, top)
    }
  }
}
