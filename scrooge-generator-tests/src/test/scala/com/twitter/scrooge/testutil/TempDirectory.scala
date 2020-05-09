package com.twitter.scrooge.testutil

import com.twitter.io.Files
import java.io.File

object TempDirectory {

  /**
   * Create a new temporary directory in the current directory,
   * which will be deleted upon the exit of the VM.
   *
   * @return File representing the directory
   */
  def create(dir: Option[File], deleteAtExit: Boolean = true): File = {
    val file = dir match {
      case Some(d) => File.createTempFile("scrooge", "temp", d)
      case None => File.createTempFile("scrooge", "temp")
    }
    file.delete()
    file.mkdir()

    if (deleteAtExit)
      Runtime
        .getRuntime().addShutdownHook(new Thread {
          override def run(): Unit = {
            Files.delete(file)
          }
        })

    file
  }
}
