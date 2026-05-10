plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.anonhu"
version = "1.0.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // PlaceholderAPI — soft dependency
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks {
    // Заменяем версию в plugin.yml при сборке
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    // Shadow jar — финальный артефакт без provided зависимостей
    shadowJar {
        archiveClassifier.set("") // без суффикса -all
        archiveFileName.set("DeathSystem-${version}.jar")

        // Provided зависимости не включаем в jar
        dependencies {
            exclude(dependency("io.papermc.paper:paper-api"))
            exclude(dependency("me.clip:placeholderapi"))
        }
    }

    // Сборка по умолчанию = shadowJar
    build {
        dependsOn(shadowJar)
    }
}
