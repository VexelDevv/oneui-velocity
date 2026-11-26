# Publishing this repository to GitHub

The source tree ships as a **ready-to-push git repository**: history is initialized,
all files are committed on `main`, and CI is configured so the APK is built for you
on the first push. There is nothing to reorganize — you only create the remote and push.

## 0. One-time: point the in-app link at your fork

`app/src/main/java/dev/velocity/oneui/core/BuildConstants.kt` contains the URL shown on
the app's Setup/About screen. Replace the placeholder owner with your GitHub username:

```bash
sed -i 's#https://github.com/VexelDevv/oneui-velocity#https://github.com/<YOU>/oneui-velocity#' \
  app/src/main/java/dev/velocity/oneui/core/BuildConstants.kt
git commit -am "Point in-app link at published repository"
```

## 1. Create the repository on GitHub

Any of these:

- **Web**: github.com → New repository → name `oneui-velocity` → Public → *do not* add a
  README/license/.gitignore (they all exist already) → Create.
- **gh CLI**: `gh repo create oneui-velocity --public --description "Measurement-first LSPosed performance module for Samsung One UI 8 (Android 16)" --source . --push`

## 2. Push

```bash
cd oneui-velocity
git remote add origin https://github.com/<YOU>/oneui-velocity.git
git push -u origin main
```

HTTPS will ask for credentials: use a
[Personal Access Token](https://github.com/settings/tokens) (classic, `repo` scope) as the
password, or set up SSH and use `git@github.com:<YOU>/oneui-velocity.git` instead.

> The local commit is authored with this machine's default git identity. To publish it
> under your own name: `git config user.name "<YOU>" && git config user.email "<you@example.com>"`
> then `git commit --amend --reset-author --no-edit` before pushing.

## 3. Tag the release (builds and attaches the APK)

```bash
git tag v1.0.0
git push origin v1.0.0
```

On a `v*` tag, the workflow in `.github/workflows/build.yml`:

1. builds `:app:assembleRelease` (JDK 17, Android SDK platform 36, Gradle 8.13),
2. uploads the APK as a workflow artifact (`velocity-oneui-apk`), and
3. creates a GitHub **Release** `v1.0.0` with the APK attached.

Every push to `main` also produces the artifact under **Actions → Build APK**, even without
a tag. The CI APK is debug-signed so it installs as-is; to distribute your own signed
build see the keystore steps in [INSTALL.md](INSTALL.md).

## 4. Recommended repository polish

- **About** (top right): description "Measurement-first LSPosed performance module for
  Samsung One UI 8 (Android 16)"; topics: `lsposed`, `xposed`, `one-ui`, `samsung`,
  `android`, `performance`, `kotlin`.
- Pin the `v1.0.0` release; paste the "What it actually does" table from the README into
  the release notes, plus your own measured before/after numbers from
  [BENCHMARKING.md](BENCHMARKING.md) once you have them.
- Enable **Discussions** if you want a bug-report channel; the template for reports is in
  [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
- Branch protection on `main` (require the CI build) once there are external contributors.

## 5. What is already in the repository

| Item | Where |
|---|---|
| MIT license | `LICENSE` |
| Build CI (artifact + release APK) | `.github/workflows/build.yml` |
| Gradle wrapper properties (8.13) | `gradle/wrapper/gradle-wrapper.properties` |
| Full engineering docs | `README.md`, `docs/` |
| Clean ignore rules (no build output, no keystores) | `.gitignore` |
