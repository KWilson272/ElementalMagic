group = "me.kwilson272.elementalmagic"
version = "1.0.0"

defaultTasks("shadowJar")

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

subprojects {
    apply(plugin = "java")
    java.toolchain.languageVersion = JavaLanguageVersion.of(21)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
}

tasks.shadowJar {
    archiveFileName.set("${project.name}-${rootProject.version}.jar")
}

defaultTasks("shadowJar")
