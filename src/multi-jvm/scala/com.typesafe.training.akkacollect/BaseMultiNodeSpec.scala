package com.typesafe.training.akkacollect

import org.scalatest.{ BeforeAndAfterAll, WordSpecLike }
import org.scalatest.Matchers
import akka.remote.testkit.MultiNodeSpecCallbacks

trait BaseMultiNodeSpec
  extends MultiNodeSpecCallbacks
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()

}