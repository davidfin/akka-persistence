package com.typesafe.training.akkacollect

import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.testkit.TestProbe

class TournamentSpec extends BaseAkkaSpec {

  "The tournament actor" should {
    "partition players into games" in {
      val playerRegistry = TestProbe()

      val gamesCreated = new AtomicInteger(0)
      val tournament = system.actorOf(Props(new Tournament(playerRegistry.ref, system.deadLetters, 2) {
        override protected def createGame(players: Set[ActorRef]): ActorRef = {
          gamesCreated.incrementAndGet()
          system.deadLetters
        }
      }))

      playerRegistry.expectMsg(PlayerRegistry.GetPlayers)
      playerRegistry.reply(PlayerRegistry.Players(Seq.tabulate(4)(_ => TestProbe().ref).toSet))

      awaitCond(gamesCreated.intValue == 2)
      system.stop(tournament)
    }
  }

}
