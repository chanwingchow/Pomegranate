"""Zip the entire Compose Windows application image, including its JVM."""
import os
from pathlib import Path
from archive_directory import archive_directory

image_root = Path('desktopApp/build/compose/binaries/main/app')
applications = [path for path in image_root.iterdir() if path.is_dir() and list(path.glob('*.exe'))]
if len(applications) != 1:
    raise RuntimeError(f'Expected one Windows application image, found {len(applications)}')
application = applications[0]
if not (application / 'app').is_dir() or not (application / 'runtime').is_dir():
    raise RuntimeError('Application image is missing dependencies or the bundled Java runtime')

Path('artifacts').mkdir(exist_ok=True)
archive_name = f"artifacts/Pomegranate-{os.environ['RELEASE_VERSION']}-windows-x64-portable"
# Keep the application folder so extraction does not scatter files.
archive_directory(application, archive_name + '.zip')
