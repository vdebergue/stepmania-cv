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

  val RedColor = AbstractCvScalar.RED
  def drawBoxes(img: IplImage, boxes: Seq[CvBox2D]): IplImage = {
    val clonedImg = img.clone()
    for (box <- boxes) {
      cvEllipseBox(clonedImg, box, RedColor, 3, 8, 0)
    }
    clonedImg
  }
}