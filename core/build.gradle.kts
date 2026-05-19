group = rootProject.group
version = rootProject.version

dependencies {
    compileOnly(project(":api"))
    compileOnly("org.spigotmc:spigot-api:1.21.11-R0.1-SNAPSHOT")
}
