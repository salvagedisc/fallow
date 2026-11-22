// Brumaire : réglages. Les valeurs entre crochets sont celles que propose le menu d'Iris.

// Brume
#define MIST_DENSITY 1.0 // [0.5 0.75 1.0 1.25 1.5 2.0]
#define MIST_HEIGHT 16.0 // [12.0 16.0 24.0 32.0 48.0]
#define MIST_LIFT 0.7 // [0.0 0.3 0.5 0.7 0.85 1.0]
#define MIST_RING 1.0 // [0.0 0.5 1.0 1.5 2.0]
#define MIST_RING_MAX 512.0 // [256.0 384.0 512.0 768.0 1024.0]
#define AIR_DENSITY 1.0 // [0.0 0.5 0.75 1.0 1.25 1.5 2.0]
#define FOG_STEPS 12 // [0 8 12 16 24]
#define FOG_RANGE 96.0 // [48.0 64.0 96.0 128.0]
#define FOG_OPTICS 1 // [0 1]
#define FOG_BILLOWS 1.0 // [0.0 0.5 1.0 1.5]
#define FOG_BLOCK_GLOW 1.0 // [0.0 0.5 1.0 1.5 2.0]
#define CAVE_HAZE 1.0 // [0.0 0.5 1.0 1.5]
#define CAVE_DETAIL 1 // [0 1]
// Les gouttes qui tombent des voûtes des grottes, dans la lumière des torches (lib/cavedrips.glsl).
#define CAVE_DRIPS 1 // [0 1]
#define LAVA_STEAM 1 // [0 1]

// Nuages
#define CLOUD_STEPS 16 // [0 10 16 24 32]
#define CLOUD_HEIGHT 220.0 // [180.0 200.0 220.0 250.0 280.0]
#define CLOUD_COVERAGE 0.45 // [0.2 0.3 0.45 0.6 0.75]
#define CLOUD_SPEED 1.0 // [0.0 0.5 1.0 2.0 3.0]
#define CLOUD_LIGHT 1.0 // [0.6 0.8 1.0 1.2 1.5]
#define CLOUD_RANGE 3000.0 // [1500.0 2000.0 3000.0 4000.0]
#define CLOUD_SHADOWS 0.8 // [0.0 0.4 0.6 0.8 1.0]
#define CLOUD_TYPES 1 // [0 1]
#define CLOUD_CIRRUS 2 // [0 1 2 3]
#define CLOUD_ALTO 2 // [0 1 2 3]
#define RAIN_ROLLS 1 // [0 1 2]
#define CLOUD_IRIDESCENCE 1 // [0 1]
#define CLOUD_MULTISCATTER 4 // [1 2 3 4]
#define CLOUD_LIGHT_STEPS 6 // [3 4 6 8]
#define CLOUD_LIGHTNING 1 // [0 1]
#define HEAT_LIGHTNING 1 // [0 1]

// Ciel
#define SKY_STARS 1 // [0 1]
#define SKY_MILKY_WAY 1 // [0 1]
#define SKY_METEORS 1 // [0 1]
#define SKY_AURORA 1 // [0 1]
#define SKY_AURORA_STRENGTH 1.0 // [0.5 1.0 1.5 2.0]
#define SKY_HALO 1 // [0 1]
#define SKY_RAINBOW 1 // [0 1]
#define SKY_TWILIGHT_BELT 1 // [0 1]
#define SKY_RAYS 1 // [0 1]
#define SKY_VENUS 1 // [0 1]
#define SKY_COMET 1 // [0 1]
#define SKY_ZODIACAL 1 // [0 1]
#define SKY_AIRGLOW 1 // [0 1]
#define SKY_NOCTILUCENT 1 // [0 1]
#define SUN_PILLAR 1 // [0 1]
#define SKY_MOONBOW 1 // [0 1]
#define EARTHSHINE 1 // [0 1]
#define MOON_UPRIGHT 1 // [0 1]

// Lumière
#define SHADOW_FILTER 8 // [1 4 6 8 12 16]
// Les ombres calées sur les pixels des blocs : leur bord avance texel par texel (lib/light.glsl).
#define PIXEL_SHADOWS 1 // [0 1]
#define SHADOW_SOFTNESS 1.0 // [0.0 0.5 0.75 1.0 1.5 2.0]
#define SHADOW_SEARCH 10 // [6 10 16]
#define CONTACT_SHADOWS 1 // [0 1]
#define SSGI 6 // [0 4 6 8]
#define SSGI_STRENGTH 1.0 // [0.0 0.5 0.75 1.0 1.5 2.0]
#define LEAF_TRANSLUCENCY 1.0 // [0.0 0.5 1.0 1.5]
#define CANOPY_SHADE 1 // [0 1 2]
#define BLOCK_LIGHT_SHAPE 1 // [0 1]
#define LIGHT_SHAFTS 10 // [0 6 10 16 24]
#define SHAFT_STRENGTH 1.0 // [0.0 0.5 0.75 1.0 1.25 1.5]
#define MOON_SHAFTS 1 // [0 1 2]
#define BLOOM
#define AO_SAMPLES 10 // [0 6 10 16]
#define AO_RADIUS 1.2 // [0.6 0.9 1.2 1.6 2.0]
#define AO_STRENGTH 1.6 // [0.8 1.2 1.6 2.0 2.5]
#define BLOOM_STRENGTH 1.0 // [0.5 0.75 1.0 1.25 1.5]
// L'éclat du soleil dans l'œil : rais, voile et reflets de l'objectif (program/grade.fsh).
#define SUN_GLARE 1 // [0 1 2]
#define SUN_GLARE_STRENGTH 1.0 // [0.5 1.0 1.5 2.0]
// Les couleurs qui se séparent un peu vers les bords de l'image.
#define LENS_DIRT 1 // [0 1]
#define CHROMATIC_ABERRATION 1 // [0 1]
// Des gouttes sur l'objectif sous la pluie, et en sortant de l'eau (program/grade.fsh).
#define LENS_DROPS 1 // [0 1]
#define LENS_SNOW 1 // [0 1]
#define COLORED_LIGHT 1 // [0 1]
#define GLASS_TINT 1 // [0 1]
// Le verre qui se raccorde à ses voisins : une verrière sans cadres entre ses blocs (lib/glass.glsl).
#define CONNECTED_GLASS 1 // [0 1]

// Vent et eau
#define WIND_STRENGTH 1.0 // [0.0 0.5 0.75 1.0 1.25 1.5]
#define FOLIAGE_SHEEN 1.0 // [0.0 0.5 1.0 1.5]
// Le pied des herbes et des fleurs s'assombrit, comme dans une touffe où les brins se font de l'ombre.
#define PLANT_ROOTS 1 // [0 1]
#define PLANT_ROOTS_DEPTH 0.35 // [0.2 0.35 0.5]
#define LEAF_FLUTTER 1 // [0 1]
// Les herbes marines et les algues ondulent dans le courant, les nénuphars dansent sur les rides.
#define WATER_PLANT_SWAY 1 // [0 1]
// Les lanternes suspendues se balancent au vent.
#define LANTERN_SWAY 1 // [0 1]
// Jusqu'où (blocs) les touffes d'herbe sont plus fournies.
#define BUSHY_DISTANCE 64 // [32 48 64 96 128]
// Les touffes plus fournies : chaque herbe, fleur ou culture proche reçoit une copie tournée (program/bushy.gsh),
// jusqu'à la distance du feuillage touffu.
#define GRASS_TUFTS 1 // [0 1]
#define WATER_WAVES 1.0 // [0.0 0.5 1.0 1.5]
#define WATER_MURK 1.0 // [0.5 0.75 1.0 1.5 2.0]
#define WATER_PIXELS 1 // [0 1]
#define WATER_CAUSTICS 2 // [0 1 2]
#define WATER_REFLECTIONS 20 // [0 12 20 32]
#define VOXEL_REFLECTIONS 64 // [0 32 64 96]
#define REFLECT_LEAF_GAPS 1 // [0 1]
#define WATER_MOTES 1 // [0 1]
#define WATER_WAKES 1 // [0 1]
#define WATER_FOAM 1 // [0 1]
#define WATER_WAVE_COUNT 20 // [8 12 20 28]
#define WATER_SWELL 1.0 // [0.0 0.5 1.0 1.5]
#define WATER_CHOP 1.0 // [0.0 0.5 1.0 1.5]
#define WATER_REFRACTION 8 // [0 5 8 12]
#define WATER_BLUR 1 // [0 1]
#define WATER_DISPERSION 1 // [0 1]
#define WATER_FLOW 1 // [0 1]
#define WATER_WHITECAPS 1 // [0 1]
#define WATER_GLITTER 1 // [0 1]
#define WATER_GUSTS 1 // [0 1]
#define FLOATING_LEAVES 1 // [0 1]
#define WATER_STARS 1 // [0 1]
#define FAR_WATER_SKY 1 // [0 1]
#define LUNAR_EVENTS 1 // [0 1]
#define LUNAR_EVENTS_STRENGTH 1.0 // [0.5 0.75 1.0]
#define PLANKTON 1 // [0 1]
#define PLANKTON_STRENGTH 0.10 // [0.04 0.06 0.10 0.15 0.25]
#define WATER_FALLS 1 // [0 1]
#define SHORE_SPRAY 1 // [0 1]
#define SPRAY_RAINBOW 1 // [0 1]
#define SHORE_WET 1 // [0 1]
#define AIR_MOTES 1 // [0 1]

// Pluie
#define SHADER_RAIN 1 // [0 1]
#define WETNESS 1.0 // [0.0 0.5 0.75 1.0 1.25]
#define PUDDLES 1 // [0 1]
#define PUDDLE_AMOUNT 1.0 // [0.5 0.75 1.0 1.25 1.5]
#define PUDDLE_ICE 1 // [0 1]
#define PUDDLE_STEPS 1 // [0 1]
#define RAIN_CURTAINS 1.0 // [0.0 0.5 1.0 1.5 2.0]
#define PATCHY_DRYING 1 // [0 1]
#define WALL_RIVULETS 1 // [0 1]
// Les gouttes qui perlent et coulent sur les vitres sous la pluie (lib/rain.glsl).
#define WINDOW_RAIN 1 // [0 1]
#define WINDOW_FROST 1 // [0 1]
#define EAVES_DRIPS 1 // [0 1]
// Les êtres et le joueur trempés sous la pluie, plus sombres et luisants (program/solid.fsh).
#define SNOWY_ENTITIES 1 // [0 1]
#define WET_ENTITIES 1 // [0 1]
#define RAIN_SPLASHES 1 // [0 1]
#define PRECIP_GLOW 1 // [0 1]
#define SNOW_BLIZZARD 1 // [0 1]
// La neige et le sable soufflés au ras du sol par grand vent (lib/drift.glsl).
#define GROUND_DRIFT 1 // [0 1]
// La brume qui fume sur l'eau les matins frais (lib/seasmoke.glsl).
#define WATER_STEAM 1 // [0 1]
#define SOIL_STEAM 1 // [0 1]
#define WET_REFLECTIONS 12 // [0 8 12 20]
#define ROUGH_REFLECTIONS 1 // [0 1]
#define ROUGH_REFLECTIONS_STRENGTH 1.0 // [0.0 0.5 0.75 1.0 1.25 1.5]

// Neige et chaleur
#define RELIEF 1 // [0 1]
#define RELIEF_STRENGTH 1.0 // [0.5 0.75 1.0 1.5 2.0]
#define RELIEF_DEPTH 0.06 // [0.03 0.06 0.1 0.15]
#define RELIEF_STEPS 12 // [0 8 12 20]
#define RELIEF_SHADOWS 1 // [0 1]
#define RELIEF_BEVEL 1 // [0 1]
#define RELIEF_RANGE 24.0 // [12.0 24.0 32.0 48.0]
#define SNOW_COVER 1 // [0 1]
#define ALPENGLOW 1 // [0 1]
#define AURORA_GLOW 1 // [0 1]
#define GROUND_TINT 1 // [0 1]
// Les forêts lointaines (Distant Horizons) : une nuance par bloc de feuillage, le dessous des couronnes plus sombre.
#define FAR_CANOPY 1 // [0 1]
#define GRASS_OVERHANG 1 // [0 1]
#define GRASS_SLOPES 1 // [0 1]
// Au loin (passé 44 à 60 blocs), les flancs d'herbe se couvrent d'herbe, comme les collines de Distant Horizons.
#define GRASS_FAR_SIDES 1 // [0 1]
// La matière de la neige posée : plaques, congères, rides du vent, éclats qui suivent le regard (lib/snow.glsl).
#define SNOW_DETAIL 1 // [0 1]
// La neige qui s'accumule pendant la chute : sous les arbres, sur les aiguilles, plaquée sur les faces au vent.
#define SNOW_BUILDUP 1 // [0 1]
#define SNOW_SLOPES 1 // [0 1]
// Le sable ridé par le vent (lib/snow.glsl).
#define SAND_DETAIL 1 // [0 1]
// La glace fissurée, avec ses bulles (lib/snow.glsl).
#define ICE_DETAIL 1 // [0 1]
// L'eau qui s'égoutte des arbres après la pluie, et la neige qui tombe des branches au vent (lib/leaffall.glsl).
#define CANOPY_DRIPS 1 // [0 1]
// Les matières des blocs : reflets de l'astre selon la rugosité, métaux, minerais, sculk (lib/material.glsl).
#define MATERIALS 1 // [0 1]
// Les cartes d'un pack de ressources labPBR (relief, matière, émission), à la place du relief tiré des textures.
#define LABPBR 1 // [0 1]
#define SPECULAR_STRENGTH 1.0 // [0.0 0.25 0.5 0.75 1.0 1.25 1.5 2.0]
#define ORE_GLINTS 1 // [0 1 2]
#define FROST 1 // [0 1]
// La rosée des matins doux sur l'herbe et les plantes (lib/cover.glsl).
#define DEW_HALO 1 // [0 1]
#define DEW 1 // [0 1]
#define WET_FOLIAGE 1 // [0 1]
#define HEAT_SHIMMER 1 // [0 1]
#define FLAME_SHIMMER 1 // [0 1]
#define MIRAGE 1 // [0 1]
#define DIAMOND_DUST 1 // [0 1]
#define DUST_DEVILS 1 // [0 1]
#define TRACKS 1 // [0 1]
#define TRACK_MINUTES 4.0 // [1.0 2.0 4.0 8.0 15.0]

// Couleurs
#define OLIVE 1.0 // [0.0 0.2 0.4 0.6 0.8 1.0]
#define FOLIAGE_TONE 1.0 // [0.0 0.5 1.0]
#define SATURATION 1.0 // [0.7 0.8 0.9 1.0 1.1 1.2]
#define CONTRAST 1.0 // [0.9 0.95 1.0 1.05 1.1 1.15]
#define EXPOSURE 1.0 // [0.7 0.85 1.0 1.15 1.3]
#define EXPOSURE_AUTO 1 // [0 1]
#define EXPOSURE_AUTO_AMOUNT 1.0 // [0.5 1.0 1.5]
#define EXPOSURE_SPEED 1.0 // [0.5 1.0 2.0]
#define RAIN_GLOOM 1 // [0 1 2]
#define DOF 0 // [0 1 2]
#define PURKINJE 1 // [0 1]
#define NIGHT_GRAIN 1 // [0 1]
#define VIGNETTE 1 // [0 1]

// Image
#define SMAA 2 // [0 1 2 3]
#define TAA 1 // [0 1]
#define TAA_SHARPEN 0.45 // [0.0 0.25 0.45 0.65 0.85]
#define TAA_DISOCCLUSION 1 // [0 1]
#define MOTION_BLUR 2 // [0 1 2 3 4]

const int shadowMapResolution = 2048; // [1024 1536 2048 3072 4096]
const float shadowDistance = 112.0; // [64.0 80.0 96.0 112.0 128.0 160.0 192.0]
#define FAR_SHADOWS 24 // [0 16 24 32]
const float sunPathRotation = -28.0; // [-40.0 -28.0 -15.0 0.0 15.0 28.0 40.0]
const bool shadowHardwareFiltering0 = false;
const bool shadowHardwareFiltering1 = true;
const float shadowDistanceRenderMul = 1.0;
const float shadowIntervalSize = 2.0;
const float entityShadowDistanceMul = 0.5;
const float ambientOcclusionLevel = 1.0;
// Iris lisse la pluie en « wetness » (en ticks) : le sol se mouille vite et sèche lentement, une minute de demi-vie,
// la même valeur que drynessHalfLife dans shaders.properties.
const float wetnessHalflife = 300.0;
const float drynessHalflife = 1200.0;

// Forme de la carte d'ombre : le centre est grossi, les lointains tassés.
#define SHADOW_BEND 0.86
#define SHADOW_DEPTH 0.5
