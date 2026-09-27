# Foco 0.9.3

versionName `0.9.3`, versionCode `18`. Same `signing/foco-debug.keystore`.

## Default page order

Reloj, Agenda, Personal, Comida, Trabajo. Landing is still the first visible Personal page, which is index 2 on that default set.

An empty stored list resolves to those five pages. A stored list that is exactly the old four defaults (`page-clock`, `page-personal`, `page-diet`, `page-work`, no custom labels, none hidden) gains Agenda after Reloj on read. Any other saved layout is left as the user arranged it. Agenda can still be added from Editar home → Nueva página.

## Agenda

Today is `America/Argentina/Cordoba`, same zone as Reloj. The page reloads on resume and at each minute, including midnight.

Personal events are instances from this user's calendar provider. Trabajo events are instances from `CalendarContract` enterprise URIs (API 29+), and only after that calendar query returns at least one row. A calendar display name does not move an event between sections. Work rows do not include a calendar name, because that column is not on the enterprise projection. If the enterprise query is empty or fails, or the phone is below API 29, Trabajo is marked unreadable and the page offers the work Calendar app. It does not show an empty work day in that case.

`READ_CALENDAR` is requested from Agenda and from Ajustes → Agenda. The first ask is the system dialog. After a permanent denial, the next ask opens Foco's app info. There is no saint or calendar HTTP API.
