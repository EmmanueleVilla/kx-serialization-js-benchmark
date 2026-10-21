package decoder

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.decodeFromDynamic
import protocol.ServerEvent

object KotlinxDopJsonDecoder : Decoder {

  private val serializer = ListSerializer(ServerEvent.serializer())

  @OptIn(ExperimentalSerializationApi::class)
  override fun decode(string: String): List<ServerEvent> {
    // parse natively first, then map the JS object tree via kotlinx (successor of DynamicObjectParser)
    val nativelyParsedString = JSON.parse<dynamic>(string)

    return KotlinxJson.json.decodeFromDynamic(serializer, nativelyParsedString)
  }
}
