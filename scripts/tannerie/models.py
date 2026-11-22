"""Generates the tannery block models and their wood sheets (fleshing beam, drying frame, tanning vat).

Usage: python3 scripts/tannerie/models.py <vanilla assets folder>
(the `assets` folder of an extracted Minecraft 1.21.1 client jar, for spruce_planks.png).
Comments are in French, like the mod's code.
"""
import sys, os
RACINE=os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
VANILLA=sys.argv[1] if len(sys.argv)>1 else 'assets'
import json
B=RACINE+'/mods-custom/fallow/src/main/resources/assets/fallow/models/'
def W(path,m):
    with open(B+path,'w') as f: json.dump(m,f,indent=2); f.write('\n')
def box(fr,to,faces,name=None):
    e={'from':fr,'to':to,'faces':faces}
    if name: e['name']=name
    return e
def F(t,uv=None,rot=None,cull=None):
    f={'texture':t}
    if uv is not None: f['uv']=uv
    if rot: f['rotation']=rot
    if cull: f['cullface']=cull
    return f
CUIR='minecraft:item/leather'

PL='minecraft:block/spruce_planks'
# ---------- des pièces peintes dans la palette des planches d'épicéa vanilla ----------
# Comme Handcrafted : une feuille de texture par bloc, chaque face à 1 texel par pixel du modèle,
# le bois pris dans spruce_planks (sans ses joints), une ligne claire en haut et une sombre en bas
# des pièces couchées, les pieds en fil vertical sans joint, les bouts encadrés d'un pixel sombre.
from PIL import Image
SP=Image.open(VANILLA+'/minecraft/textures/block/spruce_planks.png').convert('RGB')
PAL=sorted({SP.getpixel((x,y)) for x in range(16) for y in range(16)},key=sum)
def pas(c,n):
    i=PAL.index(c) if c in PAL else 3
    return PAL[max(0,min(len(PAL)-1,i+n))]
CORPS=[r for r in range(16) if r%4!=3]   # les lignes des planches, sans les joints
class Feuille:
    def __init__(s,taille=64):
        s.t=taille; s.im=Image.new('RGBA',(taille,taille),(0,0,0,0)); s.x=0; s.y=0; s.h=0; s.n=0
    def place(s,w,h):
        if s.x+w>s.t: s.x=0; s.y+=s.h; s.h=0
        p=(s.x,s.y); s.x+=w; s.h=max(s.h,h); assert s.y+h<=s.t,'feuille pleine'; return p
    def peint(s,w,h,mode):
        s.n+=1; px,py=s.place(w,h); dec=(s.n*5)%16; lig=(s.n*3)%len(CORPS)
        for j in range(h):
            for i in range(w):
                if mode=='v':   # fil vertical : une colonne de la face = une ligne des planches
                    c=SP.getpixel(((j+dec)%16, CORPS[(i+lig)%len(CORPS)]))
                    if j>=h-1: c=pas(c,-2)
                    elif j>=h-2: c=pas(c,-1)
                else:            # fil horizontal
                    c=SP.getpixel(((i+dec)%16, CORPS[(j+lig)%len(CORPS)]))
                    if mode=='h':
                        if j==0 and h>1: c=pas(c,+2)
                        elif j==h-1 and h>1: c=PAL[0]
                    elif mode=='dessus':
                        if (j==0 or j==h-1) and h>2: c=pas(c,+1)
                    elif mode=='bout':
                        if i in (0,w-1) or j in (0,h-1): c=pas(c,-2)
                s.im.putpixel((px+i,py+j),c+(255,))
        k=16/s.t
        return [px*k,py*k,(px+w)*k,(py+h)*k]
    def sauve(s,nom):
        s.im.save(RACINE+'/mods-custom/fallow/src/main/resources/assets/fallow/textures/block/tannerie/'+nom+'.png')
AX={'north':('x','y'),'south':('x','y'),'east':('z','y'),'west':('z','y'),'up':('x','z'),'down':('x','z')}
def planche(fe,fr,to,grain,cull=None):
    d={'x':to[0]-fr[0],'y':to[1]-fr[1],'z':to[2]-fr[2]}
    out={}
    for f in ('north','south','east','west','up','down'):
        h,v=AX[f]; fw,fh=d[h],d[v]
        if grain not in (h,v): mode='bout'
        elif f in ('up','down'):
            if grain==h: mode='dessus'
            else: mode='dessus_v'
        elif grain==v: mode='v'
        else: mode='h'
        if mode=='dessus_v':   # dessus d'une pièce dont le fil court en z : peinte couchée puis tournée
            uv=fe.peint(fh,fw,'dessus'); out[f]=F('#bois',uv,90,cull if (cull and f=='down' and fr[1]==0) else None)
        else:
            uv=fe.peint(fw,fh,mode); out[f]=F('#bois',uv,None,cull if (cull and f=='down' and fr[1]==0) else None)
    return box(fr,to,out)
import os; os.makedirs(RACINE+'/mods-custom/fallow/src/main/resources/assets/fallow/textures/block/tannerie',exist_ok=True)

# ---------- chevalet : une poutre sur deux tréteaux ----------
el=[]
fe=Feuille()
el.append(planche(fe,[6,10,0],[10,13,16],'z'))                              # la poutre
for z0 in (1,12):
    el.append(planche(fe,[2,8,z0],[14,10,z0+3],'x'))                        # la traverse du tréteau, sous la poutre
    for x0 in (3,10):
        el.append(planche(fe,[x0,0,z0],[x0+3,8,z0+3],'y',cull='down'))      # les pieds
fe.sauve('chevalet')
W('block/chevalet.json',{'parent':'minecraft:block/block','textures':{'bois':'fallow:block/tannerie/chevalet','particle':PL},'elements':el})

def peau_pliee(path,tex,parent=None,sangles=True,g=0.01):
    els=[]
    els.append(box([6-g,13+g,1],[10+g,13+g,15],{'up':F('#peau',[6,1,10,15]),'down':F('#peau',[6,1,10,15])}))
    els.append(box([6-g,9,1],[6-g,13+g,15],{'west':F('#peau',[2,1,6,15],270),'east':F('#peau',[2,1,6,15],270)}))
    els.append(box([10+g,9,1],[10+g,13+g,15],{'east':F('#peau',[10,1,14,15],90),'west':F('#peau',[10,1,14,15],90)}))
    if sangles:
        for z0 in (5,10):
            els.append(box([5.98,8.98,z0],[10.02,13.02,z0+1],{'east':F('#lien',[7,4,8,8]),'west':F('#lien',[8,4,9,8]),
                'up':F('#lien',[6,6,10,7]),'down':F('#lien',[6,9,10,10])}))
    m={'render_type':'minecraft:cutout','textures':{'peau':tex,'particle':tex}|({'lien':'fallow:item/cuir_mouille'} if sangles else {}),'elements':els}
    if parent: m['parent']=parent
    W(path,m)
peau_pliee('block/chevalet_peau.json','fallow:item/peau_brute')
peau_pliee('diegesis/peau_chevalet_brute.json','fallow:item/peau_brute','minecraft:block/block')
peau_pliee('diegesis/peau_chevalet_raclee.json','fallow:item/peau_raclee','minecraft:block/block',sangles=False,g=0.02)

# ---------- séchoir : un cadre de planches, le cuir pris dedans par ses bords ----------
el=[]
fe=Feuille()
for x0 in (0,13):
    el.append(planche(fe,[x0,0,2],[x0+3,2,14],'z',cull='down'))             # patins
    el.append(planche(fe,[x0,2,6],[x0+3,16,10],'y'))                        # montants
el.append(planche(fe,[3,14,6],[13,16,10],'x'))                              # traverse du haut
el.append(planche(fe,[3,0,6],[13,2,10],'x',cull='down'))                    # seuil
fe.sauve('sechoir')
W('block/sechoir.json',{'parent':'minecraft:block/block','textures':{'bois':'fallow:block/tannerie/sechoir','particle':PL},'elements':el})
def cuir_tendu(path,tex,g=0.0):
    # l'image de l'objet au pixel : lignes 1 à 14 de y 15 à 1, colonnes 2 à 13 de x 2 à 14 ; ses bords pris dans le cadre
    W(path,{'render_type':'minecraft:cutout','textures':{'peau':tex,'particle':tex},
      'elements':[box([2-g,1-g,8],[14+g,15+g,8],{'north':F('#peau',[2,1,14,15]),'south':F('#peau',[14,1,2,15])})]})
cuir_tendu('block/sechoir_humide.json','fallow:item/cuir_mouille')
cuir_tendu('block/sechoir_sec.json',CUIR)
cuir_tendu('diegesis/cuir_tendu_sec.json',CUIR,g=0.02)

# ---------- cuve : un tonneau ouvert, bâti comme le composteur vanilla ----------
el=[]
el.append(box([0,0,0],[16,2,16],{'up':F('#fond'),'down':F('#fond',cull='down'),'north':F('#douelle',cull='north'),'south':F('#douelle',cull='south'),'east':F('#douelle',cull='east'),'west':F('#douelle',cull='west')}))
def paroi(fr,to,dehors,dedans):
    f={dehors:F('#douelle',cull=dehors),dedans:F('#dedans',None,90),'up':F('#bord')}
    for k in ('north','south','east','west'):
        if k not in f and k not in (dehors,dedans): f[k]=F('#douelle',cull=k)
    return box(fr,to,f)
el.append(paroi([0,2,0],[2,16,16],'west','east'))
el.append(paroi([14,2,0],[16,16,16],'east','west'))
el.append(paroi([2,2,0],[14,16,2],'north','south'))
el.append(paroi([2,2,14],[14,16,16],'south','north'))
W('block/cuve_a_tan.json',{'parent':'minecraft:block/block','textures':{
 'douelle':'minecraft:block/barrel_side','dedans':PL,'fond':'minecraft:block/barrel_bottom','bord':'minecraft:block/barrel_top_open',
 'particle':'minecraft:block/barrel_side'},'elements':el})

def peau_flottante(path,tex,y,parent=None):
    m={'render_type':'minecraft:cutout','textures':{'peau':tex,'particle':tex},
       'elements':[box([2,y,2],[14,y,14],{'up':F('#peau',[2,2,14,14]),'down':F('#peau',[2,14,14,2])})]}
    if parent: m['parent']=parent
    W(path,m)
peau_flottante('block/cuve_a_tan_peaux.json','fallow:item/peau_raclee',13.01)
peau_flottante('block/cuve_a_tan_cuir.json','fallow:item/cuir_mouille',13.01)
peau_flottante('diegesis/peau_raclee.json','fallow:item/peau_raclee',0,'minecraft:block/block')
peau_flottante('diegesis/cuir_mouille.json','fallow:item/cuir_mouille',0,'minecraft:block/block')
print('ok')
