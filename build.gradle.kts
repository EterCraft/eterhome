plugins {
    id("java-library")
    id("maven-publish")
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // Repli : plugins publiés sur cette machine (`gradlew publishToMavenLocal`), pour tester avant de pousser
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.214-stable")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    runServer {
        minecraftVersion("26.3")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        // Déclarée comme entrée : sinon le cache de Gradle réutilise un plugin.yml avec l'ancienne version
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

// Après chaque build, copie le jar dans <eterPluginsDir>/paper-v2 (à part des jars v1 de paper/) et y supprime l'ancienne version de ce plugin.
// eterPluginsDir se règle dans ~/.gradle/gradle.properties (propre à ta machine) : sans lui, rien n'est copié.
val deployPlugin by tasks.registering(Copy::class) {
    description = "Copie le jar dans le dossier de plugins local (eterPluginsDir)"
    // Variables locales à la tâche : le cache de configuration de Gradle refuse les variables du script
    val eterPluginsDir = providers.gradleProperty("eterPluginsDir")
    val enabled = eterPluginsDir.isPresent
    onlyIf { enabled }
    val jarName = tasks.jar.flatMap { it.archiveBaseName }
    from(tasks.jar)
    into(eterPluginsDir.map { "$it/paper-v2" }.orElse(layout.buildDirectory.dir("deploy").map { it.asFile.path }))
    doFirst {
        destinationDir.listFiles { file -> file.name.startsWith(jarName.get() + "-") && file.name.endsWith(".jar") }
            ?.forEach { it.delete() }
    }
}
tasks.build { finalizedBy(deployPlugin) }

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "EterHome"
            from(components["java"])
        }
    }
}
