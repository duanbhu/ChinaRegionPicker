import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.chinaregionpicker"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Publish the Android library as a Maven artifact. JitPack invokes this
// publication when building a Git tag or commit.
afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "com.github.duanbhu"
                artifactId = "china-region-picker"
                version = providers.gradleProperty("VERSION_NAME")
                    .orElse("0.1.0-SNAPSHOT")
                    .get()

                from(components["release"])

                pom {
                    name.set("ChinaRegionPicker")
                    description.set("Four-level China region picker for Android")
                    url.set("https://github.com/duanbhu/ChinaRegionPicker")
                    licenses {
                        license {
                            name.set("The MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/duanbhu/ChinaRegionPicker.git")
                        developerConnection.set("scm:git:ssh://github.com/duanbhu/ChinaRegionPicker.git")
                        url.set("https://github.com/duanbhu/ChinaRegionPicker")
                    }
                }
            }
        }
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
}
