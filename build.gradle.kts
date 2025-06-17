plugins {
    id("java")
    `maven-publish`
}


allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    group = "com.unloadbrain"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
    }

    tasks.test {
        useJUnitPlatform()
    }
}