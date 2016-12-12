/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

import akka.actor.{ Actor, ActorLogging, Props }

object ScoresRepository {

  case class UpdateScores(update: Map[String, Long])

  case object ScoresUpdated

  case object GetScores

  case class Scores(scores: Map[String, Long])

  val name: String =
    "scores-repository"

  def props: Props =
    Props(new ScoresRepository)
}

class ScoresRepository extends Actor with ActorLogging {

  import ScoresRepository._

  private var scores = Map.empty[String, Long]

  override def receive: Receive = {
    case UpdateScores(update) => updateScores(update)
    case GetScores => sender() ! Scores(scores)
    // case GetScores =>
    //   replicator ! Replicator.Get(replicatorKey, Replicator.ReadLocal, Some(sender()))
    // case Replicator.GetSuccess(_, data: PNCounterMap, Some(originalSender: ActorRef)) =>
    //   originalSender ! Scores(data.entries)
  }

  private def updateScores(update: Map[String, Long]): Unit = {
    scores = scores merge update
    log.info("Updated scores: {}", scores mkString ", ")
    sender() ! ScoresUpdated
  }
}
