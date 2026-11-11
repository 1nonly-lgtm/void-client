# Zamienia niebo equirect (2:1) na arkusz 3x2 dla Nuit (stary 0.7 single-sprite i nowy 1.0 square-textured).
# Układ i obroty ścian są przepisane z Nuit (Utils.TEXTURE_FACES / MATRIX4F_ROTATED_FACE), więc ściany łączą się bez szwów.
# Użycie: python cube.py wejscie.png wyjscie.png [rozmiar_sciany]
import sys, numpy as np
from PIL import Image

def rx(a):
    c, s = np.cos(a), np.sin(a); return np.array([[1,0,0],[0,c,-s],[0,s,c]])
def ry(a):
    c, s = np.cos(a), np.sin(a); return np.array([[c,0,s],[0,1,0],[-s,0,c]])
def rz(a):
    c, s = np.cos(a), np.sin(a); return np.array([[c,-s,0],[s,c,0],[0,0,1]])
d = np.radians
# kolejność: 0 dół, 1 północ, 2 południe, 3 góra, 4 wschód, 5 zachód
MATS = [np.eye(3), rx(d(90)), rx(d(-90)) @ ry(d(180)), rx(d(180)),
        rz(d(90)) @ ry(d(-90)), rz(d(-90)) @ ry(d(90))]
UV = [(0,0,1/3,1/2), (1/3,1/2,2/3,1), (2/3,0,1,1/2), (1/3,0,2/3,1/2), (2/3,1/2,1,1), (0,1/2,1/3,1)]

def sample(eq, dirs):
    H, W = eq.shape[:2]
    x, y, z = dirs[...,0], dirs[...,1], dirs[...,2]
    lon = np.arctan2(x, z)                      # ta sama konwencja co sky.py (środek obrazu = +Z)
    lat = np.arcsin(np.clip(y, -1, 1))
    fx = (lon / (2*np.pi) + 0.5) * W - 0.5
    fy = (0.5 - lat / np.pi) * H - 0.5
    x0 = np.floor(fx).astype(int); y0 = np.floor(fy).astype(int)
    tx = (fx - x0)[...,None]; ty = (fy - y0)[...,None]
    def px(xx, yy): return eq[np.clip(yy,0,H-1), xx % W].astype(np.float32)
    top = px(x0,y0)*(1-tx) + px(x0+1,y0)*tx
    bot = px(x0,y0+1)*(1-tx) + px(x0+1,y0+1)*tx
    return top*(1-ty) + bot*ty

def build(src, n=512):
    eq = np.asarray(Image.open(src).convert('RGB'))
    out = np.zeros((2*n, 3*n, 3), np.float32)
    a = (np.arange(n) + 0.5) / n
    A, B = np.meshgrid(a, a)                     # A = u w ścianie, B = v w ścianie
    for f in range(6):
        p = np.stack([-1 + 2*A, -np.ones_like(A), -1 + 2*B], -1)
        dirs = p @ MATS[f].T
        dirs /= np.linalg.norm(dirs, axis=-1, keepdims=True)
        u0, v0 = int(round(UV[f][0]*3*n)), int(round(UV[f][1]*2*n))
        out[v0:v0+n, u0:u0+n] = sample(eq, dirs)
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8))

if __name__ == '__main__':
    build(sys.argv[1], int(sys.argv[3]) if len(sys.argv) > 3 else 512).save(sys.argv[2], optimize=True)
