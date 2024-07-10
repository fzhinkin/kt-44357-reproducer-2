plugins {
    kotlin("multiplatform") version "1.4.20"
}

group = "org.jetbrains.kotlinx"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val appleMain by creating
        val appleTest by creating {
            dependsOn(commonTest)
        }
    }

    macosX64() {
    }
}


