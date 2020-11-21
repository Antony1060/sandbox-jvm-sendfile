Date: 2020-11-20

# Summary

This project answers a challenge to write fast network I/O in "java".

# Build

Requirements:

- Project Panama early access build

Procedure:

`./gradlew build`

# Run

After a successful build:

`java -cp build/libs/sendfile-panama.jar --add-modules jdk.incubator.foreign -Dforeign.restricted=permit br.dev.pedrolamarao.sandbox.Program [host] [port] [file]`
