# Foco Launcher — UX v1 (MVP 0.1)

**De:** Camila · UX  
**Para:** Nico · Android · FORGE · Agus  
**Producto:** launcher Android whitelist · `com.foco.launcher`  
**Fuentes:** `launcher-android-spec-v0.md` · `foco-launcher-decisions-2026-09-19.md`  
**Alcance v0.1:** setup → default · home · whitelist (add/quitar/reordenar) · bio gate · excepción bio · Settings siempre accesible · rápido  
**Fuera v0.1:** notificaciones, intents indirectos, recomendaciones, monetización, onboarding largo, gamificación  
**Voz:** español AR, **seco y suave** (claro, sin coach, sin “laburo”, sin exclamaciones)

---

## 1. Principios UX

1. **Vacío por defecto** — no hay apps hasta que el usuario las elige.  
2. **Setup antes de default** — no se pide “usar como inicio” con home vacío.  
3. **Bio on al agregar** — cada app nueva pide huella; excepción es explícita y consciente.  
4. **Escape seguro** — Ajustes del launcher y Ajustes del sistema siempre alcanzables.  
5. **Una idea por pantalla** — sin feed, sin tips, sin gamificación.  
6. **Rápido** — home = grid/lista mínima; cero animación decorativa.

---

## 2. Mapa de pantallas v0.1

| ID | Pantalla | Rol |
|----|----------|-----|
| S0 | Bienvenida corta | 1 pantalla; qué es |
| S1 | Elegir apps mínimas | Obligatorio pre-default |
| S2 | Pedir launcher default | Intent sistema |
| H0 | Home | Solo whitelist |
| H1 | Menú overflow home | Editar / Ajustes |
| E0 | Editar apps | Add / quitar / reordenar |
| E1 | Catálogo (apps instaladas) | Marcar para whitelist |
| E2 | Excepción bio (por app) | Toggle consciente |
| B0 | Bio prompt | Sistema (BiometricPrompt) |
| A0 | Ajustes Foco | Bio global, editar apps, acerca de |
| X0 | Bloqueo suave | Si canceló bio / sin biometría |

---

## 3. Flujos

### F1 — Primer uso (setup)

```
Instala APK → abre Foco
→ S0 Bienvenida (1 tap)
→ S1 Elegir mínimas (Teléfono, Ajustes, Cámara, Mensajes sugeridos; user confirma ≥1)
→ “Listo” solo si hay ≥1 app (ideal ≥ Ajustes o Teléfono)
→ S2 Pedir “Usar como app de inicio”
→ Usuario elige Foco en el diálogo del sistema
→ H0 Home con esas apps
```

**Si salta default:** sigue en Foco como app normal; banner seco en home: `Todavía no es el inicio del teléfono.` · CTA `Elegir como inicio`.

**Si home vacío imposible post-S1:** S1 no deja continuar con 0 apps.

### F2 — Abrir app (bio on)

```
H0 tap app
→ si app.bioRequired == true → B0 BiometricPrompt
   → OK → launch app
   → cancel/fail → no abre; toast/snackbar seco opcional o silencio
→ si app.bioRequired == false → launch directo
```

### F3 — Agregar app

```
H0 → ⋮ o Ajustes → Editar apps → Agregar
→ E1 lista instaladas (no whitelisted)
→ check app(s) → Guardar
→ cada nueva: bioRequired = true (default)
→ sheet 1× por lote: “Van a pedir huella al abrir. Podés sacar la huella después en la app.”
→ vuelve a E0 / H0
```

### F4 — Excepción bio

```
E0 o detalle app → “Pedir huella” [ON]
→ al apagar: confirm
  “¿Abrir sin huella?”
  [ Seguir con huella ]  [ Abrir sin huella ]
→ solo entonces bioRequired = false
```

### F5 — Quitar / reordenar

- Quitar: swipe o 🗑 en E0 · confirm si es la última app (avisar que el home queda vacío).  
- Reordenar: long-press + drag en E0 (Compose `Reorderable`).

### F6 — Escape a Settings (siempre)

Vías simultáneas, todas v0.1:

1. Icono **Ajustes** en whitelist (sugerido en S1; si el user lo saca, warning).  
2. Overflow ⋮ en home → `Ajustes`.  
3. Gesto: long-press vacío en home → `Ajustes` (además de editar).  
4. En A0: fila `Ajustes del teléfono` → `Settings.ACTION_SETTINGS` (salida de emergencia al sistema).

Si el user intenta quitar **Ajustes del sistema** de la whitelist:  
dialog `Sin Ajustes del teléfono puede costarte salir. ¿Sacar igual?` · default cancelar.

---

## 4. Wireframes textuales (Compose-friendly)

Convención: `Column` / `Row` / `LazyVerticalGrid` · Material3 sobrio · dark o system · un acento.

### S0 — Bienvenida

```
Column(center, padding 24)
  Text("Foco")                     // titleLarge
  Text("Solo ves las apps que elegís.\nEl resto no aparece.")  // body, muted
  Spacer
  Button("Empezar")
```

Copy: seco. Sin “tu viaje hacia…”.

**Aceptación**
- [ ] Un solo CTA  
- [ ] ≤ 2 frases  
- [ ] No se puede skipear a home vacío sin S1  

---

### S1 — Elegir apps mínimas

```
Column {
  Text("Elegí lo esencial")
  Text("Después podés sumar o sacar.")
  LazyColumn {
    // sugeridas primero, checked=true por default pero editables:
    Row Check + Icon + "Teléfono"
    Row Check + Icon + "Ajustes"
    Row Check + Icon + "Mensajes"
    Row Check + Icon + "Cámara"
    Divider
    // resto instaladas, unchecked
    ...
  }
  Button("Continuar", enabled = selectedCount >= 1)
}
```

**Aceptación**
- [ ] 0 seleccionadas → Continuar disabled  
- [ ] Sugeridas aparecen arriba  
- [ ] Al continuar, cada seleccionada entra whitelist con `bioRequired=true`  
- [ ] Persiste antes de S2  

---

### S2 — Default launcher

```
Column {
  Text("Usar Foco como inicio")
  Text("Así, al tocar Inicio, ves solo tu lista.")
  Button("Elegir como inicio")  // lanza ROLE / home picker
  TextButton("Ahora no")
}
```

**Aceptación**
- [ ] Abre picker del sistema  
- [ ] Si Foco ya es default → salta a H0  
- [ ] “Ahora no” → H0 + banner  

---

### H0 — Home

```
Scaffold {
  // sin top app bar pesada
  Box {
    if (whitelist.isEmpty()) {
      Text("No hay apps todavía.")
      TextButton("Agregar apps")
    } else {
      LazyVerticalGrid(columns = 4) {  // o LazyColumn lista; default grid 4
        items(whitelistOrdered) { app ->
          Column(tap → openWithBio) {
            Icon(app)
            Text(app.label, maxLines=1)
          }
        }
      }
    }
    if (!isDefaultLauncher) {
      Banner("Todavía no es el inicio del teléfono.") + TextButton("Elegir")
    }
  }
  // long-press empty → menu: Editar apps | Ajustes
  // o IconButton ⋮ esquina
}
```

**Visual:** mucho aire, íconos sistema, tipografía pequeña en labels, sin widgets, sin reloj obligatorio (opcional status system).

**Aceptación**
- [ ] Solo whitelist  
- [ ] Tap → bio si corresponde → abre  
- [ ] Cold start home &lt; sensación nativa (meta eng: medir)  
- [ ] ⋮ o long-press llega a Editar y Ajustes  
- [ ] Cero drawer “todas las apps”  

---

### E0 — Editar apps

```
Scaffold(topBar = { Back; "Apps visibles"; Done }) {
  LazyColumn {
    items(whitelist, reorderable) {
      Row(dragHandle, Icon, label,
          Switch "Huella" OR chevron a E2,
          IconButton remove)
    }
  }
  FAB or BottomButton "Agregar app" → E1
}
```

**Aceptación**
- [ ] Reordenar persiste  
- [ ] Quitar actualiza home al volver  
- [ ] Switch/chevron bio refleja `bioRequired`  
- [ ] Agregar abre E1  

---

### E1 — Catálogo

```
Scaffold("Agregar apps") {
  SearchField (opcional v0.1; si no, lista A-Z)
  LazyColumn {
    items(installedNotInWhitelist) {
      Row(Check, Icon, label)
    }
  }
  Button("Agregar", enabled = checked ≥ 1)
}
```

Post-guardado: bio default on + snackbar: `Van a pedir huella al abrir.`

**Aceptación**
- [ ] No lista apps ya en whitelist (o las muestra disabled)  
- [ ] Nuevas con bio on  
- [ ] Cancel no muta  

---

### E2 / toggle — Excepción bio

Confirm al pasar a OFF:

```
¿Abrir sin huella?
Esta app se va a abrir directo desde Foco.

[ Seguir con huella ]   // primary soft
[ Abrir sin huella ]    // text/danger quiet
```

**Aceptación**
- [ ] No se apaga bio sin confirm  
- [ ] ON no pide confirm  
- [ ] Default al agregar = ON  

---

### B0 — Bio (sistema)

Usar `BiometricPrompt` estándar. Título app: `Desbloquear` · Sub: `{App}`.  
Negative button: `Cancelar`.

Sin biometría enrolled: dialog una vez → `Configurá una huella en el teléfono` · CTA Ajustes seguridad · fallback: permitir “abrir sin huella” solo tras confirm (o bloquear hasta enroll — **decisión v0.1 recomendada:** pedir enroll; si cancela, no abre apps con bio on).

**Aceptación**
- [ ] OK abre  
- [ ] Cancel no abre  
- [ ] No guardamos biometría propia  

---

### A0 — Ajustes Foco

```
LazyColumn {
  Row "Apps visibles" → E0
  Row "Huella al abrir (todas)"  // master: si OFF, confirm global; si ON, respeta per-app
  Row "Elegir como inicio"
  Row "Ajustes del teléfono" → system settings
  Row "Versión" 1.0.0
}
```

Master huella OFF: confirm fuerte `¿Sacar la huella en todas?`.  
Per-app sigue existiendo; master OFF fuerza todas false hasta re-ON (al re-ON, vuelve default true en todas — documentar en copy: `Al prender de nuevo, cada app pide huella.`).

**Simplificación v0.1 recomendada:** **sin master global**; solo per-app + default on al agregar. Menos estados. Master = v0.2.

**Aceptación v0.1 (sin master)**
- [ ] Apps visibles, Elegir inicio, Ajustes teléfono, Versión  
- [ ] Siempre se puede llegar acá desde home  

---

### X0 — Fallo bio

Silencio o snackbar: `No se abrió.`  
Sin culpa. Sin reintentos en loop automático (el user vuelve a tap).

---

## 5. Copy bank (seco / suave)

| Clave | Texto |
|-------|--------|
| welcome_title | Foco |
| welcome_body | Solo ves las apps que elegís. El resto no aparece. |
| welcome_cta | Empezar |
| setup_title | Elegí lo esencial |
| setup_sub | Después podés sumar o sacar. |
| setup_continue | Continuar |
| default_title | Usar Foco como inicio |
| default_body | Así, al tocar Inicio, ves solo tu lista. |
| default_cta | Elegir como inicio |
| default_skip | Ahora no |
| banner_not_default | Todavía no es el inicio del teléfono. |
| home_empty | No hay apps todavía. |
| edit_title | Apps visibles |
| add_title | Agregar apps |
| add_done | Agregar |
| bio_snack | Van a pedir huella al abrir. |
| bio_prompt_title | Desbloquear |
| bio_off_title | ¿Abrir sin huella? |
| bio_off_body | Esta app se va a abrir directo desde Foco. |
| bio_off_keep | Seguir con huella |
| bio_off_confirm | Abrir sin huella |
| remove_last_warn | Si sacás la última app, el inicio queda vacío. |
| remove_settings_warn | Sin Ajustes del teléfono puede costarte salir. ¿Sacar igual? |
| open_fail | No se abrió. |
| settings_apps | Apps visibles |
| settings_default | Elegir como inicio |
| settings_system | Ajustes del teléfono |

**Prohibido:** laburo, streaks, “¡bien!”, wellness, tips random, MELI.

---

## 6. Modelo de datos (UX → Nico)

```kotlin
data class WhitelistApp(
  val packageName: String,
  val label: String,
  val order: Int,
  val bioRequired: Boolean = true, // default al agregar
)
```

Prefs: `isOnboardingDone`, `whitelist: List<WhitelistApp>`.

---

## 7. Criterios globales MVP 0.1

- [ ] Puede ser launcher default  
- [ ] Home nunca muestra app fuera de whitelist  
- [ ] Setup obliga ≥1 app antes de empujar default  
- [ ] Bio on por default; off solo con confirm  
- [ ] Ajustes Foco + Ajustes sistema siempre alcanzables  
- [ ] Add / quitar / reorder funcionan y persisten  
- [ ] Sin notificaciones propias molestas  
- [ ] Copy según banco §5  

---

## 8. Qué no diseñar en v0.1

Drawer completo · búsqueda global de apps ocultas · notificaciones · bloqueo de intents · recomendaciones · paywall · tutorial multipaso · estadísticas.

---

## 9. Éxito de esta UX

Agus termina el setup en &lt;2 minutos, deja Foco como inicio, ve 4–8 apps, abre Teléfono (ideal excepción bio) y Mensajes (con huella) sin fricción confusa, y siempre puede entrar a Ajustes del teléfono.
