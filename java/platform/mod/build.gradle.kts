base.archivesName = "quipu-mod"

dependencies {
    api(project(":core"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<JavaCompile>("compileTestJava") {
    options.release = 17
}

tasks.test {
    useJUnitPlatform()
}

// One artifact for every loader and every Minecraft version. The module never
// references a Minecraft class, so it needs no mappings, no Loom, and no
// per-version build. Java 8 bytecode loads on every JVM from Forge 1.8.9 to
// NeoForge on Java 25.
tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}
