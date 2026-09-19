# Foco Launcher — onboarding Notification Listener (v0)

**De:** Camila · UX  
**Para:** Nico · FORGE · Agus  
**Cuándo:** post-MVP 0.1 / entrada a 0.2 (o al activar “Silenciar notificaciones”)  
**Objetivo:** que entienda que Foco va a **silenciar notifs de apps que no eligió**.  
**Voz:** español AR, suave, seco. Sin culpa, sin gamificación.  
**OEM:** Motorola / sideload — copy honesto si el path de Ajustes varía.

---

## Regla de producto (una frase)

> Si la app no está en tu lista de Foco, no debería interrumpirte.

Solo eso. No filtramos “promos vs importantes” en v0.

---

## Cuándo mostrar esta pantalla

1. Primera vez que el usuario prende en Ajustes: **`Silenciar otras notificaciones`** (OFF → ON), **o**  
2. Banner en home/Ajustes si la feature está ON pero el listener **no** está concedido.

No meterlo en el setup inicial del 0.1 (home + whitelist + huella).

---

## Una sola pantalla (Compose)

```
Column(padding 24) {
  Text("Notificaciones")                    // titleLarge
  Text(
    "Foco puede silenciar avisos de apps que no elegiste.\n" +
    "Las de tu lista siguen igual."
  )                                         // body, muted
  Text(
    "Android pide permiso de acceso a notificaciones.\n" +
    "En la lista, activá Foco."
  )                                         // body small
  Spacer
  Button("Abrir Ajustes")                   // deep link NLS
  TextButton("Ahora no")
}
```

### Deep link
`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`  
(si falla en algún OEM: `ACTION_SETTINGS` + copy de respaldo abajo).

### Al volver (onResume)
- Si listener **concedido** → cerrar pantalla / snackbar `Listo.` · feature queda ON  
- Si **no** → quedarse o banner: `Todavía no está activo. Activá Foco en la lista.`

---

## Copy bank

| Clave | Texto |
|-------|--------|
| title | Notificaciones |
| body | Foco puede silenciar avisos de apps que no elegiste. Las de tu lista siguen igual. |
| how | Android pide permiso de acceso a notificaciones. En la lista, activá Foco. |
| cta | Abrir Ajustes |
| skip | Ahora no |
| pending | Todavía no está activo. Activá Foco en la lista. |
| done | Listo. |
| settings_toggle | Silenciar otras notificaciones |
| settings_sub | Perfil personal · work sin tocar |
| settings_row | Notificaciones |
| moto_hint | En Motorola: Ajustes → Notificaciones → acceso / apps con acceso. (si el deep link no abre la lista) |

---

## Ajustes Foco (entrada)

Fila que abre la pantalla **Notificaciones** (settings completa, más abajo):

```
Notificaciones                    ›
Perfil personal · work sin tocar
```

En esa pantalla vive el switch master + lista de apps + aviso de perfil de trabajo.

---

## Motorola / sideload (nota UX)

- El permiso es del **sistema**, no “ajustes restringidos” de Accesibilidad (otro producto).  
- Si el deep link abre una lista vacía o genérica: mostrar `moto_hint` como texto secundario bajo el botón.  
- No culpar al usuario ni a Motorola.

---

## Criterios de aceptación

- [ ] 1 pantalla; un CTA primario a settings NLS  
- [ ] Copy deja claro: **no elegida = silenciada**; whitelist = igual  
- [ ] Sin permiso, la feature no se presenta como activa  
- [ ] Skip no rompe el launcher  
- [ ] Cero gamificación / culpa / wellness  

---


---

## Settings — quién puede notificar (perfil personal)

Después del permiso NLS (o desde Ajustes → Notificaciones):

```
Scaffold(topBar = { Back; "Notificaciones" }) {
  // master
  Row {
    Text("Silenciar otras notificaciones")
    Switch(checked = filteringOn)
  }
  Text("En el perfil personal, solo avisan las apps que marques abajo.")

  // honestidad work profile — siempre visible si el device tiene perfil de trabajo
  if (hasWorkProfile) {
    Card/InfoBlock {
      Text("Perfil de trabajo")
      Text(
        "Foco no gestiona el perfil de trabajo. " +
        "Esas notificaciones siguen llegando con normalidad."
      )
    }
  }

  if (filteringOn && listenerGranted) {
    Text("Apps que pueden avisar")  // section header
    LazyColumn {
      items(personalWhitelistApps) { app ->
        Row(Icon, label, Switch(allowNotif))
      }
    }
    Text("Las apps que no están en Foco no avisan.")  // footer muted
  } else if (filteringOn && !listenerGranted) {
    Text("Falta el permiso del sistema.")
    Button("Abrir Ajustes") // misma pantalla onboarding NLS
  }
}
```

### Defaults
- Al prender el master: cada app **ya en la whitelist personal** arranca con `allowNotif = true`.  
- App nueva a la whitelist: `allowNotif = true` por defecto (mismo espíritu que bio on; acá “puede avisar”).  
- App fuera de whitelist: no aparece en esta lista; se silencia si el listener está activo.  
- Solo **apps del perfil personal** (user 0). No listar ni togglear packs del work profile.

### Relación whitelist ↔ notifs
| App | En home Foco | Notifica (si master ON + NLS) |
|-----|--------------|------------------------------|
| Personal, en lista, switch ON | Sí | Sí |
| Personal, en lista, switch OFF | Sí | No (silenciada a propósito) |
| Personal, fuera de lista | No | No |
| Work profile (cualquier) | N/A / no gestionada | **Sí, por defecto del sistema** — Foco no toca |

---

## Copy — perfil de trabajo (obligatorio si hay work profile)

| Clave | Texto |
|-------|--------|
| work_title | Perfil de trabajo |
| work_body | Foco no gestiona el perfil de trabajo. Esas notificaciones siguen llegando con normalidad. |
| work_body_short | El perfil de trabajo no se gestiona; sus avisos llegan igual. |
| notif_section | Apps que pueden avisar |
| notif_footer | Las apps que no están en Foco no avisan. |
| notif_master_sub | En el perfil personal, solo avisan las apps que marques abajo. |
| personal_only_note | Esta lista es del perfil personal. |

Mostrar el bloque work:
- Si `UserManager` / APIs indican perfil de trabajo presente, **siempre** en esta pantalla (aunque el master esté OFF), para no generar falsa expectativa.  
- Si no hay work profile: no mostrar el bloque (evitar ruido).

---

## Criterios de aceptación (extra)

- [ ] Lista solo apps **personales** de la whitelist, con toggle por app  
- [ ] Default allow notif = ON al agregar a Foco  
- [ ] Copy work profile visible cuando existe; deja claro que **no se gestiona** y **llegan por defecto**  
- [ ] Ningún control UX pretendiendo silenciar el work profile  
- [ ] Master ON sin NLS → no simular filtrado  

---

## Fuera de este delta (sigue fuera)

Filtrar por canal/categoría dentro de una app, heads-up “perfectos”, gestionar o silenciar perfil de trabajo, Accessibility.
