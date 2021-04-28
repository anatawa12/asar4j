# ASAR4j
[![a12 maintenance: Active](https://anatawa12.com/short.php?q=a12-active-svg)](https://anatawa12.com/short.php?q=a12-active-doc)
[![](https://img.shields.io/maven-central/v/com.anatawa12.asar4j/common)](https://github.com/anatawa12/asar4j/releases/latest)
[![Discord](https://img.shields.io/discord/834256470580396043)](https://discord.gg/yzEdnuJMXv)

A [asar] implementation in java without runtime dependency.

**IMPORTANT: THIS LIBRARY IS EXPERIMENTAL. I MAY BREAK APIS IN THE FEATURE.**

## How to use

This library was separated to four jars to
keep small library.

- `common` The library contains Entry class. All those libraries depending on this library.
- `file` The library contains `java.util.zip.ZipFile`-like file reader.
- `writer` The library contains `java.util.zip.ZipOutputStream`-like file writer.
- `url` The library contains implementation of `asar:` url protocol like `jar:` but
  allows asar-in-asar or asar-in-jar.

This library has been published on maven central repository.
To add a dependency on asar4j using Maven, use the following:

```xml
<dependency>
  <groupId>com.anatawa12.asar4j</groupId>
  <artifactId>[choose from file, writer or url]</artifactId>
  <version>0.0.1</version>
</dependency>
```

To add a dependency using Gradle:

```kotlin
dependencies {
  implementation("com.anatawa12.asar4j:<library-name>:0.0.1")
}
```

[asar]: https://github.com/electron/asar
