package stepmaniacv

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{ Actor, ActorSystem, Props }
import akka.stream.scaladsl.{ Flow }

class RateMonitor[T](printRate: FiniteDuration, name: String) extends Actor {
  import RateMonitor.Print
  var count = 0
  override def preStart(): Unit = {
    context.system.scheduler.schedule(printRate, printRate, self, Print)
  }

  def receive = {
    case Print =>
      val rate = (count + 0.0) / printRate.toSeconds
      println(s"[$name] rate = $rate / sec ")
      count = 0
    case _ => count += 1
  }
}

object RateMonitor {
  case object Print
  case object Tick
  def asFlow[T](printRate: FiniteDuration, name: String)(implicit system: ActorSystem): Flow[T, T, _] = {
    val actorRef = createActor(printRate, name)
    Flow[T].map { t =>
      actorRef ! Tick
      t
    }
  }

  def createActor(printRate: FiniteDuration, name: String)(implicit system: ActorSystem) = {
    system.actorOf(Props(new RateMonitor(printRate, name)))
  }
}
