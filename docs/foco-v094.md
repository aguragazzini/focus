# Foco 0.9.4

versionName `0.9.4`, versionCode `19`. Same `signing/foco-debug.keystore`.

## App order

Lists of apps the user sees use a Spanish (Argentina) `Collator` at secondary strength: case is ignored, accents stay secondary, and ñ is its own letter. Ties break on package, then class.

That covers the Personal whitelist, Trabajo apps, apps inside a folder, and the Agregar picker. Folder tiles stay in the order the user gave the groups. Setup suggestions stay a short curated list.

The whitelist has no separate drag-to-reorder for loose icons. Until someone uses the up/down controls in Editar apps, Personal is alphabetical. The first move freezes that visible sequence into `order` and sets `whitelistCustomOrder`. After that, Foco keeps the stored order.

## Agenda

Trabajo still reads `CalendarContract.Instances.ENTERPRISE_CONTENT_URI` when the work profile allows it. Events on a personal calendar whose display name or account name has the whole word "work" or "trabajo", or whose account type has an exchange, activesync, or work segment, are listed under Trabajo with the calendar name. Anything else stays under Personal. If the work profile itself is not readable, the page says so and still offers the work calendar. Foco does not invent events.

## Ajustes restringidos

Foco cannot draw the system overflow on App info. Ajustes explains that, and offers app info, notification access, and the all-apps list. Those intents fail soft.
