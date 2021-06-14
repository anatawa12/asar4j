plugins {
    // for subproject configurations
    `java-library`
    `maven-publish`
    signing
    jacoco
}

group = "com.anatawa12.asar4j"
version = property("version")!!

subprojects {
    apply(plugin = "java-library")
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

    val maven = publishing.publications.create("maven", MavenPublication::class) {
        from(project.components["java"])

        pom {
            name.set(base.archivesBaseName)
            description.set("A asar implementation in java without runtime dependency.")
            url.set("https://github.com/anatawa12/asar4j")

            scm {
                url.set("https://github.com/anatawa12/asar4j")
                connection.set("scm:git:git://github.com/anatawa12/asar4j.git")
                developerConnection.set("scm:git:git@github.com:anatawa12/asar4j.git")
            }

            issueManagement {
                system.set("github")
                url.set("https://github.com/anatawa12/asar4j/issues")
            }

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                    distribution.set("repo")
                }
            }

            developers {
                developer {
                    id.set("anatawa12")
                    name.set("anatawa12")
                    roles.set(setOf("developer"))
                }
            }
        }
    }

    publishing.repositories.maven {
        name = "ossrh"
        url = if (version.toString().endsWith("SNAPSHOT"))
            uri("https://oss.sonatype.org/content/repositories/snapshots")
        else uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")

        credentials {
            username = project.findProperty("com.anatawa12.sonatype.username")?.toString() ?: ""
            password = project.findProperty("com.anatawa12.sonatype.passeord")?.toString() ?: ""
        }
    }

    signing.sign(maven)
}
