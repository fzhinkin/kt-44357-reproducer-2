rootProject.name = "to-kstring-evaluation"

pluginManagement {
    val kotlin_version: String by settings

    plugins {
        kotlin("multiplatform") version kotlin_version
    }
}

