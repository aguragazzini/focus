# GitHub public mirror

Public repo: **https://github.com/aguragazzini/focus**

**Origin (Cursor) is the source of truth.** GitHub is a mirror only. Do not rename `origin` to GitHub.

## This Cloud Agent VM (2026-09-19)

`gh` is **not** logged in. There is no `GITHUB_TOKEN` / `GH_TOKEN`, no `~/.netrc`, and no SSH key that GitHub accepts.

Attempted:

```bash
git remote add github https://github.com/aguragazzini/focus.git
GIT_TERMINAL_PROMPT=0 git push github main
```

Exact errors:

```
HTTPS: remote: No anonymous write access.
      fatal: Authentication failed for 'https://github.com/aguragazzini/focus.git/'
      (git exit 128)

SSH:   git@github.com: Permission denied (publickey).

gh:    You are not logged into any GitHub hosts. To log in, run: gh auth login
```

The public repo currently has only GitHub’s **Initial commit** (`README.md`, `a78caa01`). Our `main` was **not** mirrored. NLS 2a delivery does not depend on this push.

## Push from a machine that *is* logged in

Keep Origin as `origin`. Add the mirror remote and push `main`:

```bash
git remote add github https://github.com/aguragazzini/focus.git
# if the remote already exists:
# git remote set-url github https://github.com/aguragazzini/focus.git

git push github main
```

GitHub `main` is **not empty** (placeholder README). A normal push will be rejected as non-fast-forward until that commit is replaced. One force is OK **only** to overlay that empty README with this tree:

```bash
git push github main --force
```

After that, prefer normal `git push github main`.

## What must stay out of git

Already in `.gitignore` (verified, none of these are tracked):

- `*.apk` `*.jks` and release keystores. `signing/foco-debug.keystore` is the shared debug key and is tracked.
- `secrets.properties` `local.properties` `.env*` `google-services.json`

APKs live in the agent Artifacts panel, not in git. See `dist/TRANSFER.md`.
