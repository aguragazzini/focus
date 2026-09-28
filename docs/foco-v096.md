# Foco 0.9.6

versionName `0.9.6`, versionCode `21`. Same `signing/foco-debug.keystore`.

## Centered glance blocks

Home block captions and values are centered on a full-width column. Reloj keeps the page name at the top and shows only the digits in the block. Vetus and Novus keep one caption when they are separate blocks. Fecha, Batería, and Alarma stack a centered label over a centered value.

Personal and Trabajo section titles are centered. Their app grids stay as they were. Agenda event rows stay a list. Page edit from 0.9.5 is unchanged.

## One pill

One pill, a bell plus the mode that is on. Tap cycles **Sin pausa → Foco → Todo en pausa → Sin pausa**.

- Sin pausa: Foco does not cancel.
- Foco: the in-scope cancel. Calls, alarms, navigation, transport, media, and protected system packages stay.
- Todo en pausa: cancels every notification the listener can see, and the pill uses a stronger Paper border.
- Without a listener the pill says **Activar avisos** and opens the existing permission flow.
- If the mode cannot be saved, home shows **No se pudo cambiar el modo.**

A phone pause that was already on opens on Todo en pausa and stays there until a tap. Avisos alone opens on Foco. Pausar Trabajo stays off this cycle. A long-press still pauses one app. Nothing writes system Do Not Disturb.

## Default Reloj rhythm

Digits, then the date, then `Santo: {nombre}` at 13sp when a name exists, then battery and alarm on one line, then the temperature when it answers, then the pill. Edited pages keep their own blocks. Personal keeps the pill under its glances and above the grid.
