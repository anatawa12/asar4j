dependencies {
    api(project(":common"))
    api(project(":file"))
}

val createAsarZip by tasks.creating(Zip::class) {
    archiveFileName.set("test.zip")
    destinationDirectory.set(file("testData"))
    from("testData") {
        include("test.asar")
    }
}

tasks.test {
    dependsOn(createAsarZip)
    systemProperty("com.anatawa12.asar4j.test-asar", file("testData/test.asar").toString())
    systemProperty("com.anatawa12.asar4j.test-zip", createAsarZip.archiveFile.get().toString())
}
