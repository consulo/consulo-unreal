# Unreal Engine support for Consulo

Opens Unreal Engine projects (`.uproject`) through the External System API and lets the C/C++ plugin resolve them the way
UnrealBuildTool compiles them.

## What it does

- **Open / Import**: a `.uproject` file or a directory holding one. The engine is found from the project's
  `EngineAssociation`, in this order:
  - the engine tree around a project inside a source build;
  - an *Unreal Engine* SDK of that version;
  - an engine registered in `Install.ini` (Linux, macOS) or `LauncherInstalled.dat` (Windows);
  - the most recent *Unreal Engine* SDK.

  The engine can also be chosen in the project settings.
- **Modules**, from UnrealBuildTool `-Mode=JsonExport`:

  | Module | Contents |
  |---|---|
  | `<Project>.uproject` | The project directory; `Binaries`, `Intermediate`, `Saved` and `DerivedDataCache` excluded |
  | one per C++ module of the project and its plugins | The module directory, with module dependencies as in its `.Build.cs` |
  | `UE <version>` | `Engine/Source`, so engine headers are indexed and resolve their own includes. Can be turned off. |

- **C/C++ settings**: include paths, defines and the C++ standard of every module, from UnrealBuildTool
  `-Mode=GenerateClangDatabase`. The compile database is followed into its response files, and the defines are read from
  the force-included `Definitions.h` (`WITH_EDITOR`, `<MODULE>_API`, ...). If the database cannot be generated, the
  settings are derived from the module rules instead.
- **Auto-reload** when the `.uproject`, a `.uplugin`, or a `.Build.cs` / `.Target.cs` changes.
- **Tasks** in the *Unreal Engine* tool window and as run configurations: `build`, `rebuild`, `clean` (UnrealBuildTool),
  `editor` and `game` (Unreal Editor with `-game -windowed`).

UnrealBuildTool output goes to `<Project>/Intermediate/Consulo`.