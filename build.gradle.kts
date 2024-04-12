import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("multiplatform") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("io.kotest.multiplatform") version "5.8.1"
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


kotlin {
    val allTargets = listOf(
        jvm(),
        macosArm64(),
        macosX64(),
//        linuxArm64(),
        linuxX64(),
        mingwX64(),
    )

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            }
        }
        commonTest {
            dependencies {
                implementation("io.kotest:kotest-framework-engine:5.8.1")
                implementation("io.kotest:kotest-assertions-core:5.8.1")
                implementation("io.kotest:kotest-framework-datatest:5.8.1")
                implementation("com.squareup.okio:okio:3.9.0")
            }
        }
        jvmTest {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:5.8.1")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
