# OK arquitectura — Foco Launcher semana 1

**De:** Andrés · Arquitectura → Nico / Mateo  
**Fecha:** 2026-09-19  
**Base:** `foco-launcher-arquitectura-v1.md` + `foco-launcher-android-notes-v1.md`

## Veredicto: **OK con 3 precisiones**

Mateo puede scaffoldear ya.

---

### 1) Escape hatch Settings — **OK (duro)**

Validado tal como Nico §3:

| Regla | Estado |
|-------|--------|
| Setup **antes** de `ROLE_HOME` / default | **OK** — CTA default bloqueado hasta Settings alcanzable |
| Abrir Ajustes sistema vía `Settings.ACTION_SETTINGS` (no component hardcode) | **OK** |
| Engrane fijo / long-press vacío → Settings **sin bio** | **OK** |
| `biometricRequired` en escape Settings | **siempre false** en 0.1 |
| Settings de Foco | sin bio en 0.1 |
| Bio fail-closed en apps whitelist | OK **semana 2**; **nunca** en path de Settings |

No negociable: usuario no puede brickearse por bio + whitelist vacía.

---

### 2) Módulos Gradle — **`:app` solo las primeras 48 h**

| Opción | Decisión |
|--------|----------|
| Lean `app` + `core/model|data|security` | Bien a medio plazo |
| **Semana 1 / primeras 48 h** | **Un solo `:app`** con packages `core/`, `registry/`, `security/`, `settings/` |

Motivo: menos fricción de scaffold, sync Gradle, y Mateo llega más rápido a HOME+whitelist.  
**Semana 2:** extraer `core:security` (y si duele data, `core:data`) cuando entre bio — no antes.

Nombres de packages internos = los de arquitectura v1; no hace falta multi-módulo día 1.

---

### 3) Portrait — **abierto (Camila)**

Arquitectura: **no bloquea**. Default de implementación sugerido: `portrait` lock hasta que Camila diga libre.  
Mateo: `android:screenOrientation="portrait"` en Home está bien; no esperar UX para arrancar.

---

### Confirmaciones extra (notes Nico)

- `<queries>` MAIN/LAUNCHER primero; `QUERY_ALL_PACKAGES` solo si Moto lo exige — **OK**  
- Package change invalidate, cero polling — **OK**  
- Semana 1 sin bio / sin NLS / sin A11y — **OK**  
- Device credential fallback en bio semana 2 — **OK** (evita brick sensor)  
- minSdk 26 default → subir a 29 si el Moto de Agus ≥10 — **OK**; pegar model/API tras primer sideload  

### Fuera (sin cambio)

Intents indirectos, notificaciones, reuse foco-moto-v3, backend.

---

**GO scaffold:** `com.foco.launcher`, single `:app`, checklist Nico §1 items 1–10, setup gate + Settings escape, sin bio.
