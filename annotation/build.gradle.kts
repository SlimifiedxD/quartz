plugins {
    id("java")
    `maven-publish`
}

group = "com.github.SlimifiedxD"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.SlimifiedxD"
            artifactId = "annotation"
            version = "1.0-SNAPSHOT"

            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}