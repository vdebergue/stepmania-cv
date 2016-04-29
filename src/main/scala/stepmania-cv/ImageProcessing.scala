package stepmaniacv

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
}
