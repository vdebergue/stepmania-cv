package stepmaniacv

import java.io.File

case class Config(
  image: Option[File] = None,
  video: Option[File] = None,
  isLive: Boolean = false,
  captureFormat: String = "avfoundation",
  captureDevice: Option[String] = None
)

object Config {
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
      }

    cmd("image")
      .text("analyze an image")
      .children {
        arg[File]("<image>")
          .required()
          .action { (x, c) => c.copy(image = Some(x)) }
      }

    cmd("video")
      .text("analyze a video")
      .children {
        arg[File]("<video>")
          .required()
          .action { (x, c) => c.copy(video = Some(x)) }
      }

    checkConfig { c =>
      if (c.isLive) success
      else if (c.image.isDefined && c.video.isEmpty) success
      else if (c.image.isEmpty && c.video.isDefined) success
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
