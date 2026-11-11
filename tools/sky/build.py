# Buduje tekstury nieb do jara: arkusze 3x2 dla Nuit i miniatury do menu.
# Najpierw: python sky.py (tworzy equirect *.png w bieżącym folderze), potem:
#   python build.py <folder textures moda>
import os, sys
from PIL import Image
from cube import build

NAMES = ['pustka', 'noc', 'zachod', 'zorza', 'end', 'mglawica']
out = sys.argv[1]
os.makedirs(os.path.join(out, 'sky'), exist_ok=True)
os.makedirs(os.path.join(out, 'gui', 'sky'), exist_ok=True)
for n in NAMES:
    build(n + '.png', 512).save(os.path.join(out, 'sky', n + '.png'), optimize=True)
    eq = Image.open(n + '.png').convert('RGB')
    w, h = eq.size
    # miniatura: pas nieba nad horyzontem (środek obrazu = kierunek, w który patrzy niebo)
    crop = eq.crop((w // 4, int(h * 0.12), w * 3 // 4, h // 2 + h // 40))
    crop.resize((256, 128), Image.LANCZOS).save(os.path.join(out, 'gui', 'sky', n + '.png'), optimize=True)
    print('niebo', n)
