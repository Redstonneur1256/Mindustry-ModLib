# Contributing

If you want to contribute to the project here is a few guidelines to follow:

### Use an IDE

Please use an IDE for the good of everyone, IntelliJ idea is free and can be
downloaded [here](https://www.jetbrains.com/idea/download/), it will also help you keep the code formatted to the
project's style and will allow for testing more easily.

### Code style

Before making a pull-request please make sure that your modifications have been formatted using the project's CodeStyle.
To automatically format your changes you can use the IntelliJ codestyle that can be found at the root of the project
[IntelliJCodeStyle.xml](IntelliJCodeStyle.xml).

### Large changes

Before making large changes to the project please open a discussion explaining what the change does, why and possible a
list of classes subject to the change

### Compatibility

Please keep in mind all the changes should be mostly backwards-compatible with the mod and should not interfere with
vanilla servers/clients.

### Testing

Please test your changes before opening a pull-request, if the changes are networking related please
make sure that they do not interfere with a vanilla client/server.
To make testing easier you may rename the file [example.private.gradle](Mod/example.private.gradle) to `private.gradle`
and change the values `gameExecutable` and `gameDataDirectory` to the game's data directory & game jar path. You should then be
able to run `./gradlew Mod:buildAndRun` to compile the mod and launch the game automatically.
