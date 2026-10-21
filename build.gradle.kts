plugins {
  kotlin("multiplatform") version "2.4.21"
  kotlin("plugin.serialization") version "2.4.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

kotlin {
  js {
    browser()
    nodejs()
    binaries.executable()
  }

  sourceSets {
    jsMain.dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
      implementation("org.jetbrains.kotlinx:kotlinx-html:0.12.0")
    }
  }
}
