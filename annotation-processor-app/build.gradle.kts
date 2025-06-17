plugins {
    application
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.unloadbrain"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("org.projectlombok:lombok")

    implementation("com.unloadbrain.annotation:annotation-processor:1.0.0")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("com.unloadbrain.annotation:annotation-processor:1.0.0")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass = "com.unloadbrain.Application"
}