"""Configure the signed arm64 Android APK in the CI checkout."""
import os
from pathlib import Path


def property_value(value):
    # java.util.Properties reads ISO-8859-1 and treats backslashes as escapes.
    result = ''
    for char in value:
        if char in '\\ :=#!':
            result += '\\' + char
        elif char == '\n':
            result += '\\n'
        elif char == '\r':
            result += '\\r'
        elif char == '\t':
            result += '\\t'
        elif ord(char) > 127:
            result += ''.join(f'\\u{int.from_bytes(unit, "big"):04x}' for unit in
                              (char.encode('utf-16-be')[i:i+2] for i in range(0, len(char.encode('utf-16-be')), 2)))
        else:
            result += char
    return result


if __name__ == '__main__':
    module = Path('androidApp/module.yaml')
    text = module.read_text()
    if text.count('\n  android:\n') != 1:
        raise ValueError('Expected one Android settings block')
    if '\n    signing:' in text or '\n    abiFilters:' in text:
        raise ValueError('Android signing or ABI filters are already configured')

    values = {
        'storeFile': str(Path(os.environ['ANDROID_KEYSTORE_PATH']).resolve()),
        'storePassword': os.environ['ANDROID_KEYSTORE_PASSWORD'],
        'keyAlias': os.environ['ANDROID_KEY_ALIAS'],
        'keyPassword': os.environ['ANDROID_KEY_PASSWORD'],
    }
    path = Path('androidApp/keystore.properties')
    path.write_text(''.join(f'{key}={property_value(value)}\n' for key, value in values.items()), encoding='ascii')
    path.chmod(0o600)
    module.write_text(text.replace(
        '\n  android:\n',
        '\n  android:\n    abiFilters: [arm64-v8a]\n    signing:\n      enabled: true\n',
        1,
    ))
