package stepmaniacv

import java.io.File

case class Config(
  imageRaw: Option[String] = None,
  imageParsed: Option[Config.SourceWithArrows] = None,
  videoRaw: Option[String] = None,
  videoParsed: Option[Config.SourceWithArrows] = None,
  isLive: Boolean = false,
  liveArrows: Option[Config.Arrows] = None,
  captureFormat: String = "avfoundation",
  captureDevice: Option[String] = None
) {
  def image = imageParsed.map(_.source)
  def video = videoParsed.map(_.source)
}

case class Arrow(name: String, x: Int, y: Int, w: Int, h: Int) {
  def x2 = x + w
  def y2 = y + h
}

object Config {

  case class Arrows(arrow1: Arrow, arrow2: Arrow, arrow3: Arrow, arrow4: Arrow)
  case class SourceWithArrows(source: File, arrows: Arrows)

  val parseSourceRx = """(^:+):(.+)""".r
  def parseSource(str: String): Option[SourceWithArrows] = str match {
    case parseSourceRx(source, arrowsStr) =>
      parseArrows(arrowsStr).map { arrows =>
        SourceWithArrows(source = new File(source), arrows = arrows)
      }
    case _ => None
  }

  val parseArrowsRx = """(\d+),(\d+),(\d+),(\d+);(\d+),(\d+),(\d+),(\d+);(\d+),(\d+),(\d+),(\d+);(\d+),(\d+),(\d+),(\d+)""".r
  def parseArrows(str: String): Option[Arrows] = str match {
    case parseArrowsRx(x1, y1, h1, w1, x2, y2, h2, w2, x3, y3, h3, w3, x4, y4, h4, w4) =>
      Some(Arrows(
        arrow1 = Arrow("left", x1.toInt, y1.toInt, h1.toInt, w1.toInt),
        arrow2 = Arrow("down", x2.toInt, y2.toInt, h2.toInt, w2.toInt),
        arrow3 = Arrow("up", x3.toInt, y3.toInt, h3.toInt, w3.toInt),
        arrow4 = Arrow("right", x4.toInt, y4.toInt, h4.toInt, w4.toInt)
      ))
    case _ => None
  }

  val parser = new scopt.OptionParser[Config]("stepmania-cv") {
    head("stepmania-cv")

    help("help")

    cmd("live")
      .text("start a live analysis of the screen")
      .action { (_, c) => c.copy(isLive = true) }
      .children {
        opt[String]('f', "format")
          .text("""Format to use to capture the screen. cf ffmpeg formats
            |        On OSX use "avfoundation" - default value
            |        On Linux use "x11grab"
            |        On windows use "dshow"
          """.stripMargin)
          .optional()
          .action { (x, c) => c.copy(captureFormat = x) }

        opt[String]('i', "input")
          .text("""Input device to capture - format depends on format. cf ffmpeg.
            |        Use "0" or "1" on OSX
          """.stripMargin)
          .required()
          .action{ (x, c) => c.copy(captureDevice = Some(x)) }

        opt[String]('a', "arrows")
          .text("x1,y1,w1,h1;x2,y2,w2,h2;x3,y3,w3,h3;x4,y4,w4,h4")
          .required()
          .action{ (x, c) => c.copy(liveArrows = parseArrows(x)) }
      }

    cmd("image")
      .text("analyze an image")
      .children {
        arg[String]("<image>")
          .text("source:x1,y1,w1,h1;x2,y2,w2,h2;x3,y3,w3,h3;x4,y4,w4,h4")
          .required()
          .action { (x, c) => c.copy(imageRaw = Some(x), imageParsed = parseSource(x)) }
      }

    cmd("video")
      .text("analyze a video")
      .children {
        arg[String]("<video>")
          .text("source:x1,y1,w1,h1;x2,y2,w2,h2;x3,y3,w3,h3;x4,y4,w4,h4")
          .required()
          .action { (x, c) => c.copy(videoRaw = Some(x), videoParsed = parseSource(x)) }
      }

    checkConfig { c =>
      if (c.isLive) {
        if (c.liveArrows.isDefined) success
        else failure("Invalid arrows format: valid is x1,y1,w1,h1;x2,y2,w2,h2;x3,y3,w3,h3;x4,y4,w4,h4")
      }
      else if (c.imageRaw.isDefined && c.videoRaw.isEmpty) {
        if (c.image.isDefined) success
        else failure("Invalid image format: valid is path:")
      }
      else if (c.imageRaw.isEmpty && c.videoRaw.isDefined) {
        if (c.video.isDefined) success
        else failure("Invalid video format: valid is path:x1,y1,w1,h1;x2,y2,w2,h2;x3,y3,w3,h3;x4,y4,w4,h4")
      }
      else failure("You must define either a live capture or from an image or a video")
    }
  }

  def parseArgs(args: Array[String]): Config = {
    parser.parse(args, Config()) match {
      case Some(config) => config
      case None => sys.exit(1)
    }
  }

}
