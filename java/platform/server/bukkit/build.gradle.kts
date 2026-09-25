base.archivesName = "quipu-bukkit"

dependencies {
    api(project(":core"))
    // The oldest API supported. Everything newer, Folia included, is reached
    // through reflection so one jar covers 1.8.8 to current.
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    testImplementation("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
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
