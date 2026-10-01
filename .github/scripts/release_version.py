"""Validate release tags and apply their version to the CI checkout only."""
import argparse
import os
from pathlib import Path
import re


def parse_tag(tag):
    match = re.fullmatch(r"v?((0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*))(-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?", tag)
    if not match:
        raise ValueError("Tag must look like v1.0.0 or v1.0.0-beta.1")
    package_version = match[1]
    major, minor, patch = map(int, package_version.split('.'))
    if not (1 <= major <= 255 and minor <= 255 and patch <= 65535):
        raise ValueError("Installer versions require major 1..255, minor 0..255, patch 0..65535")
    return package_version + (match[5] or ''), package_version, bool(match[5])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    version, package_version, prerelease = parse_tag(os.environ['GITHUB_REF_NAME'])
    if args.apply:
        path = Path('gradle/libs.versions.toml')
        text, count = re.subn(r'^versionName\s*=\s*"[^"]*"', f'versionName = "{version}"', path.read_text(), flags=re.M)
        if count != 1:
            raise ValueError('Expected exactly one versionName in version catalog')
        path.write_text(text)
    elif os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
            output.write(f'version={version}\npackage_version={package_version}\nprerelease={str(prerelease).lower()}\n')


if __name__ == '__main__':
    main()
