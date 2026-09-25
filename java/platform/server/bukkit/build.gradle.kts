base.archivesName = "quipu-bukkit"

dependencies {
    api(project(":core"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}
