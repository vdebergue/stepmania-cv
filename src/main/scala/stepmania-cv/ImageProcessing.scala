package stepmaniacv

import org.bytedeco.javacpp.Loader
import org.bytedeco.javacpp.helper.opencv_core.AbstractCvScalar
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._

import scala.collection.mutable

object ImageProcessing {

  lazy val storage = cvCreateMemStorage()

  // from 2 gray scales images
  def detectMotion(prevImg: IplImage, currImg: IplImage): Seq[CvBox2D] = {
    val diff = cvCreateImage(cvGetSize(currImg), IPL_DEPTH_8U, 1)
    cvAbsDiff(currImg, prevImg, diff)
    cvThreshold(diff, diff, 64, 255, CV_THRESH_BINARY)
    var contour = new CvSeq(null)
    cvFindContours(diff, storage, contour)
    val buffer = mutable.Buffer.empty[CvBox2D]
    while (contour != null && !contour.isNull) {
      if (contour.elem_size() > 0) {
        val box = cvMinAreaRect2(contour, storage)
        if (box != null) buffer += box
      }
      contour = contour.h_next()
    }
    buffer
  }

  def fromEnv(name: String, default: Int): Int = {
    sys.env.get(name).map(_.toInt).getOrElse(default)
  }
  def drawRectangles(img: IplImage, arrow: Arrow): Unit = {
    cvRectangle(
      img,
      new CvPoint(arrow.x, arrow.y),
      new CvPoint(arrow.x2, arrow.y2),
      new CvScalar(0, 255, 0, 1),  // color (BGRA)
      5, // thickness (filled rectangle if < 0)
      8, // lineType (8-connected line)
      0  // shift
    )
  }

  val RedColor = AbstractCvScalar.RED
  def drawBoxes(img: IplImage, boxes: Seq[CvBox2D]): Unit = {
    for (box <- boxes) {
      cvEllipseBox(img, box, RedColor, 3, 8, 0)
    }
  }

  def contains(box: CvBox2D, arrow: Arrow): Boolean = {
    val x = box.center.x
    val y = box.center.y
    x > arrow.x && x < arrow.x2 && y > arrow.y && y < arrow.y2
  }

  def drawContours(img: IplImage): IplImage = {
    val gray = MediaConversion.toGrayScale(img)
    val bwImage = cvCreateImage(cvGetSize(gray), IPL_DEPTH_8U, 1)
    cvThreshold(gray, bwImage, 64, 255, CV_THRESH_BINARY)
    var contour = new CvSeq(null)
    cvFindContours(bwImage, storage, contour)
    while (contour != null && !contour.isNull) {
      if (contour.elem_size() > 0) {
        val points = cvApproxPoly(contour, Loader.sizeof(classOf[CvContour]), storage, CV_POLY_APPROX_DP, cvArcLength(contour) * 0.033, 0)
        //val points = cvApproxPoly(contour, Loader.sizeof(classOf[CvContour]), storage, CV_POLY_APPROX_DP, cvContourPerimeter(contour) * 0.03, 0)
        val total = points.total()
        val area = cvContourArea(points)
        if (total == 9 && area > 6000) {
          val (minY, maxY) = getYRangeFromPoints(points)
          val (minX, maxX) = getXRangeFromPoints(points)
          if (minY < img.height / 2) {
            println(s"Got points for contour: ${points.total()} - area = ${area} - minY = ${minY} - minX = $minX - maxY = $maxY - maxX = $maxX")
            cvDrawContours(img, points, RedColor, RedColor, -1, -1, CV_AA, cvPoint(0,0))
          }
        }
      }
      contour = contour.h_next()
    }
    img
  }

  def getYRangeFromPoints(points: CvSeq): (Int, Int) = {
    var minY = Int.MaxValue
    var maxY = 0
    for(i <- 0 until points.total) {
      val point = new CvPoint(cvGetSeqElem(points, i))
      if (point.y < minY) minY = point.y
      if (point.y > maxY) maxY = point.y
    }
    minY -> maxY
  }

  def getXRangeFromPoints(points: CvSeq): (Int, Int) = {
    var minX = Int.MaxValue
    var maxX = 0
    for(i <- 0 until points.total) {
      val point = new CvPoint(cvGetSeqElem(points, i))
      if (point.x < minX) minX = point.x
      if (point.y > maxX) maxX = point.x
    }
    minX -> maxX
  }
}
