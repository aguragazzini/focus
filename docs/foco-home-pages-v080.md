# Home pages — 0.8.0

0.9.0 keeps this layout as the fresh-install default and adds create, rename, reorder, hide, and delete. See `docs/foco-home-pages-v090.md`.

The home is four horizontal pages. Swipe between them. The landing page is Personal.

Order:

1. Reloj — time, date, battery and the next alarm, then both sanctorales.
2. Personal — the personal whitelist, groups, and drag. **Pausar avisos** lives on this page.
3. Comida — Plan Ragazzini for the week. Today is marked. Tap a day for that day's meals.
4. Trabajo — work launchables while the page is not paused. **Pausar Trabajo** lives on this page. Drag-to-group stays inside Trabajo.

A row of dots and the current page name sit under the overflow menu. The menu still opens Ajustes, the phone settings, and the work pause, on every page. A long-press on the empty pad still opens the phone settings.

## Sanctorales

Both are local. Each line is omitted on its own if its asset cannot be read.

- «1962» is the 1962 Missal, Vetus Ordo, rubrics of 1960, in `app/src/main/assets/santoral_1962.json`.
- «Novus» is the General Roman Calendar of 1969, in `app/src/main/assets/santoral_novus.json`. Fixed celebrations are bundled. Moveable days (Easter, Lent, Advent, ordinary time, and the rest of the temporal cycle) are computed. Ascension and Corpus Christi are kept on Sunday, as in Argentina. Epiphany stays on 6 January. Guadalupe on 12 December is included for Latin America and marked as such. This is not the 1962 Missal: 1 January is Santa María, Madre de Dios, and Christ the King is the last Sunday of ordinary time.

There is no diet line on the clock page.

## Diet

`plan_ragazzini.json` and `DietPlan` are unchanged in content. The page lists Lun–Dom. Today keeps a mark even when another day is open. A weekday shows breakfast, lunch, and dinner, Agus's portion note when the plan has one, and up to three alternate titles. Saturday and Sunday show `Fin de semana · misma base (proteína + verdura)` and the three short rules. The weekday is `America/Argentina/Cordoba`. Kids' and Vane's carbs are not on the page.

## What left the single column

The old home was one vertical list: time, date, 1962 santoral, battery, diet glance, pause chips, Personal, Trabajo. Diet, the two pauses, and the two app sections are now on their own pages. Banners for “not the default home” and the notification listener sit on Personal.

Shopping lists, prices, supplements, full recipe steps, and pantry cleanup stay out of the launcher.
