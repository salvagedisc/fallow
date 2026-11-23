// L'herbe qui déborde : sur le côté d'un bloc d'herbe, la bordure verte du jeu (sa seconde couche, teinte par le
// biome et découpée) s'arrête à trois ou quatre pixels, d'un trait presque droit. Elle descend ici de quelques
// pixels de plus, d'une longueur qui change d'une colonne à l'autre, avec les mêmes gris que la bordure du jeu.
// Sur une pente (GRASS_SLOPES), la contremarche entre deux paliers d'herbe se couvre d'herbe sur toute sa hauteur.
// Lu par solid.fsh, avant la découpe.

#if GRASS_OVERHANG > 0
float grassEdgeHash(vec3 p) { return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453); }

/**
 * Le texel (couleur du jeu, avant la teinte) que prend un pixel transparent de la bordure, ou rien (alpha 0).
 * world : point de la face (repère monde) ; normal : sa normale, horizontale.
 */
vec4 grassOverhang(vec3 world, vec3 normal) {
    vec3 cell = floor(world - normal * 0.01);
    float across = abs(normal.x) > 0.5 ? world.z : world.x;
    float col = floor(fract(across) * 16.0);
    float row = floor((1.0 - fract(world.y)) * 16.0);
    // De 3 à 6 pixels, plus souvent court que long ; la face voisine du même bloc tire d'autres longueurs.
    float h = grassEdgeHash(cell + normal * 3.1 + vec3(col * 0.37, 0.0, col * 0.11));
    float reach = 3.0 + floor(h * h * 4.0) * float(GRASS_OVERHANG);
    if (row >= reach) return vec4(0.0);
    // Les gris de la bordure du jeu, tirés texel par texel ; le bout de la frange un peu plus sombre.
    float g = grassEdgeHash(cell * 1.7 + normal + vec3(col, row, 0.0));
    float grey = g < 0.25 ? 0.51 : (g < 0.5 ? 0.56 : (g < 0.75 ? 0.59 : 0.66));
    grey *= row >= reach - 1.0 ? 0.90 : 1.0;
    return vec4(vec3(grey), 1.0);
}

#if GRASS_SLOPES > 0 && VOXEL_REFLECTIONS > 0
// Le volume autour de la caméra (lib/voxel.glsl) et la couleur du dessus de chaque bloc (passe d'ombre).
#ifndef VOX_TOP_DECLARED
#define VOX_TOP_DECLARED
uniform usampler3D voxTopTex;
#endif

/**
 * Vrai si cette face de côté d'un bloc d'herbe est la marche d'une pente : devant elle l'air, et sous cet air un
 * bloc plein dont le dessus est vert (de l'herbe). Comme dans les packs qui « arrondissent » l'herbe, la marche se
 * couvre alors d'herbe. Seulement dans le volume ; vers son bord, les faces basculent une à une, au hasard.
 */
bool grassSlope(vec3 world, vec3 normal) {
    ivec3 below;
    if (!voxelRiser(world, normal, grassEdgeHash(floor(world - normal * 0.01) + normal * 7.3), below)) return false;
    uint top = texelFetch(voxTopTex, below, 0).r;
    if (top == 0u) return false;
    vec3 c = vec3(float(top & 255u), float((top >> 8) & 255u), float((top >> 16) & 255u)) / 255.0;
    return c.g > c.r * 1.05 && c.g > c.b * 1.15;
}

#endif

/** Le texel d'herbe qui couvre une marche : les gris du dessus de l'herbe, tirés texel par texel. */
vec4 grassRiser(vec3 world, vec3 normal) {
    vec3 cell = floor(world - normal * 0.01);
    float across = abs(normal.x) > 0.5 ? world.z : world.x;
    vec2 px = floor(vec2(fract(across), fract(world.y)) * 16.0);
    float g = grassEdgeHash(cell * 1.3 + normal * 5.0 + vec3(px, 0.0));
    float grey = g < 0.2 ? 0.47 : (g < 0.45 ? 0.54 : (g < 0.7 ? 0.58 : (g < 0.9 ? 0.63 : 0.70)));
#if GRASS_SMOOTH > 0
    grey = mix(grey, 0.58, 0.25 * float(GRASS_SMOOTH)); // les prés lisses (solid.fsh) valent aussi pour ces marches
#endif
    return vec4(vec3(grey), 1.0);
}

#if GRASS_FAR_SIDES > 0
/**
 * Vrai pour une face de côté d'un bloc d'herbe au loin : au-delà du volume (44 à 60 blocs, les mêmes faces qui
 * quittent l'essai des marches), toute face d'herbe se couvre d'herbe, comme les collines de Distant Horizons, qui sont
 * vertes sur leurs flancs. Sans cela, l'anneau entre le volume et le lointain se lirait brun.
 */
bool grassFarSide(vec3 world, vec3 normal) {
    vec3 cell = floor(world - normal * 0.01);
    float reach = length(cell.xz + 0.5 - cameraPosition.xz);
    return grassEdgeHash(cell + normal * 7.3) < smoothstep(44.0, 60.0, reach);
}
#endif
#endif
