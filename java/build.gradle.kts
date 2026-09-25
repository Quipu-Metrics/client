plugins {
    `java-library`
}

subprojects {
    apply(plugin = "java-library")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok:1.18.46")
        "annotationProcessor"("org.projectlombok:lombok:1.18.46")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // Java 8 targets warn that source and target 8 are obsolete. Expected for
        // as long as Spigot 1.8.8 is a supported platform.
        options.compilerArgs.add("-Xlint:-options")
    }
}
