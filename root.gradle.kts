plugins {
    id("org.polyfrost.multi-version.root")
    id("com.github.johnrengelman.shadow") apply false
}

preprocess {
    "1.8.9-forge"(10809, "srg") {}
}
