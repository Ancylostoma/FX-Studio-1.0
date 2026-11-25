// Léxico propio del español. Cada entrada: { from, to, ...restricciones }.
//   from: forma(s) que se buscan (se respetan mayúsculas al sustituir)
//   to:   alternativa(s) equivalentes; se elige una con la semilla
//   start: solo al comienzo de oración
//   before / notBefore: regex que debe (o no) cumplir el texto que sigue
//   after / notAfter:   regex que debe (o no) cumplir el texto que precede
// Criterios: mismo significado, mismo género y número, misma categoría
// gramatical. Lo dudoso se deja fuera: preferimos cambiar menos a cambiar mal.

const forms = (pairs, extra = {}) => pairs.map(([from, to]) => ({ from, to, ...extra }));

// ---------- Nivel 1: redundancias (pleonasmos y rodeos vacíos) ----------
export const REDUNDANCY = [
  ...forms([
    ['subir arriba', 'subir'], ['sube arriba', 'sube'], ['suben arriba', 'suben'], ['subió arriba', 'subió'],
    ['bajar abajo', 'bajar'], ['baja abajo', 'baja'], ['bajan abajo', 'bajan'], ['bajó abajo', 'bajó'],
    ['salir afuera', 'salir'], ['sale afuera', 'sale'], ['salió afuera', 'salió'], ['salen afuera', 'salen'],
    ['entrar adentro', 'entrar'], ['entra adentro', 'entra'], ['entró adentro', 'entró'], ['entran adentro', 'entran'],
    ['volver a repetir', 'repetir'], ['vuelve a repetir', 'repite'], ['vuelven a repetir', 'repiten'],
    ['volvió a repetir', 'repitió'], ['volvieron a repetir', 'repitieron'], ['volver a repetirse', 'repetirse'],
    ['regresar de nuevo', 'regresar'], ['regresó de nuevo', 'regresó'], ['prever con antelación', 'prever'],
    ['previsto con antelación', 'previsto'], ['planificar de antemano', 'planificar'], ['planificado de antemano', 'planificado'],
    ['colaborar juntos', 'colaborar'], ['colaboran juntos', 'colaboran'], ['colaboraron juntos', 'colaboraron'],
    ['lapso de tiempo', 'lapso'], ['período de tiempo', 'período'], ['periodo de tiempo', 'periodo'],
    ['períodos de tiempo', 'períodos'], ['periodos de tiempo', 'periodos'],
    ['resultado final', 'resultado'], ['resultados finales', 'resultados'], ['nexo de unión', 'nexo'],
    ['persona humana', 'persona'], ['personas humanas', 'personas'], ['base fundamental', 'base'],
    ['bases fundamentales', 'bases'], ['erradicar totalmente', 'erradicar'], ['erradicar por completo', 'erradicar'],
    ['completamente lleno', 'lleno'], ['completamente llena', 'llena'], ['totalmente gratuito', 'gratuito'],
    ['totalmente gratuita', 'gratuita'], ['más óptimo', 'óptimo'], ['más óptima', 'óptima'], ['muy óptimo', 'óptimo'],
    ['muy óptima', 'óptima'], ['más mejor', 'mejor'], ['más peor', 'peor'], ['absolutamente imprescindible', 'imprescindible'],
    ['absolutamente imprescindibles', 'imprescindibles'], ['totalmente imprescindible', 'imprescindible'],
    ['propia autobiografía', 'autobiografía'], ['experiencia previa', 'experiencia'], ['falsa mentira', 'mentira'],
    ['réplica exacta', 'réplica'], ['totalmente nuevo', 'nuevo'], ['totalmente nueva', 'nueva'],
    ['es por eso que', 'por eso'], ['es por ello que', 'por ello'], ['fue por eso que', 'por eso'],
    ['de cara a', 'para'], ['a día de hoy', 'actualmente'], ['en el día de hoy', 'hoy'],
  ]),
  // «en el año 2020» → «en 2020» (la cifra está protegida: aquí llega como marcador)
  { from: 'en el año', to: 'en', before: /^\s*\uE000/ },
  { from: 'durante el año', to: 'durante', before: /^\s*\uE000/ },
  // «hace cinco años atrás» → «hace cinco años»: se elimina «atrás» tras una duración
  { from: 'atrás', to: '', after: /(?<![\p{L}])hace\s+(?:\uE000[\uE100-\uF8FF]\uE001|\p{L}+)(?:\s+(?:años|meses|semanas|días|horas))?\s+$/iu, raw: true },
];

// Palabras que pueden repetirse legítimamente («que que» nunca; «nos nos», casi nunca).
export const DUPLICATE_OK = new Set(['ja', 'je', 'bla', 'tan', 'muy']);

// ---------- Nivel 2: expresiones y conectores ----------
export const EXPRESSIONS = [
  ...forms([
    ['con el objetivo de', ['para', 'con el fin de']], ['con el fin de', ['para', 'con el objetivo de']],
    ['con el propósito de', ['para', 'con el fin de']], ['a fin de', 'para'], ['en aras de', 'para'],
    ['debido a que', ['porque', 'ya que']], ['dado que', ['ya que', 'puesto que']], ['ya que', ['puesto que', 'dado que']],
    ['puesto que', ['ya que', 'dado que']], ['a pesar de que', 'aunque'], ['pese a que', 'aunque'],
    ['en la actualidad', 'actualmente'], ['hoy en día', 'actualmente'], ['en el momento actual', 'actualmente'],
    ['la mayoría de', 'la mayor parte de'], ['la mayor parte de', 'la mayoría de'],
    ['con respecto a', ['en relación con', 'respecto a']], ['respecto a', ['en cuanto a', 'con respecto a']],
    ['en relación con', ['con respecto a', 'en cuanto a']], ['en relación a', 'en relación con'], ['en cuanto a', 'respecto a'],
    ['por otra parte', 'por otro lado'], ['por otro lado', 'por otra parte'],
    ['con frecuencia', 'a menudo'], ['a menudo', 'con frecuencia'], ['en primer lugar', 'primero'],
    ['es importante destacar que', ['cabe destacar que', 'conviene señalar que']], ['es importante señalar que', ['cabe señalar que', 'conviene destacar que']],
    ['cabe destacar que', ['conviene señalar que', 'es preciso destacar que']], ['cabe señalar que', ['conviene destacar que', 'es preciso señalar que']],
    ['teniendo en cuenta que', 'considerando que'], ['tener en cuenta', 'considerar'], ['tiene en cuenta', 'considera'],
    ['tienen en cuenta', 'consideran'], ['tuvo en cuenta', 'consideró'], ['tuvieron en cuenta', 'consideraron'],
    ['tenerse en cuenta', 'considerarse'], ['teniendo en cuenta', 'considerando'],
    ['tomar en consideración', 'considerar'], ['toma en consideración', 'considera'], ['tomó en consideración', 'consideró'],
    ['llevar a cabo', 'realizar'], ['lleva a cabo', 'realiza'], ['llevan a cabo', 'realizan'], ['llevó a cabo', 'realizó'],
    ['llevaron a cabo', 'realizaron'], ['llevado a cabo', 'realizado'], ['llevada a cabo', 'realizada'],
    ['llevados a cabo', 'realizados'], ['llevadas a cabo', 'realizadas'], ['llevando a cabo', 'realizando'],
    ['llevarse a cabo', 'realizarse'], ['se lleva a cabo', 'se realiza'], ['se llevó a cabo', 'se realizó'],
    ['hacer referencia a', 'referirse a'], ['hace referencia a', 'se refiere a'], ['hacen referencia a', 'se refieren a'],
    ['hizo referencia a', 'se refirió a'], ['hicieron referencia a', 'se refirieron a'], ['haciendo referencia a', 'refiriéndose a'],
    ['dar lugar a', 'originar'], ['da lugar a', 'origina'], ['dan lugar a', 'originan'], ['dio lugar a', 'originó'],
    ['dieron lugar a', 'originaron'], ['dando lugar a', 'originando'],
    ['poner de manifiesto', 'evidenciar'], ['pone de manifiesto', 'evidencia'], ['ponen de manifiesto', 'evidencian'],
    ['puso de manifiesto', 'evidenció'], ['pusieron de manifiesto', 'evidenciaron'],
    ['por medio de', 'mediante'], ['mediante', 'por medio de'],
    ['al mismo tiempo', 'simultáneamente'], ['de nuevo', 'nuevamente'], ['nuevamente', 'de nuevo'],
  ]),
  // «de forma/manera + adjetivo» adverbial; no «cambio de forma», «tipo de forma»...
  { from: 'de forma', to: 'de manera', before: /^\s+(?:\p{L}+(?:a|e|al|ar)|que)(?![\p{L}])/u, notAfter: /(?:cambio|cambios|tipo|tipos|tamaño|alteración|alteraciones|forma|formas|color|aspecto)\s+$/iu },
  { from: 'de manera', to: 'de forma', before: /^\s+(?:\p{L}+(?:a|e|al|ar)|que)(?![\p{L}])/u },
  // Conectores al inicio de oración
  ...forms([
    ['Sin embargo,', ['No obstante,', 'Ahora bien,']], ['No obstante,', ['Sin embargo,', 'Ahora bien,']],
    ['Por lo tanto,', ['Por consiguiente,', 'Por ello,']], ['Por tanto,', ['Por consiguiente,', 'Por ello,']],
    ['Por consiguiente,', ['Por lo tanto,', 'Por ello,']], ['Por ello,', ['Por esta razón,', 'Por lo tanto,']],
    ['Además,', ['Asimismo,', 'Por otra parte,']], ['Asimismo,', ['Además,', 'Del mismo modo,']],
    ['Del mismo modo,', ['Asimismo,', 'De igual manera,']], ['De igual manera,', ['Asimismo,', 'Del mismo modo,']],
    ['En conclusión,', ['En síntesis,', 'En suma,']], ['En resumen,', ['En síntesis,', 'En suma,']],
    ['Es decir,', 'Esto es,'], ['En cambio,', 'Por el contrario,'],
    ['Por el contrario,', 'En cambio,'], ['Finalmente,', 'Por último,'], ['Por último,', 'Finalmente,'],
  ], { start: true }),
];

// ---------- Nivel 3: sinónimos de una palabra (con flexión explícita) ----------
export const SYNONYMS = [
  ...forms([
    ['utilizar', 'emplear'], ['utiliza', 'emplea'], ['utilizan', 'emplean'], ['utilizó', 'empleó'], ['utilizaron', 'emplearon'],
    ['utilizado', 'empleado'], ['utilizada', 'empleada'], ['utilizados', 'empleados'], ['utilizadas', 'empleadas'],
    ['utilizando', 'empleando'], ['utilizarse', 'emplearse'], ['utilizaba', 'empleaba'], ['utilizaban', 'empleaban'],
    // «aumentar/disminuir» se usan a menudo como intransitivos («la presión disminuyó»), y
    // «incrementar/reducir» exigen entonces «se»: solo se sustituyen sustantivos y participios.
    ['aumentado', 'incrementado'], ['aumentada', 'incrementada'], ['aumentados', 'incrementados'], ['aumentadas', 'incrementadas'],
    ['aumento', 'incremento'], ['aumentos', 'incrementos'],
    ['disminuido', 'reducido'], ['disminuida', 'reducida'], ['disminuidos', 'reducidos'], ['disminuidas', 'reducidas'],
    ['disminución', 'reducción'], ['disminuciones', 'reducciones'],
    ['permite', 'posibilita'], ['permiten', 'posibilitan'], ['permitió', 'posibilitó'], ['permitieron', 'posibilitaron'],
    ['permitiría', 'posibilitaría'], ['permitirá', 'posibilitará'],
    ['analizar', 'examinar'], ['analiza', 'examina'], ['analizan', 'examinan'], ['analizó', 'examinó'], ['analizaron', 'examinaron'],
    ['analizado', 'examinado'], ['analizada', 'examinada'], ['analizados', 'examinados'], ['analizadas', 'examinadas'],
    ['analizando', 'examinando'],
    ['garantizar', 'asegurar'], ['garantiza', 'asegura'], ['garantizan', 'aseguran'], ['garantizó', 'aseguró'],
    ['garantizaron', 'aseguraron'], ['garantizando', 'asegurando'],
    ['importante', 'relevante'], ['importantes', 'relevantes'], ['relevante', 'importante'], ['relevantes', 'importantes'],
    ['fundamental', 'esencial'], ['fundamentales', 'esenciales'], ['esencial', 'fundamental'], ['esenciales', 'fundamentales'],
    ['imprescindible', 'indispensable'], ['imprescindibles', 'indispensables'], ['indispensable', 'imprescindible'],
    ['indispensables', 'imprescindibles'], ['adecuado', 'apropiado'], ['adecuada', 'apropiada'], ['adecuados', 'apropiados'],
    ['adecuadas', 'apropiadas'], ['apropiado', 'adecuado'], ['apropiada', 'adecuada'], ['apropiados', 'adecuados'],
    ['apropiadas', 'adecuadas'], ['principalmente', ['sobre todo', 'fundamentalmente']], ['frecuentemente', 'con frecuencia'],
    ['rápidamente', 'con rapidez'], ['aproximadamente', 'alrededor de'], ['posteriormente', 'después'],
    ['inicialmente', 'al principio'], ['habitualmente', 'por lo general'], ['generalmente', 'por lo general'],
    ['únicamente', 'solo'], ['exclusivamente', 'solamente'], ['numerosos', 'múltiples'], ['numerosas', 'múltiples'],
    ['diversos', 'distintos'], ['diversas', 'distintas'], ['concretamente', 'en concreto'],
    ['notable', 'considerable'], ['notables', 'considerables'],
  ]),
  // Sustantivo «objetivo» (no el adjetivo: «un dato objetivo»)
  { from: 'objetivo', to: ['propósito', 'fin'], after: /(?:^|\s)(?:el|un|este|nuestro|su|cuyo|primer|segundo|principal)\s+$/iu },
  { from: 'objetivos', to: 'propósitos', after: /(?:^|\s)(?:los|unos|estos|nuestros|sus|cuyos)\s+$/iu },
];

// ---------- Modo enfermería: lenguaje centrado en la persona (APA 7, sección 5) ----------
export const PERSON_FIRST = [
  ...forms([
    ['pacientes diabéticos', 'pacientes con diabetes'], ['paciente diabético', 'paciente con diabetes'],
    ['paciente diabética', 'paciente con diabetes'], ['pacientes hipertensos', 'pacientes con hipertensión'],
    ['paciente hipertenso', 'paciente con hipertensión'], ['paciente hipertensa', 'paciente con hipertensión'],
    ['pacientes obesos', 'pacientes con obesidad'], ['paciente obeso', 'paciente con obesidad'],
    ['pacientes asmáticos', 'pacientes con asma'], ['paciente asmático', 'paciente con asma'],
    ['pacientes epilépticos', 'pacientes con epilepsia'], ['paciente epiléptico', 'paciente con epilepsia'],
    ['pacientes oncológicos', 'pacientes con cáncer'], ['pacientes dementes', 'pacientes con demencia'],
    ['los diabéticos', 'las personas con diabetes'], ['los hipertensos', 'las personas con hipertensión'],
    ['los ancianos', 'las personas mayores'], ['ancianos', 'personas mayores'], ['el anciano', 'la persona mayor'],
    ['la anciana', 'la persona mayor'], ['los discapacitados', 'las personas con discapacidad'],
    ['discapacitados', 'personas con discapacidad'], ['minusválidos', 'personas con discapacidad'],
  ]),
];

// Nombres de persona: el objeto directo de persona lleva «a» («evaluó al paciente»)
// y el sujeto de persona impide la pasiva refleja («el paciente se trasladó» es ambiguo).
export const PERSON_NOUNS = new Set([
  'paciente', 'pacientes', 'enfermero', 'enfermera', 'enfermeros', 'enfermeras', 'médico', 'médica', 'médicos', 'médicas',
  'participante', 'participantes', 'persona', 'personas', 'niño', 'niña', 'niños', 'niñas', 'mujer', 'mujeres', 'hombre',
  'hombres', 'usuario', 'usuaria', 'usuarios', 'usuarias', 'cuidador', 'cuidadora', 'cuidadores', 'cuidadoras', 'familiar',
  'familiares', 'residente', 'residentes', 'estudiante', 'estudiantes', 'profesional', 'profesionales', 'sujeto', 'sujetos',
  'adulto', 'adultos', 'adulta', 'adultas', 'anciano', 'ancianos', 'lactante', 'lactantes', 'neonato', 'neonatos', 'madre',
  'madres', 'padre', 'padres', 'alumno', 'alumna', 'alumnos', 'alumnas', 'voluntario', 'voluntarios', 'individuo',
  'individuos', 'gestante', 'gestantes', 'embarazada', 'embarazadas', 'adolescente', 'adolescentes', 'auxiliar', 'auxiliares',
  'matrona', 'matronas', 'investigador', 'investigadora', 'investigadores', 'investigadoras', 'autor', 'autores', 'autora', 'autoras',
]);

// Agentes admitidos para pasar de pasiva con «por» a activa.
export const AGENT_NOUNS = new Set([
  ...PERSON_NOUNS, 'equipo', 'equipos', 'comité', 'comités', 'personal', 'servicio', 'servicios', 'hospital', 'hospitales',
  'institución', 'instituciones', 'ministerio', 'organización', 'organizaciones', 'unidad', 'dirección', 'grupo', 'consejo',
  'universidad', 'sociedad', 'centro', 'centros', 'supervisora', 'supervisor', 'farmacéutico', 'farmacéutica', 'farmacéuticos',
]);

export const DETERMINERS = new Set([
  'el', 'la', 'los', 'las', 'un', 'una', 'unos', 'unas', 'este', 'esta', 'estos', 'estas', 'ese', 'esa', 'esos', 'esas',
  'dicho', 'dicha', 'dichos', 'dichas', 'su', 'sus', 'nuestro', 'nuestra', 'nuestros', 'nuestras', 'cada', 'todo', 'toda',
  'todos', 'todas', 'ambos', 'ambas', 'al', 'del',
]);
export const PLURAL_DET = new Set(['los', 'las', 'unos', 'unas', 'estos', 'estas', 'esos', 'esas', 'dichos', 'dichas', 'sus', 'nuestros', 'nuestras', 'todos', 'todas', 'ambos', 'ambas']);

// Palabras que pueden pasar a minúscula sin riesgo al mover una oración.
export const LOWERABLE = new Set([
  ...DETERMINERS, 'se', 'no', 'en', 'de', 'con', 'para', 'por', 'durante', 'tras', 'según', 'ello', 'esto', 'eso', 'nosotros',
  'ellos', 'ellas', 'estas', 'esta', 'se', 'también', 'solo', 'pacientes', 'enfermeras', 'enfermeros', 'participantes',
  'profesionales', 'datos', 'resultados', 'cuidados', 'personas', 'muchos', 'muchas', 'algunos', 'algunas', 'varios', 'varias',
  'mayor', 'menor', 'existe', 'existen', 'hay', 'fue', 'fueron', 'es', 'son', 'estaba', 'estaban', 'puede', 'pueden',
]);

// Participio → pretérito (3.ª persona singular, plural) para los irregulares y
// los verbos de cambio vocálico más frecuentes en textos clínicos.
export const PARTICIPLE_IRREGULAR = {
  hecho: ['hizo', 'hicieron'], escrito: ['escribió', 'escribieron'], descrito: ['describió', 'describieron'],
  puesto: ['puso', 'pusieron'], propuesto: ['propuso', 'propusieron'], expuesto: ['expuso', 'expusieron'],
  dispuesto: ['dispuso', 'dispusieron'], impuesto: ['impuso', 'impusieron'], compuesto: ['compuso', 'compusieron'],
  visto: ['vio', 'vieron'], previsto: ['previó', 'previeron'], revisto: ['revio', 'revieron'], dicho: ['dijo', 'dijeron'],
  cubierto: ['cubrió', 'cubrieron'], descubierto: ['descubrió', 'descubrieron'], abierto: ['abrió', 'abrieron'],
  resuelto: ['resolvió', 'resolvieron'], devuelto: ['devolvió', 'devolvieron'], vuelto: ['volvió', 'volvieron'],
  impreso: ['imprimió', 'imprimieron'], roto: ['rompió', 'rompieron'], muerto: ['murió', 'murieron'],
  satisfecho: ['satisfizo', 'satisficieron'], dado: ['dio', 'dieron'], obtenido: ['obtuvo', 'obtuvieron'],
  mantenido: ['mantuvo', 'mantuvieron'], contenido: ['contuvo', 'contuvieron'], retenido: ['retuvo', 'retuvieron'],
  sostenido: ['sostuvo', 'sostuvieron'], detenido: ['detuvo', 'detuvieron'], traído: ['trajo', 'trajeron'],
  atraído: ['atrajo', 'atrajeron'], leído: ['leyó', 'leyeron'], oído: ['oyó', 'oyeron'], creído: ['creyó', 'creyeron'],
  medido: ['midió', 'midieron'], pedido: ['pidió', 'pidieron'], seguido: ['siguió', 'siguieron'], conseguido: ['consiguió', 'consiguieron'],
  perseguido: ['persiguió', 'persiguieron'], repetido: ['repitió', 'repitieron'], elegido: ['eligió', 'eligieron'],
  corregido: ['corrigió', 'corrigieron'], servido: ['sirvió', 'sirvieron'], sentido: ['sintió', 'sintieron'],
  preferido: ['prefirió', 'prefirieron'], referido: ['refirió', 'refirieron'], transferido: ['transfirió', 'transfirieron'],
  inferido: ['infirió', 'infirieron'], sugerido: ['sugirió', 'sugirieron'], requerido: ['requirió', 'requirieron'],
  convertido: ['convirtió', 'convirtieron'], advertido: ['advirtió', 'advirtieron'], invertido: ['invirtió', 'invirtieron'],
  divertido: ['divirtió', 'divirtieron'], impedido: ['impidió', 'impidieron'], despedido: ['despidió', 'despidieron'],
  concebido: ['concibió', 'concibieron'], digerido: ['digirió', 'digirieron'], herido: ['hirió', 'hirieron'],
  vestido: ['vistió', 'vistieron'], rendido: ['rindió', 'rindieron'], competido: ['compitió', 'compitieron'],
  dormido: ['durmió', 'durmieron'], consentido: ['consintió', 'consintieron'], presentido: ['presintió', 'presintieron'],
  medicado: ['medicó', 'medicaron'], reducido: ['redujo', 'redujeron'], producido: ['produjo', 'produjeron'],
  introducido: ['introdujo', 'introdujeron'], conducido: ['condujo', 'condujeron'], traducido: ['tradujo', 'tradujeron'],
  inducido: ['indujo', 'indujeron'], deducido: ['dedujo', 'dedujeron'], incluido: ['incluyó', 'incluyeron'],
  excluido: ['excluyó', 'excluyeron'], distribuido: ['distribuyó', 'distribuyeron'], construido: ['construyó', 'construyeron'],
  constituido: ['constituyó', 'constituyeron'], sustituido: ['sustituyó', 'sustituyeron'], atribuido: ['atribuyó', 'atribuyeron'],
  contribuido: ['contribuyó', 'contribuyeron'], instruido: ['instruyó', 'instruyeron'], destruido: ['destruyó', 'destruyeron'],
  concluido: ['concluyó', 'concluyeron'], diluido: ['diluyó', 'diluyeron'], restituido: ['restituyó', 'restituyeron'],
  andado: ['anduvo', 'anduvieron'], estado: null, sido: null, ido: null, tenido: ['tuvo', 'tuvieron'],
  sometido: ['sometió', 'sometieron'], prescrito: ['prescribió', 'prescribieron'], inscrito: ['inscribió', 'inscribieron'],
  suscrito: ['suscribió', 'suscribieron'], transcrito: ['transcribió', 'transcribieron'], frito: ['frió', 'frieron'],
};
