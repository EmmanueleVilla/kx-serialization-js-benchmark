package decoder

import kotlinx.serialization.json.Json

object KotlinxJson {

  val json = Json { useArrayPolymorphism = true }
}
