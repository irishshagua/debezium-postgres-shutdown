plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.61"
    application
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

repositories {
    jcenter()
}

dependencies {
    implementation("io.debezium:debezium-embedded:0.9.5.Final")
    implementation("io.debezium:debezium-connector-postgres:0.9.5.Final")
    implementation("ch.qos.logback:logback-core:1.2.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    compileOnly("org.projectlombok:lombok:1.18.10")
    annotationProcessor("org.projectlombok:lombok:1.18.10")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.testcontainers:testcontainers:1.12.4")
}

application {
    mainClassName = "com.github.igabaydulin.debezium.shutdown.sample.AppKt"
}

