// Terminología clínica y de investigación que el motor NUNCA reformula.
// Se protege en todos los modos; el modo enfermería añade NURSING_*.
// Las listas son propias y ampliables: basta con añadir términos (en minúsculas
// salvo que la grafía importe, p. ej. "SpO2").

export const CLINICAL_ES = [
  // Constantes y parámetros
  'signos vitales', 'constantes vitales', 'presión arterial', 'presión arterial sistólica', 'presión arterial diastólica',
  'presión arterial media', 'tensión arterial', 'frecuencia cardiaca', 'frecuencia cardíaca', 'frecuencia respiratoria',
  'saturación de oxígeno', 'temperatura corporal', 'glucemia capilar', 'glucemia', 'diuresis', 'balance hídrico',
  'índice de masa corporal', 'perímetro abdominal', 'nivel de conciencia', 'escala de Glasgow', 'escala de Braden',
  'escala de Norton', 'escala de Morse', 'escala de Downton', 'escala de Barthel', 'índice de Barthel', 'escala EVA',
  'escala visual analógica', 'escala numérica del dolor', 'test de Pfeiffer', 'escala de Lawton', 'escala de Zarit',
  // Diagnósticos y cuadros
  'hipertensión arterial', 'hipertensión', 'hipotensión', 'diabetes mellitus', 'diabetes mellitus tipo 1', 'diabetes mellitus tipo 2',
  'diabetes', 'insuficiencia cardiaca', 'insuficiencia cardíaca', 'insuficiencia renal', 'insuficiencia respiratoria',
  'enfermedad pulmonar obstructiva crónica', 'infarto agudo de miocardio', 'síndrome coronario agudo', 'accidente cerebrovascular',
  'ictus', 'sepsis', 'shock séptico', 'choque séptico', 'neumonía', 'neumonía asociada a ventilación mecánica',
  'infección del tracto urinario', 'infección relacionada con la asistencia sanitaria', 'infección nosocomial',
  'úlcera por presión', 'úlceras por presión', 'lesión por presión', 'lesiones por presión', 'lesión cutánea asociada a la humedad',
  'flebitis', 'extravasación', 'delirium', 'delirio', 'deterioro cognitivo', 'demencia', 'disfagia', 'desnutrición',
  'deshidratación', 'hipoglucemia', 'hiperglucemia', 'cetoacidosis diabética', 'tromboembolismo pulmonar', 'trombosis venosa profunda',
  'parada cardiorrespiratoria', 'reanimación cardiopulmonar', 'dolor agudo', 'dolor crónico', 'cáncer', 'obesidad', 'asma',
  'epilepsia', 'anemia', 'cuidados paliativos', 'cuidados intensivos',
  // Procedimientos, vías y dispositivos
  'vía oral', 'vía intravenosa', 'vía endovenosa', 'vía subcutánea', 'vía intramuscular', 'vía sublingual', 'vía rectal',
  'vía tópica', 'vía inhalatoria', 'vía periférica', 'vía central', 'catéter venoso central', 'catéter venoso periférico',
  'catéter central de inserción periférica', 'sonda vesical', 'sonda nasogástrica', 'ventilación mecánica',
  'ventilación mecánica invasiva', 'ventilación no invasiva', 'oxigenoterapia', 'nutrición enteral', 'nutrición parenteral',
  'fluidoterapia', 'hemocultivo', 'hemocultivos', 'gasometría arterial', 'electrocardiograma', 'higiene de manos',
  'lavado de manos', 'aislamiento de contacto', 'precauciones estándar', 'equipo de protección individual',
  'suero fisiológico', 'cloruro sódico', 'cloruro de sodio', 'Ringer lactato', 'solución salina',
  // Fármacos frecuentes (los demás se detectan por su terminación)
  'paracetamol', 'ibuprofeno', 'metamizol', 'insulina', 'insulina rápida', 'insulina basal', 'morfina', 'furosemida',
  'metformina', 'adrenalina', 'noradrenalina', 'dopamina', 'dobutamina', 'digoxina', 'warfarina', 'salbutamol',
  'ondansetrón', 'dexametasona', 'hidrocortisona', 'metilprednisolona', 'tramadol', 'fentanilo', 'midazolam', 'propofol',
  'ketamina', 'naloxona', 'haloperidol', 'potasio', 'heparina', 'heparina de bajo peso molecular', 'ácido acetilsalicílico',
  'clopidogrel', 'vancomicina', 'piperacilina-tazobactam', 'amoxicilina-clavulánico',
  // Investigación y estadística
  'estadísticamente significativo', 'estadísticamente significativa', 'estadísticamente significativos', 'estadísticamente significativas',
  'significativo', 'significativa', 'significativos', 'significativas', 'significativamente', 'significación estadística',
  'intervalo de confianza', 'valor p', 'odds ratio', 'razón de momios', 'riesgo relativo', 'reducción absoluta del riesgo',
  'número necesario a tratar', 'hazard ratio', 'cociente de riesgos', 'desviación estándar', 'desviación típica', 'media',
  'mediana', 'rango intercuartílico', 'tamaño del efecto', 'sensibilidad', 'especificidad', 'valor predictivo positivo',
  'valor predictivo negativo', 'incidencia', 'prevalencia', 'mortalidad', 'morbilidad', 'morbimortalidad', 'letalidad',
  'ensayo clínico', 'ensayo clínico aleatorizado', 'ensayo controlado aleatorizado', 'revisión sistemática', 'metaanálisis',
  'estudio de cohortes', 'estudio de casos y controles', 'estudio transversal', 'estudio descriptivo', 'estudio cuasiexperimental',
  'grupo control', 'grupo experimental', 'grupo intervención', 'grupo de intervención', 'muestreo aleatorio',
  'muestreo por conveniencia', 'criterios de inclusión', 'criterios de exclusión', 'variable dependiente', 'variable independiente',
  'análisis de regresión', 'regresión logística', 'regresión lineal', 'chi cuadrado', 'prueba t de Student', 'U de Mann-Whitney',
  'alfa de Cronbach', 'práctica basada en la evidencia', 'enfermería basada en la evidencia', 'nivel de evidencia',
  'grado de recomendación', 'consentimiento informado', 'comité de ética', 'evento adverso', 'eventos adversos',
  'efecto adverso', 'efectos adversos', 'reacción adversa', 'seguridad del paciente', 'error de medicación',
  'errores de medicación', 'factor de riesgo', 'factores de riesgo', 'riesgo de caídas', 'grupo placebo', 'placebo',
];

export const CLINICAL_EN = [
  'vital signs', 'blood pressure', 'systolic blood pressure', 'diastolic blood pressure', 'mean arterial pressure',
  'heart rate', 'pulse rate', 'respiratory rate', 'oxygen saturation', 'body temperature', 'capillary blood glucose',
  'blood glucose', 'urine output', 'fluid balance', 'body mass index', 'waist circumference', 'level of consciousness',
  'Glasgow Coma Scale', 'Braden Scale', 'Norton Scale', 'Morse Fall Scale', 'Barthel Index', 'visual analogue scale',
  'visual analog scale', 'numeric rating scale', 'pain score',
  'hypertension', 'hypotension', 'diabetes mellitus', 'type 1 diabetes', 'type 2 diabetes', 'diabetes', 'heart failure',
  'renal failure', 'kidney failure', 'acute kidney injury', 'chronic kidney disease', 'respiratory failure',
  'chronic obstructive pulmonary disease', 'myocardial infarction', 'acute coronary syndrome', 'stroke', 'sepsis',
  'septic shock', 'pneumonia', 'ventilator-associated pneumonia', 'urinary tract infection', 'healthcare-associated infection',
  'hospital-acquired infection', 'pressure ulcer', 'pressure ulcers', 'pressure injury', 'pressure injuries',
  'moisture-associated skin damage', 'phlebitis', 'extravasation', 'delirium', 'cognitive impairment', 'dementia',
  'dysphagia', 'malnutrition', 'dehydration', 'hypoglycemia', 'hypoglycaemia', 'hyperglycemia', 'hyperglycaemia',
  'diabetic ketoacidosis', 'pulmonary embolism', 'deep vein thrombosis', 'cardiac arrest', 'cardiopulmonary resuscitation',
  'acute pain', 'chronic pain', 'cancer', 'obesity', 'asthma', 'epilepsy', 'anemia', 'anaemia', 'essential hypertension',
  'essential tremor', 'palliative care', 'intensive care', 'critical care', 'assisted living', 'assisted ventilation',
  'oral route', 'intravenous', 'intravenously', 'subcutaneous', 'subcutaneously', 'intramuscular', 'intramuscularly',
  'sublingual', 'peripheral line', 'central line', 'central venous catheter', 'peripheral intravenous catheter',
  'peripherally inserted central catheter', 'urinary catheter', 'indwelling urinary catheter', 'nasogastric tube',
  'mechanical ventilation', 'invasive mechanical ventilation', 'non-invasive ventilation', 'oxygen therapy',
  'enteral nutrition', 'parenteral nutrition', 'fluid therapy', 'blood culture', 'blood cultures', 'arterial blood gas',
  'electrocardiogram', 'hand hygiene', 'handwashing', 'contact precautions', 'standard precautions',
  'personal protective equipment', 'normal saline', 'lactated Ringer’s', "lactated Ringer's", 'saline solution',
  'acetaminophen', 'paracetamol', 'ibuprofen', 'insulin', 'rapid-acting insulin', 'basal insulin', 'morphine', 'furosemide',
  'metformin', 'epinephrine', 'adrenaline', 'norepinephrine', 'noradrenaline', 'dopamine', 'dobutamine', 'digoxin',
  'warfarin', 'salbutamol', 'albuterol', 'ondansetron', 'dexamethasone', 'hydrocortisone', 'methylprednisolone',
  'tramadol', 'fentanyl', 'midazolam', 'propofol', 'ketamine', 'naloxone', 'haloperidol', 'potassium chloride', 'heparin',
  'low-molecular-weight heparin', 'aspirin', 'clopidogrel', 'vancomycin', 'piperacillin-tazobactam',
  'statistically significant', 'statistically significantly', 'significant', 'significantly', 'statistical significance',
  'nonsignificant', 'non-significant', 'confidence interval', 'p value', 'p-value', 'odds ratio', 'relative risk',
  'risk ratio', 'absolute risk reduction', 'number needed to treat', 'hazard ratio', 'standard deviation', 'mean', 'median',
  'interquartile range', 'effect size', 'sensitivity', 'specificity', 'positive predictive value', 'negative predictive value',
  'incidence', 'prevalence', 'mortality', 'morbidity', 'case fatality rate', 'clinical trial', 'randomized controlled trial',
  'randomised controlled trial', 'systematic review', 'meta-analysis', 'cohort study', 'case-control study',
  'cross-sectional study', 'descriptive study', 'quasi-experimental study', 'control group', 'experimental group',
  'intervention group', 'random sampling', 'convenience sampling', 'inclusion criteria', 'exclusion criteria',
  'dependent variable', 'independent variable', 'regression analysis', 'logistic regression', 'linear regression',
  'chi-square', 'chi-squared', "Student's t-test", 't-test', 'Mann-Whitney U test', "Cronbach's alpha",
  'evidence-based practice', 'evidence-based nursing', 'level of evidence', 'grade of recommendation', 'informed consent',
  'ethics committee', 'institutional review board', 'adverse event', 'adverse events', 'adverse effect', 'adverse effects',
  'adverse drug reaction', 'patient safety', 'medication error', 'medication errors', 'risk factor', 'risk factors',
  'fall risk', 'risk of falls', 'placebo', 'placebo group', 'positive', 'negative', 'false positive', 'false negative',
];

// Proceso enfermero, taxonomías y conceptos disciplinares (solo modo enfermería).
export const NURSING_ES = [
  'proceso de atención de enfermería', 'proceso enfermero', 'valoración', 'valoración de enfermería', 'diagnóstico de enfermería',
  'diagnósticos de enfermería', 'diagnóstico enfermero', 'diagnósticos enfermeros', 'planificación', 'ejecución', 'evaluación',
  'plan de cuidados', 'planes de cuidados', 'plan de cuidados estandarizado', 'cuidados de enfermería', 'intervención de enfermería',
  'intervenciones de enfermería', 'actividades de enfermería', 'resultado esperado', 'resultados esperados', 'criterio de resultado',
  'criterios de resultado', 'indicador', 'indicadores', 'característica definitoria', 'características definitorias',
  'factor relacionado', 'factores relacionados', 'problema de colaboración', 'complicación potencial', 'patrones funcionales',
  'patrones funcionales de Gordon', 'necesidades básicas', 'necesidades de Virginia Henderson', 'modelo de Virginia Henderson',
  'teoría del autocuidado', 'autocuidado', 'déficit de autocuidado', 'educación para la salud', 'adherencia terapéutica',
  'adherencia al tratamiento', 'conciliación de la medicación', 'administración de medicación', 'cinco correctos',
  'continuidad de cuidados', 'informe de continuidad de cuidados', 'cuidador principal', 'sobrecarga del cuidador',
  'atención primaria', 'atención especializada', 'práctica avanzada', 'enfermera de práctica avanzada', 'triaje',
  'cuidados centrados en la persona', 'atención centrada en el paciente', 'toma de decisiones compartida', 'riesgo de úlceras por presión',
  'deterioro de la integridad cutánea', 'riesgo de infección', 'dolor agudo', 'ansiedad', 'temor', 'conocimientos deficientes',
  'gestión ineficaz de la salud', 'riesgo de caídas', 'patrón respiratorio ineficaz', 'limpieza ineficaz de las vías aéreas',
];

export const NURSING_EN = [
  'nursing process', 'nursing assessment', 'assessment', 'assess', 'assessed', 'assessing', 'nursing diagnosis', 'nursing diagnoses',
  'planning', 'implementation', 'evaluation', 'care plan', 'care plans', 'nursing care plan', 'nursing care', 'nursing intervention',
  'nursing interventions', 'nursing activities', 'expected outcome', 'expected outcomes', 'nursing outcome', 'nursing outcomes',
  'outcome indicator', 'outcome indicators', 'defining characteristic', 'defining characteristics', 'related factor', 'related factors',
  'collaborative problem', 'potential complication', 'functional health patterns', "Gordon's functional health patterns",
  "Henderson's basic needs", 'self-care', 'self-care deficit', 'self-care deficit theory', 'health education', 'patient education',
  'therapeutic adherence', 'medication adherence', 'treatment adherence', 'medication reconciliation', 'medication administration',
  'five rights', 'rights of medication administration', 'continuity of care', 'discharge planning', 'primary caregiver',
  'caregiver burden', 'primary care', 'advanced practice nurse', 'advanced practice nursing', 'nurse practitioner', 'triage',
  'person-centred care', 'person-centered care', 'patient-centered care', 'patient-centred care', 'shared decision-making',
  'impaired skin integrity', 'risk for infection', 'risk for falls', 'anxiety', 'fear', 'deficient knowledge',
  'ineffective health management', 'ineffective breathing pattern', 'ineffective airway clearance',
];

// Terminaciones típicas de nombres de fármacos (DCI). Detecta los que no están en las listas.
export const DRUG_SUFFIX_SRC =
  '(?:mab|cillin|cilina|mycin|micina|pril|sartan|sartán|olol|statin|statina|estatina|prazole|prazol|dipine|dipino|floxacin|floxacino|' +
  'parin|parina|tidine|tidina|azole|azol|navir|vudine|vudina|cycline|ciclina|semide|semida|thiazide|tiazida|azepam|zolam|codone|' +
  'codona|tinib|ximab|zumab|gliptin|gliptina|glitazone|glitazona|lukast|lukast|setron|setrón|triptan|triptán|dronate|dronato|' +
  'profen|profeno|oxetine|oxetina|afil|terol)';

// Abreviaturas que la Joint Commission / ISMP desaconsejan. Solo se avisa: nunca se cambian.
export const DO_NOT_USE = [
  { re: /(?<=\d\s?)(?:U|u)(?![\p{L}\d])/gu, msg: '«U» por «unidades» puede confundirse con 0 o 4: escríbelo completo.' },
  { re: /(?<=\d\s?)IU(?![\p{L}\d])/gu, msg: '«IU» puede leerse como IV o 10: escribe «international units / unidades internacionales».' },
  { re: /(?<![\p{L}])(?:Q\.?D\.?|q\.d\.|QOD|q\.o\.d\.)(?![\p{L}])/gu, msg: '«QD/QOD» están desaconsejadas: escribe «daily / diario» o «every other day / días alternos».' },
  { re: /(?<![\p{L}])(?:MSO4|MgSO4|MS)(?![\p{L}\d])/gu, msg: '«MS/MSO4/MgSO4» se confunden (morfina / sulfato de magnesio): escribe el nombre completo.' },
  { re: /(?<![\d.,])\d+[.,]0(?![\d])\s?(?:mg|mL|ml|g|mcg|µg)(?![\p{L}])/gu, msg: 'Cero final en una dosis (p. ej. «1.0 mg»): puede leerse como 10 mg.' },
  { re: /(?<![\d\p{L}])[.,]\d+\s?(?:mg|mL|ml|g|mcg|µg)(?![\p{L}])/gu, msg: 'Falta el cero inicial en una dosis (p. ej. «.5 mg»): escribe «0.5 mg».' },
  { re: /(?<=\d\s?)cc(?![\p{L}])/gu, msg: '«cc» se confunde con «U»: usa «mL».' },
];
