import os
import xml.etree.ElementTree as ET

root = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'res')
root = os.path.normpath(root)
print('Scanning res directory:', root)
errors = 0
for dirpath, dirs, files in os.walk(root):
    for f in files:
        if f.lower().endswith('.xml'):
            p = os.path.join(dirpath, f)
            try:
                ET.parse(p)
            except Exception as e:
                print('PARSE ERROR:', p)
                print('  ', repr(e))
                errors += 1

if errors == 0:
    print('All XML files parsed OK')
else:
    print('Found', errors, 'XML parse errors')

