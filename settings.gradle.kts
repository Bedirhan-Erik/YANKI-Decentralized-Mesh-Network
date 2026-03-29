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

rootProject.name = "YANKI-Decentralized-Mesh-Network"
include(":android-app")
// Not: backend-server bir Node.js projesi olduğu için include etmiyoruz, 
// ancak Project görünümünde 'Project' moduna geçtiğinizde sorunsuz görünmelidir.
