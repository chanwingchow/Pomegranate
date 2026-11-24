"""Collect the single release APK produced by the native Toolchain Android task."""
import os
from pathlib import Path
import shutil
from zipfile import ZipFile

# Toolchain 0.13 copies the final APK to the task root. Its internal Gradle
# workspace can contain a second copy; only collect the published task result.
files = [p for p in Path('build/tasks/_androidApp_buildAndroidRelease').glob('*.apk')
         if not p.name.endswith('-unsigned.apk')]
if len(files) != 1:
    raise RuntimeError(f'Expected exactly one Android release APK, found {len(files)}: {files}')

abi = 'arm64-v8a'
with ZipFile(files[0]) as apk:
    abis = {name.split('/')[1] for name in apk.namelist()
            if name.startswith('lib/') and name.endswith('.so')}
if abis != {abi}:
    raise RuntimeError(f'Expected a release APK for {abi}, found native libraries for {sorted(abis)}')

Path('artifacts').mkdir(exist_ok=True)
shutil.copy2(files[0], f"artifacts/Pomegranate-{os.environ['RELEASE_VERSION']}-android-{abi}-release.apk")
