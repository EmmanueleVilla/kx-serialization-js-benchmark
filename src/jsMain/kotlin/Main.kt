import decoder.*
import jsonData.DataJson200KB
import kotlinx.browser.document
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

private val competitors = listOf(
  Triple(KotlinxJsonDecoder, "kotlinx 200KB", "kotlinx"),
  Triple(KotlinxDopJsonDecoder, "kotlinx dynamic 200KB", "kotlinx decodeFromDynamic"),
  Triple(ManualWhenJsonDecoder, "manual when 200KB", "manual when"),
  Triple(ManualKotlinMapJsonDecoder, "manual kotlin map 200KB", "manual kotlin map"),
  Triple(ManualJsMapJsonDecoder, "manual js map 200KB", "manual js map"),
)

private const val WARM_UP_REPETITIONS = 50
private const val REPETITIONS = 100

// works both in browsers and in Node.js
private fun nowMs(): Double = js("performance.now()").unsafeCast<Double>()

private inline fun measureTimeMs(crossinline block: () -> Unit): Double {
  val startMs = nowMs()
  block()
  val endMs = nowMs()
  return endMs - startMs
}

private fun Double.format() = asDynamic().toFixed(2).unsafeCast<String>()

fun warmUp(decoder: Decoder, jsonData: String) {
  repeat(WARM_UP_REPETITIONS) {
    decoder.decode(jsonData)
  }
}

fun measure(decoder: Decoder, jsonData: String, name: String): String {
  val times = List(REPETITIONS) {
    measureTimeMs {
      decoder.decode(jsonData)
    }
  }.sorted()

  val avg = times.sum() / times.size
  val median = times[times.size / 2]
  return "$name: avg = ${avg.format()} ms, median = ${median.format()} ms, " +
    "(min, max) = (${times.first().format()} ms, ${times.last().format()} ms)"
}

fun addResult(place: HTMLElement, decoder: Decoder, jsonData: String, name: String): (Event) -> Unit = {
  warmUp(decoder, jsonData)

  kotlinx.browser.window.setTimeout(
    handler = {
      val result = measure(decoder, jsonData, name)
      place.append.div { +result }
    },
    timeout = 1000
  )
}

fun main() {
  val isBrowser = js("typeof document !== 'undefined'").unsafeCast<Boolean>()

  if (!isBrowser) {
    // sanity check: every competitor must decode the same events
    val reference = KotlinxJsonDecoder.decode(DataJson200KB.data)
    competitors.forEach { (decoder, name, _) ->
      check(decoder.decode(DataJson200KB.data) == reference) { "$name decodes differently from kotlinx" }
    }
    println("All decoders produce equal results (${reference.size} events)")

    competitors.forEach { (decoder, name, _) ->
      warmUp(decoder, DataJson200KB.data)
      println(measure(decoder, DataJson200KB.data, name))
    }
    return
  }

  document.body!!.append.div {
    competitors.forEach { (decoder, name, label) ->
      button {
        onClickFunction = addResult(document.body!!, decoder, DataJson200KB.data, name)
        +label
      }
    }
  }
}
