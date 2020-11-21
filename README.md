Date: 2020-11-19

# Summary

This project answers a challenge to write fast network I/O in "java".

# Build

Requirements:

- JDK version 11
- Visual C++ compiler version 14

Procedure:

`./gradlew build`

# Run

After a successful build:

`java -cp build/libs/sendfile-jni.jar br.dev.pedrolamarao.sandbox.Program [host] [port] [file]`
