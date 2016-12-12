/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

class GameSpec extends BaseSpec {

  import Game._

  "Calling move" should {
    "return the proper Position" in {
      Position(0, 0) move Direction.North shouldEqual Position(0, 1)
      Position(0, 0) move Direction.NorthEast shouldEqual Position(1, 1)
      Position(0, 0) move Direction.East shouldEqual Position(1, 0)
      Position(0, 0) move Direction.SouthEast shouldEqual Position(1, -1)
      Position(0, 0) move Direction.South shouldEqual Position(0, -1)
      Position(0, 0) move Direction.SouthWest shouldEqual Position(-1, -1)
      Position(0, 0) move Direction.West shouldEqual Position(-1, 0)
      Position(0, 0) move Direction.NorthWest shouldEqual Position(-1, 1)
    }
  }

  "Calling - or minus" should {
    "" in {
      Position(3, 4) - Position(1, 2) shouldEqual Position(2, 2)
      Position(3, 4) minus Position(1, 2) shouldEqual Position(2, 2)
    }
  }

  "Calling direction" should {
    "throw an IllegalArgumentException for (0, 0)" in {
      an[IllegalArgumentException] should be thrownBy Position(0, 0).direction
    }
    "return the proper value for exact positions other that (0, 0)" in {
      Position(0, 1).direction shouldEqual Direction.North
      Position(2, 2).direction shouldEqual Direction.NorthEast
      Position(3, 0).direction shouldEqual Direction.East
      Position(2, -2).direction shouldEqual Direction.SouthEast
      Position(0, -2).direction shouldEqual Direction.South
      Position(-1, -1).direction shouldEqual Direction.SouthWest
      Position(-3, 0).direction shouldEqual Direction.West
      Position(-2, 2).direction shouldEqual Direction.NorthWest
    }
    "return the proper value for non-exact positions other that (0, 0)" in {
      Position(4, 10).direction shouldEqual Direction.North
      Position(5, 10).direction shouldEqual Direction.North
      Position(6, 10).direction shouldEqual Direction.NorthEast
      Position(10, 6).direction shouldEqual Direction.NorthEast
      Position(10, 5).direction shouldEqual Direction.East
      Position(10, 4).direction shouldEqual Direction.East
    }
  }
}
