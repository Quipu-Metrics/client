base.archivesName = "quipu-sponge"

dependencies {
    api(project(":core"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}
