/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

import akka.actor.{ PossiblyHarmful, Props }
import scala.util.parsing.combinator.RegexParsers

trait Terminal {

  sealed trait Command extends PossiblyHarmful

  object Command {

    case class Register(name: String, props: Props, count: Int = 1) extends Command

    case object Shutdown extends Command

    case class Unknown(command: String, message: String) extends Command

    def apply(command: String): Command =
      CommandParser.parseAsCommand(command)
  }

  object CommandParser extends RegexParsers {

    def parseAsCommand(s: String): Command =
      parseAll(parser, s) match {
        case Success(command, _)   => command
        case NoSuccess(message, _) => Command.Unknown(s, message)
      }

    def register: Parser[Command] =
      ("register|r".r ~> ("""\w+""".r ~ (random | simple) ~ opt(count))) ^^ {
        case name ~ props ~ Some(count) => Command.Register(name, props, count)
        case name ~ props ~ None        => Command.Register(name, props)
      }

    def shutdown: Parser[Command] =
      "shutdown|s".r ^^ (_ => Command.Shutdown)

    private def random: Parser[Props] =
      "random|r".r ^^ (_ => RandomPlayer.props)

    private def simple: Parser[Props] =
      "simple|s".r ^^ (_ => SimplePlayer.props)

    private def count: Parser[Int] =
      """\d{1,9}""".r ^^ (_.toInt)
  }

  protected val parser: CommandParser.Parser[Command]
}
