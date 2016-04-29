package stepmaniacv

import java.util.concurrent.Executors
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

import akka.NotUsed
import akka.actor.{ Props, ActorSystem, ActorLogging, ActorRef }
import akka.stream.{ OverflowStrategy }
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{ Cancel, Request }
import akka.stream.scaladsl.Source
import org.bytedeco.javacv._
import FrameGrabber.ImageMode

object Screen {

  def sourceTick(deviceId: String, format: String): Source[Frame, _] = {
    val grabber = buildGrabber(deviceId, format)
    Source.tick(0.second, 1.second / 60, "")
      .map{ _ =>
        // println(s"[${Thread.currentThread().getName}] grab image")
        grabber.grab()
      }
  }

  def buildGrabber(deviceId: String, format: String, dim: Dimensions = Dimensions.sd): FrameGrabber = {
    val g = FFmpegFrameGrabber.createDefault(deviceId)
    g.setFormat(format)
    g.setImageWidth(dim.width)
    g.setImageHeight(dim.height)
    g.setFrameRate(100)
    g.start()
    println(s"Grabber gamma ${g.getGamma()}")
    g
  }

  def fromVideo(file: java.io.File, fps: Int = 60): Source[Frame, _] = {
    val grabber = new FFmpegFrameGrabber(file)
    grabber.start()
    println(s"Grabber gamma ${grabber.getGamma()}")
    Source.tick(0.second, 1.second / fps, "")
      .map { _ =>
        grabber.grab()
      }
  }
}

case class Dimensions(width: Int, height: Int)
object Dimensions {
  val sd = Dimensions(800, 600)
}
