rootProject.name = "ElementalMagic"

include("api")
include("core")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
        maven("https://oss.sonatype.org/content/groups/public")
    }
}
