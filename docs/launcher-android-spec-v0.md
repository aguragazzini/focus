# Especificación funcional y técnica — Launcher Android minimalista

## 1. Objetivo

Construir una aplicación Android que funcione como **launcher principal** del dispositivo y tenga dos objetivos centrales:

1. **Reducir distracciones** mostrando únicamente las aplicaciones que el usuario eligió explícitamente.
2. **Aumentar la seguridad** agregando una capa adicional de control antes de abrir aplicaciones y reduciendo exposición innecesaria.

La aplicación debe ser extremadamente liviana, rápida y predecible. No debe agregar overhead perceptible al uso normal del teléfono.

---

## 2. Principio general del producto

El launcher funciona bajo un modelo de **lista blanca estricta**:

- Por defecto, **ninguna aplicación se muestra**.
- El usuario elige manualmente qué aplicaciones quiere ver.
- Sólo las aplicaciones seleccionadas aparecen en el launcher.
- Todo lo demás queda oculto.
- Si una aplicación no está permitida, el launcher no debe ofrecer accesos indirectos a ella.

El producto debe priorizar:

- simplicidad;
- control explícito;
- mínima distracción;
- máxima performance;
- seguridad;
- comportamiento predecible.

---

## 3. Alcance funcional inicial

### 3.1 Selección manual de aplicaciones visibles

Debe existir una pantalla de configuración donde el usuario pueda:

- ver todas las aplicaciones instaladas;
- seleccionar manualmente cuáles estarán visibles;
- desmarcar aplicaciones ya seleccionadas;
- guardar la configuración.

Regla principal:

> Ninguna aplicación debe quedar visible por defecto.

El usuario construye su launcher agregando únicamente las aplicaciones que considera necesarias.

---

## 4. Home / pantalla principal

La pantalla principal del launcher debe:

- mostrar únicamente las aplicaciones permitidas;
- evitar elementos innecesarios;
- ser visualmente minimalista;
- abrir muy rápido;
- evitar animaciones costosas o decorativas;
- priorizar acceso simple y rápido a las apps seleccionadas.

No debe existir un app drawer tradicional con todas las aplicaciones instaladas.

---

## 5. Aplicaciones ocultas

Toda aplicación que no esté en la lista blanca:

- no debe aparecer en la pantalla principal;
- no debe aparecer en búsquedas del launcher;
- no debe aparecer en un drawer interno;
- no debe ser sugerida por el launcher;
- no debe aparecer como recomendación automática.

El objetivo es que esas aplicaciones sean, a efectos prácticos, invisibles desde la experiencia normal del launcher.

---

## 6. Protección biométrica

Se desea una capa adicional de seguridad antes de abrir aplicaciones.

### Requerimiento

Cuando el usuario intenta abrir una aplicación visible:

1. el launcher solicita autenticación biométrica;
2. si la autenticación es válida, abre la aplicación;
3. si falla o se cancela, no abre nada.

Prioridad:

- huella digital cuando esté disponible;
- usar las APIs biométricas estándar de Android;
- no almacenar información biométrica;
- delegar autenticación al sistema operativo.

### Consideración técnica

No todas las aperturas de aplicaciones en Android pueden interceptarse universalmente sólo con un launcher.

La primera versión debe proteger, como mínimo, todas las aplicaciones abiertas **desde el launcher**.

Si luego se quiere proteger cualquier apertura desde cualquier origen, habrá que evaluar mecanismos adicionales de Android y sus restricciones de seguridad.

---

## 7. Bloqueo de accesos indirectos

Si desde una aplicación permitida se intenta abrir otra aplicación que no está permitida, el comportamiento deseado es:

> bloquear el acceso.

Ejemplo:

- navegador permitido;
- enlace intenta abrir Instagram;
- Instagram no está permitida;
- el sistema no debería facilitar esa apertura.

### Nota técnica

Este comportamiento debe evaluarse cuidadosamente porque Android puede resolver intents fuera del launcher.

Puede requerir mecanismos adicionales y probablemente tendrá limitaciones según versión de Android y permisos disponibles.

Debe diseñarse como una capacidad separada del núcleo del launcher.

---

## 8. Gestión de notificaciones

El launcher debe ayudar a reducir distracciones también desde las notificaciones.

### Objetivo

Permitir un modo de filtrado agresivo donde sólo lleguen notificaciones consideradas importantes.

### Requerimientos

El usuario debe poder:

- habilitar o deshabilitar gestión de notificaciones;
- elegir qué aplicaciones pueden notificar;
- reducir notificaciones promocionales o irrelevantes;
- permitir sólo notificaciones relevantes cuando sea técnicamente posible.

### Principio

Las aplicaciones ocultas deberían, idealmente, no generar interrupciones visibles.

### Consideración técnica

El filtrado fino del contenido de notificaciones depende de las APIs y permisos de Android.

Puede requerir:

- Notification Listener Service;
- permisos explícitos del usuario;
- reglas por aplicación;
- eventualmente reglas por categoría o contenido.

No se debe asumir que todas las notificaciones podrán bloquearse o modificar su comportamiento con precisión absoluta.

---

## 9. Recomendaciones de aplicaciones alternativas

El launcher podrá incluir recomendaciones opcionales.

Ejemplo:

El usuario usa una aplicación pesada o distractora para una función simple, como leer PDFs.

El launcher puede sugerir:

- una alternativa más liviana;
- una app open source;
- una aplicación sin publicidad;
- una aplicación con menos funciones distractoras.

### Regla

El launcher **nunca instala, elimina o reemplaza aplicaciones automáticamente**.

Sólo recomienda.

El usuario:

- ve la recomendación;
- decide;
- instala o elimina manualmente.

---

## 10. Rendimiento

El rendimiento es un requisito crítico.

El launcher debe sentirse igual o más rápido que un launcher nativo simple.

### Requerimientos técnicos

Priorizar:

- bajo uso de CPU;
- bajo uso de memoria;
- bajo consumo de batería;
- carga inicial rápida;
- pocas operaciones en background;
- cero polling innecesario;
- caché local de metadatos de aplicaciones;
- uso mínimo de red;
- sin procesos residentes innecesarios.

### Meta conceptual

El usuario no debe sentir que agregó una capa extra al sistema.

Debe sentirse como:

> un Android más simple.

---

## 11. Principio de no intervención

La aplicación debe ser deliberadamente conservadora.

Si otra aplicación:

- falla;
- se cierra;
- no abre;
- queda colgada;
- lanza un error;

el launcher **no debe intentar repararla, reiniciarla ni intervenir**.

El launcher sólo controla:

- visibilidad;
- acceso;
- organización;
- autenticación;
- notificaciones;
- reglas de distracción.

No debe comportarse como un administrador agresivo del sistema.

---

## 12. Configuración

La configuración debe ser simple y manual.

### Pantalla mínima de ajustes

Debe permitir:

- seleccionar aplicaciones visibles;
- cambiar el orden de apps;
- activar/desactivar autenticación biométrica;
- configurar notificaciones;
- activar/desactivar recomendaciones;
- revisar permisos;
- modificar reglas futuras.

No se desea un onboarding complejo ni un asistente paso a paso.

El usuario configura directamente lo que quiere.

---

## 13. Seguridad

La aplicación debe:

- no almacenar credenciales;
- no almacenar biometría;
- usar Android Biometric APIs;
- minimizar permisos;
- explicar claramente cualquier permiso sensible;
- evitar recolectar información innecesaria;
- no enviar datos a servidores salvo que una función futura lo requiera.

Preferencia arquitectónica inicial:

> local-first.

---

## 14. Privacidad

Idealmente la versión inicial debería funcionar completamente offline.

Datos como:

- apps seleccionadas;
- preferencias;
- orden;
- configuración;
- reglas;

deben guardarse localmente.

No se requiere cuenta de usuario para el MVP.

---

## 15. UX deseada

La experiencia debe ser:

- minimalista;
- sobria;
- rápida;
- sin gamificación;
- sin feed;
- sin contenido;
- sin publicidad;
- sin recomendaciones intrusivas;
- sin notificaciones propias innecesarias.

El producto no debe transformarse en una nueva fuente de distracción.

---

## 16. Lo que NO debe tener inicialmente

Fuera del alcance del MVP:

- marketplace;
- perfiles familiares;
- modo niños;
- red social;
- ranking;
- gamificación;
- feed;
- estadísticas complejas;
- sistema de recompensas;
- IA conversacional;
- sincronización cloud;
- automatización agresiva;
- reparación o reinicio de apps;
- instalación automática;
- desinstalación automática.

---

## 17. Arquitectura sugerida para MVP

### Plataforma

- Android nativo.
- Kotlin.
- Jetpack Compose para UI.
- Android Launcher / Home Intent.
- PackageManager para consultar apps instaladas.
- DataStore para configuración.
- BiometricPrompt para autenticación.
- NotificationListenerService para capacidades futuras de filtrado.
- Arquitectura simple, idealmente MVVM liviano o similar.

### Filosofía

Evitar sobrearquitectura.

El producto es pequeño y debe mantenerse pequeño.

---

## 18. Módulos propuestos

### Core Launcher

Responsable de:

- mostrar apps permitidas;
- abrir apps;
- bloquear apps no permitidas;
- manejar navegación principal.

### App Registry

Responsable de:

- leer aplicaciones instaladas;
- guardar whitelist;
- detectar instalaciones/desinstalaciones.

### Security Layer

Responsable de:

- autenticación biométrica;
- reglas de acceso;
- fallback seguro.

### Notification Layer

Responsable de:

- observar notificaciones;
- aplicar reglas;
- reducir distracciones.

### Settings

Responsable de:

- preferencias;
- configuración;
- permisos;
- orden de apps.

### Recommendation Engine

Futuro / opcional.

Responsable de:

- recomendar alternativas más simples o seguras.

No debería formar parte del camino crítico del launcher.

---

## 19. Estados principales

### Estado normal

Se muestran únicamente apps autorizadas.

### App autorizada

Tap → biometría → apertura.

### App no autorizada

No se muestra.

### Intent hacia app no autorizada

Cuando sea técnicamente posible:

- bloquear;
- no mostrar fallback distractor;
- mantener comportamiento silencioso.

### Error externo

No intervenir.

---

## 20. MVP recomendado

### Versión 0.1

Debe tener únicamente:

1. launcher funcional;
2. lista manual de aplicaciones visibles;
3. persistencia de configuración;
4. reordenamiento simple;
5. apertura de aplicaciones;
6. autenticación biométrica antes de abrir;
7. pantalla de ajustes;
8. excelente rendimiento.

### Versión 0.2

Agregar:

- manejo básico de notificaciones;
- reglas por aplicación.

### Versión 0.3

Evaluar:

- bloqueo de intents indirectos;
- recomendaciones de apps;
- mejoras de seguridad.

---

## 21. Criterios de éxito del MVP

La primera versión está bien construida si:

- puede ser launcher predeterminado;
- arranca rápido;
- no muestra apps no elegidas;
- no pierde configuración;
- autenticación funciona de forma confiable;
- no drena batería;
- no se siente lenta;
- no genera crashes;
- no interfiere con otras apps;
- puede usarse como launcher principal durante varios días sin necesidad de volver al launcher anterior.

---

## 22. Regla de producto

Toda feature futura debe superar esta pregunta:

> ¿Esto hace que el teléfono sea más simple, más seguro o menos distractor?

Si la respuesta es no:

> no entra.

---

## 23. Estado actual de decisión

Decisiones ya tomadas:

- Android primero.
- Debe funcionar como launcher.
- Whitelist manual.
- Cero apps visibles por defecto.
- Apps no seleccionadas ocultas.
- Biometría antes de abrir apps.
- Foco fuerte en seguridad.
- Filtrado de notificaciones.
- Intentar bloquear accesos a apps no permitidas.
- Recomendaciones, no automatizaciones.
- Sin modo niños.
- Sin reparación automática de apps.
- Rendimiento como requisito prioritario.
- Arquitectura simple.
- Preferencia local-first.
- MVP incremental.

---

## 24. Documento vivo

Este archivo debe tratarse como una especificación viva.

En futuras sesiones se podrán:

- agregar funcionalidades;
- eliminar funcionalidades;
- modificar prioridades;
- agregar decisiones técnicas;
- registrar limitaciones encontradas durante desarrollo;
- transformar requerimientos en backlog;
- agregar criterios de aceptación;
- documentar decisiones arquitectónicas.

La prioridad es mantener una única fuente de verdad clara sobre qué se está construyendo y por qué.
