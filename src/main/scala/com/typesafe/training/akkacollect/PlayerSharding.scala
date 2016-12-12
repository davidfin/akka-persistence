package com.typesafe.training.akkacollect

import akka.actor.{Actor, ActorLogging, ActorRef, ExtendedActorSystem, Extension, ExtensionKey, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.PersistentActor

object PlayerSharding extends ExtensionKey[PlayerSharding] {

  object Player {

    case class Initialize(props: Props)

    case class Initialized(props: Props)

    case class Envelope(name: String, payload: Any)

    val typeName: String =
      "player"

    val extractEntityId: ShardRegion.ExtractEntityId = {
      case Player.Envelope(name, payload) => (name, payload)
    }

    // calculate the shard Id based on the number of shards that we want.
    def extractShardId(shardCount: Int): ShardRegion.ExtractShardId= {
      case Player.Envelope(name, _) => name.hashCode % shardCount toString
    }

    def props: Props =
      Props(new Player)
  }

  class Player extends PersistentActor with ActorLogging {

    import Player._


    override val persistenceId: String =
      self.path.name

    override def receiveCommand: Receive = {
      case Initialize(props) => persist(Initialized(props))(_ => becomeInitialized(props))
    }

    override def receiveRecover: Receive = {
      case Initialized(props) => becomeInitialized(props)
    }

    private def becomeInitialized(props: Props): Unit = {
      log.info("Initializing player with name {}", self.path.name)
      val player = context.actorOf(props, self.path.name)
      context become PartialFunction(player.forward) // from now on send to this actor.
    }
  }
}

class PlayerSharding(system: ExtendedActorSystem) extends Extension {

  import PlayerSharding._

  // This could be done dynamically but we specify this as a design constraint in this example
  private val shardCount = Settings(system).playerRegistry.shardCount

  // NOTE PROPS CANNOT TAKE PARAMS ( random players vs. simple players which are different classes, what do we do? )
  // need to create another actor: Player (line 33) which started using the Initialize message which contains the
  // actual props that we need!

  // use start when you need to communicate and host
  def start(): Unit =
    ClusterSharding(system).start(
      Player.typeName,
      Player.props,
      ClusterShardingSettings(system).withRole("player-registry"),
      Player.extractEntityId,
      Player.extractShardId(shardCount))

  // use startProxy for communicate but not host
  // special region that does not host shards, it will ask the shard coordinator when it needs to send messages to
  // shards.
  def startProxy(): Unit =
    ClusterSharding(system).startProxy(
      Player.typeName,
      Some("player-registry"), // role
      Player.extractEntityId,
      Player.extractShardId(shardCount)
    )

  def tellPlayer(name: String, message: Any)(implicit sender: ActorRef = ActorRef.noSender): Unit =
    shardRegion ! Player.Envelope(name, message)

  private def shardRegion: ActorRef =
    ClusterSharding(system).shardRegion(Player.typeName)

}