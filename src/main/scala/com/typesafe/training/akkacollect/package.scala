/**
 * Copyright © 2014, 2015 Typesafe, Inc. All rights reserved. [http://www.typesafe.com]
 */

package com.typesafe.training

package object akkacollect {

  type Traversable[+A] = scala.collection.immutable.Traversable[A]

  type Iterable[+A] = scala.collection.immutable.Iterable[A]

  type Seq[+A] = scala.collection.immutable.Seq[A]

  type IndexedSeq[+A] = scala.collection.immutable.IndexedSeq[A]

  implicit class DoubleOps(val d: Double) extends AnyVal {
    def toOddInt: Int = {
      val floor = d.floor.toInt
      if (floor % 2 == 0)
        floor + 1
      else
        floor
    }
  }

  implicit class MapOps[A, B: Numeric](left: Map[A, B]) {
    def merge(right: Map[A, B]): Map[A, B] =
      if (left.size < right.size)
        right merge left
      else {
        val numeric = implicitly[Numeric[B]]
        val (rightDuplicates, rightUniques) = right partition { case (a, b) => left contains a }
        ((left ++ rightUniques) /: rightDuplicates) { case (acc, (a, b)) => acc + (a -> numeric.plus(acc(a), b)) }
      }
  }
/*
  implicit class VectorOps[A](as: Vector[A]) {
    def -(a: A): Vector[A] =
      as diff Vector(a)
  }

  val VNil: Vector[Nothing] =
    Vector.empty
*/
}
