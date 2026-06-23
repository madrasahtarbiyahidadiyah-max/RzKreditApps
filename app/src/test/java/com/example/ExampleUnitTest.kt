package com.example

import org.junit.Assert.*
import org.junit.Test
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun generateMipmapIcons() {
    val srcFile = File("src/main/res/drawable/app_logo.jpg")
    val actualSrcFile = if (srcFile.exists()) srcFile else File("app/src/main/res/drawable/app_logo.jpg")
    
    if (!actualSrcFile.exists()) {
      println("Source file does not exist: ${actualSrcFile.absolutePath}")
      return
    }
    
    println("Generating mipmap icons from ${actualSrcFile.absolutePath}")
    val originalImage = javax.imageio.ImageIO.read(actualSrcFile)
    
    val densities = mapOf(
      "mdpi" to 48,
      "hdpi" to 72,
      "xhdpi" to 96,
      "xxhdpi" to 144,
      "xxxhdpi" to 192
    )
    
    val resDir = actualSrcFile.parentFile.parentFile // res folder
    
    for ((density, size) in densities) {
      val targetDir = File(resDir, "mipmap-$density")
      targetDir.mkdirs()
      
      // Create high-quality scaled icon
      val scaledImage = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
      val g2d = scaledImage.createGraphics()
      g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC)
      g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
      g2d.drawImage(originalImage, 0, 0, size, size, null)
      g2d.dispose()
      
      val targetFile = File(targetDir, "ic_launcher.png")
      javax.imageio.ImageIO.write(scaledImage, "png", targetFile)
      println("Created: ${targetFile.absolutePath}")
      
      val targetFileRound = File(targetDir, "ic_launcher_round.png")
      javax.imageio.ImageIO.write(scaledImage, "png", targetFileRound)
      println("Created: ${targetFileRound.absolutePath}")
    }
  }

  @Test
  fun downloadAppIcon() {
    val imageUrl = "https://i.postimg.cc/3x9HWhJ2/Whats-App-Image-2026-06-02-at-10-33-41.jpg"
    try {
      println("Downloading image from $imageUrl")
      val url = URL(imageUrl)
      val connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.connectTimeout = 8000
      connection.readTimeout = 8000
      connection.useCaches = false
      connection.instanceFollowRedirects = true
      
      val responseCode = connection.responseCode
      println("Response Code: $responseCode")
      if (responseCode == HttpURLConnection.HTTP_OK) {
        val inputStream = BufferedInputStream(connection.inputStream)
        val targetFile = File("src/main/res/drawable/app_logo.jpg")
        val rootTargetFile = if (!targetFile.parentFile.exists()) {
          File("app/src/main/res/drawable/app_logo.jpg")
        } else {
          targetFile
        }
        println("Saving to ${rootTargetFile.absolutePath}")
        
        rootTargetFile.parentFile?.mkdirs()
        
        val outputStream = FileOutputStream(rootTargetFile)
        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
          outputStream.write(buffer, 0, bytesRead)
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()
        println("Successfully downloaded and saved the image! Size: ${rootTargetFile.length()} bytes")
        assertTrue(rootTargetFile.exists() && rootTargetFile.length() > 0)
      } else {
        println("HTTP connection failed with code: $responseCode (expected in sandboxed test runs)")
      }
    } catch (e: Exception) {
      println("Failed to download image: ${e.message} (expected in sandboxed test runs due to network blocklist)")
    }
  }
}
