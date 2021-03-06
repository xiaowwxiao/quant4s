/**
  *
  */
package org.quant4s.actors.strategy

import akka.actor.{Actor, ActorRef, Props}
import org.quant4s.actors._
import org.quant4s.actors.persistence.StrategyPersistorActor
import org.quant4s.rest.{HttpServer, Strategy}
import org.quant4s.actors._

import scala.collection.mutable
case class UpdatePortfolio(strategy: Strategy)


/**
  * 1、策略的CRUD 操作
  * 2、买入|卖出股票
  * 3、
  */
class StrategiesManagerActor extends Actor{
  val persisRef = context.actorSelection("/user/" + StrategyPersistorActor.path)
  val restRef = context.actorSelection("/user/" + HttpServer.path)

  var strategyRefs = new mutable.HashMap[Int, ActorRef]()

  override def receive: Receive = {
    // 策略相关操作
    case s: NewStrategy => _createStrategy(s.strategy)
    case s: DeleteStrategy => persisRef ! new DeleteStrategy(s.id)
    case s: ListStrategies => persisRef ! new ListStrategies()

    // 持久化层返回的一步消息，通知WEB层，构建缓存
    case s: Array[Strategy] => _createStrategyActors(s)
    case s: Strategy => restRef ! s
  }

  private def _createStrategy(strategy: Strategy) = {
    if(!strategyRefs.contains(strategy.id)) {
      val ref = context.actorOf(StrategyActor.props(strategy.id), strategy.id.toString)
      strategyRefs += (strategy.id -> ref)
      persisRef ! new NewStrategy(strategy)
    }
  }

  private def _createStrategyActors(strategies: Array[Strategy]): Unit = {
    for(s <- strategies) {
      if(!strategyRefs.contains(s.id)) {
        val ref = context.actorOf(StrategyActor.props(s.id), s.id.toString)
        strategyRefs += (s.id -> ref)
      }
    }

    restRef ! strategies
  }

}

object StrategiesManagerActor {
  def props = Props(classOf[StrategiesManagerActor])

  val path = PATH_STRATEGY_MANAGER_ACTOR
}
