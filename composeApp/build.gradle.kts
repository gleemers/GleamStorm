import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.internal.os.OperatingSystem

val currentOs: OperatingSystem = OperatingSystem.current()

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.thoq.gleamstorm.MainKt"

        nativeDistributions {
            packageName = "dev.thoq.gleamstorm"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("../brand/gleamstorm_1024.png"))
                targetFormats(TargetFormat.Dmg)
            }
            windows {
                iconFile.set(project.file("../brand/gleamstorm_1024.png"))
                targetFormats(TargetFormat.Msi)
            }
            linux {
                iconFile.set(project.file("../brand/gleamstorm_1024.png"))

                // we need this as macos thinks its funny to try to
                // use the appimage, it needs the `if` statment
                if(currentOs.isLinux) {
                    targetFormats(TargetFormat.Deb, TargetFormat.AppImage)
                } else {
                    targetFormats(TargetFormat.Deb)
                }
            }
        }
    }
}
