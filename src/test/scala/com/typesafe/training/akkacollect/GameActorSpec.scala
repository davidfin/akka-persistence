package com.typesafe.training.akkacollect

import akka.actor._
import akka.testkit.TestProbe
import com.typesafe.training.akkacollect.Game.GameOver
import scala.concurrent.Promise
import scala.concurrent.duration._

class GameActorSpec extends BaseAkkaSpec {

  "The game actor" should {

    val gameProps = Props(new Game(
      players = Set("Player1", "Player2"),
      moveCount = 1,
      moveTimeout = 1.millisecond,
      sparseness = 23
    ){
      override protected def tellPlayer(player: String, message: Any): Unit = {}
    })

    "end move after timeout passed" in {
      val probe = TestProbe()
      val onMoveTimmeoutWasCalled = Promise[Unit]()
      val game = system.actorOf(Props(new Game(
        players = Set("Player1", "Player2"),
        moveCount = 1,
        moveTimeout = 1.millisecond,
        sparseness = 23) {
        override def onMoveTimeout(moveNumber: Long): Unit = {
          onMoveTimmeoutWasCalled.success(Unit)
          super.onMoveTimeout(moveNumber)
        }
        override protected def tellPlayer(player: String, message: Any): Unit = {}
      }))


      val completedF = onMoveTimmeoutWasCalled.future
      awaitCond(completedF.isCompleted)

      system.stop(game)
    }

    "end the game after the number of moves are up" in {
      val probe = TestProbe()
      val game = system.actorOf(Props(new Game(
        players = Set("Player1", "Player2"),
        moveCount = 1,
        moveTimeout = 1.millisecond,
        sparseness = 23
      ){
        override protected def tellPlayer(player: String, message: Any): Unit = {}
      }))
      probe.watch(game)

      probe.expectTerminated(game)

      system.stop(game)
    }

    "send score to parent when ending the game" in {
      val parentGotScore = Promise[Unit]()
      class Parent extends Actor {
        val game = context.actorOf(Props(new Game(
          players = Set("Player1", "Player2"),
          moveCount = 1,
          moveTimeout = 1.millisecond,
          sparseness = 23
        ){
          override protected def tellPlayer(player: String, message: Any): Unit = {}
        }))

        def receive = {
          case _ :GameOver => parentGotScore.success(Unit)
        }
      }

      val parent = system.actorOf(Props(new Parent))

      val completedF = parentGotScore.future
      awaitCond(completedF.isCompleted)

      system.stop(parent)
    }

  }


}
