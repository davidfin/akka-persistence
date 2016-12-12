/**
  * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
  */

package com.typesafe.training.akkacollect

import akka.actor.{ ActorLogging, ActorPath, Address, Props, RootActorPath }
import akka.persistence.{ PersistentActor, SnapshotOffer }

object PlayerRegistry {

  case class RegisterPlayer(name: String, props: Props)

  case class PlayerNameTaken(name: String)

  case class PlayerRegistered(name: String)

  case object GetPlayers

  case class Players(players: Set[String])

  case class Snapshot(players: Set[String])

  val name: String =
    "player-registry"

  def pathFor(address: Address): ActorPath =
    RootActorPath(address) / "user" / name

  def props: Props =
    Props(new PlayerRegistry)
}

class PlayerRegistry extends PersistentActor with SettingsActor with ActorLogging {

  import PlayerRegistry._

  private var players = Set.empty[String]

  override val persistenceId: String =
    name

  override def receiveCommand: Receive = {
    case RegisterPlayer(name, _) if isNameTaken(name) => playerNameTaken(name: String)
    case RegisterPlayer(name, props)                  => registerPlayer(name, props)
    case GetPlayers                                   => sender() ! Players(players)
  }

  override def receiveRecover: Receive = {
    case playerRegistered: PlayerRegistered   => onPlayerRegistered(playerRegistered)
    case SnapshotOffer(_, snapshot: Snapshot) => players = snapshot.players
  }

  private def playerNameTaken(name: String): Unit = {
    log.warning("Player name {} taken", name)
    sender() ! PlayerNameTaken(name)
  }

  private def registerPlayer(name: String, props: Props): Unit = {
    persist(PlayerRegistered(name)) { playerRegistered =>
      onPlayerRegistered(playerRegistered)
      createPlayer(name, props)
      sender() ! playerRegistered
    }
  }

  private def onPlayerRegistered(playerRegistered: PlayerRegistered): Unit = {
    val name = playerRegistered.name
    log.info("Registering player {}", name)
    players += name
    if (players.size % 100 == 0)
      saveSnapshot(Snapshot(players))
  }

  protected def createPlayer(name: String, props: Props): Unit =
    PlayerSharding(context.system).tellPlayer(name, PlayerSharding.Player.Initialize(props))

  private def isNameTaken(name: String): Boolean =
    players contains name
}
