package com.twitter.scrooge.testutil

import java.io.File
import com.twitter.io.Files

object TempDirectory {
  /**
   * Create a new temporary directory in the current directory,
   * which will be deleted upon the exit of the VM.
   *
   * @return File representing the directory
   */
  def create(dir: Option[File], deleteAtExit: Boolean = true): File = {
    val file = dir match {
      case Some(d) => File.createTempFile("temp", "dir", d)
      case None => File.createTempFile("temp", "dir")
    }
    file.delete()
    file.mkdir()

    if (deleteAtExit)
      Runtime.getRuntime().addShutdownHook(new Thread {
        override def run() {
          Files.delete(file)
        }
      })

    file
  }
}