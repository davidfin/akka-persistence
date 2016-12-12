/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

import akka.actor.{Actor, ActorLogging, ActorRef, FSM, Props, Terminated}
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.routing.FromConfig

import scala.concurrent.duration.FiniteDuration

object GameEngine {

  sealed trait State

  object State {

    case object Pausing extends State

    case object Running extends State
  }

  case class Data(tournament: Option[ActorRef] = None)

  val name: String =
    "game-engine"

  def props(tournamentInterval: FiniteDuration): Props =
    Props(new GameEngine(tournamentInterval))
}

class GameEngine(tournamentInterval: FiniteDuration)
  extends Actor with FSM[GameEngine.State, GameEngine.Data] with SettingsActor with ActorLogging {

  import GameEngine._
  val playerRegistry = createPlayerRegistry()
  val scoresRepository = createScoresRepository()

  startWith(State.Pausing, Data())

  when(State.Pausing, tournamentInterval) {
    case Event(StateTimeout, data) =>
      goto(State.Running) using data.copy(tournament = Some(startTournament(playerRegistry)))
  }

  when(State.Running) {
    case Event(Terminated(_), data) =>
      goto(State.Pausing) using data.copy(tournament = None)
  }

  onTransition {
    case _ -> State.Pausing => log.debug("Transitioning into pausing state")
    case _ -> State.Running => log.debug("Transitioning into running state")
  }

  initialize()

  def startTournament(playerRegistry:ActorRef): ActorRef = {
    log.info("Starting tournament")
    context.watch(createTournament(playerRegistry))
  }

  def createTournament(playerRegistry:ActorRef): ActorRef = {
    import settings.tournament._
    context.actorOf(Tournament.props(playerRegistry, scoresRepository, maxPlayerCountPerGame, askTimeout))
  }

  private def createScoresRepository(): ActorRef = {
    context.actorOf(FromConfig.props(Props[ScoresRepository]), "scoresRouter")
  }

  def createPlayerRegistry(): ActorRef = {
    context.actorOf(
      ClusterSingletonProxy.props(
        s"/user/player-registry-singleton-manager",
        ClusterSingletonProxySettings(context.system).withRole("player-registry")),
      "player-registry-proxy"
    )
  }
}
