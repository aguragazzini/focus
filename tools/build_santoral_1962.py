#!/usr/bin/env python3
"""Build the bundled 1962 Missal sanctorale (1960 rubrics, Vetus Ordo).

Source of truth: Divinum Officium kalendaria layered 1570 → 1960, plus the
Latin Sancti rank line tagged rubrica 196 / 1960 / 1962. Rubrica innovata and
rubrica 1963 are later experiments and are ignored. This is not the 1969
General Roman Calendar.

Spanish collects are taken from Divinum Officium Espanol/Sancti when the
file has a Spanish [Oratio]. Titles are translated here; the office incipit
for 29 September stays "In Dedicatione" in the Latin file, but the 1962
calendar title is the three archangels.
"""

from __future__ import annotations

import json
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KAL = Path("/tmp/kal")
LAT = Path("/tmp/divinum-officium/web/www/horas/Latin/Sancti")
ES = Path("/tmp/divinum-officium/web/www/horas/Espanol/Sancti")
OUT = ROOT / "app/src/main/assets/santoral_1962.json"

# Folded genitive (or stem) → Spanish nominative.
NAMES = {
    "abachum": "Abaco",
    "abdon": "Abdón",
    "achillei": "Aquileo",
    "adriani": "Adrián",
    "agapiti": "Agapito",
    "agathae": "Águeda",
    "agnetis": "Inés",
    "alberti": "Alberto",
    "alexandri": "Alejandro",
    "alacoque": "Alacoque",
    "ambrosii": "Ambrosio",
    "anastasii": "Anastasio",
    "andreae": "Andrés",
    "angelae": "Ángela",
    "aniceti": "Aniceto",
    "annae": "Ana",
    "anselmi": "Anselmo",
    "antonii": "Antonio",
    "antonini": "Antonino",
    "apollinaris": "Apolinar",
    "apolloniae": "Apolonia",
    "apuleji": "Apuleyo",
    "athanasii": "Atanasio",
    "audifacis": "Audifax",
    "augustini": "Agustín",
    "bacchi": "Baco",
    "barbarae": "Bárbara",
    "barnabae": "Bernabé",
    "bartholomaei": "Bartolomé",
    "basilii": "Basilio",
    "bedae": "Beda",
    "benedicti": "Benito",
    "benitii": "Benicio",
    "bernardi": "Bernardo",
    "bernardini": "Bernardino",
    "bibianae": "Bibiana",
    "birgittae": "Brígida",
    "blasii": "Blas",
    "bonifatii": "Bonifacio",
    "borgiae": "Borja",
    "brunonis": "Bruno",
    "caeciliae": "Cecilia",
    "caietani": "Cayetano",
    "cajetani": "Cayetano",
    "caji": "Cayo",
    "calasanctii": "Calasanz",
    "callisti": "Calixto",
    "canisii": "Canisio",
    "canuti": "Canuto",
    "caracciolo": "Caracciolo",
    "caroli": "Carlos",
    "casimiri": "Casimiro",
    "cassiani": "Casiano",
    "catharinae": "Catalina",
    "christinae": "Cristina",
    "christophori": "Cristóbal",
    "chrysanthi": "Crisanto",
    "chrysogoni": "Crisógono",
    "chrysologi": "Crisólogo",
    "chrysostomi": "Crisóstomo",
    "claret": "Claret",
    "clarae": "Clara",
    "clementis": "Clemente",
    "cleti": "Cleto",
    "cornelii": "Cornelio",
    "corsini": "Corsini",
    "cosmae": "Cosme",
    "crescentiae": "Crescencia",
    "cupertino": "Cupertino",
    "cypriani": "Cipriano",
    "cyriaci": "Ciríaco",
    "cyriacii": "Ciríaco",
    "cyrilli": "Cirilo",
    "damasi": "Dámaso",
    "damiani": "Damián",
    "dariae": "Daría",
    "didaci": "Diego",
    "dominici": "Domingo",
    "domitillae": "Domitila",
    "donati": "Donato",
    "dorotheae": "Dorotea",
    "eduardi": "Eduardo",
    "aegidii": "Gil",
    "eleutherii": "Eleuterio",
    "eleuthery": "Eleuterio",
    "elisabeth": "Isabel",
    "emerentianae": "Emerenciana",
    "ephraem": "Efrén",
    "epimachi": "Epímaco",
    "erasmi": "Erasmo",
    "eusebii": "Eusebio",
    "eustachii": "Eustaquio",
    "evaristi": "Evaristo",
    "eventii": "Eventio",
    "fabiani": "Fabián",
    "faustini": "Faustino",
    "felicis": "Félix",
    "felicitatis": "Felicidad",
    "felicissimi": "Felicísimo",
    "fidelis": "Fidel",
    "francisci": "Francisco",
    "franciscae": "Francisca",
    "gabrielis": "Gabriel",
    "geminiani": "Geminiano",
    "georgii": "Jorge",
    "gertrudis": "Gertrudis",
    "gordiani": "Gordiano",
    "gorgonii": "Gorgonio",
    "gregorii": "Gregorio",
    "hedwigis": "Eduviges",
    "hermenegildi": "Hermenegildo",
    "hermetis": "Hermes",
    "hieronymi": "Jerónimo",
    "hilarii": "Hilario",
    "hilarionis": "Hilarión",
    "hippolyti": "Hipólito",
    "hyacinthi": "Jacinto",
    "hygini": "Higinio",
    "ignatii": "Ignacio",
    "irenai": "Ireneo",
    "irenaei": "Ireneo",
    "isidori": "Isidoro",
    "jacobi": "Santiago",
    "januarii": "Jenaro",
    "joachim": "Joaquín",
    "joannae": "Juana",
    "joannis": "Juan",
    "josephi": "José",
    "josaphat": "Josafat",
    "jovitae": "Jovita",
    "julianae": "Juliana",
    "justinae": "Justina",
    "justini": "Justino",
    "juvenalis": "Juvenal",
    "laurentii": "Lorenzo",
    "leonardi": "Leonardi",
    "leonis": "León",
    "liborii": "Liborio",
    "lini": "Lino",
    "lucae": "Lucas",
    "luciae": "Lucía",
    "lucii": "Lucio",
    "ludovici": "Luis",
    "marcelli": "Marcelo",
    "marcellini": "Marcelino",
    "marci": "Marcos",
    "margaritae": "Margarita",
    "mariae": "María",
    "marii": "Mario",
    "marthae": "Marta",
    "martinae": "Martina",
    "martini": "Martín",
    "matthaei": "Mateo",
    "matthiae": "Matías",
    "mauri": "Mauro",
    "mauritii": "Mauricio",
    "maximi": "Máximo",
    "melchiadis": "Melquíades",
    "menna": "Menas",
    "mericiae": "Merici",
    "methodii": "Metodio",
    "modesti": "Modesto",
    "monicae": "Mónica",
    "nazianzeni": "Nacianceno",
    "nerei": "Nereo",
    "nicolai": "Nicolás",
    "nicomedis": "Nicomedes",
    "norberti": "Norberto",
    "pancratii": "Pancracio",
    "pantaleonis": "Pantaleón",
    "paschalis": "Pascual",
    "patricii": "Patricio",
    "pauli": "Pablo",
    "petri": "Pedro",
    "petronillae": "Petronila",
    "philippi": "Felipe",
    "pii": "Pío",
    "placidi": "Plácido",
    "polycarpi": "Policarpo",
    "pontiani": "Ponciano",
    "pontianae": "Ponciano",
    "praxedis": "Práxedes",
    "primi": "Primo",
    "priscae": "Prisca",
    "processi": "Proceso",
    "proti": "Proto",
    "pudentianae": "Pudenciana",
    "raphaelis": "Rafael",
    "raymundi": "Raimundo",
    "remigii": "Remigio",
    "roberti": "Roberto",
    "romae": "Roma",
    "romanae": "Romana",
    "romualdi": "Romualdo",
    "rosae": "Rosa",
    "rustici": "Rústico",
    "sabbae": "Sabas",
    "sabinae": "Sabina",
    "saturnini": "Saturnino",
    "scholasticae": "Escolástica",
    "sebastiani": "Sebastián",
    "sennen": "Senén",
    "sergii": "Sergio",
    "sigmaringa": "Sigmaringa",
    "silvestri": "Silvestre",
    "silverii": "Silverio",
    "simeonis": "Simeón",
    "simonis": "Simón",
    "soteris": "Sotero",
    "stanislai": "Estanislao",
    "stephani": "Esteban",
    "susannae": "Susana",
    "symphoriani": "Sinforiano",
    "telesphori": "Telesforo",
    "teresiae": "Teresa",
    "theresiae": "Teresa",
    "theclae": "Tecla",
    "theodori": "Teodoro",
    "theoduli": "Teódulo",
    "thomae": "Tomás",
    "tiburtiae": "Tiburcia",
    "tiburtii": "Tiburcio",
    "timothei": "Timoteo",
    "titi": "Tito",
    "tryphonis": "Trifón",
    "ubaldi": "Ubaldo",
    "urbani": "Urbano",
    "ursulae": "Úrsula",
    "valentini": "Valentín",
    "valeriani": "Valeriano",
    "venantii": "Venancio",
    "vincentii": "Vicente",
    "vitalis": "Vital",
    "viti": "Vito",
    "wenceslai": "Wenceslao",
    "xysti": "Sixto",
    "zephyrini": "Ceferino",
    "nazarii": "Nazario",
    "celsi": "Celso",
    "victoris": "Víctor",
    "innocentii": "Inocencio",
    "baptistae": "Bautista",
    "magdalenae": "Magdalena",
    "pazzis": "Pazzi",
    "feliciani": "Feliciano",
    "falconeriis": "Falconieri",
    "aloisii": "Luis",
    "gonzagae": "Gonzaga",
    "paulini": "Paulino",
    "gulielmi": "Guillermo",
    "septem": "siete",
    "fratrum": "hermanos",
    "rufinae": "Rufina",
    "secundae": "Segunda",
    "gualberti": "Gualberto",
    "bonaventurae": "Buenaventura",
    "henrici": "Enrique",
    "alexii": "Alejo",
    "camilli": "Camilo",
    "lellis": "Lelis",
    "aemiliani": "Emiliani",
    "alfonsi": "Alfonso",
    "ligorio": "Ligorio",
    "ferrerii": "Ferrer",
    "portugaliae": "Portugal",
    "avellini": "Avelino",
    "judae": "Judas",
    "cantii": "Cancio",
    "neri": "Neri",
    "eremitae": "ermitaño",
    "primi": "primer",
    "michaelis": "Miguel",
    "michaeli": "Miguel",
    "joseph": "José",
    "baylon": "Bailón",
    "cantuaria": "Cantorbery",
    "cantuariensis": "de Cantorbery",
    "vianney": "Vianney",
    "eudes": "Eudes",
    "salesii": "de Sales",
    "nolasci": "Nolasco",
    "bosco": "Bosco",
    "xaverii": "Javier",
    "alcantara": "Alcántara",
    "padua": "Padua",
    "paula": "Paula",
    "senensis": "de Siena",
    "senensis": "de Siena",
    "limanae": "de Lima",
    "hungariae": "de Hungría",
    "francorum": "de Francia",
    "franciae": "de Francia",
    "alexandrini": "de Alejandría",
    "hierosolymitani": "de Jerusalén",
    "damasceni": "Damasceno",
    "aquino": "de Aquino",
    "capistrano": "de Capistrano",
    "brundusio": "de Brindis",
    "matha": "de Mata",
    "deo": "de Dios",
    "cruce": "de la Cruz",
    "valois": "de Valois",
    "nonnati": "Nonato",
    "facundo": "Facundo",
    "magni": "Magno",
    "thaumaturgi": "Taumaturgo",
    "venerabilis": "el Venerable",
    "celestini": "Celestino",
    "bellarmino": "Bellarmino",
    "barbadici": "Barbarigo",
    "syri": "el Sirio",
    "fremiot": "Frémiot",
    "chantal": "Chantal",
    "perpetuae": "Perpetua",
    "felicitatis": "Felicidad",
    "martiniani": "Martiniano",
    "agricolae": "Agrícola",
    "justiniani": "Justiniano",
    "gordiani": "Gordiano",
    "achillei": "Aquileo",
    "domitillae": "Domitila",
    "pancratii": "Pancracio",
    "nerei": "Nereo",
}

ROLES = [
    ("summorum pontificum et martyrum", "papas y mártires"),
    ("episcopi confessoris et ecclesiae doctoris", "obispo, confesor y doctor de la Iglesia"),
    ("episcopi et confessoris et ecclesiae doctoris", "obispo, confesor y doctor de la Iglesia"),
    ("confessoris et ecclesiae doctoris", "confesor y doctor de la Iglesia"),
    ("papae confessoris et ecclesiae doctoris", "papa, confesor y doctor de la Iglesia"),
    ("papae et confessoris", "papa y confesor"),
    ("papae confessoris", "papa y confesor"),
    ("episcopis confessoris et ecclesiae doctoris", "obispo, confesor y doctor de la Iglesia"),
    ("papae et martyris", "papa y mártir"),
    ("episcopi et martyris", "obispo y mártir"),
    ("episcopi et confessoris", "obispo y confesor"),
    ("presbyteri et martyris", "presbítero y mártir"),
    ("virginis et martyris", "virgen y mártir"),
    ("apostoli et evangelistae", "apóstol y evangelista"),
    ("matris b.m.v.", "madre de la Virgen"),
    ("patris b. m. v.", "padre de la Virgen"),
    ("patris b.m.v.", "padre de la Virgen"),
    ("sponsi b.m.v. confessoris", "esposo de la Virgen, confesor"),
    ("virginis", "virgen"),
    ("viduae", "viuda"),
    ("vidue", "viuda"),
    ("abbatis", "abad"),
    ("confessoris", "confesor"),
    ("martyrum", "mártires"),
    ("martyris", "mártir"),
    ("martyres", "mártires"),
    ("maryres", "mártires"),
    ("apostolorum", "apóstoles"),
    ("apostoli", "apóstol"),
    ("evangelistae", "evangelista"),
    ("episcopi", "obispo"),
    ("episcopis", "obispo"),
    ("papae", "papa"),
    ("pp", "papa"),
    ("presbyteri", "presbítero"),
    ("regis", "rey"),
    ("ducis", "duque"),
    ("protomartyris", "protomártir"),
    ("primi eremitae et confessoris", "primer ermitaño y confesor"),
    ("regis franciae confessoris", "rey de Francia y confesor"),
    ("regis confessoris", "rey y confesor"),
    ("imperatoris confessoris", "emperador y confesor"),
    ("poenitentis", "penitente"),
    ("presbyteris", "presbítero"),
    ("reginae", "reina"),
    ("doctoris", "doctor de la Iglesia"),
    ("archangeli", "arcángel"),
    ("archangelorum", "arcángeles"),
    ("archangelis", "arcángeles"),
]

LITURGICAL = {
    "die octavae nativitatis domini": "Octava de Navidad",
    "in epiphania domini": "Epifanía del Señor",
    "in commemoratione baptismatis domini nostri jesu christi": "Conmemoración del Bautismo del Señor",
    "in conversione s. pauli apostoli": "Conversión de san Pablo",
    "in purificatione beatae mariae virginis": "Purificación de la Bienaventurada Virgen María",
    "in apparitione beatae mariae virginis immaculatae": "Aparición de la Bienaventurada Virgen María Inmaculada",
    "in cathedra s. petri apostoli antiochiae": "Cátedra de san Pedro en Antioquía",
    "s. joseph sponsi b.m.v. confessoris": "San José, esposo de la Virgen",
    "s. joseph opificis": "San José Obrero",
    "in annuntiatione beatae mariae virginis": "Anunciación de la Bienaventurada Virgen María",
    "beatae mariae virginis reginae": "Bienaventurada Virgen María Reina",
    "in vigilia s. joannis baptistae": "Vigilia de san Juan Bautista",
    "in nativitate s. joannis baptistae": "Natividad de san Juan Bautista",
    "in vigilia ss. petri et pauli apostolorum": "Vigilia de los santos Pedro y Pablo",
    "ss. apostolorum petri et pauli": "Santos Pedro y Pablo, apóstoles",
    "in commemoratione s. pauli apostoli": "Conmemoración de san Pablo",
    "pretiosissimi sanguinis domini nostri jesu christi": "Preciosísima Sangre de Nuestro Señor Jesucristo",
    "in visitatione beatae mariae virginis": "Visitación de la Bienaventurada Virgen María",
    "beatae mariae virginis de monte carmelo": "Bienaventurada Virgen María del Monte Carmelo",
    "s. annae matris b.m.v.": "Santa Ana, madre de la Virgen",
    "ss. martyrum machabaeorum": "Santos macabeos, mártires",
    "sanctae mariae virginis ad nives": "Santa María de las Nieves",
    "in transfiguratione domini nostri jesu christi": "Transfiguración de Nuestro Señor Jesucristo",
    "in vigilia s. laurentii mart.": "Vigilia de san Lorenzo",
    "in vigilia assumptionis b.m.v.": "Vigilia de la Asunción",
    "in assumptione beatae mariae virginis": "Asunción de la Bienaventurada Virgen María",
    "immaculati cordis beatae mariae virginis": "Inmaculado Corazón de la Bienaventurada Virgen María",
    "in decollatione s. joannis baptistae": "Degollación de san Juan Bautista",
    "in nativitate beatae mariae virginis": "Natividad de la Bienaventurada Virgen María",
    "s. nominis beatae mariae virginis": "Santísimo Nombre de María",
    "in exaltatione sanctae crucis": "Exaltación de la Santa Cruz",
    "septem dolorum beatae mariae virginis": "Siete Dolores de la Bienaventurada Virgen María",
    "impressionis stigmatum s. francisci": "Impresión de las llagas de san Francisco",
    "in dedicatione s. michaelis archangelis": "Santos Miguel, Gabriel y Rafael, arcángeles",
    "beatae mariae virginis de mercede": "Bienaventurada Virgen María de la Merced",
    "ss. angelorum custodum": "Santos Ángeles Custodios",
    "beatae mariae virginis a rosario": "Bienaventurada Virgen María del Rosario",
    "maternitatis beatae mariae virginis": "Maternidad de la Bienaventurada Virgen María",
    "omnium sanctorum": "Todos los Santos",
    "in commemoratione omnium fidelium defunctorum": "Conmemoración de todos los fieles difuntos",
    "in dedicatione archibasilicae ss. salvatoris": "Dedicación de la archibasílica del Salvador",
    "in dedicatione basilicarum ss. apostolorum petri et pauli": "Dedicación de las basílicas de san Pedro y san Pablo",
    "in praesentatione beatae mariae virginis": "Presentación de la Bienaventurada Virgen María",
    "in conceptione immaculata beatae mariae virginis": "Inmaculada Concepción de la Bienaventurada Virgen María",
    "in vigilia nativitatis domini": "Vigilia de Navidad",
    "in nativitate domini": "Natividad del Señor",
    "die quarta infra octavam nativitatis": "Cuarto día de la octava de Navidad",
    "die quinta infra octavam nativitatis": "Quinto día de la octava de Navidad",
    "die septima infra octavam nativitatis": "Séptimo día de la octava de Navidad",
    "sanctissimi nominis iesu": "Santísimo Nombre de Jesús",
    "sanctissimi nominis jesu": "Santísimo Nombre de Jesús",
    "ss. septem fundatorum ordinis servorum b. m. v.": "Siete fundadores de los Siervos de la Virgen",
    "ss. septem fundatorum ordinis servorum b.m.v.": "Siete fundadores de los Siervos de la Virgen",
    "ss. quadraginta martyrum": "Cuarenta mártires",
    "ss. quatuor coronatorum martyrum": "Cuatro santos coronados, mártires",
    "commemoratio ss. innocentium": "Santos Inocentes",
    "commemoratio s. thomae episcopi et martyris": "Santo Tomás Becket, obispo y mártir",
    "commemoratio s. silvestri papae et conf": "San Silvestre, papa",
}


def fold(value: str) -> str:
    value = value.replace("æ", "ae").replace("œ", "oe").replace("Æ", "ae").replace("Œ", "oe")
    value = "".join(ch for ch in unicodedata.normalize("NFD", value) if unicodedata.category(ch) != "Mn")
    value = value.lower().replace("ñ", "n")
    value = value.replace("–", " ").replace("—", " ")
    value = re.sub(r"[^a-z0-9.+ ]", " ", value)
    value = re.sub(r"\s+", " ", value).strip()
    return value


def parse_kal(path: Path) -> dict:
    out = {}
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or line.startswith("*") or "=" not in line:
            continue
        date, rest = line.split("=", 1)
        date = date.strip()
        if not re.match(r"\d{2}-\d{2}", date):
            continue
        if rest.strip().startswith("XXXXX"):
            out[date] = None
            continue
        parts = rest.split("=")
        out[date] = {
            "files": parts[0].strip(),
            "name": parts[1].strip() if len(parts) > 1 else "",
            "rank": parts[2].strip() if len(parts) > 2 else "",
            "comm": parts[3].strip() if len(parts) > 3 else "",
        }
    return out


def is_1960_tag(tag: str) -> bool:
    text = tag.lower()
    if re.search(r"196[3-9]", text):
        return False
    if not re.search(r"rubrica 1960|rubrica 1962|rubrica 196(?!\d)", text):
        return False
    blockers = ("innovata", "trident", "cister", "1570", "1617", "divino", "monastic", "barroux", "praedic")
    return not any(token in text for token in blockers)


def rank_entries(text: str):
    found = []
    for match in re.finditer(r"\[Rank\]([^\n]*)\n(.*?)(?=\n\[|\Z)", text, re.S):
        header = match.group(1).strip()
        active = header
        for line in match.group(2).splitlines():
            line = line.strip()
            if not line:
                continue
            if line.startswith("("):
                active = line
                continue
            if line.startswith("@"):
                continue
            bits = line.split(";;")
            found.append(
                (
                    active,
                    bits[0].strip(),
                    bits[1].strip() if len(bits) > 1 else "",
                    bits[2].strip() if len(bits) > 2 else "",
                )
            )
            active = header
    return found


def pick_rank(text: str):
    entries = rank_entries(text)
    tagged = [entry for entry in entries if is_1960_tag(entry[0])]
    if tagged:
        return tagged[-1]
    for entry in entries:
        if entry[0].strip() == "":
            return entry
    return ("", "", "", "")


def officium_name(text: str) -> str:
    match = re.search(r"\[Officium\][^\n]*\n(.*?)(?=\n\[|\Z)", text, re.S)
    if not match:
        return ""
    active = ""
    default = ""
    tagged = ""
    for line in match.group(1).splitlines():
        line = line.strip()
        if not line:
            continue
        if line.startswith("("):
            active = line
            continue
        if line.startswith("@"):
            continue
        if is_1960_tag(active):
            tagged = line
        elif active == "" and not default:
            default = line
        active = ""
    return tagged or default


def parse_num(raw: str):
    match = re.match(r"\d+(?:\.\d+)?", raw or "")
    return float(match.group(0)) if match else None


def to_class(klass: str, numeric: str):
    text = fold(klass)
    if "feria" in text:
        return None
    if re.search(r"\bi\s*\.?\s*class|\b1\s*\.?\s*class", text):
        return "I"
    if re.search(r"\bii\s*\.?\s*class|\b2\s*(nd)?\s*\.?\s*class", text):
        return "II"
    if any(token in text for token in ("simplex", "commemor", "comemor", "memoria")):
        return "IV"
    number = parse_num(numeric)
    if number is not None:
        if number >= 6:
            return "I"
        if number >= 5:
            return "II"
        if number >= 2:
            return "III"
        return "IV"
    if any(token in text for token in ("duplex", "semiduplex", "majus", "vigilia")):
        return "III"
    return "III"


def class_from_kal_rank(raw: str):
    number = parse_num(raw)
    if number is None:
        return "III"
    if number >= 6:
        return "I"
    if number >= 5:
        return "II"
    if number >= 2:
        return "III"
    return "IV"


FEMALE = {
    "inés", "águeda", "ana", "apolonia", "bárbara", "bibiana", "brígida", "cecilia",
    "clara", "cristina", "dorotea", "eduviges", "elena", "escolástica", "felicidad",
    "gertrudis", "isabel", "juliana", "justina", "lucía", "margarita", "maría",
    "marta", "martina", "mónica", "perpetua", "petronila", "prisca", "púdenes",
    "rosa", "rufina", "sabina", "susana", "tecla", "teresa", "úrsula", "segunda",
    "emerenciana", "francisca", "juana", "ágata",
}

ROMAN = {"i": "I", "ii": "II", "iii": "III", "iv": "IV", "v": "V", "vi": "VI", "vii": "VII", "ix": "IX", "x": "X", "xii": "XII"}


def strip_role(working: str):
    ordered = sorted(ROLES, key=lambda item: len(item[0]), reverse=True)
    for needle, spanish in ordered:
        pattern = rf"(?:^|\s){re.escape(needle)}(?:\s|$)"
        if re.search(pattern, working):
            working = re.sub(pattern, " ", working)
            working = re.sub(r"\s+", " ", working).strip(" ,")
            return working, spanish
    return working, ""


def render_person(part: str) -> str:
    rendered = []
    tokens = part.split()
    index = 0
    while index < len(tokens):
        token = tokens[index]
        if token in {"de", "a", "ab", "ad"} and index + 1 < len(tokens):
            nxt = tokens[index + 1]
            if nxt in NAMES:
                piece = NAMES[nxt]
                if piece.startswith("de "):
                    rendered.append(piece)
                else:
                    rendered.append(f"de {piece}" if token == "de" else piece)
                index += 2
                continue
        if token in ROMAN:
            rendered.append(ROMAN[token])
            index += 1
            continue
        if token in NAMES:
            rendered.append(NAMES[token])
        elif token in {"y", "compañeros"}:
            rendered.append("y compañeros" if token == "compañeros" else "y")
        elif token not in {"s", "ss", "mm", "ep", "conf", "mart", "virg", "soc", "et"}:
            rendered.append(token.capitalize())
        index += 1
    return re.sub(r"\s+", " ", " ".join(rendered)).strip(" ,")


def translate(latin: str) -> str:
    folded = fold(latin).replace("b. m. v.", "b.m.v.").replace("b m v", "b.m.v.")
    if folded in LITURGICAL:
        return LITURGICAL[folded]
    ordinals = {"quarta": "Cuarto", "quinta": "Quinto", "sexta": "Sexto", "septima": "Séptimo"}
    octave = re.match(r"die (\w+) infra octavam nativitatis", folded)
    if octave and octave.group(1) in ordinals:
        return f"{ordinals[octave.group(1)]} día de la octava de Navidad"
    working = folded
    prefix = ""
    prefixes = [
        ("in nativitate ", "Natividad de "),
        ("in conversione ", "Conversión de "),
        ("in dedicatione ", "Dedicación de "),
        ("in vigilia ", "Vigilia de "),
        ("in commemoratione ", "Conmemoración de "),
        ("in assumptione ", "Asunción de "),
        ("in purificatione ", "Purificación de "),
        ("in annuntiatione ", "Anunciación de "),
        ("in exaltatione ", "Exaltación de "),
        ("in transfiguratione ", "Transfiguración de "),
        ("in decollatione ", "Degollación de "),
        ("in conceptione immaculata ", "Inmaculada Concepción de "),
        ("in praesentatione ", "Presentación de "),
        ("in visitatione ", "Visitación de "),
        ("in apparitione ", "Aparición de "),
        ("in cathedra ", "Cátedra de "),
        ("commemoratio ", "Conmemoración de "),
    ]
    for needle, spanish in prefixes:
        if working.startswith(needle):
            prefix = spanish
            working = working[len(needle):]
            break
    working = re.sub(r"^(?:ss\.|s\.|ss|s)\s+", "", working)
    plural = folded.startswith("ss")
    working, role = strip_role(working)
    working = working.replace(" atque ", " et ").replace(" ac ", " et ")
    working = working.replace(" et sociorum", " y compañeros")
    working = working.replace(" et socii", " y compañeros")
    parts = [part.strip(" ,") for part in working.split(" et ") if part.strip(" ,")]
    people = [render_person(part) for part in parts]
    people = [person for person in people if person]
    if not people:
        people = [working.title() or "la fiesta"]
    saint = " y ".join(people)
    saint = re.sub(r"\s+", " ", saint).replace(" y y ", " y ").strip(" ,")
    first = saint.split(" y ")[0].split()[0].lower() if saint else ""
    if prefix:
        titled = (prefix + saint).strip()
    elif plural or len(people) > 1 or saint.startswith("siete "):
        titled = "Santos " + saint
    elif role.startswith("virgen") or role.startswith("viuda") or first in FEMALE:
        titled = "Santa " + saint
    else:
        titled = "San " + saint
    if role:
        titled = f"{titled}, {role}"
    titled = titled.replace("San Santos ", "Santos ").replace("Santa Santos ", "Santos ")
    titled = re.sub(r"^(San|Santa|Santos)\s+[Ss]s?\.\s+", r"\1 ", titled)
    titled = titled.replace("Santos siete ", "Siete ")
    return re.sub(r"\s+", " ", titled).strip(" ,")


def looks_spanish(text: str) -> bool:
    if not text or len(text) < 40:
        return False
    if re.search(r"[áéíóúñÁÉÍÓÚÑ¿¡]", text):
        return True
    words = set(re.findall(r"[A-Za-záéíóúñ]+", text.lower()))
    return len(words & {"dios", "señor", "que", "tu", "nos", "conced"}) >= 2


def spanish_oration(text: str) -> str:
    if not text:
        return ""
    blocks = re.findall(r"\[Oratio\]([^\n]*)\n(.*?)(?=\n\[|\Z)", text, re.S)
    for _header, body in blocks:
        lines = []
        for line in body.splitlines():
            stripped = line.strip()
            if not stripped or stripped.startswith("(") or stripped.startswith("@") or stripped.startswith("!"):
                continue
            if stripped.startswith("$"):
                break
            lines.append(stripped)
        collected = re.sub(r"\s+", " ", " ".join(lines)).strip()
        collected = re.sub(r"^v\.\s+", "", collected, flags=re.I)
        if looks_spanish(collected):
            return collected
    return ""


def clip_oration(text: str) -> str:
    parts = [part.strip() for part in re.split(r"(?<=[\.!?])\s+", text.strip()) if part.strip()]
    if not parts:
        return ""
    first = parts[0]
    if len(first) > 190:
        cut = first[:190]
        pivot = max(cut.rfind(","), cut.rfind(";"))
        if pivot > 80:
            cut = cut[:pivot]
        first = cut.rstrip(" ,;") + "."
    return first


def fallback_summary(name: str, rank: str) -> str:
    if rank == "IV":
        return f"Conmemoración de {name} en el calendario tradicional."
    if name.lower().startswith("vigilia"):
        return f"{name}, víspera de la fiesta en el misal tradicional."
    return f"Fiesta de {name} en el calendario tradicional."


def load_text(root: Path, file_id: str, date: str) -> str:
    for candidate in (root / f"{file_id}.txt", root / f"{date}.txt"):
        if candidate.exists():
            return candidate.read_text(encoding="utf-8", errors="replace")
    return ""


NAME_FIX = {
    "07-28": "Santos Nazario y Celso, mártires, y los papas Víctor I e Inocencio I",
    "07-10": "Siete hermanos mártires, y las vírgenes Rufina y Segunda",
    "06-02": "Santos Marcelino, Pedro y Erasmo, mártires",
    "09-16": "Santos Cornelio, papa, y Cipriano, obispo, mártires",
    "09-19": "San Jenaro, obispo, y compañeros mártires",
    "01-14": "San Hilario, obispo, confesor y doctor de la Iglesia",
    "03-18": "San Cirilo de Jerusalén, obispo, confesor y doctor de la Iglesia",
    "08-16": "San Joaquín, padre de la Virgen, confesor",
    "08-20": "San Bernardo, abad y doctor de la Iglesia",
    "12-26": "San Esteban, protomártir",
    "06-12": "San Juan de Sahagún, confesor",
    "06-10": "Santa Margarita, reina y viuda",
    "08-11": "Santos Tiburcio y Susana, mártires",
    "09-26": "Santos Cipriano y Justina, mártires",
    "09-28": "San Wenceslao, duque y mártir",
    "09-30": "San Jerónimo, presbítero, confesor y doctor de la Iglesia",
}

SUMMARY_FIX = {
    "09-29": "La Iglesia celebra juntos a los tres arcángeles nombrados en la Escritura y pide su defensa.",
    "11-02": "La Iglesia ruega por todos los fieles difuntos, para que alcancen el descanso eterno.",
}


def usable_commemoration(latin: str, primary: str) -> str:
    folded = fold(latin)
    if not folded or folded in {"xxxxx", "nihil", "nihil de octava s joannis"}:
        return ""
    if "nihil" in folded or folded == "xxxxx":
        return ""
    spanish = translate(latin)
    if fold(spanish) == fold(primary):
        return ""
    leftover = fold(spanish)
    if any(token in leftover.split() for token in ("virginis", "martyris", "episcopi", "confessoris", "papae", "abbatis")):
        return ""
    if re.search(r"\b[A-Z]{2,}\b", spanish):
        return ""
    return spanish


def main() -> None:
    calendar = {}
    for year in ("1570", "1888", "1906", "1939", "1954", "1955", "1960"):
        for key, value in parse_kal(KAL / f"{year}.txt").items():
            if value is None:
                calendar.pop(key, None)
            else:
                calendar[key] = value

    days = {}
    leftovers = []
    for date, entry in sorted(calendar.items()):
        if not re.match(r"\d{2}-\d{2}$", date):
            continue
        file_id = entry["files"].split("~")[0].strip()
        latin_text = load_text(LAT, file_id, date)
        spanish_text = load_text(ES, file_id, date)
        _tag, rank_name, klass, numeric = pick_rank(latin_text) if latin_text else ("", "", "", "")
        latin_name = rank_name or (officium_name(latin_text) if latin_text else "") or entry["name"]
        rank = to_class(klass, numeric)
        comm_latin = entry["comm"]
        if rank is None:
            # 1960 feria. Keep a real commemoration when the kalendarium still lists one.
            if not comm_latin or "XXXX" in comm_latin:
                continue
            latin_name = comm_latin
            rank = "IV"
            comm_latin = ""
        if re.search(r"infra octavam nativitatis", fold(entry["name"])):
            latin_name = entry["name"]
            rank = class_from_kal_rank(entry["rank"])
            if not comm_latin:
                comm_latin = rank_name
        if date == "09-29":
            latin_name = "In Dedicatione S. Michaelis Archangelis"
            rank = "I"
        spanish_name = NAME_FIX.get(date, translate(latin_name))
        folded_out = fold(spanish_name)
        if any(token in folded_out.split() for token in ("virginis", "martyris", "episcopi", "confessoris", "domini", "beatae", "sancti", "sanctae")):
            leftovers.append((date, latin_name, spanish_name))
        oration = clip_oration(spanish_oration(spanish_text))
        summary = SUMMARY_FIX.get(date) or oration or fallback_summary(spanish_name, rank)
        note = ""
        comm = usable_commemoration(comm_latin, spanish_name) if comm_latin else ""
        if comm:
            note = f"Conmemoración: {comm}."
        days[date] = {
            "name": spanish_name,
            "rank": rank,
            "summary": summary,
            "note": note,
        }

    payload = {
        "calendar": "1962",
        "rubrics": "1960",
        "rite": "vetus-ordo",
        "source": "Universal Roman calendar of the 1962 Missal (rubrics of 1960), from the Divinum Officium kalendaria layered through 1960 and Sancti ranks tagged rubrica 196. Not the 1969 calendar.",
        "days": days,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(days)} feasts to {OUT}")
    print("leftovers", len(leftovers))
    for row in leftovers:
        print(" !", row)


if __name__ == "__main__":
    main()
