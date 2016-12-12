/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

// should use a production level implementations such as Kafka, Cassandra, Redis, etc.

package com.typesafe.training.akkacollect

import akka.actor.{ Actor, ActorLogging, ActorIdentity, ActorPath, ActorRef, ActorSystem, Address, Identify, Props, ReceiveTimeout, RootActorPath }
import akka.cluster.{ Cluster, Member }
import akka.cluster.ClusterEvent.{ InitialStateAsEvents, MemberUp }
import akka.persistence.journal.leveldb.{ SharedLeveldbJournal, SharedLeveldbStore }
import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.io.StdIn

object SharedJournal {

  val name: String =
    "shared-journal"

  def pathFor(address: Address): ActorPath =
    RootActorPath(address) / "user" / name
}

/**
 * The shared journal is a single point of failure and must not be used in production.
 * This app must be running in order for persistence and cluster sharding to work.
 */
object SharedJournalApp extends BaseApp with Terminal {

  override protected val parser: CommandParser.Parser[Command] =
    CommandParser.shutdown

  override def initialize(system: ActorSystem, settings: Settings): Unit = {
    val sharedJournal = system.actorOf(Props(new SharedLeveldbStore), SharedJournal.name)
    SharedLeveldbJournal.setStore(sharedJournal, system)
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

object SharedJournalSetter {

  val name: String =
    "shared-journal-setter"

  def props: Props =
    Props(new SharedJournalSetter)
}

/**
 * This actor must be started and registered as a cluster event listener by all actor systems
 * that need to use the shared journal, e.g. in order to use persistence or cluster sharding.
 */
class SharedJournalSetter extends Actor with ActorLogging {

  override def preStart(): Unit =
    Cluster(context.system).subscribe(self, InitialStateAsEvents, classOf[MemberUp])

  override def receive: Receive =
    waiting

  private def waiting: Receive = {
    case MemberUp(member) if member hasRole SharedJournal.name => onSharedJournalMemberUp(member)
  }

  private def becomeIdentifying(): Unit = {
    context.setReceiveTimeout(10 seconds)
    context become identifying
  }

  private def identifying: Receive = {
    case ActorIdentity(_, Some(sharedJournal)) =>
      SharedLeveldbJournal.setStore(sharedJournal, context.system)
      log.info("Succssfully set shared journal {}", sharedJournal)
      context.stop(self)
    case ActorIdentity(_, None) =>
      log.error("Can't identify shared journal!")
      context.stop(self)
    case ReceiveTimeout =>
      log.error("Timeout identifying shared journal!")
      context.stop(self)
  }

  private def onSharedJournalMemberUp(member: Member): Unit = {
    val sharedJournal = context actorSelection SharedJournal.pathFor(member.address)
    sharedJournal ! Identify(None)
    becomeIdentifying()
  }
}
