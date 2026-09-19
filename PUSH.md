# Publicar Foco Launcher en GitHub

Este entorno **no tiene `gh` autenticado** (ni `GITHUB_TOKEN`). El código vive en Origin. Cuando GitHub esté conectado en tu máquina, creá el repo **público** `foco-launcher` así.

## Condiciones

- Cuenta personal de GitHub autenticada: `gh auth status` debe mostrar tu usuario.
- Working tree limpio (`git status`).
- No commitear `local.properties`, `.env*`, `*.jks`, `*.keystore`, `secrets.properties`, `google-services.json`.

El `origin` de este clone apunta a **Cursor Origin**, no a GitHub. `gh repo create ... --remote=origin` falla si `origin` ya existe. Renombralo primero.

## Comandos

```bash
# 1) Login (una vez)
gh auth login

# 2) Conservar Origin como remoto aparte
git remote rename origin cursor-origin

# 3) Crear el repo público y pushear main
gh repo create foco-launcher --public --source=. --remote=origin --push
```

Repo esperado: `https://github.com/<tu-usuario>/foco-launcher`

Si `foco-launcher` ya existe vacío en tu cuenta:

```bash
git remote rename origin cursor-origin
git remote add origin git@github.com:<tu-usuario>/foco-launcher.git
git push -u origin main
```

## Qué no subir

- Keystores con password / claves privadas
- Tokens, `.env`, `secrets.properties`
- `local.properties` (ruta del SDK)
- APKs (`*.apk` está en `.gitignore`)

El APK de debug se arma en cada máquina con el keystore de debug de Gradle (`~/.android/debug.keystore`), que es local y no forma parte del repo.
