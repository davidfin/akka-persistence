/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training.akkacollect

import org.scalatest.{ Matchers, WordSpec }

class PackageSpec extends BaseSpec {

  "Calling toOddInt" should {
    "return the next odd integer for even values" in {
      0.0.toOddInt shouldEqual 1
      2.2.toOddInt shouldEqual 3
      4.8.toOddInt shouldEqual 5
    }
    "return the current odd integer for odd values" in {
      1.0.toOddInt shouldEqual 1
      3.2.toOddInt shouldEqual 3
      5.8.toOddInt shouldEqual 5
    }
  }

  "Calling merge" should {
    "merge disjunct maps" in {
      val map1 = Map('a -> 1, 'b -> 2)
      val map2 = Map('c -> 3, 'd -> 4)
      map1 merge map2 shouldEqual Map('a -> 1, 'b -> 2, 'c -> 3, 'd -> 4)
    }
    "merge maps with overlapping keys" in {
      val map1 = Map('a -> 1, 'b -> 2)
      val map2 = Map('b -> 3, 'c -> 4)
      map1 merge map2 shouldEqual Map('a -> 1, 'b -> 5, 'c -> 4)
    }
  }
}
