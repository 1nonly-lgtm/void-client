import numpy as np
from PIL import Image
W,H=2048,1024
lon=(np.arange(W)+0.5)/W*2*np.pi-np.pi
lat=np.pi/2-(np.arange(H)+0.5)/H*np.pi
LON,LAT=np.meshgrid(lon,lat)
X=np.cos(LAT)*np.sin(LON); Y=np.sin(LAT); Z=np.cos(LAT)*np.cos(LON)
D=np.stack([X,Y,Z],-1)

def hash3(ix,iy,iz,seed=0):
    h=(ix*73856093) ^ (iy*19349663) ^ (iz*83492791) ^ (seed*2654435761)
    h=(h ^ (h>>13))*1274126177
    return ((h ^ (h>>16)) & 0xffffff)/float(0xffffff)
def vnoise(P,freq,seed=0):
    p=P*freq; i=np.floor(p).astype(np.int64); f=p-i; f=f*f*(3-2*f)
    out=0
    for dx in (0,1):
        for dy in (0,1):
            for dz in (0,1):
                w=(f[...,0] if dx else 1-f[...,0])*(f[...,1] if dy else 1-f[...,1])*(f[...,2] if dz else 1-f[...,2])
                out=out+w*hash3(i[...,0]+dx,i[...,1]+dy,i[...,2]+dz,seed)
    return out
def fbm(P,freq,oct=5,seed=0,warp=True):
    if warp:
        q=np.stack([fbm(P,freq*0.7,3,seed+50,False),fbm(P+5.2,freq*0.7,3,seed+60,False),fbm(P+1.3,freq*0.7,3,seed+70,False)],-1)
        P=P+(q-0.5)*1.2
    s=0;a=0.5;n=0
    for o in range(oct):
        s=s+a*vnoise(P,freq*(2**o),seed+o); n+=a; a*=0.5
    return s/n
def stars(img,density,seed,bright=1.0,tint=None,mask=None):
    # gwiazdy w komórkach siatki 3D – ciągłe na całej sferze
    for cell,th,sz in [(260,0.9965,0.0011),(110,0.988,0.0019)]:
        p=D*cell; i=np.floor(p).astype(np.int64)
        r=hash3(i[...,0],i[...,1],i[...,2],seed+cell)
        c=(i+0.5+ (np.stack([hash3(i[...,0],i[...,1],i[...,2],seed+k) for k in (1,2,3)],-1)-0.5)*0.6)/cell
        cn=c/np.linalg.norm(c,axis=-1,keepdims=True)
        dist=np.linalg.norm(D-cn,axis=-1)
        on=(r>th-(density-1)*0.004)
        b=np.exp(-(dist/sz)**2)*on*bright*(0.5+0.5*hash3(i[...,0],i[...,1],i[...,2],seed+9))
        if mask is not None: b=b*mask
        col=np.array(tint if tint is not None else [255,255,255],float)
        img+= b[...,None]*col
    return img
def grad(stops):
    # stops: list of (lat_deg, rgb) from bottom to top
    lats=np.degrees(LAT); out=np.zeros(LAT.shape+(3,))
    for k in range(len(stops)-1):
        a0,c0=stops[k]; a1,c1=stops[k+1]
        t=np.clip((lats-a0)/(a1-a0),0,1); t=t*t*(3-2*t)
        m=(lats>=a0)&(lats<=a1)
        out[m]=(np.array(c0)*(1-t[m,None])+np.array(c1)*t[m,None])
    out[lats<stops[0][0]]=stops[0][1]; out[lats>stops[-1][0]]=stops[-1][1]
    return out.astype(float)
def save(name,img):
    Image.fromarray(np.clip(img,0,255).astype(np.uint8)).save(name+'.png')

# 1. Pustka
img=grad([(-90,(4,2,10)),(-5,(14,6,30)),(10,(30,12,60)),(40,(16,7,36)),(90,(8,4,20))])
n=fbm(D,2.2,5,1); n2=fbm(D+3,4,4,7)
neb=np.clip((n-0.45)*3,0,1)**1.6
img+= neb[...,None]*np.array([120,45,200])*0.8 + (np.clip((n2-0.55)*4,0,1)**2)[...,None]*np.array([220,70,230])*0.5
stars(img,1,11,255)
# kula pustki z pierścieniem nad horyzontem (kierunek)
o=np.array([0.0,0.25,1.0]); o/=np.linalg.norm(o); ang=np.degrees(np.arccos(np.clip(D@o,-1,1)))
R=9
img+= (np.exp(-((ang-R)/4)**2)*0.9)[...,None]*np.array([168,85,247])
ring=np.exp(-((ang-R)/0.5)**2); img=img*(1-ring[...,None])+np.array([200,140,255])*ring[...,None]
inner=ang<R*0.95; img[inner]=np.array([8,3,18])
# dysk akrecyjny – spłaszczona elipsa w płaszczyźnie
u=np.cross(o,[0,1,0]); u/=np.linalg.norm(u); v=np.cross(u,o)
px=D@u; py=D@v; front=(D@o)>0
e=np.sqrt((px/np.radians(R*2.3))**2+(py/np.radians(R*0.5))**2)
band=np.exp(-((e-1)/0.07)**2)*front*((py<0)|(ang>R))
img=img*(1-band[...,None]*0.85)+np.array([240,171,252])*band[...,None]*0.85
save('pustka',img)

# 2. Gwiaździsta noc z Drogą Mleczną
img=grad([(-90,(2,4,12)),(0,(10,18,40)),(25,(6,10,28)),(90,(2,4,14))])
mw=np.array([0.3,0.85,0.4]); mw/=np.linalg.norm(mw)
bandd=np.abs(D@mw)
milk=np.exp(-(bandd/0.16)**2)*(0.4+0.6*fbm(D,5,5,21))
img+= milk[...,None]*np.array([120,130,190])*0.9
dust=np.exp(-(bandd/0.05)**2)*np.clip(fbm(D,9,4,33)-0.45,0,1)*2
img-= dust[...,None]*np.array([60,60,80])
stars(img,2,31,255,mask=1+milk*2)
save('noc',img)

# 3. Różowy zachód
img=grad([(-90,(40,18,50)),(-3,(255,150,90)),(4,(255,120,140)),(18,(200,90,170)),(45,(90,50,140)),(90,(30,20,70))])
sun=np.array([0,0.04,1.0]); sun/=np.linalg.norm(sun); sa=np.degrees(np.arccos(np.clip(D@sun,-1,1)))
img+= (np.exp(-(sa/18)**2)*0.7)[...,None]*np.array([255,200,140])
cl=fbm(D*np.array([1,3.5,1]),3,5,41)
clm=np.clip((cl-0.55)*3,0,1)*(np.degrees(LAT)>-2)*(np.degrees(LAT)<35)
img=img*(1-clm[...,None]*0.6)+clm[...,None]*np.array([255,170,190])*0.6
disk=np.clip((2.6-sa)/0.6,0,1)
img=img*(1-disk[...,None])+np.array([255,246,225])*disk[...,None]
stars(img,1,51,200,mask=np.clip((np.degrees(LAT)-40)/30,0,1))
save('zachod',img)

# 4. Zorza polarna
img=grad([(-90,(2,6,10)),(0,(6,22,30)),(30,(4,12,24)),(90,(2,4,12))])
stars(img,1,61,255)
for k,(off,col) in enumerate([(0.0,(60,255,160)),(0.9,(120,90,255)),(2.0,(80,240,200))]):
    curtain_lon=LON+off
    base=np.radians(14+8*k)+0.12*np.sin(curtain_lon*3+k)+0.06*np.sin(curtain_lon*7+2*k)
    h=LAT-base
    ray=0.5+0.5*np.sin(curtain_lon*90+fbm(D,6,3,70+k)*20)
    a=np.where(h>0,np.exp(-h/0.22),np.exp(h/0.025))*(0.55+0.45*ray)*(0.6+0.4*np.cos(curtain_lon*2+k))
    a=np.clip(a,0,1)*0.85
    img=img+a[...,None]*np.array(col)
save('zorza',img)

# 5. Niebo Endu
img=grad([(-90,(6,2,10)),(0,(26,10,36)),(30,(14,6,24)),(90,(6,3,12))])
n=fbm(D,3,5,81)
img+= (np.clip((n-0.5)*3,0,1)**1.5)[...,None]*np.array([130,60,150])
streak=np.clip(fbm(D*np.array([6,0.6,6]),3,4,91)-0.55,0,1)*3
img+= streak[...,None]*np.array([90,40,120])*0.6
stars(img,1,85,200,tint=[230,200,255])
save('end',img)

# 6. Mgławica (niebiesko-różowa)
img=grad([(-90,(4,4,14)),(0,(10,10,30)),(90,(6,6,20))])
n1=fbm(D,1.8,6,101); n2=fbm(D,2.5,6,111)
img+= (np.clip((n1-0.48)*2.5,0,1)**1.4)[...,None]*np.array([60,120,255])*0.9
img+= (np.clip((n2-0.5)*2.8,0,1)**1.4)[...,None]*np.array([255,80,180])*0.8
stars(img,2,121,255)
save('mglawica',img)
# podgląd
names=['pustka','noc','zachod','zorza','end','mglawica']
sheet=Image.new('RGB',(1024,512*3//2))
for i,nm in enumerate(names):
    im=Image.open(nm+'.png').resize((512,256)); sheet.paste(im,((i%2)*512,(i//2)*256))
sheet.save('sheet.png')
