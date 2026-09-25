base.archivesName = "quipu-core"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

tasks.named<JavaCompile>("compileTestJava") {
    options.release = 17
}

tasks.test {
    useJUnitPlatform()
}
