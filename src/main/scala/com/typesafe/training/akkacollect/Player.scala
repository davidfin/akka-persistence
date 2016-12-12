/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

import akka.actor.{ Actor, Props }

object RandomPlayer {
  def props: Props =
    Props(new RandomPlayer)
}

class RandomPlayer extends Actor {
  override def receive: Receive = {
    case Game.MakeMove(moveNumber, _, _) => sender() ! Game.MoveMade(moveNumber, Game.Direction.random())
  }
}

object SimplePlayer {
  def props: Props =
    Props(new SimplePlayer)
}

class SimplePlayer extends Actor {

  override def receive: Receive = {
    case Game.MakeMove(moveNumber, _, coinPositions) => makeMove(moveNumber, coinPositions)
  }

  private def makeMove(moveNumber: Long, coinPositions: Set[Game.Position]): Unit = {
    val direction = {
      def distanceFromZero(position: Game.Position) = position distanceFrom Game.Position(0, 0)
      if (coinPositions.isEmpty)
        Game.Direction.random()
      else
        ((coinPositions zip (coinPositions map distanceFromZero)) minBy { case (_, dist) => dist })._1.direction
    }
    sender() ! Game.MoveMade(moveNumber, direction)
  }
}
