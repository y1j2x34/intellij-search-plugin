# VSCode Search Panel

<!-- Plugin description -->
A VS Code-style search and replace panel for JetBrains IDEs.
<!-- Plugin description end -->

![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ_IDEA-2025.2+-blue?logo=intellijidea&logoColor=white)
![Version](https://img.shields.io/badge/version-2.4.1-green)
![License](https://img.shields.io/badge/license-MIT-blue)

## Features

- Full-text search across your project with real-time, streaming results
- Search and replace with confirmation dialogs and inline per-match / per-file replace buttons
- VS Code-style toggle options: Match Case (`Aa`), Whole Word (`Ab`), Regex (`.*`), Preserve Case (`aB`)
- Glob-based include/exclude file filters (e.g. `src/**`, `*.ts`, `**/node_modules/**`)
- Search only in open editors
- Use exclude settings and ignore files toggle
- Search history navigation with `↑` / `↓` arrow keys
- Collapsible replace input via a VS Code-style arrow toggle
- Tree-view results with highlighted matches, collapse/expand all, and double-click to navigate to source
- Loading spinner during search

## Installation

### From JetBrains Marketplace

Search for "VSCode Search Panel" in `Settings → Plugins → Marketplace`.

### Manual

1. Download the latest release `.zip` from [Releases](https://github.com/y1j2x34/intellij-search-plugin/releases)
2. In your IDE, go to `Settings → Plugins → ⚙️ → Install Plugin from Disk...`
3. Select the downloaded file and restart

## Usage

Open the panel with `Ctrl+Alt+Shift+F`, or find "Open VSCode Search Panel" in the main menu.

Type your query and press `Enter` to search. Click the `►` arrow to expand the replace input. Use the toggle buttons in the search field to enable case sensitivity, whole word, or regex matching.

Results appear in a tree grouped by file. Double-click any match to jump to that line in the editor. Hover over a result to see inline replace and dismiss buttons.

## Building from Source

```bash
./gradlew buildPlugin
```

The plugin artifact will be in `build/distributions/`.

To run a sandboxed IDE instance with the plugin loaded:

```bash
./gradlew runIde
```

## Compatibility

- IntelliJ IDEA 2025.2+
- All JetBrains IDEs based on the IntelliJ Platform

## License

[MIT](LICENSE)
