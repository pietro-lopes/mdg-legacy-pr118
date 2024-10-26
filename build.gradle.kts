import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    java
    idea
    `maven-publish`
    id("net.neoforged.moddev.legacy") version "2.0.61-beta-pr-118-legacy"
}

val minecraftVersion: String by project
val minecraftVersionRange: String by project
val forgeVersion: String by project
val forgeVersionRange: String by project
val loaderVersionRange: String by project
val parchmentMcVersion: String by project
val parchmentVersion: String by project
val modId: String by project
val modName: String by project
val modLicense: String by project
val modVersion: String by project
val modGroupId: String by project
val modAuthors: String by project
val modDescription: String by project

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven("https://cursemaven.com")
        }
        filter {
            includeGroup("curse.maven")
        }
    }

    flatDir {
        dir("libs")
    }
}

base {
    archivesName = modId
    group = modGroupId
    version = modVersion
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

sourceSets {
    main {
        resources.srcDir("src/generated/resources")
    }
    create("archLoom") {
        compileClasspath += sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
    }
    create("mdgLegacy") {
        compileClasspath += sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
    }
    create("jarJarlessWorkaround") {
        compileClasspath += sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
    }
}

obfuscation {
    val archLoomRuntimeOnly: Configuration by configurations.getting
    val mdgLegacyRuntimeOnly: Configuration by configurations.getting
    val jarJarlessWorkaroundRuntimeOnly: Configuration by configurations.getting

    createRemappingConfiguration(archLoomRuntimeOnly)
    createRemappingConfiguration(mdgLegacyRuntimeOnly)
    createRemappingConfiguration(jarJarlessWorkaroundRuntimeOnly)
}

neoForge {
    version = "$minecraftVersion-$forgeVersion"
    runs {
        register("client-Mdg-Legacy") {
            client()
            sourceSet = sourceSets.getByName("mdgLegacy")
        }
        register("client-Arch-Loom") {
            client()
            sourceSet = sourceSets.getByName("archLoom")
        }
        register("client-JarJarless") {
            client()
            sourceSet = sourceSets.getByName("jarJarlessWorkaround")
        }

        configureEach {
            systemProperty("forge.logging.console.level", "debug")
            jvmArgument("-Xmx3000m")
            jvmArgument("-XX:+IgnoreUnrecognizedVMOptions")
            jvmArgument("-XX:+AllowEnhancedClassRedefinition")
            if (type.get().startsWith("client")) {
                programArguments.addAll("--width", "1920", "--height", "1080")
                gameDirectory = file("runs/client")
                systemProperty("mixin.debug.export", "true")
                jvmArguments.addAll(
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+UseG1GC",
                    "-XX:G1NewSizePercent=20",
                    "-XX:G1ReservePercent=20",
                    "-XX:MaxGCPauseMillis=50",
                    "-XX:G1HeapRegionSize=32M"
                )
            }
        }
    }
    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
    parchment {
        minecraftVersion = parchmentMcVersion
        mappingsVersion = parchmentVersion
    }
}

mixin {
    add(sourceSets.main.get(),"examplemod.refmap.json")
    config("examplemod.mixins.json")
}

afterEvaluate {
    tasks.withType(Jar::class).configureEach {
        manifest.attributes(
            mapOf(
            "MixinConfigs" to "examplemod.mixins.json",
            "Specification-Title" to project.name,
            "Specification-Vendor" to modAuthors,
            "Specification-Version" to modVersion,
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to modAuthors,
            "Implementation-Timestamp" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        ))
    }
}

val modArchLoomRuntimeOnly: Configuration by configurations.getting
val modMdgLegacyRuntimeOnly: Configuration by configurations.getting
val modJarJarlessWorkaroundRuntimeOnly: Configuration by configurations.getting
dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")

    //Mixins
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")!!)
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:0.4.1")!!)
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    //THIS WORKS, AT THIS VERSION THEY HAD CHANGED ALREADY TO MDG PR118
    modMdgLegacyRuntimeOnly("curse.maven:gregtechceu-modern-890405:5812598")

    //THIS DOES NOT WORK, AT THIS VERSION THEY WERE USING ARCH LOOM AT THAT TIME
    modArchLoomRuntimeOnly("curse.maven:gregtechceu-modern-890405:5641637")

    // same happens with normal modRuntimeOnly, so not a config issue and run the "Client" run
    // modRuntimeOnly("curse.maven:gregtechceu-modern-890405:5641637")

    // WORKAROUND
    /*
    1. Download jar
    2. Remove jarjar and jars folder
     */
    modJarJarlessWorkaroundRuntimeOnly("blank:gregtechceu-modern-890405:5641637-jarjarless")

    // this could be from curse maven too, doesn't matter
    modJarJarlessWorkaroundRuntimeOnly("blank:configuration-forge-1.20.1:2.2.0")
    modJarJarlessWorkaroundRuntimeOnly("blank:ldlib-forge-1.20.1:1.0.26.b")
    modJarJarlessWorkaroundRuntimeOnly("blank:Registrate-MC1.20:1.3.3")

}

tasks {
    processResources {
        val replaceProperties = mapOf(
            "minecraft_version" to minecraftVersion,
            "minecraft_version_range" to minecraftVersionRange,
            "forge_version" to forgeVersion,
            "forge_version_range" to forgeVersionRange,
            "loader_version_range" to loaderVersionRange,
            "mod_id" to modId,
            "mod_name" to modName,
            "mod_license" to modLicense,
            "mod_version" to modVersion,
            "mod_authors" to modAuthors,
            "mod_description" to modDescription
        )

        inputs.properties(replaceProperties)
        filesMatching(listOf("META-INF/mods.toml")) {
            expand(replaceProperties)
        }
    }
    compileJava {
        options.encoding = "UTF-8"
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components.getByName("java"))
        }
    }
    repositories {
        maven("file://$projectDir/repo")
    }
}

idea {
    project {
        jdkName = java.sourceCompatibility.toString()
        languageLevel = IdeaLanguageLevel(java.sourceCompatibility.toString())
    }
}
