# JSON decoding benchmarks for Kotlin/JS
Let's determine the best way of decoding JSON in K/JS!

## Data
Data classes from [Projector protocol](https://github.com/JetBrains/projector-client) are taken. A 200 KB JSON containing a snapshot of IntelliJ IDEA UI is used.

## Competitors
- `kotlinx.serialization.json.Json.decodeFromString` ([KotlinxJsonDecoder.kt](src/jsMain/kotlin/decoder/KotlinxJsonDecoder.kt)).
- `JSON.parse` + `kotlinx.serialization.json.Json.decodeFromDynamic` ([KotlinxDopJsonDecoder.kt](src/jsMain/kotlin/decoder/KotlinxDopJsonDecoder.kt)), the successor of `DynamicObjectParser`.
- A manual parser which uses `when` to handle polymorphism ([ManualWhenJsonDecoder.kt](src/jsMain/kotlin/decoder/ManualWhenJsonDecoder.kt)).
- A manual parser which uses `mapOf` to handle polymorphism ([ManualKotlinMapJsonDecoder.kt](src/jsMain/kotlin/decoder/ManualKotlinMapJsonDecoder.kt)).
- A manual parser which uses `json` to handle polymorphism ([ManualJsMapJsonDecoder.kt](src/jsMain/kotlin/decoder/ManualJsMapJsonDecoder.kt)).

## Running
### Online
<https://emmanuelevilla.github.io/kx-serialization-js-benchmark/index.html>.

### Local, browser
Build the project via `./gradlew jsBrowserDistribution`. After it, serve `build/dist/js/productionExecutable/` (for example with `python3 -m http.server`) and open `index.html` in a browser.

You will see 5 buttons, each launches its test.

### Local, Node.js
Run `./gradlew jsNodeProductionRun`. It checks that all decoders produce equal results and then runs all the tests in a row.

## Results
Absolute times come from different machines and runtimes, so they are not comparable between runs.
What matters is how much slower the slowest decoder (kotlinx `decodeFromString`) is than the fastest one (manual), measured on the same machine (by average time):

| Run | Slowest vs fastest |
|---|---|
| 2026: Kotlin 2.4.21, kotlinx.serialization 1.12.0 | ~11x, +1008% (5.32 ms vs 0.48 ms) |
| 2020: Kotlin 1.3.72, kotlinx.serialization 0.20.0 | ~18x, +1736% (40.4 ms vs 2.2 ms) |

### Recommendation for KMP projects
On the JS target, prefer `JSON.parse` + `decodeFromDynamic` over `decodeFromString`: in the 2026 run it is ~28% faster (3.84 ms vs 5.32 ms), while keeping the same serializers.
Hide it behind `expect`/`actual` so other targets keep using `decodeFromString`:
```kotlin
// commonMain
expect fun <T> Json.decodeFast(deserializer: DeserializationStrategy<T>, string: String): T

// jsMain
@OptIn(ExperimentalSerializationApi::class)
actual fun <T> Json.decodeFast(deserializer: DeserializationStrategy<T>, string: String): T =
  decodeFromDynamic(deserializer, JSON.parse(string))

// other targets
actual fun <T> Json.decodeFast(deserializer: DeserializationStrategy<T>, string: String): T =
  decodeFromString(deserializer, string)
```
Caveats: `decodeFromDynamic` is marked `@ExperimentalSerializationApi`, and `JSON.parse` truncates `Long` values beyond 2^53 (`decodeFromString` doesn't). If you need such values, send them as strings with `LongAsStringSerializer`.

### 2026: Kotlin 2.4.21, kotlinx.serialization 1.12.0
Node.js 24.16.0 (V8) on macOS 26.6, Apple M4. 50 warm-up iterations, 100 measured iterations:
```
kotlinx 200KB:           avg = 5.32 ms, median = 5.30 ms, (min, max) = (5.05 ms, 6.21 ms)
kotlinx dynamic 200KB:   avg = 3.84 ms, median = 3.76 ms, (min, max) = (3.44 ms, 5.49 ms)
manual when 200KB:       avg = 0.48 ms, median = 0.48 ms, (min, max) = (0.46 ms, 0.67 ms)
manual kotlin map 200KB: avg = 0.49 ms, median = 0.49 ms, (min, max) = (0.48 ms, 0.73 ms)
manual js map 200KB:     avg = 0.49 ms, median = 0.48 ms, (min, max) = (0.46 ms, 0.73 ms)
```

### 2020: Kotlin 1.3.72, kotlinx.serialization 0.20.0
I use Chromium 78 on Linux Mint 19.3. CPU is i7-7700HQ.

There is an error every launch, but the main thing is clear: kx is much slower than manual decoding code:
```
kotlinx 200KB:           avg = 40.4 ms, (min, max) = (37.4 ms, 48.3 ms)
kotlinx DOP 200KB:       avg = 36.9 ms, (min, max) = (35.8 ms, 39.0 ms)
manual when 200KB:       avg =  2.2 ms, (min, max) = ( 1.8 ms,  4.6 ms)
manual kotlin map 200KB: avg =  2.2 ms, (min, max) = ( 2.0 ms,  2.7 ms)
manual js map 200KB:     avg =  2.3 ms, (min, max) = ( 1.9 ms,  4.6 ms)
```
Note: in the 2020 code the "manual js map" button actually ran the manual Kotlin map decoder.
