# [![KubeJS](https://repository-images.githubusercontent.com/46427577/d2446680-c366-11ea-8e6b-8a4da776b475)](https://kubejs.com)

**Note: If you are a script developer (i.e. pack dev, server admin, etc.), you likely want to visit our [wiki](https://kubejs.com) instead.**

(For a Table Of Contents, click the menu icon in the top left!)

## Introduction

KubeJS is a Minecraft mod which lets you create scripts in the JavaScript programming language to manage your server using events, change recipes, add ~~and edit~~ (coming soon!) loot tables, customise your world generation, add new blocks and items, or use custom integration with other mods like [FTB Quests](https://mods.latvian.dev/books/kubejs/page/ftb-quests-integration) for even more advanced features!

## Issues and Feedback

If you think you've found a bug with the mod or want to ask for a new feature, feel free to [open an issue](https://github.com/KubeJS-Mods/KubeJS/issues) here on GitHub, we'll try to get on it as soon as we can! Alternatively, you can also discuss your feature requests and suggestions with others using the [Discussions](https://github.com/KubeJS-Mods/KubeJS/discussions) tab.

And if you're just looking for help with KubeJS overall and the wiki didn't have the answer for what you were looking for, you can join our [Discord server](https://discord.gg/bPFfH6P) and ask for help in the support channels, as well!

## License

KubeJS is distributed under the GNU Lesser General Public License v3.0, or LGPLv3. See our [LICENSE](https://github.com/KubeJS-Mods/KubeJS/blob/1.18/main/LICENSE.txt) file for more information.

## Creating addons

~~Creating addon mods for KubeJS is easy! Just follow the following steps to your own liking, depending on how deep you want your integration to go!~~

Those are lies and deceit.

To add a Gradle dependency on KubeJS, you will need to add the following repositories to your `build.gradle`'s `repositories`:

```groovy
repositories {
    maven {
        url "https://maven.latvian.dev/releases"
        content {
            includeGroup "dev.latvian.mods"
            includeGroup "dev.latvian.apps"
        }
    }

    maven {
        url 'https://jitpack.io'
        content {
            includeGroup "com.github.rtyley"
        }
    }
}
```

You can then declare KubeJS as a regular compile-time dependency in your `dependencies` block:

```groovy
api("dev.latvian.mods:kubejs-neoforge:$kubejs_version")
interfaceInjectionData("dev.latvian.mods:kubejs-neoforge:$kubejs_version") // optional
```

Just set the versions with most up-to-date version of the required mod(s), which you also find using these badges:

<p align="center">
    <a href="https://maven.latvian.dev/#/releases/dev/latvian/mods/kubejs-neoforge">
        <img src="https://flat.badgen.net/maven/v/metadata-url/https/maven.latvian.dev/releases/dev/latvian/mods/kubejs-neoforge/maven-metadata.xml?color=C186E6&label=KubeJS" alt="KubeJS Latest Version">
    </a>
	<a href="https://maven.latvian.dev/#/releases/dev/latvian/mods/rhino">
        <img src="https://flat.badgen.net/maven/v/metadata-url/https/maven.latvian.dev/releases/dev/latvian/mods/rhino/maven-metadata.xml?color=3498DB&label=Rhino" alt="Rhino Latest Version">
    </a>
</p>

(Note: The above badges may not represent the *true* latest version of these mods. As a basic rule of thumb, for KubeJS and Rhino, you should always be using the latest version compiled against your version of Minecraft, for example `1802.+` for Minecraft 1.18.2, while for Architectury, the corresponding major version will be provided. You can also click on the badge to see all versions of each mod)

### Creating a plugin

KubeJS [plugins](https://github.com/KubeJS-Mods/KubeJS/blob/main/src/main/java/dev/latvian/mods/kubejs/plugin/KubeJSPlugin.java) are the main way to add KubeJS integration to your mods through code. They contain various convenient hooks for addon developers, to allow things such as:

- performing certain actions during or after plugin or KubeJS initialisation (`init`, `afterInit`, etc.)
- adding classes to the class filter (`registerClasses`) as well as custom bindings (`registerBindings`) and type wrappers (`registerWrappers`) for easier interaction with native code in user scripts (See below for an explanation and example use cases)
- registering custom recipe handlers for modded recipe types (`registerRecipeTypes` - See below for an example)
- registering custom event handler groups for the KubeJS event system (`registerEvents`, this is **necessary** in order to have the event group be accessible from scripts)
- attaching extra data to players, worlds or the server, such that it can be accessed by script developers later (`attach(Player|World|Server)Data` - [Example](https://github.com/FTBTeam/FTB-Quests/blob/11311be070273008483d4c734ff9b96cc6a85b02/common/src/main/java/dev/ftb/mods/ftbquests/integration/kubejs/KubeJSIntegration.java#L40-L43))

You must add your plugin class in a `src/main/resources/kube.plugin.json` file, optionally with mod id at end, e.g.:

```json5
{
    "plugins": [
        {
            // Optional - highly recommended to provide the mod id
            "id": "mod_id",
            // Required - Class path to the plugin
            "class": "path.to.my.awesome.integrator.for.MyKubeJSPlugin",
            // Optional - Tells Kube to only load plugin on the client
            "client_only": true,
            // Optional - Only load the plugin if the following mods are present
            "required_mods": ["some_mod"],
            // Optional - Load this plugin after another plugin has loaded
            "after": ["some_plugin"],
            // Optional - Specify class filters
            // "kubejs.classfilter.txt" will still load just fine
            "class_filter": {
                // Optional - 
                "allow": [
                    "some.util.MyUtilClass",
                    "thing.api.SoupifierData",
                    "some.package"
                ],
                "deny": [
                    "some.saver.FileSystemWriter",
                    "some.internals"
                ]
            }
        }
    ]
}
```

### Adding bindings

Similarly to adding custom recipe types, you may also add **custom bindings** to KubeJS (see [AntimatterAPI](https://github.com/GregTech-Intergalactical/AntimatterAPI/blob/dev-1.18/common/src/main/java/muramasa/antimatter/integration/kubejs/AntimatterKubeJS.java) for a simple example). Bindings can be anything from single value constants (like the global `HOUR = 3600000 (ms)`) to Java class and method wrappers (such as the builtin `Item` binding, which is wrapping the `ItemWrapper` class), and can be constrained to individual scopes, contexts and script types, as well!

### Setting class filters

KubeJS offers native Java type access in script files, meaning that basic Java types can be referenced directly by using for example `Java.loadClass("package.class")`. While builtin filters exist to prevent users from accessing any internal or potentially harmful packages added by Minecraft or its libraries directly, you may still want to explicitly deny access to certain classes or packages in your mod (or explicitly allow certain classes *within* a generally blacklisted package). To do this, you may either provide a class filter using a KubeJS plugin *or* you can avoid adding KubeJS as a dependency entirely by providing a simple `kubejs.classfilter.txt` file in your mod's `resources` with the following format (Note that comments aren't allowed in the actual file):

```diff
+mymod.api // This will *explicitly allow* anything from the mymod.api package to be used in KubeJS
-mymod.api.MyModAPIImpl // This will deny access to the MyModAPIImpl class, while keeping the rest of the package accessible
-mymod.internal.HttpUtil // This will *explicitly deny* your class from being used in KubeJS
```

### Recipe Schemas

In addition to registering recipe schemas through your plugin, they can be made via a json file in the `kubejs/recipe_schema` registry. Json schemas are the preferred method of declaring schemas and can be [datagenned](https://docs.neoforged.net/docs/1.21.1/resources/#data-generation) with the provided `RecipeSchemaProvider`!

**Important!** In order to use this data provider, you *must* add kubejs as an existing mod with the `'--existing-mod', 'kubejs'` arguments. See [NeoForge's docs](https://docs.neoforged.net/docs/1.21.1/resources/#command-line-arguments) for how to do that.

After that, using the provider is just like any other

```java
@EventBusSubscriber(modid = "mod")
public class DataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        event.addProvider(new RecipeSchemaProvider("Mod Recipe Schemas", event) {
           @Override
           public void add(HolderLookup.Provider lookup) {
               add(ResourceLocation.fromNamespaceAndPath("mod", "recipe"), builder -> {
                   builder.hidden();
                   builder.mappings("modRecipe", "hungry");
                   // And so on, the javadocs of the methods give a brief description of what their data is used for
               });
           } 
        });
    }
}
```

## Contributing to KubeJS

### Getting Started

If you want to contribute to KubeJS, you will first need to set up a development environment, which should be fairly simple. Just clone the repository using Git:

```sh
git clone https://github.com/KubeJS-Mods/KubeJS.git
```

and import the gradle project using an IDE of your choice!

### Building

Building KubeJS from source should be rather straightforward, as well, just run `gradlew build` in the root project, and the jar files should be in `build/libs` directory (`kubejs-<loader>-<version>.jar`).

### Creating Pull Requests

When creating a pull request to KubeJS, please make sure you acknowledge the following:

- We will *usually* not accept pull requests that add mod-specific support; those things are better suited as KubeJS plugins (optimally within the target mods themselves) or integration mods.
- Please be sure to test your feature **before** creating a pull request. We are a rather small team and as such, can't spend all too much time on reviewing pull requests.
- While not *strictly* required, please try to adhere by the repository's code style and conventions. A sample `.editorconfig` file exists to make this even easier for you to achieve (see the relevant EditorConfig plugin for your IDE)
- If you are unsure whether your feature fits into KubeJS and would like to hear additional opinions on the matter, feel free to open an `issue` or a `discussion` to discuss it further with us first!
