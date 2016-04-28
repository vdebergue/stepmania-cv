package stepmaniacv

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}
import org.bytedeco.javacv.{CanvasFrame, Frame}
import org.bytedeco.javacpp.opencv_core.{CvBox2D, IplImage, Mat}

object Main {
  def main(args: Array[String]): Unit = {
    val config = Config.parseArgs(args)

    if (config.isLive) {
      startLive(config)
    }
  }

  def startLive(config: Config) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    val source = Screen.sourceTick(config.captureDevice.get, config.captureFormat)

    // out
    val canvas = new CanvasFrame("screen", 1)
    canvas.setCanvasScale(0.5)
    canvas.setCanvasSize(400,300)
    canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)

    def displayImage(frame: Frame): Frame = {
      if (frame.imageWidth > 0 && frame.imageHeight > 0) {
        // println(s"[${Thread.currentThread().getName}] Display image")
        canvas.showImage(frame)
      }
      frame
    }
    val ratePrint = 2.seconds

    val graph = source
      // .map(logFrame)
      .via(RateMonitor.asFlow[Frame](ratePrint, "grab"))
      .map(displayImage)
      .via(RateMonitor.asFlow[Frame](ratePrint, "display"))
      .map(MediaConversion.toIplImage)
      // .map(logMat)
      // .map(logImage)
      .via(RateMonitor.asFlow[IplImage](ratePrint, "converted"))
      .map(i => WithGrey(i, MediaConversion.toGrayScale(i)))
      //      .map(logImage)
      .sliding(2, 1)
      .map { imgs =>
        val boxes = ImageProcessing.detectMotion(imgs(1).grey, imgs(0).grey)
        imgs(1) -> boxes.filter(b => b.size.width > 0 && b.size.height > 0)
      }
      .map{ t => logBoxes(t._2); t }
      .map(t => ImageProcessing.drawBoxes(t._1.orig, t._2))
      .map(MediaConversion.toFrame)
      .map(displayImage)
      .to(Sink.ignore)

    graph.run()
  }

  case class WithGrey(orig: IplImage, grey: IplImage)

  def logFrame(frame: Frame): Frame = {
    println(s"Got frame: ${frame.imageWidth}x${frame.imageHeight}")
    frame
  }

  def logMat(mat: Mat): Mat = {
    println(s"Got Mat: cols = ${mat.cols} - rows = ${mat.rows} - channels = ${mat.channels} - type = ${mat.`type`} ")
    mat
  }

  def logImage(img: IplImage): IplImage = {
    println(s"Got img: ${img.width}x${img.height} - channels = ${img.nChannels()}")
    img
  }

  def logBoxes(boxes: Seq[CvBox2D]): Seq[CvBox2D] = {
    println(s"Got ${boxes.size} boxes")
    if (boxes.size > 0) {
      println(s"center = ${boxes.head.center()} - size = ${boxes.head.size()}")
    }
    boxes
  }

}