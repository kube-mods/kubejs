# changelog

## Unreleased
- Fix invalid fluid lookup for fluid tag ingredients
- Stop bundling Better Advanced Tooltips to fix CTD (use the one from CF / MR instead)
- use TypeWrappers for HolderSets (#1169), this should fix some wrapping issues

## [8.0.4] - 2026-07-23

- Fix dedicated server crash from block tint function client types (#1159)
- Add `/kubejs browse` command to open the `kubejs` directory directly from the game
- Fix component initialization in item modification
- [port from 1.21] Rework item builders for integration with IItemExtension (#1131)
- [port from 1.21] `global` write error message for client and server scripts (#1126)
- [port from 1.21] mkdirs automatically when using `JsonIO`

## [8.0.3] - 2026-06-22

- Builders now generate the required model data for item and block-item tinting (#1156)
- Fix `Client` binding not being available from startup/client scripts (#1152)
- Fixed circular initialization between ConsoleJS and ScriptType before script consoles are ready (#1147)
- Fixed creative tab modification not firing correctly (#1149)

## [8.0.2] - 2026-06-12

- Readd error handling during the recipe event (#1142)
- Fix BlockItemBuilder translation key resolution/duplicate key (#1143)
- Reimplement basic JEI integration. EMI and REI at the time of writing are not released for 26.1 yet and will follow later.
- Fix registry type coercion not working (...because it didn't return a value)

## [8.0.1] - 2026-06-08

- Hotfix for the release version of NeoForge, sorry to anybody who got a Mixin crash when they tried to open a world!

## [8.0.0] - 2026-06-08

- Initial port to 26.1. For script developers, this version aims to maintain parity with how KubeJS worked in 1.21, obviously excluding changes that *had* to be made for Vanilla parity. Most notably:
    - Empty ingredients are now **fully disallowed** in Vanilla, and things like `Ingredient.empty` will throw in KubeJS, too.
    - In general, using `Ingredient` solely to resolve to a list of item stacks is now *discouraged*, to be more in line with their purpose as a filter in Vanilla.
    - Type wrappers were simplified and made a little more consistent.
    - Component functions were reworked under the hood.
    - The tag event was (again) reworked; if things break, yell at Max.
    - Fluid Rendering: New `tintFunction` and `bucketColor` callbacks were added.
    - Capabilities were fully reworked to fit with Neo's new attachment APIs.
    - A (preliminary) rework for FoodBuilder
    - A **lot** of cleanup under the hood, including no more `UtilsJS#wrap` and removing the static script manager reference.
