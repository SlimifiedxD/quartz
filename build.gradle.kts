plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm")
}

group = "com.github.SlimifiedxD"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven("https://jitpack.io")
}

dependencies {
    api("io.github.classgraph:classgraph:4.8.173")
    api("fr.mrmicky:fastboard:2.1.5")
    api("com.github.SlimifiedxD.funmands:funmands-paper:fcdd800552")
    implementation("org.yaml:snakeyaml:2.5")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.SlimifiedxD"
            artifactId = "bedrock"
            version = "1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}
