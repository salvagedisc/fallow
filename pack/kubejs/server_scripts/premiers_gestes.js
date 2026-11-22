// Fallow : les premiers gestes. À mains nues, on ramasse mais on ne coupe pas.
// La règle elle-même (tronc et roche impossibles sans le bon outil) est dans le module
// `outils` du mod fallow ; ce script règle les recettes et le butin qui vont avec.
// Voir docs/plan/premiers-gestes.md.
//
// La chaîne :
//   gravier -> silex
//   silex frappé sur une roche -> pierre à tailler (Overgeared), ou terre et gravier creusés -> parfois une pierre à tailler
//   feuilles secouées main vide -> parfois un bâton (module glanage du mod fallow), feuilles cassées -> parfois un bâton
//   pierre à tailler, clic droit -> tête d'outil ; tête + bâton -> outil de pierre (Overgeared)
//   hache + bûche -> 2 planches ; scie mécanique de Create -> 6 planches

// Outils en bois : retirés, cachés d'EMI (client_scripts/premiers_gestes.js, même liste)
// et retirés des coffres. Le premier outil est en pierre taillée.
const PREMIERS_GESTES_OBJETS = [
  'minecraft:wooden_sword', 'minecraft:wooden_shovel', 'minecraft:wooden_pickaxe',
  'minecraft:wooden_axe', 'minecraft:wooden_hoe'
]

// Planches par bûche à la hache (4 en vanilla). La scie de Create garde ses 6.
const PREMIERS_GESTES_PLANCHES_A_LA_HACHE = 2

// Chance de plus qu'une feuille cassée lâche un bâton (vanilla : 2 %). Secouer les feuilles main vide en donne aussi
// (module glanage du mod fallow) : rien n'est posé au sol (décision 0052).
const PREMIERS_GESTES_BATONS_FEUILLES = 0.06

// Terres qui cachent parfois une pierre à tailler quand on les creuse, et les chances (terre, gravier).
const PREMIERS_GESTES_TERRES = [
  'minecraft:dirt', 'minecraft:coarse_dirt', 'minecraft:rooted_dirt', 'minecraft:grass_block', 'minecraft:podzol'
]
const PREMIERS_GESTES_PIERRE_TERRE = 0.05
const PREMIERS_GESTES_PIERRE_GRAVIER = 0.12

// Écrit une entrée de recette JSON (`{"item": …}` ou `{"tag": …}`) comme KubeJS la lit.
function premiersGestesIngredient(json) {
  if (json.has('tag')) return `#${json.get('tag').getAsString()}`
  if (json.has('item')) return json.get('item').getAsString()
  return null
}

ServerEvents.recipes(event => {
  event.remove({ output: PREMIERS_GESTES_OBJETS })

  // Bûche -> planches : chaque recette d'une seule bûche (vanilla, Regions Unexplored, autres bois)
  // demande désormais une hache, qui reste dans la grille et perd un point.
  let planches = []
  event.forEachRecipe({ type: 'minecraft:crafting_shapeless', output: '#minecraft:planks' }, recipe => {
    let ingredients = recipe.json.getAsJsonArray('ingredients')
    if (ingredients == null || ingredients.size() !== 1 || !ingredients.get(0).isJsonObject()) return
    let bois = premiersGestesIngredient(ingredients.get(0).getAsJsonObject())
    let resultat = recipe.json.getAsJsonObject('result')
    if (bois == null || resultat == null) return
    planches.push({
      id: String(recipe.getOrCreateId()),
      bois: bois,
      planche: resultat.get('id').getAsString(),
      nombre: resultat.has('count') ? resultat.get('count').getAsInt() : 1
    })
  })
  planches.forEach(r => {
    event.remove({ id: r.id })
    let nombre = Math.max(1, Math.floor(r.nombre * PREMIERS_GESTES_PLANCHES_A_LA_HACHE / 4))
    event.shapeless(Item.of(r.planche, nombre), [r.bois, '#minecraft:axes'])
      .damageIngredient('#minecraft:axes')
      .id(`fallow:premiers_gestes/planches/${r.id.replace(':', '/')}`)
  })
  console.info(`[premiers gestes] ${planches.length} recettes de planches passées à la hache`)

  // Le feu de camp s'allume au silex : on peut faire du feu dès le premier jour,
  // avant le four et le charbon de bois.
  event.replaceInput({ id: 'minecraft:campfire' }, '#minecraft:coals', 'minecraft:flint')

  // Un galet de Twigs (posé par un joueur, Twigs n'en génère plus) est déjà une pierre : il se taille comme celle d'Overgeared.
  event.shapeless('overgeared:knappable_rock', ['twigs:pebble'])
    .id('fallow:premiers_gestes/galet_a_tailler')
})

LootJS.lootTables(event => {
  event.forEachTable(table => {
    PREMIERS_GESTES_OBJETS.forEach(id => table.removeItem(id))
  })
})

LootJS.modifiers(event => {
  event.addBlockModifier('#minecraft:leaves')
    .randomChance(PREMIERS_GESTES_BATONS_FEUILLES)
    .addLoot('minecraft:stick')

  // En creusant, on trouve parfois une pierre à tailler : dans la terre de temps en temps, plus souvent dans le gravier.
  event.addBlockModifier(PREMIERS_GESTES_TERRES)
    .randomChance(PREMIERS_GESTES_PIERRE_TERRE)
    .addLoot('overgeared:knappable_rock')
  event.addBlockModifier('minecraft:gravel')
    .randomChance(PREMIERS_GESTES_PIERRE_GRAVIER)
    .addLoot('overgeared:knappable_rock')
})
