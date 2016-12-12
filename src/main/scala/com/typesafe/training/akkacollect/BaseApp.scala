/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

import akka.actor.{ ActorRef, ActorSystem }
import scala.collection.breakOut
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object BaseApp {

  val Opt = """(\S+)=(\S+)""".r

  def argsToOpts(args: Seq[String]): Map[String, String] =
    args.collect { case Opt(key, value) => key -> value }(breakOut)

  def applySystemProperties(options: Map[String, String]): Unit =
    for ((key, value) <- options if key startsWith "-D")
      System.setProperty(key substring 2, value)
}

abstract class BaseApp {

  import BaseApp._

  def main(args: Array[String]): Unit = {
    val opts = argsToOpts(args.toList)
    applySystemProperties(opts)
    val name = opts.getOrElse("name", "akkollect")

    val system = ActorSystem(s"$name-system")
    val settings = Settings(system)
    initialize(system, settings)
    val top = createTop(system, settings)

    system.log.warning(f"{} running%nEnter "
      + Console.BLUE + "commands" + Console.RESET
      + " into the terminal: "
      + Console.BLUE + "[e.g. `s` or `shutdown`]" + Console.RESET, getClass.getSimpleName)
    commandLoop(system, settings, top)
    Await.ready(system.whenTerminated, Duration.Inf)
  }

  def initialize(system: ActorSystem, settings: Settings): Unit =
    ()

  def createTop(system: ActorSystem, settings: Settings): ActorRef =
    system.deadLetters

  protected def commandLoop(system: ActorSystem, settings: Settings, top: ActorRef): Unit
}
