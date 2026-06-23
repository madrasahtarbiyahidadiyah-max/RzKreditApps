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
  fun generateAllIcons() {
    val imageUrl = "https://i.postimg.cc/vT94c1wx/logo-rzkredit.jpg"
    val resDir = File("app/src/main/res")
    val actualResDir = if (resDir.exists()) resDir else File("src/main/res")
    
    val drawableDir = File(actualResDir, "drawable")
    drawableDir.mkdirs()
    
    val targetPng = File(drawableDir, "app_logo.png")
    val tempJpg = File(drawableDir, "temp_logo.jpg")
    
    var imageDownloaded = false
    
    // 1. Try to download the latest image
    try {
      println("Downloading image from $imageUrl")
      val url = URL(imageUrl)
      val connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = "GET"
      connection.connectTimeout = 10000
      connection.readTimeout = 10000
      connection.useCaches = false
      connection.instanceFollowRedirects = true
      
      val responseCode = connection.responseCode
      println("Response Code: $responseCode")
      if (responseCode == HttpURLConnection.HTTP_OK) {
        val inputStream = BufferedInputStream(connection.inputStream)
        val outputStream = FileOutputStream(tempJpg)
        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
          outputStream.write(buffer, 0, bytesRead)
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()
        println("Downloaded temporary image to ${tempJpg.absolutePath}")
        imageDownloaded = true
      }
    } catch (e: Exception) {
      println("Failed to download image via network: ${e.message}")
    }
    
    // 2. Load the source image (downloaded one or fallback existing jpg)
    val srcJpg = File(drawableDir, "app_logo.jpg")
    val sourceFile = if (imageDownloaded && tempJpg.exists() && tempJpg.length() > 0) {
      tempJpg
    } else if (srcJpg.exists()) {
      println("Using existing fallback app_logo.jpg")
      srcJpg
    } else {
      println("No source image found to generate icons!")
      return
    }
    
    // Decode and save as high-quality PNG
    val originalImage = javax.imageio.ImageIO.read(sourceFile)
    if (originalImage == null) {
      println("Failed to decode the source image!")
      return
    }
    
    // Write out the PNG resource
    javax.imageio.ImageIO.write(originalImage, "png", targetPng)
    println("Saved high-quality app_logo.png to ${targetPng.absolutePath}")
    
    // Clean up temporary files or old JPG to avoid duplicate resource names
    if (tempJpg.exists()) tempJpg.delete()
    if (srcJpg.exists()) {
      srcJpg.delete()
      println("Deleted legacy app_logo.jpg to prevent resource duplication")
    }
    
    // 3. Generate mipmaps (ic_launcher.png and ic_launcher_round.png)
    val densities = mapOf(
      "mdpi" to 48,
      "hdpi" to 72,
      "xhdpi" to 96,
      "xxhdpi" to 144,
      "xxxhdpi" to 192
    )
    
    for ((density, size) in densities) {
      val mipmapDir = File(actualResDir, "mipmap-$density")
      mipmapDir.mkdirs()
      
      // Create high-quality scaled icon
      val scaledImage = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
      val g2d = scaledImage.createGraphics()
      g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC)
      g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
      g2d.drawImage(originalImage, 0, 0, size, size, null)
      g2d.dispose()
      
      val targetFile = File(mipmapDir, "ic_launcher.png")
      javax.imageio.ImageIO.write(scaledImage, "png", targetFile)
      println("Created: ${targetFile.absolutePath}")
      
      // Also generate a masked round version if desired, but since original is already round, we just use same high quality image
      val targetFileRound = File(mipmapDir, "ic_launcher_round.png")
      javax.imageio.ImageIO.write(scaledImage, "png", targetFileRound)
      println("Created: ${targetFileRound.absolutePath}")
    }
    
    assertTrue(targetPng.exists() && targetPng.length() > 0)
  }
}
