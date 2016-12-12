/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */
package com.typesafe.training.akkacollect

import akka.actor._
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.{ImplicitSender, TestProbe}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

/**
 * A two node config for running the game engine and the player registry on using
 * remoting for communication in between.
 */
object RemotingNodeConfig extends MultiNodeConfig {
  val configOverrides = """
    |akkollect.game {
    |  move-count = 4
    |  move-timeout = 10 ms
    |}
  """.stripMargin

  commonConfig(ConfigFactory.parseString(configOverrides).withFallback(ConfigFactory.load()))
  val gameEngineNode = role("ge")
  val playerRegistryNode = role("pr")

}


class RemotingMultiJvmNode1 extends RemotingMultiSpec
class RemotingMultiJvmNode2 extends RemotingMultiSpec


class RemotingMultiSpec extends MultiNodeSpec(RemotingNodeConfig)
  with BaseMultiNodeSpec with ImplicitSender {

  import RemotingNodeConfig._

  def initialParticipants = roles.size

  "the tournament actor" should {

    "wait for all nodes to start" in {
      enterBarrier("startup")
    }
    
    "play through a game with the players from another node" in {

      runOn(gameEngineNode) {
        enterBarrier("player-registry-set-up")
        val playerRegistryAddress = node(playerRegistryNode).address
        val playerRegistrySelection = system.actorSelection(PlayerRegistry.pathFor(playerRegistryAddress))

        // fail test fast if the actor isn't running in the other system
        playerRegistrySelection ! Identify(None)
        expectMsgType[ActorIdentity]

        val scoresRepository = TestProbe()

        system.actorOf(Tournament.props(playerRegistrySelection, scoresRepository.ref, 10, 10.seconds), "tournament")
        scoresRepository.expectMsgType[ScoresRepository.UpdateScores]
      }

      runOn(playerRegistryNode) {
        val playerRegistry = system.actorOf(PlayerRegistry.props, PlayerRegistry.name)
        playerRegistry ! PlayerRegistry.RegisterPlayer("jill", SimplePlayer.props)
        playerRegistry ! PlayerRegistry.RegisterPlayer("james", SimplePlayer.props)
        expectMsg(PlayerRegistry.PlayerRegistered("jill"))
        expectMsg(PlayerRegistry.PlayerRegistered("james"))
        enterBarrier("player-registry-set-up")
      }

      // keep both nodes running
      enterBarrier("finished")
    }

  }

}