"""Generates the tannery block models (fleshing bench, drying rack, tanning vat).

Usage: python3 scripts/tannerie/models.py
The wood refers to Handcrafted's spruce table sheet at runtime (Handcrafted ships in the pack; nothing of it is
copied here), the vat to vanilla barrel textures, the hides to the items' own sprites.
Comments are in French, like the mod's code.
"""
import sys, os
RACINE=os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
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
# ---------- le bois : la feuille de la table en épicéa de Handcrafted, prise par renvoi ----------
# Disposition de sa feuille (64 × 64, uv en seizièmes : 1 = 4 texels) : plateau 16 × 4 × 16 (dessus x 16 à 32,
# dessous 32 à 48, lignes 0 à 16 ; côtés en bande de 16 × 4 à la ligne 16, est 0, nord 16, ouest 32, sud 48),
# pied 3 × 12 × 3 (côtés colonnes 0 à 12, lignes 3 à 15, sombres en bas ; bouts colonnes 3 à 9, lignes 0 à 3).
HC='handcrafted:block/table/table/spruce_table'
def U(x0,y0,x1,y1): return [x0/4,y0/4,x1/4,y1/4]
BANDE={'east':0,'north':16,'west':32,'south':48}
def plateau(fr,to,long):
    """Pièce couchée (plateau, traverse) : côtés pris dans les bandes du plateau, dessus dans son dessus."""
    d={'x':to[0]-fr[0],'y':to[1]-fr[1],'z':to[2]-fr[2]}
    assert d['y']<=4 and max(d['x'],d['z'])<=16
    f={}
    for k in ('north','south','east','west'):
        w=d['x'] if k in ('north','south') else d['z']
        # les bandes nord et sud montrent le fil en long, est et ouest les bouts des planches : une pièce en long
        # sur z prend donc les bandes nord et sud pour ses côtés est et ouest, et inversement
        q=k if long=='x' else {'north':'east','south':'west','east':'north','west':'south'}[k]
        b=BANDE[q]; f[k]=F('#bois',U(b,20-d['y'],b+w,20))
    if long=='x':
        f['up']=F('#bois',U(16,0,16+d['x'],d['z'])); f['down']=F('#bois',U(32,0,32+d['x'],d['z']))
    else:   # le fil des planches suit la pièce : image tournée d'un quart
        f['up']=F('#bois',U(16,0,16+d['z'],d['x']),90); f['down']=F('#bois',U(32,0,32+d['z'],d['x']),90)
    return box(fr,to,f)
def pied(fr,to):
    """Pièce debout : côtés pris dans ceux du pied, calés en bas (le bas sombre reste au sol)."""
    w,h,p=to[0]-fr[0],to[1]-fr[1],to[2]-fr[2]
    assert w<=3 and p<=3 and h<=12
    col={'east':0,'north':3,'west':6,'south':9}
    f={}
    for k in ('north','south','east','west'):
        l=w if k in ('north','south') else p
        f[k]=F('#bois',U(col[k],15-h,col[k]+l,15))
    f['up']=F('#bois',U(3,0,3+w,p)); f['down']=F('#bois',U(6,0,6+w,p))
    return box(fr,to,f)
def objet(path,parent,els,tex):
    m={'render_type':'minecraft:cutout','textures':{'peau':tex,'particle':tex},'elements':els}
    if parent: m['parent']=parent
    W(path,m)

# ---------- chevalet : un banc de tanneur, la peau posée dessus comme un objet de Hands-On ----------
el=[plateau([3,8,0],[13,12,16],'z')]                                       # le plateau, 10 × 4 × 16
el.append(plateau([5,6,3],[11,8,13],'z'))                                  # la ceinture sous le plateau, comme la table de chevet de Handcrafted
for z0 in (2,11):
    for x0 in (4,9):
        el.append(pied([x0,0,z0],[x0+3,8,z0+3]))                           # quatre pieds, 3 × 8 × 3
for x0 in (4,9):
    el.append(plateau([x0+1,2,5],[x0+2,3,11],'z'))                         # les entretoises basses, d'un pied à l'autre
W('block/chevalet.json',{'parent':'minecraft:block/block','textures':{'bois':HC,'particle':PL},'elements':el})
def peau_posee(path,tex,parent=None,g=0.01):
    # l'image de l'objet à plat, 8 × 8 (la moitié de sa taille, comme dans un cadre ou sur l'établi de Hands-On)
    objet(path,parent,[box([4,12+g,4],[12,12+g,12],{'up':F('#peau',[0,0,16,16]),'down':F('#peau',[0,16,16,0])})],tex)
peau_posee('block/chevalet_peau.json','fallow:item/peau_brute')
peau_posee('diegesis/peau_chevalet_brute.json','fallow:item/peau_brute','minecraft:block/block')
peau_posee('diegesis/peau_chevalet_raclee.json','fallow:item/peau_raclee','minecraft:block/block',g=0.02)

# ---------- séchoir : une barre sur deux montants, le cuir pendu dessous comme un objet ----------
el=[]
for x0 in (1,12):
    el.append(plateau([x0,0,3],[x0+3,2,13],'z'))                           # patins
    el.append(pied([x0,2,7],[x0+3,12,10]))                                 # montants
el.append(plateau([0,12,6],[16,16,10],'x'))                                # la barre, 16 × 4 × 4
for x0 in (4,11):
    el.append(pied([x0,10,5],[x0+1,12,6]))                                 # deux chevilles, où le cuir est accroché
W('block/sechoir.json',{'parent':'minecraft:block/block','textures':{'bois':HC,'particle':PL},'elements':el})
def cuir_pendu(path,tex,g=0.0):
    # l'image de l'objet, 10 × 10, pendue sous la barre, devant elle (z 6)
    objet(path,None,[box([3-g,2-g,5.99-g],[13+g,12,5.99-g],{'north':F('#peau',[0,0,16,16]),'south':F('#peau',[16,0,0,16])})],tex)
cuir_pendu('block/sechoir_humide.json','fallow:item/cuir_mouille')
cuir_pendu('block/sechoir_sec.json',CUIR)
cuir_pendu('diegesis/cuir_tendu_sec.json',CUIR,g=0.02)

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
    # l'image de l'objet à plat, 6 × 6, comme les objets posés de Hands-On
    objet(path,parent,[box([5,y,5],[11,y,11],{'up':F('#peau',[0,0,16,16]),'down':F('#peau',[0,16,16,0])})],tex)
peau_flottante('block/cuve_a_tan_peaux.json','fallow:item/peau_raclee',13.01)
peau_flottante('block/cuve_a_tan_cuir.json','fallow:item/cuir_mouille',13.01)
peau_flottante('diegesis/peau_raclee.json','fallow:item/peau_raclee',0,'minecraft:block/block')
peau_flottante('diegesis/cuir_mouille.json','fallow:item/cuir_mouille',0,'minecraft:block/block')
print('ok')
