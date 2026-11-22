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

FER='minecraft:block/barrel_side'
def fer(fr,to):
    """Ferrure : le fer des cerclages du tonneau vanilla (lignes 3 et 4 de barrel_side : clair dessus, sombre dessous),
    au pixel ; les faces debout prennent le cerclage de haut en bas, le dessus et le dessous sa ligne claire."""
    w,h,p=to[0]-fr[0],to[1]-fr[1],to[2]-fr[2]
    f={}
    for k in ('north','south','east','west'):
        l=w if k in ('north','south') else p
        if h<=2:
            f[k]=F('#fer',[0,3,l,3+h])
        else:   # pièce debout : le cerclage tourné d'un quart, clair d'un côté, sombre de l'autre
            assert l<=2
            f[k]=F('#fer',[0,3,h,3+l],90)
    assert min(w,p)<=2
    if p<=2: f['up']=F('#fer',[0,3,w,3+p]); f['down']=F('#fer',[0,3,w,3+p])
    else: f['up']=F('#fer',[0,3,p,3+w],90); f['down']=F('#fer',[0,3,p,3+w],90)
    return box(fr,to,f)
PIERRE='minecraft:block/andesite'

# ---------- chevalet : un banc de tanneur, une serre de fer tient la peau ----------
el=[plateau([3,8,0],[13,12,16],'z')]                                       # le plateau, 10 × 4 × 16
el.append(plateau([5,6,3],[11,8,13],'z'))                                  # la ceinture sous le plateau
for z0 in (2,11):
    for x0 in (4,9):
        el.append(pied([x0,0,z0],[x0+3,8,z0+3]))                           # quatre pieds, 3 × 8 × 3
for x0 in (4,9):
    el.append(plateau([x0+1,2,5],[x0+2,3,11],'z'))                         # les entretoises basses
# la serre au fond, loin de soi : le couteau tire la peau vers soi, contre elle
el.append(fer([4,12,13],[12,13,15]))                                       # la barre de serrage, une lame de fer 8 × 1 × 2 sur la peau
for x0 in (3,12):
    el.append(fer([x0,12,13],[x0+1,14,15]))                                # ses deux tiges, qui dépassent d'un pixel
# la pierre à aiguiser, à portée de main : une pierre d'andésite 4 × 1 × 2, sur sa semelle de bois
el.append(plateau([7,12,1],[13,13,5],'x'))
el.append(box([8,13,2],[12,14,4],{'north':F('#pierre',[0,0,4,1]),'south':F('#pierre',[4,0,8,1]),'east':F('#pierre',[8,0,10,1]),
    'west':F('#pierre',[10,0,12,1]),'up':F('#pierre',[2,2,6,4]),'down':F('#pierre',[2,6,6,8])}))
W('block/chevalet.json',{'parent':'minecraft:block/block','textures':{'bois':HC,'fer':FER,'pierre':PIERRE,'particle':PL},'elements':el})
def peau_posee(path,tex,parent=None,g=0.01):
    # l'image de l'objet à plat, 8 × 8 (la moitié de sa taille, comme sur l'établi de Hands-On), son bout sous la serre
    objet(path,parent,[box([4,12+g,6],[12,12+g,14],{'up':F('#peau',[0,0,16,16]),'down':F('#peau',[0,16,16,0])})],tex)
peau_posee('block/chevalet_peau.json','fallow:item/peau_brute')
peau_posee('diegesis/peau_chevalet_brute.json','fallow:item/peau_brute','minecraft:block/block')
peau_posee('diegesis/peau_chevalet_raclee.json','fallow:item/peau_raclee','minecraft:block/block',g=0.02)

# ---------- séchoir : un cadre de tanneur accroché au mur, au soleil, le cuir cloué dedans ----------
# Vu du nord (facing north), le mur est au sud (z 16) : le cadre se tient à un pixel du mur, sur quatre cales.
el=[]
el.append(plateau([1,13,13],[15,15,15],'x'))                               # traverse haute, 14 × 2 × 2
el.append(plateau([1,1,13],[15,3,15],'x'))                                 # traverse basse
for x0 in (1,13):
    el.append(pied([x0,3,13],[x0+2,13,15]))                                # montants, 2 × 10 × 2
for x0 in (2,12):
    for y0 in (1,13):
        cale=plateau([x0,y0,15],[x0+2,y0+2,16],'x'); cale['faces']['south']['cullface']='south'
        el.append(cale)                                                    # les cales contre le mur
piton=fer([7,15,14],[9,16,16]); piton['faces']['south']['cullface']='south'
el.append(piton)                                                           # le piton, planté dans le mur, où le cadre est pendu
W('block/sechoir.json',{'parent':'minecraft:block/block','textures':{'bois':HC,'fer':FER,'particle':PL},'elements':el})
def cuir_pendu(path,tex,g=0.0,clous=True):
    # l'image de l'objet à moitié de sa taille, 8 × 8 (x 4 à 12, y 4 à 12), dans l'ouverture du cadre ; ses quatre bouts
    # cloués : une tête de clou d'un pixel, claire (le fer du bloc de fer), à plat devant le cuir (le cuir sec, peint par-dessus au poste, n'en a pas)
    els=[box([4-g,4-g,13.99-g],[12+g,12+g,13.99-g],{'north':F('#peau',[0,0,16,16]),'south':F('#peau',[16,0,0,16])})]
    if clous:
        for x0,y0 in ((5,10),(10,10),(5,5),(10,5)):
            els.append(box([x0,y0,13.98],[x0+1,y0+1,13.98],{'north':F('#clou',[7,7,8,8])}))
    objet(path,None,els,tex)
    if clous:
        m=json.load(open(B+path)); m['textures']['clou']='minecraft:block/iron_block'; W(path,m)
cuir_pendu('block/sechoir_humide.json','fallow:item/cuir_mouille')
cuir_pendu('block/sechoir_sec.json',CUIR)
W('diegesis/cuir_tendu_humide.json',{'parent':'fallow:block/sechoir_humide'})
cuir_pendu('diegesis/cuir_tendu_sec.json',CUIR,g=0.02,clous=False)

# ---------- cuve : un tonneau ouvert, ses anses de fer et son robinet ----------
el=[]
el.append(box([1,0,1],[15,2,15],{'up':F('#fond'),'down':F('#fond',cull='down'),'north':F('#douelle'),'south':F('#douelle'),'east':F('#douelle'),'west':F('#douelle')}))
def paroi(fr,to,dehors,dedans):
    f={dehors:F('#douelle'),dedans:F('#dedans',None,90),'up':F('#bord')}
    for k in ('north','south','east','west'):
        if k not in f and k not in (dehors,dedans): f[k]=F('#douelle')
    return box(fr,to,f)
el.append(paroi([1,2,1],[2,16,15],'west','east'))
el.append(paroi([14,2,1],[15,16,15],'east','west'))
el.append(paroi([2,2,1],[14,16,2],'north','south'))
el.append(paroi([2,2,14],[14,16,15],'south','north'))
for x0 in (0,15):                                                          # les anses : une poignée sur deux pattes
    el.append(fer([x0,8,6],[x0+1,9,10]))
    el.append(fer([x0,9,6],[x0+1,10,7])); el.append(fer([x0,9,9],[x0+1,10,10]))
el.append(box([7,7,0],[9,9,1],{'north':F('#bois',U(0,6,2,8)),'south':F('#bois',U(0,6,2,8)),'east':F('#bois',U(0,6,1,8)),
    'west':F('#bois',U(0,6,1,8)),'up':F('#bois',U(3,0,5,1)),'down':F('#bois',U(6,0,8,1))}))  # le robinet, au-dessus du cerclage bas
el.append(fer([7,6,0],[9,7,1]))                                            # son bec
el.append(fer([7,9,0],[9,10,1]))                                           # sa clé
W('block/cuve_a_tan.json',{'parent':'minecraft:block/block','textures':{
 'douelle':'minecraft:block/barrel_side','dedans':PL,'fond':'minecraft:block/barrel_bottom','bord':'minecraft:block/barrel_top_open',
 'fer':FER,'bois':HC,'particle':'minecraft:block/barrel_side'},'elements':el})

def peau_flottante(path,tex,y,parent=None):
    # l'image de l'objet à plat, 8 × 8 (la moitié de sa taille, comme les objets posés de Hands-On), dans l'ouverture de 12
    objet(path,parent,[box([4,y,4],[12,y,12],{'up':F('#peau',[0,0,16,16]),'down':F('#peau',[0,16,16,0])})],tex)
peau_flottante('block/cuve_a_tan_peaux.json','fallow:item/peau_raclee',13.01)
peau_flottante('block/cuve_a_tan_cuir.json','fallow:item/cuir_mouille',13.01)
peau_flottante('diegesis/peau_raclee.json','fallow:item/peau_raclee',0,'minecraft:block/block')
peau_flottante('diegesis/cuir_mouille.json','fallow:item/cuir_mouille',0,'minecraft:block/block')
print('ok')
