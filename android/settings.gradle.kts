pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // TarsosDSP (YIN pitch detection for the Tuner) is published only to its author's
        // repository. Scoped to its group so nothing else can resolve from there.
        exclusiveContent {
            forRepository { maven("https://mvn.0110.be/releases") }
            filter { includeGroup("be.tarsos.dsp") }
        }
    }
}

rootProject.name = "DanThanhGuzheng"
include(":app")
