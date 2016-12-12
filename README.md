#akka-persistence

This project was developed as part of the advanced akka training course by LightBend.

Akka persistence solves the problem of restoring an actor's state upon JVM crash, supervisor crash, or migration in a cluster. 

## Event Sourcing 

All state changes (events) are persisted. In order to restore the current state of the actor, all of the events are replayed. The PersistentActors distinguish between commands and events and allows for persisting events duriung command handling. In this project, events are stored in a pluggable journal (SharedJournal). This is of course a toy example and cannot be used in production. Rather, Apache Cassandra should be used for the snapshot store for developing serious production-ready applications. 

The ```receiveCommand``` is for processing commands 

```scala 
override def receiveCommand: Receive = {
    case RegisterPlayer(name, _) if isNameTaken(name) => playerNameTaken(name: String)
    case RegisterPlayer(name, props)                  => registerPlayer(name, props)
    case GetPlayers                                   => sender() ! Players(players)
  }
```

whereas ```receiveRecover``` is for replayed events. 

```scala 
override def receiveRecover: Receive = {
    case playerRegistered: PlayerRegistered   => onPlayerRegistered(playerRegistered)
    case SnapshotOffer(_, snapshot: Snapshot) => players = snapshot.players
  }
```


```persist``` takes an event and a handler function which is invoked after the event has been persisted. 

```scala 
persist(PlayerRegistered(name)) { playerRegistered =>
      onPlayerRegistered(playerRegistered)
      createPlayer(name, props)
      sender() ! playerRegistered
}
```

A unique Id must be used in order to avoid collision in the pluggable journal. 
```scala 
override def persistenceId: String = "UniqueId"
```

Could use the actors' path for global uniqueness. 

Replaying a long history of events can be very ineffective. Therefore Akka Persistence lets you to create snapshots with saveSnapshot. 

## Recovery 


By default the persistentActor receives ALL events on start/restart. We can change this behaviour by overriding preStart and preRestart. Can initiate a recover by sending a Recover command.

```scala
persistentActor ! Recover(toSequenceNr = 123) 
```
arguments: 
fromSnapshot:  snapshot selection  (latest snapshot is default) 
toSequenceNr: defines an upper bound to restore an earlier state, by default Long.MaxValue is used

Useful values accessible bby persistentActors: lastSequenceNr and snapshotSequenceNr. 

## Exercise 
1. Implement PlayerRegistry as a persistent actor 
 1. implement receiveCommand, playerNameTaken, persist in registerPlayer, receiveRecover, saveSnapShot for every 100 players.
2. In PlayerSharding.scala , implement Player as a persistent actor. 
 1. receiveCommand, receiveRecover. 

