# Foco 0.9.1

versionName `0.9.1`, versionCode `16`. Same `signing/foco-debug.keystore`. Editable pages from 0.9.0 stay.

## Drag to group

Personal and Trabajo still use the same hold-then-drag. The 0.9.0 pager was winning that gesture.

`homeTileGesture` used to give up as soon as the finger moved past touch slop before 140 ms. On a pager, that horizontal tremor is a page swipe, so the icon never lifted. A vertical move still goes to the list. A fast horizontal move still changes page. A slow horizontal drift is consumed until the hold arms, then the drag starts. Drop rules are unchanged: overlap and dwell to create or join a group, drag out to remove, Personal ↔ Trabajo snaps back.

Gesture: press an icon and hold about a fifth of a second without flicking, then drag it onto another icon or a folder. A long-press that never moves still opens the sheet.

## PIN

Ajustes → **Pedir PIN**. The first time, enter 4 digits and repeat them. **Cambiar PIN** asks for the current PIN, then the new one twice. Turning the switch off asks for the current PIN.

While it is on, every launch of a non-Foco activity asks again. There is no grace period. A wrong PIN stays on the pad and does not wipe anything. Home paging, page edit, Foco Ajustes, and the system-settings escape do not ask.

The PIN is a salted SHA-256 hash in `EncryptedSharedPreferences` (`foco_pin`), not in the launcher DataStore JSON. If the keystore cannot be opened, the gate stays off so Ajustes still works.

## Pauses and notifications

The listener still cancels. Nothing here writes system Do Not Disturb or device-policy quiet mode.

- **Pausar avisos** — same silence as 0.8/0.9. Spares calls, alarms, navigation, transport, media, and protected system packages.
- **Pausar Trabajo** — still hides the Trabajo section and cancels in-scope work notifications.
- **Pausar teléfono** — chip on Personal, and a switch in Ajustes. Cancels every notification the listener can see, including ones the quieter pauses spare. Android may repost a call, a foreground service, or a system notification; those cancels fail soft. Turning it off stops new cancels and does not bring back ones already removed. It persists like the other pauses.
- **Pausar app** — long-press a Personal or Trabajo icon. That package does not launch until **Reanudar app**, and the listener cancels notifications from that package. A paused app wins over the PIN: the launch does not start.

Full-phone pause does not replace Pausar Trabajo. Trabajo stays hidden only while Pausar Trabajo is on.

## Santoral

No network. Vetus is `app/src/main/assets/santoral_1962.json` plus computus in `Santoral1962`. Novus is `santoral_novus.json` plus the same Easter computus in `SantoralNovus`. The visible labels are **Vetus** and **Novus**.

The civil date is `America/Argentina/Cordoba`, the same zone as Plan Ragazzini. The Reloj page reads the clock when it resumes and again at each minute, including midnight, so the saint follows the date without a reinstall.

## Spacing

`FocoSpace` is one step larger than the 0.9.0 paddings (page inset 32dp, section gaps 22dp, stack gaps 12dp). Icon glyphs stay 48dp. The page dots’ hit target is 48dp. The same scale is used on Reloj, Personal, Comida, Trabajo, the edit sheet, the PIN pad, and Settings rows.
