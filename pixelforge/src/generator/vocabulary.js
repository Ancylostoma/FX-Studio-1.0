// Vocabulario español/inglés → conceptos. Para ampliar el motor offline,
// añade aquí sinónimos (sin tildes, en minúsculas; "|" separa alternativas).
// Etiquetas: s=sujeto  b=estructura  f=flora/atrezo  biome  feat  time  w=clima
// c=astro  fx  col  style  size  fly  n=cantidad  part  mat  theme  verb
const V = [
  // --- Ampliación: criaturas, personajes y edificios de kits/props ---
  ['s:tiger', 'tigre|tigres|tiger'], ['s:leopard', 'leopardo|guepardo|jaguar|leopard|cheetah'], ['s:panther', 'pantera|panther'],
  ['s:zebra', 'cebra|zebra'], ['s:giraffe', 'jirafa|giraffe'], ['s:elephant', 'elefante|mamut|elephant|mammoth'], ['s:boar', 'jabali|boar'], ['s:camel', 'camello|dromedario|camel'],
  ['s:fairy', 'hada|hadas|fairy|pixie'], ['s:angel', 'angel|angeles|angel'], ['s:demon', 'demonio|diablo|demon|devil'],
  ['s:troll', 'trol|troll|ogro|ogre|gigante'], ['s:orc', 'orco|orcos|orc'], ['s:goblin', 'goblin|duende|trasgo'], ['s:golem', 'golem'],
  ['s:vampire', 'vampiro|vampira|vampire|dracula'], ['s:witch', 'bruja|brujo|witch'], ['s:archer', 'arquero|arquera|archer'], ['s:viking', 'vikingo|viking|barbaro'],
  ['s:villager', 'aldeano|aldeana|campesino|campesina|villager|farmer|granjero'], ['s:monk', 'monje|monk|sacerdote'],
  ['s:owl', 'buho|lechuza|owl'], ['s:eagle', 'aguila|halcon|eagle|hawk'], ['s:bee', 'abeja|abejas|bee'], ['s:dragonfly', 'libelula|dragonfly'],
  ['s:penguin', 'pinguino|penguin'], ['s:spider', 'arana|arana gigante|spider'], ['s:griffin', 'grifo|griffin|gryphon'],
  ['s:train', 'tren|ferrocarril|locomotora|train|locomotive'], ['s:spaceship', 'nave espacial|naves espaciales|spaceship|starship|nave'], ['s:submarine', 'submarino|submarine'],
  ['s:explosion', 'explosion|explosiones|estallido|explosion'], ['s:fireball', 'bola de fuego|fireball'], ['s:magic', 'hechizo|conjuro|magia|spell|magic orb'], ['s:smoke', 'humo|humareda|smoke'],
  ['b:cathedral', 'catedral|cathedral'], ['b:church', 'iglesia|capilla|ermita|church|chapel'], ['b:hut', 'choza|chozas|hut|huts'], ['b:medievalhouse', 'casa medieval|casas medievales'],
  ['b:mosque', 'mezquita|mosque'], ['b:palace', 'palacio|palace'], ['b:museum', 'museo|museum'], ['b:bridge', 'puente|puentes|bridge'],
  ['b:wall', 'muralla|murallas|muro|wall'], ['b:fountain', 'fuente|fountain'], ['b:well', 'pozo|well'],
  ['f:bookshelf', 'estanteria|estanterias|libreria|libros|libro|bookshelf|books'], ['f:table', 'mesa|escritorio|table|desk'], ['f:chair', 'silla|sillas|chair'],
  ['f:candle', 'vela|velas|candle|candles'], ['f:fireplace', 'chimenea|hogar de piedra|fireplace'], ['f:barrel', 'barril|barriles|tonel|barrel'],
  ['f:throne', 'trono|throne'], ['f:banner', 'estandarte|estandartes|bandera|banner|flag'], ['f:torch', 'antorcha|antorchas|torch'],
  ['f:globe', 'globo terraqueo|globe'], ['f:rug', 'alfombra|tapiz|rug|carpet'], ['f:cauldron', 'caldero|cauldron'],
  // --- Escenarios, asentamientos, acciones, vistas, paletas y luz ---
  ['setting:interior', 'interior de|el interior|en el interior|dentro de|interior|inside|indoors'],
  ['room:library', 'biblioteca|library'], ['room:tavern', 'taberna|posada|tavern|inn'], ['room:dungeon', 'mazmorra|calabozo|dungeon|celda'],
  ['room:throne', 'sala del trono|salon del trono|throne room|gran salon'], ['room:lab', 'laboratorio|alquimista|laboratory|lab'], ['room:room', 'habitacion|cuarto|dormitorio|bedroom'],
  ['town:village', 'aldea|aldeas|pueblo|pueblos|villa|village|town|poblado'], ['town:city', 'ciudad medieval|ciudades medievales|medieval city'],
  ['battle', 'batalla|batallas|lucha|luchando|luchan|combate|combatiendo|guerra|pelea|peleando|enfrentamiento|duelo|battle|fight|fighting|war'],
  ['crowd', 'criaturas|monstruos|bestias|creatures|monsters|beasts'],
  ['view:top', 'vista superior|vista aerea|desde arriba|cenital|top down|mapa|map|birds eye'], ['view:panorama', 'panoramica|panorama|vista amplia|gran angular|wide shot'],
  ['view:front', 'retrato|portrait|primer plano de'],
  ['light:right', 'luz desde la derecha|iluminado desde la derecha|light from the right'], ['light:left', 'luz desde la izquierda|iluminado desde la izquierda|light from the left'],
  ['theme:postapoc', 'postapocaliptico|postapocaliptica|apocaliptico|apocaliptica|ruinas|en ruinas|abandonado|abandonada|desolado|desolada|post apocalyptic|ruins'],
  ['theme:tropical', 'tropical|tropicales|caribe|paradisiaco|paradisiaca'], ['theme:dark', 'oscuro|oscura|sombrio|sombria|siniestro|siniestra|gotico|gotica|gothic|grim'],
  ['planets', 'rodeada de planetas|rodeado de planetas|entre planetas|surrounded by planets'],
  ['feat:waterfall', 'cascada|cascadas|catarata|waterfall'],
  // --- Personajes ---
  ['s:dragon', 'dragon|dragones|drake|wyvern'],
  ['s:knight', 'caballero|knight|guerrero|guerrera|warrior|paladin|soldado'],
  ['s:wizard', 'mago|maga|wizard|hechicero|hechicera|sorcerer|nigromante'],
  ['s:astronaut', 'astronauta|astronaut|cosmonauta'],
  ['s:robot', 'robot|androide|android|mecha|automata'],
  ['s:samurai', 'samurai'], ['s:ninja', 'ninja|shinobi'], ['s:pirate', 'pirata|pirate|corsario'],
  ['s:king', 'rey|king|emperador'], ['s:princess', 'princesa|princess|reina|queen'],
  ['s:skeleton', 'esqueleto|skeleton'], ['s:alien', 'alien|alienigena|extraterrestre|marciano'],
  ['s:zombie', 'zombi|zombie'], ['s:elf', 'elfo|elfa|elf|arquero|arquera'],
  ['s:character', 'personaje|character|heroe|heroina|hero|persona|hombre|mujer|nino|nina|chico|chica|aventurero|aventurera|jugador|person|man|woman|boy|girl|viajero|explorador'],
  // --- Animales y criaturas ---
  ['s:cat', 'gato|gata|gatito|cat|kitten'], ['s:dog', 'perro|perra|perrito|cachorro|dog|puppy'],
  ['s:wolf', 'lobo|wolf'], ['s:fox', 'zorro|fox'], ['s:horse', 'caballo|yegua|horse|corcel'],
  ['s:unicorn', 'unicornio|unicorn'], ['s:cow', 'vaca|cow|toro|bull'], ['s:pig', 'cerdo|cerdito|pig|puerco'],
  ['s:sheep', 'oveja|sheep|cordero'], ['s:bear', 'oso|osa|bear'], ['s:deer', 'ciervo|venado|reno|deer'],
  ['s:rabbit', 'conejo|conejito|liebre|rabbit|bunny'], ['s:lion', 'leon|lion'],
  ['s:bird', 'pajaro|ave|aves|bird|gaviota|cuervo|paloma|crow'],
  ['s:bat', 'murcielago|bat'], ['s:butterfly', 'mariposa|butterfly'],
  ['s:fish', 'pez|peces|fish'], ['s:shark', 'tiburon|shark'], ['s:whale', 'ballena|whale'],
  ['s:slime', 'slime|limo|baba|gelatina'], ['s:ghost', 'fantasma|ghost|espectro|espiritu'],
  // --- Vehículos ---
  ['s:car', 'coche|carro|auto|automovil|car|vehiculo'], ['s:boat', 'barco|bote|velero|barca|navio|boat|ship|sailboat'],
  ['s:rocket', 'cohete|rocket|transbordador'], ['s:ufo', 'ovni|ufo|platillo volante|platillo'],
  ['s:plane', 'avion|avioneta|plane|airplane'], ['s:balloon', 'globo aerostatico|globo|hot air balloon|balloon'],
  // --- Objetos ---
  ['s:sword', 'espada|sword|sable|katana'], ['s:shield', 'escudo|shield'], ['s:potion', 'pocion|potion|elixir|frasco'],
  ['s:heart', 'corazon|heart'], ['s:gem', 'gema|gem|joya|diamante|rubi|zafiro|diamond'], ['s:key', 'llave|key'],
  ['s:crown', 'corona|crown'], ['s:coin', 'moneda|coin'], ['s:apple', 'manzana|apple'], ['s:mug', 'taza|mug|cup'],
  ['s:chest', 'cofre|chest|tesoro|treasure'], ['s:skull', 'calavera|skull|craneo'],
  // --- Estructuras ---
  ['b:castle', 'castillo|castle|fortaleza|fortress|alcazar'], ['b:house', 'casa|casita|cabana|hogar|house|cabin|cottage|granja'],
  ['b:tower', 'torre|tower|torreon'], ['b:lighthouse', 'faro|lighthouse'], ['b:temple', 'templo|pagoda|santuario|temple|shrine'],
  ['b:torii', 'torii'], ['b:pyramid', 'piramide|pyramid'], ['b:windmill', 'molino|windmill'],
  ['b:tent', 'tienda de campana|tienda|carpa|campamento|tent|camp'], ['b:skyscraper', 'rascacielos|edificio|skyscraper|building'],
  // --- Flora y atrezo ---
  ['f:cherry', 'arbol de cerezo|cerezo|sakura|cherry tree|cherry blossom|cherry'], ['f:deadtree', 'arbol seco|arbol muerto|dead tree'],
  ['f:tree', 'arbol|arboles|tree|roble|oak'], ['f:pine', 'pino|abeto|pine|fir'], ['f:palm', 'palmera|palma|palm'],
  ['f:cactus', 'cactus|cacto'], ['f:bush', 'arbusto|matorral|bush'], ['f:flower', 'flor|flores|flower|rosa silvestre|tulipan'],
  ['f:rock', 'roca|piedra|rock|stone'], ['f:mushroom', 'seta|hongo|champinon|mushroom'], ['f:campfire', 'hoguera|fogata|campfire|bonfire'],
  ['f:crystal', 'cristal|cristales|crystal'], ['f:lamp', 'farola|farol|street lamp|lampara'], ['f:crater', 'crater'],
  // --- Biomas y relieve ---
  ['biome:meadow', 'pradera|prado|campo|llanura|valle|meadow|field|plain|valley|campina'],
  ['jungle', 'selva|jungla|jungle|rainforest'], ['biome:forest', 'bosque|forest|woods|arboleda'],
  ['biome:desert', 'desierto|desert|dunas'], ['biome:mars', 'marte|mars|planeta rojo'],
  ['biome:moonland', 'en la luna|sobre la luna|superficie lunar|on the moon'],
  ['biome:sea', 'mar|oceano|ocean|alta mar|the sea'], ['biome:beach', 'playa|costa|isla|beach|island|orilla'],
  ['biome:city', 'ciudad|urbano|urbana|calle|metropolis|city|street'],
  ['biome:cyber', 'cyberpunk|ciberpunk|neon'], ['biome:snow', 'nevado|nevada|invernal|invierno|winter|artico|tundra|polar'],
  ['biome:volcano', 'volcan|volcano|volcanico|volcanica'], ['biome:space', 'espacio exterior|espacio|galaxia|cosmos|universo|space|galaxy|orbita'],
  ['feat:fuji', 'monte fuji|fuji'], ['feat:mountains', 'montana|montanas|sierra|cordillera|mountain|mountains|picos|cumbre'],
  ['feat:hills', 'colina|colinas|loma|hill|hills'], ['feat:lake', 'lago|laguna|estanque|rio|lake|pond|river'],
  // --- Hora, clima, astros ---
  ['time:night', 'noche|nocturno|nocturna|medianoche|night|midnight|de noche'],
  ['time:day', 'dia|mediodia|diurno|soleado|soleada|day|noon|sunny'],
  ['time:sunset', 'atardecer|ocaso|crepusculo|puesta de sol|sunset|dusk|anochecer'],
  ['time:dawn', 'amanecer|alba|madrugada|dawn|sunrise'],
  ['w:rain', 'lluvia|lloviendo|lluvioso|lluviosa|rain|rainy|raining|llovizna'], ['w:storm', 'tormenta|tempestad|relampago|relampagos|rayo|rayos|storm|lightning|thunder'],
  ['w:snow', 'nieve|nevando|snow|snowing|copos'], ['w:fog', 'niebla|bruma|neblina|fog|mist'],
  ['c:crescent', 'media luna|luna creciente|luna menguante|crescent moon|crescent'], ['c:moon', 'luna llena|luna|moon|full moon'], ['c:sun', 'sol|sun'],
  ['c:comet', 'estrella fugaz|cometa|meteoro|comet|shooting star|meteor'],
  ['c:stars', 'estrella|estrellas|estrellado|estrellada|stars|star|starry'], ['c:clouds', 'nube|nubes|nublado|cloud|clouds|cloudy'],
  ['c:aurora', 'aurora boreal|aurora|northern lights'], ['c:rainbow', 'arcoiris|arco iris|rainbow'], ['c:planet', 'planeta|planetas|planet|planets'],
  ['fx:fireflies', 'luciernaga|luciernagas|firefly|fireflies'],
  // --- Colores ---
  ['col:red', 'rojo|roja|rojos|rojas|red|carmesi|escarlata'], ['col:blue', 'azul|azules|blue|celeste'], ['col:green', 'verde|verdes|green|esmeralda'],
  ['col:gold', 'dorado|dorada|dorados|doradas|oro|gold|golden'], ['col:silver', 'plateado|plateada|plata|silver'],
  ['col:black', 'negro|negra|negros|negras|black'], ['col:white', 'blanco|blanca|blancos|blancas|white'],
  ['col:purple', 'purpura|morado|morada|violeta|lila|purple|violet'], ['col:pink', 'rosa|rosado|rosada|pink'],
  ['col:orange', 'naranja|anaranjado|anaranjada|orange'], ['col:yellow', 'amarillo|amarilla|yellow'],
  ['col:brown', 'marron|castano|pardo|brown'], ['col:gray', 'gris|grises|gray|grey'], ['col:cyan', 'cian|turquesa|aguamarina|cyan|teal'],
  // --- Modificadores ---
  ['style:bit8', '8 bit|8 bits|8bit|retro|nes'], ['style:bit16', '16 bit|16 bits|16bit|snes'], ['style:rpg', 'rpg|jrpg'],
  ['style:iso', 'isometrico|isometrica|isometric'], ['style:minimal', 'minimalista|minimal|minimalist|simple'],
  ['style:detailed', 'detallado|detallada|detailed'], ['style:fantasy', 'fantasia|fantasy'], ['style:sprite', 'sprite|icono|icon'],
  ['size:small', 'pequeno|pequena|pequenos|pequenas|chico|chica|diminuto|diminuta|small|tiny|little'],
  ['size:big', 'grande|grandes|gran|enorme|gigante|inmenso|inmensa|big|huge|giant|large'],
  ['anim:walk', 'caminando|andando|corriendo|paseando|walking|running'], ['anim:attack', 'atacando|luchando|peleando|combatiendo|attacking|fighting'],
  // --- Posición y relaciones espaciales ---
  ['facing:left', 'mirando hacia la izquierda|mirando a la izquierda|mira hacia la izquierda|mira a la izquierda|looking left|facing left'], ['facing:right', 'mirando hacia la derecha|mirando a la derecha|mira hacia la derecha|mira a la derecha|looking right|facing right'],
  ['side:left', 'a la izquierda|al lado izquierdo|lado izquierdo|izquierda|izquierdo|on the left|to the left|left'], ['side:right', 'a la derecha|al lado derecho|lado derecho|derecha|derecho|on the right|to the right|right'],
  ['bright:up', 'mas luz|mas iluminacion|incrementa la iluminacion|aumenta la iluminacion|aumenta la luz|mas brillante|mas luminoso|mas luminosa|ilumina|brighter|more light'], ['bright:down', 'menos luz|menos iluminacion|mas oscuro|mas oscura|oscurece|darker|less light'],
  ['side:center', 'en el centro|centro|en medio|in the middle|in the center|center|middle'],
  ['depth:back', 'al fondo|a lo lejos|en la distancia|en segundo plano|in the background|in the distance|far away'],
  ['depth:front', 'en primer plano|primer plano|in the foreground|foreground'],
  ['rel:on', 'encima de|encima|sobre|arriba de|on top of|atop|above|over|on'], ['rel:below', 'debajo de|debajo|bajo|under|below|beneath'],
  ['rel:behind', 'detras de|detras|tras|behind'], ['rel:front', 'delante de|delante|frente a|frente|in front of'],
  ['rel:beside', 'junto a|junto|al lado de|al lado|cerca de|next to|beside|near'],
  ['fly', 'volando|vuela|volador|voladora|sobrevolando|flying|flies|en el aire|flotando'],
  ['n:2', 'dos|two|par de'], ['n:3', 'tres|three'], ['n:4', 'cuatro|four|varios|varias|several'], ['n:5', 'cinco|five|muchos|muchas|many'],
  ['part:armor', 'armadura|coraza|armor|armour'], ['part:helmet', 'casco|yelmo|helmet'], ['part:hat', 'sombrero|gorro|hat'],
  ['part:cape', 'capa|manto|cape|cloak'], ['part:sky', 'cielo|sky|firmamento'], ['part:staff', 'baston|vara|cetro|staff|wand|varita'],
  ['mat:fire', 'fuego|llamas|llama|ardiente|flameante|llameante|fire|flames|flaming|burning'], ['mat:ice', 'hielo|helado|helada|congelado|ice|frozen'],
  ['theme:japan', 'japones|japonesa|japon|oriental|japanese|japan'], ['theme:magic', 'fantastico|fantastica|fantasticos|fantasticas|magico|magica|encantado|encantada|hechizado|hechizada|magic|magical|enchanted|mistico|mistica'],
  ['theme:future', 'futurista|futuristic|ciencia ficcion|sci fi|scifi'], ['theme:spooky', 'embrujado|embrujada|tenebroso|tenebrosa|terror|haunted|spooky'],
  ['theme:medieval', 'medieval'],
  // --- Verbos de edición ---
  ['verb:add', 'agrega|agregar|anade|anadir|incluye|incluir|pon|poner|coloca|colocar|add|put|suma'],
  ['verb:remove', 'quita|quitar|elimina|eliminar|borra|borrar|remove|delete|sin'],
  ['verb:change', 'cambia|cambiar|haz|vuelve|convierte|pinta|change|make|turn|modifica|mueve|mover|move|desplaza|situa|ubica'],
  ['verb:keep', 'conserva|conservar|manten|mantener|preserva|deja|keep|preserve'],
];

const STOP = 'un una unos unas el la los las lo de del al a en con y e o u que por para su sus se es esta este son muy mas como sobre bajo frente junto cerca entre durante hacia desde tras encima debajo lado the an of on in at with and to over under near front by is its from into behind above below pixel art pixelart estilo style paisaje landscape escena scene imagen image dibujo ilustracion fondo background atravesando cruzando acechando meditando explorando volando_ rodeada rodeado sosteniendo holding montado montada riding posado posada sentado sentada mirando parado standing llevando wearing usando tiene hay color colores tipo algo mientras donde cual cuyo todo toda color'.split(' ');

export const PHRASES = new Map();
for (const [tag, words] of V) for (const w of words.split('|')) if (!PHRASES.has(w)) PHRASES.set(w, tag);
export const STOPWORDS = new Set(STOP);
export const MAX_PHRASE = 4;

export function normalize(text) {
  return String(text).toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z0-9]+/g, ' ').trim();
}

// Lista de ejemplos de vocabulario para mostrar en la ayuda de la interfaz.
export function vocabularySummary() {
  const out = {};
  for (const [tag, words] of V) {
    const kind = tag.split(':')[0];
    (out[kind] ||= []).push(words.split('|')[0]);
  }
  return out;
}
