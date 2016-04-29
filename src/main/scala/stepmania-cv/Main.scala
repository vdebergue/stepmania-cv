package stepmaniacv

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.bytedeco.javacv.{CanvasFrame, Frame}
import org.bytedeco.javacpp.opencv_core.{CvBox2D, IplImage, Mat}

object Main {
  def main(args: Array[String]): Unit = {
    val config = Config.parseArgs(args)

    if (config.isLive) {
      startLive(config)
    } else if (config.image.isDefined) {
      analyzeImage(config.image.get, config)
    } else if (config.video.isDefined) {
      analyzeVideo(config.video.get, config)
    }
  }

  def analyzeSource(source: Source[Frame, _], config: Config): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

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
      // .map(displayImage)
      .via(RateMonitor.asFlow[Frame](ratePrint, "display"))
      .map(MediaConversion.toIplImage)
      .map { t => drawArrowBoxes(config, t); t }
      // .map(logMat)
      // .map(logImage)
      .via(RateMonitor.asFlow[IplImage](ratePrint, "converted"))
      .map(i => WithGrey(i, MediaConversion.toGrayScale(i)))
      //      .map(logImage)
      .sliding(2, 1)
      .map { imgs =>
        val boxes = ImageProcessing.detectMotion(imgs(1).grey, imgs(0).grey)
        imgs(1).orig -> boxes.filter(b => b.size.width > 0 && b.size.height > 0)
      }
      // .map{ t => logBoxes(t._2); t }
      .map { case (img, boxes) =>

        val arrowsAndBoxes = boxes.flatMap(arrowContainingBox(config))
        if (!arrowsAndBoxes.isEmpty) {
          val arrows = arrowsAndBoxes.map { case (arrow, _) => arrow.name }.toSet
          def a(name: String) = if (arrows(name)) name.toUpperCase else (" " * name.length)
          println(s"""${a("left")}  ${a("down")}  ${a("up")}  ${a("right")}""")
        }
        val containedBoxes = arrowsAndBoxes.map { case (_, box) => box }
        (img, containedBoxes)
      }
      .map { case (img, boxes) => ImageProcessing.drawBoxes(img, boxes); img }
      .map(MediaConversion.toFrame)
      .map(displayImage(_))
      .to(Sink.ignore)

    graph.run()
  }

  def drawArrowBoxes(config: Config, img: IplImage): Unit = {
    config.liveArrows.foreach { arrows =>
      ImageProcessing.drawRectangles(img, arrows.arrow1)
      ImageProcessing.drawRectangles(img, arrows.arrow2)
      ImageProcessing.drawRectangles(img, arrows.arrow3)
      ImageProcessing.drawRectangles(img, arrows.arrow4)
    }
  }

  def arrowContainingBox(config: Config)(box: CvBox2D): Option[(Arrow, CvBox2D)] = {
    def arrowIfContains(arrow: Arrow) = {
      if (ImageProcessing.contains(box, arrow)) Some(arrow -> box)
      else None
    }
    config.liveArrows.flatMap { arrows =>
      Seq(
        arrowIfContains(arrows.arrow1),
        arrowIfContains(arrows.arrow2),
        arrowIfContains(arrows.arrow3),
        arrowIfContains(arrows.arrow4)
      ).flatten.headOption
    }
  }

  def startLive(config: Config) {
    val source = Screen.sourceTick(config.captureDevice.get, config.captureFormat)
    analyzeSource(source, config)
  }

  def analyzeImage(image: java.io.File, config: Config) {
    val img = MediaConversion.toIplImage(image)
    val canvas = new CanvasFrame(image.getPath, 1)
    val withContours = ImageProcessing.drawContours(img)
    canvas.setSize(img.width /2 , img.height /2)
    canvas.setCanvasScale(0.5)
    canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
    canvas.showImage(MediaConversion.toFrame(withContours))
  }

  def analyzeVideo(video: java.io.File, config: Config) {
    val source = Screen.fromVideo(video, fps = 24)
    analyzeSource(source, config)
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
