/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import scala.math.abs
import scala.util.Random


object Game {

  case class MakeMove(moveNumber: Long, playerPositions: Map[String, Position], coinPositions: Set[Position])

  case class MoveMade(moveNumber: Long, direction: Direction)

  case class GameOver(scores: Map[String, Long])

  private case class MoveTimeout(moveNumber: Long)

  sealed trait Direction

  object Direction {

    case object North extends Direction

    case object NorthEast extends Direction

    case object East extends Direction

    case object SouthEast extends Direction

    case object South extends Direction

    case object SouthWest extends Direction

    case object West extends Direction

    case object NorthWest extends Direction

    val all: Set[Direction] =
      Set(North, NorthEast, East, SouthEast, South, SouthWest, West, NorthWest)

    def random(): Direction =
      all.toVector(Random.nextInt(all.size))
  }

  object Position {
    implicit def fromIntPair(pair: (Int, Int)): Position =
      Position(pair._1, pair._2)
  }

  case class Position(x: Int, y: Int) {

    def move(direction: Direction): Position = {
      import Direction._
      direction match {
        case North     => copy(y = y + 1)
        case NorthEast => copy(x = x + 1, y = y + 1)
        case East      => copy(x = x + 1)
        case SouthEast => copy(x = x + 1, y = y - 1)
        case South     => copy(y = y - 1)
        case SouthWest => copy(x = x - 1, y = y - 1)
        case West      => copy(x = x - 1)
        case NorthWest => copy(x = x - 1, y = y + 1)
      }
    }

    def -(that: Position): Position =
      minus(that)

    def minus(that: Position): Position =
      Position(this.x - that.x, this.y - that.y)

    def direction: Direction = {
      import Direction._
      require(x != 0 || y != 0)
      val ratio = x.toDouble / y
      if (ratio >= -0.5 && ratio <= 0.5)
        if (y > 0) North else South
      else if (ratio <= -2 || ratio >= 2)
        if (x > 0) East else West
      else {
        if (x > 0 && y > 0) NorthEast
        else if (x > 0 && y < 0) SouthEast
        else if (x < 0 && y < 0) SouthWest
        else NorthWest
      }
    }

    def distanceFrom(that: Position): Int =
      abs(this.x - that.x) + abs(this.y - that.y)
  }

  def props(players: Set[String], moveCount: Int, moveTimeout: FiniteDuration, sparseness: Int): Props =
    Props(new Game(players, moveCount, moveTimeout, sparseness))
}

class Game(players: Set[String], moveCount: Long, moveTimeout: FiniteDuration, sparseness: Int)
    extends Actor with ActorLogging {

  import Game._
  import context.dispatcher

  private val fieldWidth = players.size * sparseness

  private val sectorRadius = (0.2 * fieldWidth).toOddInt / 2

  private var scores = Map.empty[String, Long] withDefaultValue 0L

  private var (playerPositions, coinPositions) = initialPositions()

  override def preStart(): Unit = {
    log.info("Game started with players: {}", players mkString ", ")
    becomeHandlingMove(1)
  }

  override def receive: Receive =
    Actor.emptyBehavior // `becomeHandlingMove`, called from `preStart`, sets the proper initial behavior

  def becomeHandlingMove(moveNumber: Long): Unit = {
    if (coinPositions.size < players.size)
      coinPositions ++= additionalCoinPositions()
    sendMakeMove(moveNumber)
    context.system.scheduler.scheduleOnce(moveTimeout, self, MoveTimeout(moveNumber))
    context become handlingMove(moveNumber)
  }

  def handlingMove(moveNumber: Long): Receive = {
    case MoveMade(`moveNumber`, direction) => onMoveMade(direction, moveNumber)
    case MoveTimeout(`moveNumber`)         => onMoveTimeout(moveNumber)
  }

  def onMoveMade(direction: Direction, moveNumber: Long): Unit = {
    val player = sender().path.name
    val newPosition = playerPositions(player) move direction
    if (!isOccupied(newPosition)) {
      playerPositions += player -> newPosition
      if (holdsCoin(newPosition)) {
        coinPositions -= newPosition
        scores += player -> (scores(player) + 1)
      }
    }
  }

  def onMoveTimeout(moveNumber: Long): Unit = {
    if (moveNumber < moveCount)
      becomeHandlingMove(moveNumber + 1)
    else {
      log.info("Game over with scores: {}", scores mkString ", ")
      context.parent ! GameOver(scores)
      context.stop(self)
    }
  }

  def sendMakeMove(moveNumber: Long): Unit = {
    def relativePlayerPositions(player: String) = {
      val playerPosition = playerPositions(player)
      for {
        (name, position) <- playerPositions
        if (name != player) && ((position distanceFrom playerPosition) <= sectorRadius)
      } yield name -> (position - playerPosition)
    }
    def relativeCoinPositions(player: String) = {
      val playerPosition = playerPositions(player)
      for {
        position <- coinPositions
        if (position distanceFrom playerPosition) <= sectorRadius
      } yield position - playerPosition
    }
    for (playerActor <- players; player = playerActor) {
      val message = MakeMove(moveNumber, relativePlayerPositions(player), relativeCoinPositions(player))
      tellPlayer(playerActor, message)
    }
  }

  private def initialPositions(): (Map[String, Position], Set[Position]) = {
    val (playerPositions, coinPositions) = randomPositions(players.size * 2) splitAt players.size
    val playerToPosition = (players zip playerPositions).toMap
    (playerToPosition, coinPositions)
  }

  private def additionalCoinPositions(): Set[Position] = {
    val existingPositions = playerPositions.valuesIterator.toSet ++ coinPositions
    randomPositions(players.size * 2, existingPositions) -- existingPositions
  }

  @tailrec
  private def randomPositions(count: Int, positions: Set[Position] = Set.empty): Set[Position] =
    if (positions.size == count)
      positions
    else
      randomPositions(count, positions + Position(Random.nextInt(fieldWidth), Random.nextInt(fieldWidth)))

  private def isOccupied(position: Position): Boolean =
    playerPositions.valuesIterator contains position

  private def holdsCoin(position: Position): Boolean =
    coinPositions contains position

  protected def tellPlayer(name:String, message: Any): Unit = {
    PlayerSharding(context.system).tellPlayer(name: String, message: Any)
  }

}
