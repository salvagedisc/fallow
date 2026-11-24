// La surface de l'eau, vue du dessus ou d'en dessous. Elle rassemble tout ce qui fait l'eau :
//  - la forme : les vagues (lib/waves.glsl) levées par le vent selon la place libre autour, ralenties sur les
//    hauts-fonds, emportées par le courant là où l'eau coule ; les sillages (lib/wakes.glsl), la pluie, les chutes ;
//  - ce qu'elle renvoie : le reflet du paysage, flou là où les rides sont trop fines pour l'œil (rugosité tirée des
//    vagues retirées), et l'éclat de l'astre en micro-facettes (GGX), dans la proportion exacte de Fresnel ;
//  - ce qu'elle laisse voir : le fond, trouvé en suivant le rayon réfracté à l'écran, éteint le long de ce trajet,
//    et la lumière que l'eau diffuse d'elle-même sur le même trajet (lib/water.glsl), plus vive vers l'astre ;
//  - l'écume (lib/waterfoam.glsl) : ressac, bouillon des chutes, moutons, sillages et gouttes.
// Inclus par solid.fsh pour le programme de l'eau seulement.

#ifndef HAS_FRAME_COUNTER
uniform int frameCounter;
#define HAS_FRAME_COUNTER
#endif

/** Un bruit qui change à chaque image, pour que l'anticrénelage temporel en fasse la moyenne. */
vec2 waterNoise(vec2 frag) {
    float a = dither(frag);
    float i = float(frameCounter & 255);
    return fract(vec2(a + i * 0.6180340, dither(frag.yx + 17.0) + i * 0.7548777));
}

/** Une micro-facette tirée selon GGX autour de n (rugosité a) : le reflet flou, une direction par image. */
vec3 ggxFacet(vec3 n, float a, vec2 u) {
    float phi = 6.2831853 * u.x;
    float cosT = sqrt((1.0 - u.y) / (1.0 + (a * a - 1.0) * u.y));
    float sinT = sqrt(max(1.0 - cosT * cosT, 0.0));
    vec3 t = normalize(abs(n.y) < 0.99 ? cross(n, vec3(0.0, 1.0, 0.0)) : cross(n, vec3(1.0, 0.0, 0.0)));
    vec3 b = cross(n, t);
    return normalize(t * (cos(phi) * sinT) + b * (sin(phi) * sinT) + n * cosT);
}

/**
 * Le rayon réfracté suivi à l'écran jusqu'au fond : des pas qui s'allongent, puis un resserrement par dichotomie.
 * start, ray : repère vue ; rend la position à l'écran du fond touché (xy) et la longueur d'eau traversée (z), ou
 * une longueur négative si le rayon sort de l'écran ou passe derrière un objet posé sur l'eau.
 */
vec3 screenRefraction(vec3 start, vec3 ray, float jitter, float surfaceZ) {
#if WATER_REFRACTION > 0
    float t = 0.0;
    float stride = 0.18 * (0.6 + 0.8 * jitter);
    float before = 0.0;
    vec3 s = vec3(0.0);
    bool hit = false;
    for (int i = 0; i < WATER_REFRACTION; i++) {
        before = t;
        t += stride;
        stride *= 1.65;
        s = screenOf(start + ray * t);
        if (any(lessThan(s.xy, vec2(0.0))) || any(greaterThan(s.xy, vec2(1.0)))) return vec3(s.xy, -1.0);
        float depth = texture(depthtex1, s.xy).r;
        if (depth >= 1.0) continue;
        if ((start + ray * t).z < viewAt(s.xy, depth).z) {
            hit = true;
            break;
        }
    }
    if (!hit) return vec3(s.xy, t);
    float lo = before, hi = t;
    for (int i = 0; i < 5; i++) {
        float mid = 0.5 * (lo + hi);
        vec3 m = screenOf(start + ray * mid);
        float depth = texture(depthtex1, m.xy).r;
        if (depth < 1.0 && (start + ray * mid).z < viewAt(m.xy, depth).z) hi = mid;
        else lo = mid;
    }
    s = screenOf(start + ray * hi);
    // Ce qu'on a touché est devant la surface (une barque, une herbe) : ce n'est pas le fond.
    float depth = texture(depthtex1, s.xy).r;
    if (depth < 1.0 && viewAt(s.xy, depth).z > surfaceZ - 0.02) return vec3(s.xy, -1.0);
    return vec3(s.xy, hi);
#else
    return vec3(0.0, 0.0, -1.0);
#endif
}

#if FLOATING_LEAVES > 0 && defined VOXELS
#ifndef LEAFFALL_GLSL
uniform sampler2D canopyTex;
#endif
/**
 * Le feuillage au-dessus d'une eau (repère monde) : sa couleur (sRGB) et 1 si la surface est sous lui, à moins de
 * douze blocs de son dessous (shadowcomp2, program/canopy.csh). Comme pour les feuilles qui tombent, une colonne
 * voisine de deux blocs au plus compte aussi : le vent porte les feuilles un peu au-delà du bord.
 */
vec4 canopyOverWater(vec3 at) {
    ivec2 c0 = ivec2(floor(at.xz - cameraPosition.xz + fract(cameraPosition.xz))) + ivec2(VOX_W / 2);
    for (int k = 0; k < 5; k++) {
        ivec2 c = c0 + (k == 0 ? ivec2(0) : ivec2(k == 1 ? 2 : (k == 2 ? -2 : 0), k == 3 ? 2 : (k == 4 ? -2 : 0)));
        if (any(lessThan(c, ivec2(0))) || any(greaterThanEqual(c, ivec2(VOX_W)))) continue;
        vec4 v = texelFetch(canopyTex, c, 0);
        float under = v.a + floor(cameraPosition.y);
        if (v.a < -999.0 || at.y > under + 0.5 || at.y < under - 12.0) continue;
        return vec4(v.rgb, 1.0);
    }
    return vec4(0.0);
}

/**
 * Les feuilles tombées sur l'eau : sous un arbre, quelques feuilles flottent à plat, de la couleur du feuillage
 * tirée vers le brun, et dérivent lentement dans le sens du vent en tournant sur elles-mêmes, puis s'enfoncent et
 * disparaissent ; d'autres prennent leur place. Bien plus nombreuses sous un feuillage d'automne. Le courant les
 * emporte : il n'en reste pas sur une eau qui coule vite. Rend la part de la surface couverte et sa couleur
 * (linéaire) dans tint.
 */
float floatingLeaves(vec3 world, float flow, float footprint, float t, out vec3 tint) {
    tint = vec3(0.0);
    const float scale = 1.5;
    float px = footprint * scale;
    float keep = (1.0 - smoothstep(0.5, 1.4, flow)) * (1.0 - smoothstep(0.10, 0.22, px));
    if (keep <= 0.01) return 0.0;
    vec4 canopy = canopyOverWater(world);
    if (canopy.a <= 0.0) return 0.0;
    // Un feuillage d'automne, jaune ou roux, a autant de rouge que de vert ; celui de l'été, plus vert, en perd peu.
    float autumn = smoothstep(-0.06, 0.06, canopy.r - canopy.g);
    float density = 0.05 + 0.33 * autumn;
    vec2 wind = brumaireLink > 0.5 ? normalize(brumaireWind.xy + vec2(1e-4, 0.0)) : vec2(0.8, 0.6);
    float force = brumaireLink > 0.5 ? brumaireWind.z : 0.2;
#if WATER_PIXELS > 0
    // Au pixel de texture près, comme l'eau : une feuille dessinée par carrés de 1/16 de bloc.
    vec2 p = (floor(world.xz * 16.0) + 0.5) / 16.0 * scale;
    px = max(px, 1e-3) * 0.25;
#else
    vec2 p = world.xz * scale;
#endif
    vec2 base = floor(p);
    float best = 0.0;
    for (int k = 0; k < 9; k++) {
        vec2 cell = base + vec2(k % 3 - 1, k / 3 - 1);
        float hs = fract(sin(dot(cell, vec2(127.1, 311.7))) * 43758.5453);
        if (hs >= density) continue;
        float h2 = fract(sin(dot(cell, vec2(269.5, 183.3))) * 43758.5453);
        float h3 = fract(sin(dot(cell, vec2(419.2, 371.9))) * 43758.5453);
        // Sa vie : elle arrive au bord de sa case, la traverse sous le vent en une minute ou deux, puis coule.
        float life = 60.0 + 80.0 * h2;
        float u = fract(t * (1.0 + force) / life + h3);
        vec2 side = vec2(-wind.y, wind.x);
        vec2 centre = cell + 0.5 + (u - 0.5) * 0.9 * wind + side * (h2 - 0.5) * 0.5;
        float angle = h3 * 6.2832 + (u - 0.5) * (h2 - 0.5) * 3.0 + 0.15 * sin(t * 0.5 + h2 * 20.0);
        vec2 d = p - centre;
        float ca = cos(angle), sa = sin(angle);
        vec2 q = vec2(ca * d.x + sa * d.y, -sa * d.x + ca * d.y);
        float len = 0.34 + 0.12 * hs / max(density, 1e-3);
        float x = q.x / len;
        // Sa queue : un trait derrière elle.
        float stem = step(-1.3, x) * step(x, -0.9) * step(abs(q.y), 0.05);
        if (abs(x) >= 1.0 && stem <= 0.0) continue;
        // Une feuille : pointue au bout, plus large vers la queue.
        float xc = clamp(x, -1.0, 1.0);
        float halfWidth = len * 0.42 * pow(1.0 - xc * xc, 0.7) * (1.0 - 0.25 * xc);
        float shape = max(1.0 - smoothstep(-px, px, abs(q.y) - halfWidth), stem);
        float fade = smoothstep(0.0, 0.08, u) * (1.0 - smoothstep(0.82, 1.0, u));
        float cover = shape * fade;
        if (cover > best) {
            best = cover;
            vec3 leaf = toLinear(canopy.rgb);
            tint = mix(leaf, vec3(0.30, 0.13, 0.04), 0.25 + 0.45 * h2) * (0.8 + 0.35 * h3);
            // La nervure, plus sombre, et la feuille qui s'imbibe en s'enfonçant.
            tint *= 1.0 - 0.3 * step(abs(q.y), 0.05) * step(x, 0.8);
            tint *= 1.0 - 0.45 * smoothstep(0.6, 0.95, u);
        }
    }
    return best * keep;
}
#endif

#if WATER_GUSTS > 0
/**
 * Les risées : par vent faible ou moyen, des souffles passent sur l'eau libre en plaques étirées dans le sens du vent,
 * qui avancent avec lui et se défont. Sous une risée l'eau se ride et ternit ; entre elles elle reste lisse et renvoie
 * le ciel comme un miroir. Rend la part de risée (0..1) et la force du vent qui la dessine dans amount.
 */
float waterGust(vec2 p, float t, WaveEnv env, out float amount) {
    // Rien sans vent ; par grand vent toute l'eau est ridée et les risées ne se voient plus.
    amount = smoothstep(0.03, 0.15, env.force) * (1.0 - smoothstep(0.8, 1.0, env.force)) * smoothstep(0.2, 0.5, env.open)
           * (1.0 - env.storm);
    if (amount <= 0.0) return 0.0;
    vec2 w = env.wind;
    vec2 side = vec2(-w.y, w.x);
    // Elles filent avec le vent, un peu plus vite que lui sur l'eau ; étirées dans son sens.
    vec2 q = p - w * t * (1.2 + 3.0 * env.force);
    vec2 r = vec2(dot(q, w) / 1.8, dot(q, side));
    float n = cloudNoise(vec3(r * 0.010, 0.37 + t * 0.0025)).r * 0.7 + cloudNoise(vec3(r * 0.031, 0.81 + t * 0.006)).g * 0.3;
    return smoothstep(0.42, 0.60, n);
}
#endif

vec4 water(vec3 biome, vec3 normal, vec2 lm) {
    vec3 world = vPos + cameraPosition;
    vec3 eye = normalize(-vPos);
    bool below = isEyeInWater == 1;
    bool top = abs(normal.y) > 0.5;
    float t = frameTimeCounter;
    // Avant toute branche : la pente réelle de la face (l'eau qui coule penche) et la taille du pixel sur l'eau.
    vec3 geo = normalize(cross(dFdx(vPos), dFdy(vPos)));
    if (geo.y < 0.0) geo = -geo;
    float footprint = max(length(fwidth(world.xz)), 1e-3);

    vec2 frag = gl_FragCoord.xy / vec2(viewWidth, viewHeight);
    vec3 view = (gbufferModelView * vec4(vPos, 1.0)).xyz;
#if TAA > 0
    // Le bruit des pas de reflet change à chaque image : l'anticrénelage le fond, au lieu d'une trame en damier figée
    // dans les reflets des falaises.
    float jitter = dither(gl_FragCoord.xy + 5.588238 * float(frameCounter & 63));
#else
    float jitter = dither(gl_FragCoord.xy);
#endif
    vec2 noise = waterNoise(gl_FragCoord.xy);
    float straight = texelFetch(depthtex1, ivec2(gl_FragCoord.xy), 0).r;
    float through = straight >= 1.0 ? 64.0 : max(length(viewAt(frag, straight)) - length(view), 0.0);

    float lit = shadowAt(vPos, normal, jitter, 4) * cloudShadow(world, gLightDir);
    float sky = lm.y * lm.y;
    vec3 hue = waterHue(biome);
    vec3 ext = waterExtinction(hue);

    // La forme de la surface.
    WaterSurround around = waterSurround(vPos);
    WaveEnv env = waveEnvDefault();
    env.open = around.open;
    env.depth = below ? 4.0 : clamp(through * max(eye.y, 0.2), 0.1, 16.0);
#if WATER_FLOW > 0
    // L'eau qui coule descend la pente de sa face ; le courant couche les rides dans son sens.
    float grade = length(geo.xz) / max(geo.y, 0.1);
    if (top && grade > 0.02) {
        float speed = smoothstep(0.02, 0.10, grade) * (1.1 + 4.0 * grade);
        env.flow = normalize(geo.xz) * speed;
        env.wind = normalize(mix(env.wind, normalize(geo.xz), 0.8));
        env.force = max(env.force, min(speed * 0.35, 0.9));
    }
#endif
    float rain = rainStrength * rainHere * smoothstep(0.80, 0.95, lm.y);
    vec3 n = normal;
    float foam = 0.0;
    float rough = 0.06;
    bool falling = false;
    WaveSample wave = WaveSample(0.0, vec2(0.0), vec3(1.0, 1.0, 0.0), vec3(0.0), 0.0, 0.0);
    if (top) {
        vec2 p = world.xz;
#if WATER_PIXELS > 0
        // Les vagues se lisent au seizième de bloc, comme les pixels des textures.
        p = (floor(p * 16.0) + 0.5) / 16.0;
        footprint = max(footprint, 1.0 / 16.0);
#endif
        wave = waveField(p, t, env, WATER_WAVE_COUNT, footprint);
        vec2 slope = wave.slope;
        float gustRough = 0.0;
#if WATER_GUSTS > 0
        if (normal.y > 0.5) {
            float gustAmount;
            float gust = waterGust(p, t, env, gustAmount);
            // Sous une risée les rides se creusent ; entre elles la surface se calme.
            slope *= mix(1.0, mix(0.35, 1.7, gust), gustAmount);
            gustRough = 0.020 * gust * gustAmount;
        }
#endif
#if WATER_WAKES > 0
        // Les rides de ce qui passe (lib/wakes.glsl).
        vec3 wake = wakeSlope(p);
        slope += clamp(wake.xy * 0.45, -0.45, 0.45);
        foam = wake.z;
#endif
        foam = max(foam, fallChurn(around, world, t, slope));
        if (normal.y > 0.5) {
            float energy = clamp(luma(vec3(env.force, env.storm, env.open)) * 1.6, 0.0, 1.0);
            foam = max(foam, shoreFoam(around, world, energy, t));
            foam = max(foam, whitecaps(p, t, env, waveFold(wave)));
            // Une eau qui dévale fait de l'écume là où elle va le plus vite.
            foam = max(foam, smoothstep(1.6, 3.2, length(env.flow)) * 0.5);
        }
#if WATER_PIXELS > 0
        // Et par paliers : des plages de reflet aux bords francs, comme une texture dessinée.
        slope = floor(slope / 0.03 + 0.5) * 0.03;
#endif
        n = normalize(vec3(-slope.x, 1.0, -slope.y));
        // La rugosité : les rides trop fines pour le pixel, et la pluie qui grêle la surface.
        rough = sqrt(2.0 * wave.variance + 0.0009 + 0.004 * rain + gustRough);
    } else {
        float streak;
        falling = waterFalling(vPos, normal);
        n = fallFace(world, normal, t, falling, streak);
        foam = streak;
        rough = 0.18;
    }
    rough = clamp(rough, 0.03, 0.45);
    if (below) n = -n;
    if (dot(n, eye) < 0.05) n = normalize(n + eye * (0.05 - dot(n, eye)));

    if (below) {
        // Vue d'en dessous : le monde du dessus passe par la fenêtre de Snell (48° autour de la verticale) ; au-delà
        // de l'angle limite la surface renvoie tout, et l'eau s'y reflète elle-même.
        float cosV = max(dot(n, eye), 0.0);
        float F = fresnel(cosV, WATER_IOR, 1.0);
        vec3 r = reflect(-eye, n);
        float dist;
        vec4 hit = screenReflection(view, mat3(gbufferModelView) * r, jitter, -1.0, WATER_REFLECTIONS, dist);
        vec3 inner = waterInscatter(hue, r, 32.0, gLightColor * lit, gAmbient * max(sky, 0.08));
        inner = mix(inner, hit.rgb, hit.a);
        vec3 tr = refract(-eye, n, WATER_IOR);
        if (F >= 1.0 || dot(tr, tr) < 1e-4) return vec4(inner, 1.0);
        vec3 s = screenOf(view + mat3(gbufferModelView) * tr * 12.0);
        vec3 above = texture(colortex6, clamp(s.xy, 0.001, 0.999)).rgb;
        if (s.z > 1.0 || any(lessThan(s.xy, vec2(0.0))) || any(greaterThan(s.xy, vec2(1.0)))) above = skyAt(tr) * mix(0.12, 1.0, sky);
        // Le bord de la fenêtre s'irise un peu et s'assombrit.
        float rim = smoothstep(0.75, 1.0, F);
        above *= 1.0 - 0.35 * rim;
        return vec4(mix(above, inner, F), 1.0);
    }

    // Ce qu'elle laisse voir : le fond au bout du rayon réfracté, éteint par l'eau traversée, plus ce que l'eau diffuse
    // d'elle-même le long du trajet.
    vec3 down = refract(-eye, n, 1.0 / WATER_IOR);
    if (dot(down, down) < 1e-4) down = -eye;
    vec2 uv = frag;
    float travel = through;
#if WATER_REFRACTION > 0
    vec3 hitRef = screenRefraction(view, mat3(gbufferModelView) * down, jitter, view.z);
    if (hitRef.z > 0.0) {
        uv = hitRef.xy;
        travel = hitRef.z;
    } else if (straight < 1.0) {
        // Sorti de l'écran : on garde le fond droit dessous, à la longueur que le rayon aurait faite.
        travel = through;
    }
#else
    // Sans recherche : le fond décalé par la pente, comme une loupe.
    vec3 bend = mat3(gbufferModelView) * vec3(n.x, 0.0, n.z);
    uv = frag + bend.xy * gbufferProjection[1][1] * 0.35 * min(through, 3.0) / max(-view.z, 1.0);
    float d = texture(depthtex1, uv).r;
    if (d < 1.0 && viewAt(uv, d).z > view.z - 0.02) uv = frag;
    else travel = d >= 1.0 ? 64.0 : max(length(viewAt(uv, d)) - length(view), 0.0);
#endif
    float floorDepth = texture(depthtex1, uv).r;
    if (floorDepth >= 1.0) travel = 64.0;
    // Une eau qui tombe n'a que l'épaisseur de sa colonne, et l'air qu'elle emporte la blanchit.
    if (falling) travel = min(travel, 0.9);
    vec3 pass = exp(-ext * travel);
    float sunIn = 1.0 - fresnel(max(gLightDir.y, 0.0), 1.0, WATER_IOR);
    vec3 glow = waterInscatter(hue, down, travel, gLightColor * lit * sunIn, gAmbient * max(sky, 0.06));
    vec3 under = texture(colortex6, uv).rgb * pass + glow;

    // La crête d'une vague à contre-jour laisse passer un peu de soleil, verdi par l'eau.
    float back = pow(max(dot(-eye, gLightDir), 0.0), 3.0) * smoothstep(-0.02, 0.06, wave.height);
    under += gLightColor * lit * waterAlbedo(hue) * back * 0.35;

    // Ce qu'elle renvoie : la scène quand le rayon la trouve à l'écran, ce qui est hors cadre bloc par bloc, sinon le
    // ciel. Les rides trop fines pour le pixel brouillent le reflet : une facette tirée au sort à chaque image.
    vec3 facet = n;
#if WATER_BLUR > 0
    if (rough > 0.06) facet = ggxFacet(n, min((rough - 0.04) * 0.6, 0.25), noise);
#endif
    vec3 r = reflect(-eye, facet);
    if (dot(r, n) < 0.0) r = reflect(-eye, n);
    r.y = max(r.y, 0.01);
    float cosV = max(dot(n, eye), 0.0);
    // Un peu plus de reflet que la physique ne le veut : l'eau se lit d'abord par ce qu'elle reflète.
    float F = min(fresnel(cosV, 1.0, WATER_IOR) * 1.35 + 0.012, 1.0);
    vec3 mirror = skyReflection(world, r) * mix(0.12, 1.0, sky);
    // De jour, le ciel que renvoie l'eau perd son bleu pour un gris à peine vert : un lac se lit vert-de-gris, comme
    // l'eau de Recrafted, et non comme un miroir du ciel bleu. Le paysage reflété garde ses couleurs.
    mirror = mix(mirror, vec3(luma(mirror)) * vec3(0.96, 1.0, 0.92), 0.8 * gDay);
    // Et il en renvoie moins : un lac vu de loin reste d'un gris-vert sombre, pas d'un blanc de ciel.
    mirror *= mix(1.0, 0.5, gDay);
    // La nuit, au contraire, le ciel renvoyé compte triple : sans lui un lac se perd dans le noir du terrain.
    mirror *= mix(3.0, 1.0, gDay);
    float dist;
    vec4 hit = screenReflection(view, mat3(gbufferModelView) * r, jitter, 1.0, WATER_REFLECTIONS, dist);
#if VOXEL_REFLECTIONS > 0
    if (hit.a < 0.99) {
        float reach;
        vec4 vox = voxelReflection(vPos + normal * 0.02, r, reach);
        if (vox.a > 0.0) mirror = mix(mirror, veiled(vox.rgb, r * reach * 0.4), vox.a);
    }
#endif
    if (hit.a > 0.0) {
        // Le point reflété, voilé par l'air et la brume de la surface jusqu'à lui (le chemin de la caméra à la
        // surface est voilé par composite1 comme le reste). La brume basse est déjà comptée une fois sur ce
        // chemin : le reflet n'en reprend qu'une part, sans quoi les rives disparaîtraient de l'eau.
        mirror = mix(mirror, veiled(hit.rgb, r * dist * 0.4), hit.a);
    }
    vec3 color = mix(under, mirror, F);

    // L'éclat de l'astre : micro-facettes GGX, au moins aussi larges que son disque.
    vec3 h = normalize(gLightDir + eye);
    float nl = max(dot(n, gLightDir), 0.0);
    if (nl > 0.0) {
        float a = max(rough, 0.05);
        float spec = ggx(max(dot(n, h), 0.0), a) * smithG1(nl, a) * smithG1(max(cosV, 1e-3), a)
            * fresnel(max(dot(eye, h), 0.0), 1.0, WATER_IOR) / (4.0 * max(cosV, 0.05));
        color += gLightColor * lit * min(spec, 24.0) * (1.0 - 0.8 * rain);
#if WATER_GLITTER > 0
        // Le chemin de paillettes : sur les vaguelettes trop petites pour être dessinées, une facette ici ou là renvoie
        // l'astre droit dans l'œil un instant. Elles sont semées dans un lobe plus large que l'éclat, qui s'allument et
        // s'éteignent sans cesse ; au loin, trop fines, elles se fondent dans l'éclat.
        if (top && !below) {
            float aw = 0.20;
            float lobe = ggx(max(dot(n, h), 0.0), aw) * aw * aw * 3.14159265;
            vec2 cell = floor(world.xz * 9.0);
            float seed = fract(sin(dot(cell, vec2(12.9898, 78.233))) * 43758.5453);
            float frame = floor(t * 7.0 + seed * 7.0);
            float hs = fract(sin(dot(cell + frame * vec2(0.37, 1.13), vec2(39.346, 11.135))) * 43758.5453);
            float near_ = 1.0 - smoothstep(24.0, 64.0, length(vPos));
            float spark = step(0.94, hs) * smoothstep(0.15, 0.6, lobe) * near_;
            color += gLightColor * lit * spark * 5.0 * (1.0 - rain);
        }
#endif
    }

    // L'écume : une matière blanche et mate posée sur l'eau, qui prend le jour et le ciel et cache le reflet.
    float cover = top ? foamLace(world, foam, t) : foam;
    if (falling) cover = max(cover, 0.22);
    if (cover > 0.0) {
        vec3 light = gLightColor * lit * (0.25 + 0.75 * max(gLightDir.y, 0.0)) * 0.9 + gAmbient * max(sky, 0.1) * 0.9;
        color = mix(color, vec3(0.64, 0.68, 0.67) * light, 0.85 * cover);
    }
#if FLOATING_LEAVES > 0 && defined VOXELS
    if (top && !below && !falling) {
        vec3 leafTint;
        float leaf = floatingLeaves(world, length(env.flow), footprint, t, leafTint);
        if (leaf > 0.0) {
            vec3 light = gLightColor * lit * (0.25 + 0.75 * max(gLightDir.y, 0.0)) + gAmbient * max(sky, 0.1);
            color = mix(color, leafTint * light, 0.95 * leaf);
        }
    }
#endif
#if PLANKTON > 0
    // Le plancton des mers chaudes : la nuit, ce qui remue l'eau (vagues qui déferlent, sillages, pluie) s'allume
    // d'un bleu froid, semé de points plus vifs qui clignent. Une eau chaude se reconnaît à sa teinte (vert-bleu).
    if (top && foam > 0.01) {
        float dark = 1.0 - smoothstep(0.05, 0.35, gDay);
        float gb = biome.g / max(biome.b, 1e-3);
        float warm = smoothstep(0.6, 0.85, gb) * (1.0 - smoothstep(0.95, 1.1, gb));
        if (dark * warm > 0.01) {
            vec2 speck = floor(world.xz * 16.0);
            float hs = fract(sin(dot(speck, vec2(127.1, 311.7))) * 43758.5453);
            float blink = pow(max(sin(t * 2.5 + hs * 60.0), 0.0), 6.0) * step(0.85, hs);
            float glow = foam * (0.35 + 2.5 * blink);
            color += vec3(0.12, 0.55, 1.0) * glow * dark * warm * PLANKTON_STRENGTH;
        }
    }
#endif
    return vec4(color, 1.0);
}
