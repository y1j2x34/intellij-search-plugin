<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# VSCode Search Panel Changelog

## [1.0.0]

### Added
- VS Code-style search and replace tool window panel for JetBrains IDEs
- Full-text search across project files with real-time streaming results
- Search and replace functionality with confirmation dialogs
- VS Code-style toggle options: Match Case (`Aa`), Whole Word (`Ab`), Regex (`.*`)
- Preserve Case (`aB`) option for both search and replace operations
- Glob-based include/exclude file filters (e.g. `src/**`, `*.ts`, `**/node_modules/**`)
- Search only in open editors filter
- Use exclude settings and ignore files toggle
- Search history navigation with `↑` / `↓` arrow keys in the search field
- Collapsible replace input via a VS Code-style arrow toggle
- Tree-view results grouped by file with highlighted matches
- Collapse/expand all button for search result tree
- Double-click on a match to navigate to the source line in the editor
- VS Code-style inline replace buttons (per-match and per-file) on hover
- Inline dismiss buttons to remove individual matches or files from results
- Loading spinner animation during search operations
- Placeholder text in search, replace, include, and exclude fields
- Keyboard shortcut `Ctrl+Alt+Shift+F` to open the search panel
- Menu action "Open VSCode Search Panel" in the main menu
- EDT thread safety for UI updates
- Optimized file iteration using `ProjectFileIndex`

