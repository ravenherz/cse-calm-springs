# cse-admin

Backend for generic editor sections. Feature modules publish a form descriptor and a row store. This module renders neither HTML nor a feature entity.

## Does

- Collect `AdminSectionSource` and `AdminSectionRecords` beans.
- Serve list, create, edit, and delete for `/editor/sections/{sectionId}`.
- Serve the API desk at `/editor/api`. The page copy is `PublicApiDocs`. Session lookup and sidebar flags stay behind `AccountAccessor` and `EditorChrome`; the WAR implements both.
- Serve the logs page at `/editor/logs`, including the JSON tail and the plain-text download. The WAR reads the file through `LogTail`.
- Serve the instance page at `/editor/instance` and the JSON sample at `/editor/instance/snapshot`. The WAR probes the machine through `InstanceCapture`.
- Serve the transcode page at `/editor/transcode` and the JSON queue at `/editor/transcode/queue`. The WAR builds the rows through `TranscodeQueue`.
- Serve `/editor/scripting` and `/editor/scripting/runs`. The WAR stores scripts and run rows through `ScriptDesk`.
- Serve `/editor/settings` and `/editor/settings/save`. The WAR reads and writes the groups through `SettingsForm`.
- Serve `/editor/roles` and the role create, archive, delete, matrix, save, and transfer posts. Stores and the matrix view stay in `cse-security`.
- Serve `/editor/accounts` and `/editor/account/delete`. Login search and the account rows live with this page.
- Serve `/editor/site-data`, the zip export, and the replace import. The WAR writes the archive through `SiteDataDesk`.
- Serve `/editor/video/progress`. The WAR reads live progress and stored videos through `VideoProgressFeed`.
- Serve URL template list, create, edit, save, and delete. The WAR stores the rows through `UrlTemplateDesk`.
- Serve category list, create, edit, save, and delete. The WAR stores the rows and their pages through `CategoryDesk`.
- Serve playlist list, create, edit, save, and delete. The WAR stores the rows and their tracks through `PlaylistDesk`.
- Serve album create. The WAR stores the item and fills the edit form through `AlbumDesk`. Saving an existing album stays beside the page editor.
- Serve page list, create, edit, save, delete, category move, and editor data. The WAR stores the items through `PageDesk`.
- Serve theme list, upload, activate, reorder, and delete. The WAR installs the pack through `ThemeDesk`.
- Serve app list, upload, delete, and store limits. The WAR installs the pack through `AppDesk`.
- Serve the resource tree, upload, download, group edits, batch delete, and catalog rename. The WAR stores the files through `ResourceDesk`.
- Reject a missing required field, a bad enum, a bad boolean, a path that does not start with `/`, and an entity id that is not 24 hex characters. Those errors stay on the form.
- Pass the declared field map to the module that owns the section, then keep the field errors that module returns.

## Does not

- Depend on `cse-redirect` or any other feature entity.
- Perform the editor access check. That gate stays in `cse-security`.
- Ship the HTML pack. `app-admin` does that.
