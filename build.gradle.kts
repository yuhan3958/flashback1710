
plugins {
    id("com.gtnewhorizons.gtnhconvention")
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
        )
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(
        "com.github.GTNewHorizons:ModularUI2:2.3.89-1.7.10:dev"
    )
    testImplementation(kotlin("test"))
}
