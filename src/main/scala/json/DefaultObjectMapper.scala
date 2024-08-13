package json

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

class DefaultObjectMapper extends ObjectMapper with ScalaObjectMapper {
  registerModule(new DefaultScalaModule)
  configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}
