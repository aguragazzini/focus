# Editable home pages — 0.9.0

0.9.0 keeps the 0.8.0 home and lets the user own the pager. versionName `0.9.0`, versionCode `15`. The debug APK is still signed with `signing/foco-debug.keystore`.

## What stayed from 0.8.0

The default home is still four swipe pages, in this order, landing on Personal:

1. Reloj — time, date, battery and the next alarm, Vetus 1962 and Novus. No diet on the clock.
2. Personal — whitelist, groups, drag, **Pausar avisos**. Banners for “not the default home” and the notification listener sit on the first visible Personal page.
3. Comida — Plan Ragazzini by weekday, today marked, day sheet unchanged.
4. Trabajo — work apps while the page is visible, **Pausar Trabajo**, drag-to-group inside Trabajo.

Overflow still opens Ajustes, the phone settings, and the work pause on every page. A long-press on an empty pad still opens the phone settings. Monochrome and names-only stay opt-in. Pausar avisos still cancels in-scope notifications. It does not write system Do Not Disturb. Pausar Trabajo still does not write quiet mode. There is no `AppWidgetHost`.

## Edit model

Two ways in:

- Long-press the page name under the dots.
- Ajustes → **Editar home**. That brings the launcher forward and opens the same sheet, then closes Ajustes.

The sheet is ink on paper. **Listo** closes it. For each page, including hidden ones:

- Tap the name to rename it. A blank name clears the custom label and the type name shows again. Names are trimmed and capped at 24 characters, same as groups.
- **Subir** / **Bajar** swap that page with its neighbor in the stored list. Hidden pages stay in that list, so order is the full layout, not only what is on screen.
- **Ocultar** keeps the page and drops it from the pager. **Mostrar** puts it back. The control is absent when hiding would leave nothing on screen.
- **Quitar** deletes the page. It is absent when the delete would leave no visible page.
- **Nueva página** picks Reloj, Personal, Apps, Comida, or Trabajo. The new page is appended and visible. The list stops at 12 pages.

The pager keeps at least one visible page. Ajustes stays reachable from the overflow menu, from a long-press on an empty pad, and from `foco://settings`.

## Persistence

Pages live on `LauncherPrefs.pages` in the same DataStore document as groups (`foco_launcher_prefs` / `launcher_prefs_json`).

An empty list means “not customized”. The UI resolves that to the four defaults and does not write them until the first real edit. The first edit stores the resolved list, so later launches do not depend on the implicit default.

`HomePages.resolve` runs on every read:

- Unknown types, blank ids, and duplicate ids are dropped.
- Labels are trimmed, inner whitespace is collapsed, and the name is cut at 24 characters.
- If nothing valid remains, or every remaining page is hidden, the four defaults are restored. The rest of the prefs document is left alone. A wholly unreadable JSON document still resets to `LauncherPrefs()`, which has an empty page list and therefore the same four pages.

Landing is not stored. It is the first visible Personal page, or the first visible page when Personal is hidden or absent.

## Type rules

The same type may appear more than once. Extra pages share the underlying data. They are not a second catalog.

| Type | What it shows |
| --- | --- |
| `CLOCK` | The same clock, battery, alarm, and both sanctorales. |
| `PERSONAL` | The personal whitelist, groups, and drag. **Pausar avisos** on every Personal page. Banners only on the first visible one. |
| `APPS` | The same personal grid, without Pausar avisos and without banners. It looks empty only when Personal is empty. It is not its own whitelist. |
| `DIET` | The same Plan Ragazzini. |
| `WORK` | The same work catalog, pause, and Trabajo groups. |

## Out of this cut

Open-Meteo, a dock, drag-reorder of whole groups, shopping lists, prices, supplements, R8, keystore rotation, and `AppWidgetHost`. Page order is the up/down controls in the sheet.
