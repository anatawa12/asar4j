plugins {
    // for subproject configurations
    java
    `maven-publish`
    signing
    jacoco
}

group = "com.anatawa12.asar4j"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "jacoco")

    group = project(":").group
    version = project(":").version

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        withJavadocJar()
        withSourcesJar()
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation("org.apache.commons:commons-compress:1.20")
        testImplementation("com.google.guava:guava:30.1.1-jre")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    }

    tasks.test {
        useJUnitPlatform()

        finalizedBy(tasks.jacocoTestReport.get())
    }

    tasks.jacocoTestReport {
        reports {
            html.isEnabled = true
        }
    }

    tasks.compileJava {
        options.compilerArgs.add("-Xlint")
    }
}
