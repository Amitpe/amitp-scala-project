import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets

object LogFileReader extends App {
  val filename = "src/main/resources/log.text"

  // Read all lines from the file
  val lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8)

  // Process each line
  for (line <- lines.toArray) {
    println(line.toString)  // Or perform any other processing
  }
}
