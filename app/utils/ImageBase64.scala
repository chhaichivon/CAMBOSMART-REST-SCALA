package utils

import java.io.{ File, FileInputStream, FileOutputStream }
import sun.misc.{ BASE64Encoder, BASE64Decoder }

object ImageBase64 {
  def filePath = new java.io.File(".").getCanonicalPath + "/public/images/"
  def profilePath = new java.io.File(".").getCanonicalPath.replace("cambosmart-rest-scala", "cambosmart-web-react") + "/public/images/profiles/"
  def productFilePath = new java.io.File(".").getCanonicalPath + "/public/images/product/"
  def imagePath = new java.io.File(".").getCanonicalPath.replace("cambosmart-rest-scala", "cambosmart-web-react") + "/public/images/products/"
  def advertisementPath = new java.io.File(".").getCanonicalPath.replace("cambosmart-rest-scala", "cambosmart-web-react") + "/public/images/advertisements/"
  def storeBannerPath = new java.io.File(".").getCanonicalPath.replace("cambosmart-rest-scala", "cambosmart-web-react") + "/public/images/stores/"

  def encodeImage(filename: String): String = {
    val file = new File(filename)
    val in = new FileInputStream(file)
    val bytes = new Array[Byte](file.length.toInt)
    in.read(bytes)
    in.close()

    //val encodedFile = new File(filename + ".base64")
    val encoded =
      new BASE64Encoder()
        .encode(bytes)
        .replace("\n", "")
        .replace("\r", "")
    //val encodedStream = new FileOutputStream(encodedFile)
    //encodedStream.write(encoded.getBytes)
    //encodedStream.close()
    encoded
  }

  def decodeImage(filename: String, encoded: String) = {
    val decodedFile = new File(filename + ".decoded")
    val decoded = new BASE64Decoder().decodeBuffer(encoded)
    val decodedStream = new FileOutputStream(decodedFile)
    decodedStream.write(decoded)
    decodedStream.close()
    decoded
  }

  implicit class FileMonad(f: File) {
    def check = if (f.exists) Some(f) else None //returns "Maybe" monad
    def remove = if (f.delete()) Some(f) else None //returns "Maybe" monad
  }

  def removeFile(path: String) = {
    for {
      foundFile <- new File(path).check
      deletedFile <- foundFile.remove
    } yield deletedFile
  }

  def image64(img: String): String = {
    if (img == "") {
      img
    } else {
      encodeImage(filePath + img)
    }
  }
  def imageP64(img: String): String = {
    if (img == "") {
      img
    } else {
      encodeImage(productFilePath + img)
    }
  }
}

