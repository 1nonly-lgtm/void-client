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

# 7. Krwawy księżyc
img=grad([(-90,(14,2,4)),(-3,(70,10,14)),(6,(110,20,24)),(25,(40,6,12)),(90,(10,2,6))])
moon=np.array([0.25,0.32,1.0]); moon/=np.linalg.norm(moon); ma=np.degrees(np.arccos(np.clip(D@moon,-1,1)))
img+= (np.exp(-(ma/14)**2)*0.8)[...,None]*np.array([255,60,40])
crater=fbm(D,26,4,201,False)
mdisk=np.clip((7.0-ma)/0.35,0,1)
mcol=np.array([235,70,45])[None,None,:]*(0.72+0.5*(crater[...,None]-0.5))
img=img*(1-mdisk[...,None])+mcol*mdisk[...,None]
cl=fbm(D*np.array([1,3,1]),2.6,5,211)
clm=np.clip((cl-0.52)*3.2,0,1)*(np.degrees(LAT)>-2)*(np.degrees(LAT)<40)
img=img*(1-clm[...,None]*0.75)+clm[...,None]*np.array([40,6,10])*0.75
rim=clm*np.exp(-(ma/22)**2)
img+= rim[...,None]*np.array([255,90,60])*0.6
stars(img,1,221,170,tint=[255,200,190],mask=np.clip((np.degrees(LAT)-25)/30,0,1))
save('krwawy',img)

# 8. Planeta z pierścieniami
img=grad([(-90,(2,2,8)),(0,(8,6,22)),(90,(3,2,10))])
n=fbm(D,2,5,301); img+= (np.clip((n-0.5)*2.5,0,1)**1.6)[...,None]*np.array([90,40,160])*0.7
stars(img,2,311,255)
pc=np.array([-0.15,0.42,1.0]); pc/=np.linalg.norm(pc); pa=np.degrees(np.arccos(np.clip(D@pc,-1,1)))
PR=16.0
u=np.cross(pc,[0.25,1,0]); u/=np.linalg.norm(u); v=np.cross(u,pc)
px=np.degrees(D@u); py=np.degrees(D@v)
front=(D@pc)>0
light=np.array([0.7,0.4,0.5]); light/=np.linalg.norm(light)
# kula: współrzędne na tarczy → normalna
r2=(px**2+py**2)/PR**2
body=(r2<1)&front
nz=np.sqrt(np.clip(1-r2,0,1)); nrm=np.stack([px/PR,py/PR,nz],-1)
lam=np.clip(nrm@np.array([0.6,0.35,0.72]),0,1)
bands=0.5+0.5*np.sin((py/PR)*14+fbm(D,9,3,321,False)*3)
pcol=np.stack([170+70*bands,90+60*bands,200-40*bands],-1)*(0.12+0.95*lam[...,None])
# pierścień: elipsa nachylona
rx=px/(PR*2.2); ry=(py+px*0.28)/(PR*0.42)
re=np.sqrt(rx**2+ry**2)
ring=((re>0.62)&(re<1.0))*front*(0.55+0.45*np.sin(re*60))
behind=body&(ry>0)
img=np.where(body[...,None],pcol,img)
ringcol=np.array([230,190,255])
rm=(ring*(~behind | (ry<=0)))[...,None]*0.8
rm=np.where(body[...,None]&(ry[...,None]>0),0,rm)
img=img*(1-rm)+ringcol*rm
img+= (np.exp(-((np.sqrt(r2)-1)/0.04)**2)*front*0.6)[...,None]*np.array([200,140,255])
save('planeta',img)

# 9. Burza
img=grad([(-90,(10,10,16)),(0,(46,44,62)),(30,(26,24,38)),(90,(14,14,22))])
c1=fbm(D*np.array([1,2.2,1]),2.2,6,401)
c2=fbm(D*np.array([1,2.2,1])+2,4.5,5,411)
dens=np.clip((c1*0.7+c2*0.5-0.45)*2.4,0,1)
img=img*(1-dens[...,None]*0.85)+np.array([22,20,32])*dens[...,None]*0.85
fl=np.array([0.5,0.18,1.0]); fl/=np.linalg.norm(fl); fa=np.degrees(np.arccos(np.clip(D@fl,-1,1)))
glow=np.exp(-(fa/24)**2)*(0.5+0.8*np.clip(c2-0.4,0,1))
img+= glow[...,None]*np.array([170,120,255])*0.75
# błyskawica: łamana od chmur w dół
rng=np.random.default_rng(7); pts=[np.array([0.5,0.32,1.0])]
for k in range(14):
    d=pts[-1]+np.array([rng.normal(0,0.035),-0.03,0]); pts.append(d)
bolt=np.zeros(LAT.shape)
for a_,b_ in zip(pts,pts[1:]):
    for t in np.linspace(0,1,10):
        p=a_*(1-t)+b_*t; p=p/np.linalg.norm(p)
        dd=np.linalg.norm(D-p,axis=-1)
        bolt=np.maximum(bolt,np.exp(-(dd/0.0035)**2))
img=img*(1-bolt[...,None])+np.array([235,225,255])*bolt[...,None]
save('burza',img)

# 10. Złoty świt nad chmurami
img=grad([(-90,(60,50,70)),(-4,(255,190,110)),(3,(255,170,90)),(12,(250,200,150)),(35,(120,160,220)),(90,(40,70,140))])
sun=np.array([0.35,0.06,1.0]); sun/=np.linalg.norm(sun); sa=np.degrees(np.arccos(np.clip(D@sun,-1,1)))
img+= (np.exp(-(sa/12)**2)*0.9)[...,None]*np.array([255,220,150])
img+= (np.exp(-(sa/40)**2)*0.35)[...,None]*np.array([255,180,120])
sea=fbm(D*np.array([1,5,1]),3,5,501)
clm=np.clip((sea-0.4)*3,0,1)*(np.degrees(LAT)<4)
lit=np.exp(-(sa/35)**2)
img=img*(1-clm[...,None]*0.8)+(np.array([255,230,210])*(0.55+0.45*lit[...,None]))*clm[...,None]*0.8
hi=fbm(D*np.array([1,4,1]),2.5,5,511); hm=np.clip((hi-0.58)*3,0,1)*(np.degrees(LAT)>10)*(np.degrees(LAT)<45)
img=img*(1-hm[...,None]*0.5)+np.array([255,200,170])*hm[...,None]*0.5
disk=np.clip((2.4-sa)/0.5,0,1); img=img*(1-disk[...,None])+np.array([255,252,235])*disk[...,None]
save('swit',img)

# 11. Synthwave
img=grad([(-90,(20,4,40)),(-2,(255,60,170)),(4,(255,110,90)),(14,(150,30,170)),(40,(40,10,90)),(90,(8,4,30))])
stars(img,2,601,230,mask=np.clip((np.degrees(LAT)-15)/25,0,1))
sun=np.array([0,0.13,1.0]); sun/=np.linalg.norm(sun)
u=np.array([1.0,0,0]); v=np.cross(sun,u)*-1
sx=np.degrees(D@u); sy=np.degrees(D@v); fr=(D@sun)>0
sr=np.sqrt(sx**2+sy**2); SR=13
sunm=(sr<SR)&fr
t=np.clip((sy+SR)/(2*SR),0,1)
scol=np.stack([255+0*t,80+160*t,120*(1-t)+40],-1)
s_=np.clip((sy+SR*0.2)/(SR*1.2),0,1)
stripes=(sy> -SR*0.2)&(((s_*7)%1)<0.12+0.4*s_)
m=sunm&~stripes
img=np.where(m[...,None],scol,img)
img+= (np.exp(-((sr-SR)/6)**2)*fr*0.5)[...,None]*np.array([255,80,200])
# siatka pod horyzontem
below=np.degrees(LAT)<0
depth=1/np.maximum(-np.tan(LAT),0.02)
gx=np.abs(((X*depth)*0.25+1000)%1-0.5); gz=np.abs(((Z*depth)*0.25+1000)%1-0.5)
line=np.clip(1-np.minimum(gx,gz)/ (0.02+0.004*depth),0,1)*below*np.exp(-depth/60)
img=img*(1-below[...,None]*0.6)+line[...,None]*np.array([255,80,220])
save('synthwave',img)

# 12. Spiralna galaktyka
img=grad([(-90,(2,2,6)),(0,(6,6,16)),(90,(2,2,8))])
stars(img,2,701,255)
gc=np.array([0.1,0.35,1.0]); gc/=np.linalg.norm(gc)
u=np.cross(gc,[0.4,1,0]); u/=np.linalg.norm(u); v=np.cross(u,gc)
gx=np.degrees(D@u); gy=np.degrees(D@v)/0.45; fr=(D@gc)>0
r=np.sqrt(gx**2+gy**2); th=np.arctan2(gy,gx)
arms=0.5+0.5*np.cos(2*(th-np.log(r+1)*2.2))
gal=np.exp(-r/14)*(0.35+0.9*arms**3)*fr
core=np.exp(-(r/3.2)**2)*fr
dust=np.clip(fbm(D,12,4,711,False)-0.5,0,1)*2*np.exp(-r/18)*fr
img+= gal[...,None]*np.array([150,170,255])*1.1 + core[...,None]*np.array([255,230,200])*1.2
img-= dust[...,None]*np.array([60,50,70])
img+= (np.clip(fbm(D,2,5,721)-0.55,0,1)*2)[...,None]*np.array([70,40,120])
save('galaktyka',img)

# 13–18: nowe nieba (dopisywane do sky.py przed „# podgląd”)

# 13. Dwa słońca
img=grad([(-90,(60,30,20)),(-3,(255,170,90)),(6,(240,140,80)),(25,(150,90,130)),(90,(40,30,80))])
for v,col,sz in [([.3,.14,1],(255,220,150),3.2),([.55,.22,1],(255,150,110),2.2)]:
    s_=np.array(v,float); s_/=np.linalg.norm(s_); a=np.degrees(np.arccos(np.clip(D@s_,-1,1)))
    img+= (np.exp(-(a/14)**2)*.7)[...,None]*np.array(col)
    d=np.clip((sz-a)/.4,0,1); img=img*(1-d[...,None])+np.array([255,250,235])*d[...,None]
dune=fbm(D*np.array([1,6,1]),2.5,4,801)
hz=np.clip((np.degrees(LAT)+2-dune*5)/1.2,0,1)
img=img*hz[...,None]+np.array([90,45,30])*(1-hz[...,None])*(.6+.6*dune[...,None])
stars(img,1,811,140,mask=np.clip((np.degrees(LAT)-30)/30,0,1))
save('dwa_slonca',img)

# 14. Deszcz meteorytów
img=grad([(-90,(4,4,12)),(0,(14,10,34)),(40,(8,6,22)),(90,(4,3,12))])
stars(img,2,821,240)
rng=np.random.default_rng(5)
for k in range(40):
    p=rng.normal(size=3); p[1]=abs(p[1])+.25; p/=np.linalg.norm(p)
    t_=np.cross(p,[0,1,0]); t_/=np.linalg.norm(t_); t_=t_*np.cos(.6)+np.array([0,-1,0])*np.sin(.6); t_/=np.linalg.norm(t_)
    L=.12+rng.random()*.18
    for u in np.linspace(0,1,24):
        q=p+t_*L*u; q/=np.linalg.norm(q); dd=np.linalg.norm(D-q,axis=-1)
        img+= (np.exp(-(dd/(.003+.003*(1-u)))**2)*(1-u)**1.5*520)[...,None]*np.array([.85,.8,1])
neb=fbm(D,2,5,831); img+= (np.clip(neb-.55,0,1)*2)[...,None]*np.array([80,40,140])
save('meteory',img)

# 15. Pod wodą
img=grad([(-90,(2,10,24)),(-20,(4,30,60)),(20,(10,80,120)),(70,(40,160,190)),(90,(80,200,220))])
ca=fbm(D*np.array([1,.3,1]),6,4,841,False); caus=np.clip(1-np.abs(ca-.5)*9,0,1)**2
up=np.clip(np.degrees(LAT)/70,0,1); img+= (caus*up)[...,None]*np.array([120,220,230])*.6
ang=np.arctan2(X,Z); rays=(np.sin(ang*14+fbm(D,3,3,851)*3)*.5+.5)**6*np.clip(np.degrees(LAT)/40,0,1)
img+= rays[...,None]*np.array([140,230,240])*.35
bub=fbm(D,40,2,861,False); img+= (np.clip(bub-.82,0,1)*8*(np.degrees(LAT)>-10))[...,None]*np.array([200,240,255])
save('podwodne',img)

# 16. Wata cukrowa
img=grad([(-90,(255,200,230)),(0,(255,190,220)),(20,(200,190,255)),(60,(150,190,255)),(90,(120,170,250))])
c=fbm(D*np.array([1,2.4,1]),2.4,6,871)
cm=np.clip((c-.42)*4.5,0,1)*(np.degrees(LAT)>-6)
shade=np.clip(fbm(D*np.array([1,2.4,1])+.05,2.4,6,871)-c+.5,0,1)
col=np.stack([255-shade*40,236-shade*110,250-shade*40],-1)
img=img*(1-cm[...,None])+col*cm[...,None]
stars(img,1,881,120,tint=[255,240,255],mask=np.clip((np.degrees(LAT)-45)/30,0,1))
save('cukrowa',img)

# 17. Pierścień planety (łuk przez całe niebo)
img=grad([(-90,(3,3,10)),(0,(18,14,40)),(30,(8,8,24)),(90,(3,3,12))])
stars(img,2,891,230)
nrm=np.array([.25,1,-.35]); nrm/=np.linalg.norm(nrm)
dp=D@nrm
band=np.exp(-((dp-.05)/.05)**2)
stripes=.6+.4*np.sin(dp*260)
gap=1-np.exp(-((dp-.06)/.004)**2)*.85
lit=np.clip((D@np.array([.5,.4,.75]))*.8+.6,0,1)
img+= (band*stripes*gap*lit)[...,None]*np.array([235,200,170])*.95
pl=np.array([-.6,-.25,.75]); pl/=np.linalg.norm(pl); pa=np.degrees(np.arccos(np.clip(D@pl,-1,1)))
disk=np.clip((24-pa)/.4,0,1); bands=.5+.5*np.sin(np.degrees(np.arcsin(np.clip(D@nrm,-1,1)))*.9)
img=img*(1-disk[...,None])+(np.stack([160+50*bands,110+40*bands,90+30*bands],-1)*np.clip(1.1-pa/30,.2,1)[...,None])*disk[...,None]
save('pierscien',img)

# 18. Noir (czarno-białe z księżycem)
img=grad([(-90,(6,6,6)),(0,(40,40,42)),(30,(18,18,20)),(90,(6,6,8))])
c=fbm(D*np.array([1,2.2,1]),2.6,6,901); cm=np.clip((c-.5)*3,0,1)*(np.degrees(LAT)>-4)
mo=np.array([-.25,.38,1]); mo/=np.linalg.norm(mo); ma=np.degrees(np.arccos(np.clip(D@mo,-1,1)))
img+= (np.exp(-(ma/16)**2)*.6)[...,None]*np.array([220,220,225])
img=img*(1-cm[...,None]*.7)+np.array([30,30,32])*cm[...,None]*.7
rim=cm*np.exp(-(ma/30)**2); img+= rim[...,None]*np.array([200,200,205])*.6
md=np.clip((5.5-ma)/.35,0,1); cr=fbm(D,30,3,911,False)
img=img*(1-md[...,None])+(np.array([235,235,238])*(.8+.3*(cr[...,None]-.5)))*md[...,None]
stars(img,1,921,160,mask=np.clip((np.degrees(LAT)-20)/30,0,1))
save('noir',img)

# podgląd
names=['pustka','noc','zachod','zorza','end','mglawica','krwawy','planeta','burza','swit','synthwave','galaktyka','dwa_slonca','meteory','podwodne','cukrowa','pierscien','noir']
sheet=Image.new('RGB',(1024,256*((len(names)+1)//2)))
for i,nm in enumerate(names):
    im=Image.open(nm+'.png').resize((512,256)); sheet.paste(im,((i%2)*512,(i//2)*256))
sheet.save('sheet.png')
