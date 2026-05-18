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
    }
}

rootProject.name = "FiatBravo198"

include(":app")

include(":core:common")
include(":core:ipc")
include(":core:canbus")
include(":core:obd2")
include(":core:ai")

include(":diagnostic:collector")
include(":diagnostic:uploader")

include(":ota:github")

include(":feature:dashboard")
include(":feature:radio")
include(":feature:radiofav")
include(":feature:drivingreport")
include(":feature:maintenance")
include(":feature:settings")
include(":feature:autobrightness")
include(":feature:dualdisplay")
include(":feature:otaupdater_legacy")
include(":feature:radiomanager")
include(":feature:thememanager")

include(":externalapp:smartcamera")
include(":externalapp:emergency")
include(":externalapp:voice")
