base.archivesName = "quipu-velocity"

dependencies {
    api(project(":core"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}
