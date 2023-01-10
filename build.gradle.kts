import gg.essential.gradle.util.noServerRunConfigs

plugins {
    kotlin("jvm")
    id("gg.essential.multi-version")
    id("gg.essential.defaults")
}

val modGroup: String by project
val modBaseName: String by project
group = modGroup
base.archivesName.set("$modBaseName-${platform.mcVersionStr}")

loom {
    noServerRunConfigs()
    mixin {
        defaultRefmapName.set("mixins.levelhead.refmap.json")
    }
    launchConfigs {
        getByName("client") {
            property("patcher.debugBytecode", "true")
            property("mixin.debug.verbose", "true")
            property("mixin.debug.export", "true")
            property("mixin.dumpTargetOnFailure", "true")
            arg("--tweakClass", "gg.essential.loader.stage0.EssentialSetupTweaker")
            arg("--mixin", "mixins.levelhead.json")
        }
    }
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

val embed by configurations.creating
configurations.implementation.get().extendsFrom(embed)

dependencies {
    compileOnly("gg.essential:essential-$platform:4246+g8be73312c")
    embed("gg.essential:loader-launchwrapper:1.1.3")

    embed("com.squareup.okhttp3:okhttp:3.14.9")
    compileOnly("org.spongepowered:mixin:0.8.5-SNAPSHOT")
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xno-param-assertions", "-Xjvm-default=all-compatibility")
    }
}

tasks.jar {
    from(embed.files.map { zipTree(it) })

    manifest.attributes(mapOf(
        "ModSide" to "CLIENT",
        "FMLCorePluginContainsFMLMod" to "Yes, yes it does",
        "TweakClass" to "gg.essential.loader.stage0.EssentialSetupTweaker",
        "TweakOrder" to "0",
        "MixinConfigs" to "mixins.levelhead.json"
    ))
}