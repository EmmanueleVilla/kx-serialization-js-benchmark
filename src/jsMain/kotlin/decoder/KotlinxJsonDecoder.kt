package decoder

import kotlinx.serialization.builtins.ListSerializer
import protocol.ServerEvent

object KotlinxJsonDecoder : Decoder {

  private val serializer = ListSerializer(ServerEvent.serializer())

  override fun decode(string: String): List<ServerEvent> {
    return KotlinxJson.json.decodeFromString(serializer, string)
  }
}
