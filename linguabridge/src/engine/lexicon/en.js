// Own English lexicon. Same entry format as es.js.
// Rule of thumb: same meaning, same part of speech, same inflection.
// Irregular verbs whose past tense differs from the participle are avoided
// ("demonstrated" → "showed/shown" cannot be decided without parsing).

const forms = (pairs, extra = {}) => pairs.map(([from, to]) => ({ from, to, ...extra }));

// ---------- Level 1: redundancy ----------
export const REDUNDANCY = [
  ...forms([
    ['each and every', 'every'], ['first and foremost', 'first'], ['end result', 'result'], ['end results', 'results'],
    ['final outcome', 'outcome'], ['final outcomes', 'outcomes'], ['future plans', 'plans'], ['basic fundamentals', 'fundamentals'],
    ['completely eliminate', 'eliminate'], ['completely eliminated', 'eliminated'], ['completely eliminates', 'eliminates'],
    ['repeat again', 'repeat'], ['repeated again', 'repeated'], ['join together', 'join'], ['joined together', 'joined'],
    ['combine together', 'combine'], ['combined together', 'combined'], ['revert back', 'revert'], ['reverted back', 'reverted'],
    ['return back', 'return'], ['returned back', 'returned'], ['close proximity', 'proximity'], ['advance planning', 'planning'],
    ['absolutely essential', 'essential'], ['unexpected surprise', 'surprise'], ['past experience', 'experience'],
    ['past experiences', 'experiences'], ['new innovation', 'innovation'], ['new innovations', 'innovations'],
    ['collaborate together', 'collaborate'], ['collaborated together', 'collaborated'], ['mutual cooperation', 'cooperation'],
    ['period of time', 'period'], ['periods of time', 'periods'], ['brief summary', 'summary'], ['added bonus', 'bonus'],
    ['true fact', 'fact'], ['true facts', 'facts'], ['still remains', 'remains'], ['still remain', 'remain'],
    ['very unique', 'unique'], ['the reason why is because', 'the reason is that'], ['the reason is because', 'the reason is that'],
    ['plan ahead', 'plan'], ['planned ahead', 'planned'], ['estimated at about', 'estimated at'], ['circle around', 'circle'],
    ['in the year', 'in'], ['during the year', 'during'],
  ]),
];
// «in the year 2020»: only before a protected number
for (const e of REDUNDANCY) if (/the year$/.test(e.from)) e.before = /^\s*\uE000/;

export const DUPLICATE_OK = new Set(['that', 'had', 'is', 'do', 'ha', 'bye', 'so', 'very', 'really', 'blah']);

// ---------- Level 2: expressions and connectors ----------
export const EXPRESSIONS = [
  ...forms([
    ['in order to', 'to'], ['due to the fact that', 'because'], ['owing to the fact that', 'because'],
    ['in view of the fact that', 'because'], ['on account of the fact that', 'because'], ['in spite of the fact that', 'although'],
    ['despite the fact that', 'although'], ['in the event that', 'if'], ['prior to', 'before'], ['subsequent to', 'after'],
    ['at this point in time', 'currently'], ['at the present time', 'currently'], ['at the present moment', 'currently'],
    ['a large number of', ['many', 'numerous']], ['a great number of', 'many'], ['a great deal of', 'much'],
    ['the majority of', 'most'], ['a majority of', 'most'], ['has the ability to', 'can'], ['have the ability to', 'can'],
    ['is able to', 'can'], ['are able to', 'can'], ['with regard to', ['regarding', 'concerning']],
    ['with respect to', ['regarding', 'concerning']], ['in relation to', ['regarding', 'concerning']], ['in regard to', 'regarding'],
    ['for the purpose of', 'for'], ['as a result of', 'because of'], ['as a consequence of', 'because of'],
    ['on a daily basis', 'daily'], ['on a regular basis', 'regularly'], ['on a weekly basis', 'weekly'],
    ['in close proximity to', 'near'], ['at the same time', 'simultaneously'], ['in addition to', 'besides'],
    ['in a timely manner', 'promptly'], ['in a similar manner', 'similarly'], ['in a similar way', 'similarly'],
    ['take into account', 'consider'], ['takes into account', 'considers'], ['took into account', 'considered'],
    ['taken into account', 'considered'], ['taking into account', 'considering'],
    ['take into consideration', 'consider'], ['takes into consideration', 'considers'], ['took into consideration', 'considered'],
    ['taken into consideration', 'considered'], ['taking into consideration', 'considering'],
    ['carry out', 'conduct'], ['carries out', 'conducts'], ['carried out', 'conducted'], ['carrying out', 'conducting'],
    ['give rise to', 'cause'], ['gives rise to', 'causes'], ['gave rise to', 'caused'], ['giving rise to', 'causing'],
    ['make use of', 'use'], ['makes use of', 'uses'], ['made use of', 'used'], ['making use of', 'using'],
    ['a number of', 'several'], ['in terms of', 'regarding'],
  ]),
  ...forms([
    ['It is important to note that', ['Notably,', 'It should be noted that']], ['It should be noted that', ['Notably,', 'It is worth noting that']],
    ['It is worth noting that', ['Notably,', 'It should be noted that']],
    ['However,', ['Nevertheless,', 'Nonetheless,']], ['Nevertheless,', ['However,', 'Nonetheless,']], ['Nonetheless,', ['However,', 'Nevertheless,']],
    ['Therefore,', ['Consequently,', 'Thus,']], ['Consequently,', ['Therefore,', 'As a result,']], ['Thus,', ['Therefore,', 'Hence,']],
    ['Hence,', ['Therefore,', 'Thus,']], ['As a result,', ['Consequently,', 'Therefore,']],
    ['Furthermore,', ['Moreover,', 'In addition,']], ['Moreover,', ['Furthermore,', 'In addition,']],
    ['In addition,', ['Additionally,', 'Furthermore,']], ['Additionally,', ['In addition,', 'Moreover,']],
    ['For example,', 'For instance,'], ['For instance,', 'For example,'], ['In conclusion,', ['In summary,', 'To conclude,']],
    ['In summary,', ['In conclusion,', 'To summarize,']], ['On the other hand,', 'Conversely,'], ['Similarly,', 'Likewise,'],
    ['Likewise,', 'Similarly,'], ['Finally,', 'Lastly,'], ['Lastly,', 'Finally,'], ['In other words,', 'That is,'],
  ], { start: true }),
];

// ---------- Level 3: single-word synonyms ----------
export const SYNONYMS = [
  ...forms([
    ['utilize', 'use'], ['utilizes', 'uses'], ['utilized', 'used'], ['utilizing', 'using'], ['utilise', 'use'], ['utilises', 'uses'],
    ['utilised', 'used'], ['utilising', 'using'], ['utilization', 'use'], ['utilisation', 'use'],
    ['numerous', 'many'], ['frequently', 'often'], ['often', 'frequently'], ['primarily', 'mainly'], ['mainly', 'primarily'],
    ['crucial', 'essential'], ['adequate', 'sufficient'], ['sufficient', 'adequate'], ['relevant', 'pertinent'],
    ['highlight', 'emphasize'], ['highlights', 'emphasizes'], ['highlighted', 'emphasized'], ['highlighting', 'emphasizing'],
    ['emphasize', 'underscore'], ['emphasizes', 'underscores'], ['emphasized', 'underscored'], ['emphasizing', 'underscoring'],
    ['require', 'need'], ['requires', 'needs'], ['required', 'needed'], ['requiring', 'needing'],
    ['identify', 'recognize'], ['identifies', 'recognizes'], ['identified', 'recognized'], ['identifying', 'recognizing'],
    ['approximately', ['roughly', 'about']], ['currently', 'at present'], ['subsequently', 'later'], ['initially', 'at first'],
    ['commonly', 'frequently'], ['rapidly', 'quickly'], ['quickly', 'rapidly'], ['various', 'diverse'], ['several', 'various'],
    ['additionally', 'furthermore'], ['moreover', 'furthermore'], ['therefore', 'thus'], ['thus', 'therefore'],
    ['assist', 'help'], ['assists', 'helps'], ['assisting', 'helping'], ['obvious', 'evident'],
    ['evident', 'clear'], ['considerable', 'substantial'], ['substantial', 'considerable'], ['particularly', 'especially'],
    ['especially', 'particularly'], ['solely', 'only'], ['purpose', 'aim'],
  ]),
  // "improve" only when transitive ("pain improved" must not become "pain enhanced")
  ...forms([['improve', 'enhance'], ['improves', 'enhances'], ['improved', 'enhanced'], ['improving', 'enhancing']],
    { before: /^\s+(?:the|a|an|their|his|her|its|our|this|these|those|patient|patients'?|nursing|care|quality|clinical|health|overall|communication|safety|knowledge|adherence|outcomes?|access|efficiency|accuracy)\b/i }),
  { from: 'vital', to: ['essential', 'crucial'], notBefore: /^\s+(?:signs?|capacity|statistics)/i },
  { from: 'assisted', to: 'helped', notBefore: /^\s+(?:living|ventilation|suicide|dying|reproduct|delivery|cough)/i },
  { from: 'objective', to: 'aim', after: /(?:^|\s)(?:the|an|our|this|main|primary|secondary|general|specific|overall)\s+$/i, before: /^\s*(?:of|was|is|were|are|[,.;:])/i },
  { from: 'objectives', to: 'aims', after: /(?:^|\s)(?:the|our|these|main|primary|secondary|specific)\s+$/i, before: /^\s*(?:of|was|is|were|are|[,.;:])/i },
];

// ---------- Nursing mode: person-first / bias-free language (APA 7, ch. 5) ----------
export const PERSON_FIRST = [
  ...forms([
    ['diabetic patients', 'patients with diabetes'], ['diabetic patient', 'patient with diabetes'],
    ['diabetics', 'people with diabetes'], ['hypertensive patients', 'patients with hypertension'],
    ['hypertensive patient', 'patient with hypertension'], ['obese patients', 'patients with obesity'],
    ['obese patient', 'patient with obesity'], ['asthmatic patients', 'patients with asthma'], ['asthmatics', 'people with asthma'],
    ['epileptic patients', 'patients with epilepsy'], ['epileptics', 'people with epilepsy'],
    ['cancer patients', 'patients with cancer'], ['demented patients', 'patients with dementia'],
    ['the elderly', 'older adults'], ['elderly patients', 'older patients'], ['elderly people', 'older people'],
    ['elderly adults', 'older adults'], ['the disabled', 'people with disabilities'], ['disabled people', 'people with disabilities'],
    ['handicapped', 'disabled'], ['schizophrenics', 'people with schizophrenia'], ['addicts', 'people with substance use disorders'],
  ]),
];

export const AGENT_NOUNS = new Set([
  'nurse', 'nurses', 'physician', 'physicians', 'doctor', 'doctors', 'researcher', 'researchers', 'author', 'authors', 'team',
  'teams', 'committee', 'committees', 'staff', 'investigator', 'investigators', 'participant', 'participants', 'patient',
  'patients', 'clinician', 'clinicians', 'caregiver', 'caregivers', 'hospital', 'hospitals', 'institution', 'board', 'ministry',
  'organization', 'organisation', 'practitioner', 'practitioners', 'pharmacist', 'pharmacists', 'surgeon', 'surgeons',
  'midwife', 'midwives', 'student', 'students', 'unit', 'department', 'service', 'panel', 'reviewer', 'reviewers',
]);
// Agents for active → passive (methods-section style): generic research agents only.
export const GENERIC_AGENTS = /^(?:the\s+)?(?:researchers|authors|investigators|research team|nursing staff|study team|nurses|clinicians|reviewers)$/i;

export const DETERMINERS = new Set(['the', 'a', 'an', 'this', 'that', 'these', 'those', 'each', 'every', 'all', 'our', 'their', 'its', 'his', 'her', 'both']);

export const LOWERABLE = new Set([
  ...DETERMINERS, 'it', 'they', 'we', 'there', 'patients', 'nurses', 'participants', 'data', 'results', 'pain', 'most', 'many',
  'some', 'several', 'care', 'staff', 'clinicians', 'researchers', 'findings', 'outcomes', 'adherence', 'mortality',
  'this', 'such', 'no', 'not', 'only', 'symptoms', 'people', 'women', 'men', 'children', 'adults', 'families', 'caregivers',
]);

// participle → simple past where they differ (passive → active conversion)
export const PARTICIPLE_TO_PAST = {
  written: 'wrote', given: 'gave', taken: 'took', shown: 'showed', seen: 'saw', done: 'did', chosen: 'chose', drawn: 'drew',
  known: 'knew', begun: 'began', undertaken: 'undertook', overseen: 'oversaw', forgotten: 'forgot', driven: 'drove',
  spoken: 'spoke', withdrawn: 'withdrew', proven: 'proved', grown: 'grew', thrown: 'threw', broken: 'broke', eaten: 'ate',
  frozen: 'froze', hidden: 'hid', ridden: 'rode', stolen: 'stole', worn: 'wore', torn: 'tore', sworn: 'swore', woken: 'woke',
  made: 'made', found: 'found', held: 'held', led: 'led', brought: 'brought', taught: 'taught', thought: 'thought',
  bought: 'bought', caught: 'caught', sought: 'sought', sent: 'sent', spent: 'spent', built: 'built', kept: 'kept',
  felt: 'felt', left: 'left', met: 'met', paid: 'paid', said: 'said', told: 'told', sold: 'sold', read: 'read', put: 'put',
  set: 'set', cut: 'cut', run: 'ran', heard: 'heard', understood: 'understood', fed: 'fed', bled: 'bled', won: 'won',
  administered: 'administered',
};
export const PAST_TO_PARTICIPLE = Object.fromEntries(Object.entries(PARTICIPLE_TO_PAST).map(([p, s]) => [s, p]));
// Transitive verbs allowed for active → passive (regular -ed forms unless listed above)
export const PASSIVIZABLE = new Set([
  'collected', 'analyzed', 'analysed', 'recorded', 'measured', 'administered', 'monitored', 'evaluated', 'reviewed', 'conducted',
  'performed', 'designed', 'selected', 'recruited', 'interviewed', 'documented', 'implemented', 'applied', 'calculated',
  'compared', 'coded', 'transcribed', 'examined', 'identified', 'obtained', 'developed', 'validated', 'translated', 'piloted',
  'distributed', 'categorized', 'categorised', 'screened', 'excluded', 'included', 'randomized', 'randomised', 'assigned',
  'used', 'adapted', 'tested', 'estimated', 'extracted', 'verified', 'gathered', 'completed', 'delivered', 'provided',
]);
export const PLURAL_INVARIANT = new Set(['data', 'criteria', 'phenomena', 'people', 'children', 'women', 'men', 'staff', 'feet', 'teeth', 'media']);
export const SINGULAR_S = /(?:ss|us|is|ics|sis)$/i;
