# Desktop release packaging

`./kotlin do packageDesktop -m desktopApp` shrinks and obfuscates the desktop runtime with ProGuard, imports the Compose 1.12.1 defaults and dependency consumer/service rules, then calls JDK `jpackage`.

Installers are written to `desktopApp/build/release/desktop/{dmg,msi,deb}`. `PACKAGE_FORMAT=app-image` writes a portable application with its Java runtime to `desktopApp/build/release/desktop/app`.

Environment variables:

- `PACKAGE_VERSION`: numeric installer version; defaults to the catalog's `versionName`.
- `PACKAGE_FORMAT`: `dmg`, `msi`, `deb` or `app-image`; takes precedence over `PACKAGE_EXTENSION`.
- `PACKAGE_EXTENSION`: CI's installer format; defaults to the host's installer format.
- `JPACKAGE_JAVA_HOME`: full JDK containing `jpackage` and `jmods`; falls back to `JAVA_HOME`, then the build JVM. Use JDK 21 to match CI.

Run packaging on its target OS. Windows requires WiX; Linux DEB packaging requires the system packaging tools. The task does not sign, upload or publish installers.
