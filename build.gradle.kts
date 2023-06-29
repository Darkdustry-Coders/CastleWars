import groovy.json.JsonSlurper

plugins {
    java
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    val json = JsonSlurper().parseText(file("src/main/resources/plugin.json").readText()) as Map<*, *>
    project.version = json["version"]!!

    val mindustryVersion = json["minGameVersion"]
    val usefulHash = "46c1314234"

    compileOnly("com.github.Anuken.Arc:arc-core:v$mindustryVersion")
    compileOnly("com.github.Anuken.MindustryJitpack:core:v$mindustryVersion")

    implementation("com.github.xzxADIxzx.useful-stuffs:bundle:$usefulHash")
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}