base.archivesName = "quipu-bungeecord"

dependencies {
    api(project(":core"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}
