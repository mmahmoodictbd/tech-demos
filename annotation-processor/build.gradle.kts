group = "com.unloadbrain.annotation"
version = "1.0.0"

dependencies {
    implementation("com.google.auto.service:auto-service:1.1.1")
    implementation("com.palantir.javapoet:javapoet:0.7.0")

    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("org.springframework.data:spring-data-jpa:3.4.4")
    implementation("org.projectlombok:lombok:1.18.36")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.unloadbrain.annotation"
            artifactId = "annotation-processor"
            version = "1.0.0"

            from(components["java"])
        }
    }
}