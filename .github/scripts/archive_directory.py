"""Archive a complete app directory without macOS Finder metadata."""
from pathlib import Path
import sys
import zipfile


def archive_directory(source, destination):
    source = Path(source)
    if not source.is_dir():
        raise FileNotFoundError(source)
    with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(source.rglob('*')):
            relative = path.relative_to(source.parent)
            if any(part in {'.DS_Store', '__MACOSX'} or part.startswith('._') for part in relative.parts):
                continue
            if path.is_file():
                archive.write(path, relative.as_posix())


if __name__ == '__main__':
    archive_directory(sys.argv[1], sys.argv[2])
