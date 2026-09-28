# Foco 0.9.5

versionName `0.9.5`, versionCode `20`. Same `signing/foco-debug.keystore`.

## Page edit

Deleting Agenda from the default five pages left the old four-page ids. The next read treated that as the 0.9.2 layout and put Agenda back. Edits now set `pageLayoutEdited`, and that splice runs only while the flag is false. Empty or fully hidden layouts still restore the defaults.

Editar home is a full screen inside the padded home column, with Nueva página pinned at the bottom, so the 3-button bar no longer covers Agregar or Quitar.

## Blocks

A page keeps its type (so Personal and Trabajo still show their grids, and landing is still the first visible Personal page). It also has an ordered list of blocks. Until the user edits them, Reloj is clock, date, battery, alarm, Vetus, and Novus; Agenda is today's events; Comida is the Ragazzini plan. Personal and Trabajo start with no extra blocks. Vetus and Novus can be removed and added on any page.

Duplicates are refused except Vacío, Nota, Contador, and Favoritos.
