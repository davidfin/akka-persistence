cluster-sharding

## Exercise 6 > Cluster Sharding

In this exercise we use a **Cluster Sharding** to split off the `Player` actors. In order to do this, we will focus on changing the following:

- `application.conf`:
    - add the `player-registry` section with a `shard-count` of 20.
- `PlayerSharding.scala` (create this akka extension)

```scala
package com.typesafe.training.akkacollect

import akka.actor.{ Actor, ActorLogging, ActorRef, ExtendedActorSystem, Extension, ExtensionKey, Props }
import akka.cluster.sharding.{ClusterShardingSettings, ClusterSharding, ShardRegion}

object PlayerSharding extends ExtensionKey[PlayerSharding] {

  object Player {

    case class Initialize(props: Props)

    case class Envelope(name: String, payload: Any)

    val typeName: String =
      "player"

    val extractEntityId: ShardRegion.ExtractEntityId = {
      case Player.Envelope(name, payload) => (name, payload)
    }

    def extractShardId(shardCount: Int): ShardRegion.ExtractShardId= {
      case Player.Envelope(name, _) => name.hashCode % shardCount toString
    }

    def props: Props =
      Props(new Player)
  }

  class Player extends Actor with ActorLogging {

    import Player._

    override def receive: Receive = {
      case Initialize(props) => becomeInitialized(props)
    }

    private def becomeInitialized(props: Props): Unit = {
      log.info("Initializing player with name {}", self.path.name)
      val player = context.actorOf(props, self.path.name)
      context become PartialFunction(player.forward)
    }
  }
}

class PlayerSharding(system: ExtendedActorSystem) extends Extension {

  import PlayerSharding._

  private val shardCount = Settings(system).playerRegistry.shardCount

  def start(): Unit =
    ClusterSharding(system).start(
      Player.typeName,
      Player.props,
      ClusterShardingSettings(system).withRole("player-registry"),
      Player.extractEntityId,
      Player.extractShardId(shardCount))

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
```

- `Game.scala`:
    - The `Player` actors are no longer referenced by `ActorRef`.
    - Create a protected `tellPlayer` method that calls the `tellPlayer` method from `PlayerSharding.scala`.
    - In `def sendMakeMove` call your `tellPlayer` method.
    - **HINT**: Lookup cluster sharding to understand how to reference a sharded actor.
    - **HINT**: You might have to make a change to `def initialPositions`.
- `GameEngineApp.scala`:
    - You will need to override the `initialize` method from BaseApp to include:
        - Creates the shared journal with something like `... (SharedJournalSetter.props, "shared-journal-setter")`.
        - `PlayerSharding(system).startProxy()`.
- `PlayerRegistry.scala`:
    - **HINT**: Remember, you no longer refer to `Player` as an `ActorRef`.
    - You are going to need a local reference (think `Set`) for players.
    - In `def createPlayer` you will need to use `tellPlayer`.
    - **HINT**: You will probably need to change `isNameTaken`.
- `PlayerRegistryApp.scala`:
    - You will need to override the `initialize` method from BaseApp to include:
        - Creates the shared journal with something like `... (SharedJournalSetter.props, "shared-journal-setter")`.
        - `PlayerSharding(system).start()`.
- `Settings.scala`:
    - You will need to add `object playerRegistry` which has a `shardCount`.
    - **HINT**: Where do you think you will get `shard-count` from?
- `Tournament.scala`:
    - **HINT**: Remember, you no longer refer to `Player` as an `ActorRef`.

We have taken a big step in `elasticity` by introducing **cluster sharding**. Now we can scala across machines to support many players. Let's test our solution.

- Use the `sj` command alias to bootstrap `ShareJournal`.
- Open a second terminal window and start another `sbt` session:
    - Use the `sr` command alias to bootstrap `ScoresRepository`.
- Open a third terminal window and start another `sbt` session:
    - Use the `ge` command alias to bootstrap the `GameEngine`.
- Open a fourth terminal window and start another `sbt` session:
    - Use the `pr` command alias to bootstrap the `PlayerRegistry`.
    - From the `PlayerRegistry` prompt, create two `simple` players.
- Open up a fifth terminal session:
    - `tail -f akka-collect.log` and verify the Akka Collect is working as expected.
- Use the `test` command to verify everything works as expected.
- Use the `koan next` command to move to the next exercise.
