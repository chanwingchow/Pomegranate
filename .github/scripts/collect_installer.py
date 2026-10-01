"""Give installers unique, predictable names before merging CI artifacts."""
import os
from pathlib import Path
import shutil

extension = os.environ['PACKAGE_EXTENSION']
files = list(Path('desktopApp/build/compose/binaries/main-release').rglob(f'*.{extension}'))
if len(files) != 1:
    raise RuntimeError(f'Expected one {extension} installer, found {len(files)}')
Path('artifacts').mkdir(exist_ok=True)
shutil.copy2(files[0], f"artifacts/Pomegranate-{os.environ['RELEASE_VERSION']}-{os.environ['PACKAGE_PLATFORM']}.{extension}")
