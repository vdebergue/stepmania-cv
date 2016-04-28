package stepmaniacv

import java.util.function.Supplier

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacv.{ Frame, OpenCVFrameConverter }
import org.bytedeco.javacpp.opencv_imgproc.{ CV_BGR2GRAY, cvCvtColor, cvtColor, COLOR_BGR2GRAY }
import org.bytedeco.javacpp.opencv_imgcodecs.{ cvLoadImage, imread }

object MediaConversion {

  def toFrame(mat: Mat): Frame = frameToMatConverter.get().convert(mat)
  def toFrame(img: IplImage): Frame = frameToIplImageConverter.get().convert(img)

  def toMat(frame: Frame): Mat = frameToMatConverter.get().convert(frame)
  def toMat(file: java.io.File): Mat = imread(file.getAbsolutePath)
  def toIplImage(frame: Frame): IplImage = frameToIplImageConverter.get().convert(frame)
  def toIplImage(file: java.io.File): IplImage = cvLoadImage(file.getAbsolutePath)

  def toGrayScale(mat: Mat): Mat = {
    if (mat.channels() == 1) mat // already grey
    else {
      val gray = new Mat(mat.rows, mat.cols, 8)
      cvtColor(mat, gray, COLOR_BGR2GRAY, 1)
      gray
    }
  }

  def toGrayScale(img: IplImage): IplImage = {
    val grayImage = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 1)
    cvCvtColor(img, grayImage, CV_BGR2GRAY)
    grayImage
  }


  private val frameToMatConverter = ThreadLocal.withInitial(new Supplier[OpenCVFrameConverter.ToMat] {
    def get(): OpenCVFrameConverter.ToMat = new OpenCVFrameConverter.ToMat
  })

  private val frameToIplImageConverter = ThreadLocal.withInitial(new Supplier[OpenCVFrameConverter.ToIplImage] {
    def get(): OpenCVFrameConverter.ToIplImage = new OpenCVFrameConverter.ToIplImage
  })
}
