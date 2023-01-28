# Mindustry-ModLib
Library to help developers creating Mindustry mods and optimize some game features  
If you think some utility or optimization should be added open an issue describing the feature.

![Release version](https://img.shields.io/github/v/release/Redstonneur1256/Mindustry-ModLib?style=for-the-badge)
![JitPack version](https://img.shields.io/jitpack/version/com.github.Redstonneur1256/Mindustry-ModLib?style=for-the-badge)
![Mod download count](https://img.shields.io/github/downloads/Redstonneur1256/Mindustry-ModLib/total?label=Mod%20downloads&style=for-the-badge)
![License](https://img.shields.io/github/license/Redstonneur1256/Mindustry-ModLib?style=for-the-badge)

✔ **Fully compatible with vanilla servers/clients !**

Current development tools:
-----

* Class mixins
* No packet limit
* Packet chaining (sending replies to received packets)
* `Call` like classes supporting custom parameters and return values
* Synchronized registrable types in TypeIO

Current game utilities/optimizations:
-----

* 📃 Logger showing calling class name
* 📶 Optimized server ping & dns lookup, no longer creating a billion threads
* 📶 Button to refresh the server list

Using the library:
-----

- Add the dependency using JitPack on Gradle:
  ```groovy
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io/' }
        maven { url 'https://repo.spongepowered.org/repository/maven-public/' }
    }
    
    dependencies {
        compileOnly 'com.github.Redstonneur1256.Mindustry-ModLib:Mod:VERSION'
    }
  ```
- Update your `mod.json`/`plugin.json` to add the library as a dependency.
  ```json
  "dependencies": [
    "!mod-library"
  ]
  ```

See the `Example` module for usage examples.

Contributors:
-------

Special thanks to [Eliott SRL](https://github.com/Eliott-Srl) for making the icon.
