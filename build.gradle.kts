plugins {
	java
}

group = "eu.gangui.hub"
version = "0.1.0-SNAPSHOT"

val minestom = "2026.07.12-26.2"
val canvas = "0.2.0"

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(25))
	}
}

repositories {
	mavenCentral()
	maven {
		url = uri("https://jitpack.io")
	}
}

dependencies {
	implementation("net.minestom:minestom:$minestom")
	implementation("net.kyori:adventure-text-minimessage:5.2.0")
	implementation("com.github.Chubbyduck1.Canvas:canvas-api:$canvas")
	implementation("com.github.Chubbyduck1.Canvas:canvas-core:$canvas")
	implementation("com.github.Chubbyduck1.Canvas:canvas-pack:$canvas")
	implementation("com.github.Chubbyduck1.Canvas:canvas-elements:$canvas")
	implementation("org.yaml:snakeyaml:2.3")

	testImplementation("net.minestom:minestom:$minestom")
	testImplementation("net.minestom:testing:$minestom")
	testImplementation(platform("org.junit:junit-bom:6.0.0"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
	options.compilerArgs.addAll(listOf("-Xlint:all,-this-escape", "-Werror"))
	options.encoding = "UTF-8"
}

tasks.test {
	useJUnitPlatform()
	systemProperty("minestom.inside-test", "true")
}

tasks.register<Jar>("fatJar") {
	archiveClassifier.set("fat")
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	manifest { attributes(mapOf("Main-Class" to "eu.gangui.hub.HubBootstrap")) }
	from(sourceSets["main"].output)
	from({
		configurations.runtimeClasspath.get()
			.filter { it.name.endsWith(".jar") }
			.map { zipTree(it) }
	})
	exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}
