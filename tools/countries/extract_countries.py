#!/usr/bin/env python3
"""Builds the countries course asset from the official Polish list of country names.

Run from anywhere:  python3 tools/countries/extract_countries.py <Urzedowy_wykaz_panstw_2025.pdf>

Reads
    the "Urzędowy wykaz nazw państw i terytoriów niesamodzielnych", 8th edition (2025), by the
    Komisja Standaryzacji Nazw Geograficznych poza Granicami Rzeczypospolitej Polskiej (KSNG),
    free to download from https://www.gov.pl/web/ksng/Urzedowy-wykaz-nazw-panstw-i-terytoriow-niesamodzielnych

Writes
    data/src/androidMain/assets/countries.json

Every country of part I of the list (the 197 states Poland recognises) with its Polish name,
its capital, and the words for a man and a woman from it ("obyw." line). Fifteen entries give no
such words; they keep only the country and the capital. The two updates printed at the end of
the publication (Equatorial Guinea's capital, Nauru's new name Naoero) replace their entries.
The flag (ISO 3166 code) and the English names of the country and the capital are added here,
since the list is in Polish only.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path

ASSET = Path(__file__).resolve().parents[2] / "data/src/androidMain/assets/countries.json"
CONTINENT = r"(?:Europa|Azja|Afryka|Ameryka Północna|Ameryka Południowa|Australia i Oceania)"
HEADER = re.compile(r"^(\S.*?)\s{3,}(" + CONTINENT + r"(?: i (?:Europa|Azja|Afryka))?)\s*$")
ALSO_ACCEPTED = {"Naoero": ["Nauru"]}

CODES = {
"Afganistan":("AF","Afghanistan"),"Albania":("AL","Albania"),"Algieria":("DZ","Algeria"),"Andora":("AD","Andorra"),"Angola":("AO","Angola"),
"Arabia Saudyjska":("SA","Saudi Arabia"),"Argentyna":("AR","Argentina"),"Armenia":("AM","Armenia"),"Australia":("AU","Australia"),"Austria":("AT","Austria"),
"Azerbejdżan":("AZ","Azerbaijan"),"Bahamy":("BS","Bahamas"),"Bahrajn":("BH","Bahrain"),"Bangladesz":("BD","Bangladesh"),"Belgia":("BE","Belgium"),
"Belize":("BZ","Belize"),"Benin":("BJ","Benin"),"Bhutan":("BT","Bhutan"),"Białoruś":("BY","Belarus"),"Boliwia":("BO","Bolivia"),
"Botswana":("BW","Botswana"),"Brazylia":("BR","Brazil"),"Brunei":("BN","Brunei"),"Bułgaria":("BG","Bulgaria"),"Burkina Faso":("BF","Burkina Faso"),
"Burundi":("BI","Burundi"),"Chile":("CL","Chile"),"Chiny":("CN","China"),"Chorwacja":("HR","Croatia"),"Cypr":("CY","Cyprus"),
"Czad":("TD","Chad"),"Czarnogóra":("ME","Montenegro"),"Czechy":("CZ","Czechia"),"Dania":("DK","Denmark"),
"Demokratyczna Republika Konga":("CD","Democratic Republic of the Congo"),"Dominika":("DM","Dominica"),"Dominikana":("DO","Dominican Republic"),
"Dżibuti":("DJ","Djibouti"),"Egipt":("EG","Egypt"),"Ekwador":("EC","Ecuador"),"Erytrea":("ER","Eritrea"),"Estonia":("EE","Estonia"),
"Eswatini":("SZ","Eswatini"),"Etiopia":("ET","Ethiopia"),"Fidżi":("FJ","Fiji"),"Filipiny":("PH","Philippines"),"Finlandia":("FI","Finland"),
"Francja":("FR","France"),"Gabon":("GA","Gabon"),"Gambia":("GM","Gambia"),"Ghana":("GH","Ghana"),"Grecja":("GR","Greece"),
"Grenada":("GD","Grenada"),"Gruzja":("GE","Georgia"),"Gujana":("GY","Guyana"),"Gwatemala":("GT","Guatemala"),"Gwinea":("GN","Guinea"),
"Gwinea Bissau":("GW","Guinea-Bissau"),"Gwinea Równikowa":("GQ","Equatorial Guinea"),"Haiti":("HT","Haiti"),"Hiszpania":("ES","Spain"),
"Holandia":("NL","Netherlands"),"Honduras":("HN","Honduras"),"Indie":("IN","India"),"Indonezja":("ID","Indonesia"),"Irak":("IQ","Iraq"),
"Iran":("IR","Iran"),"Irlandia":("IE","Ireland"),"Islandia":("IS","Iceland"),"Izrael":("IL","Israel"),"Jamajka":("JM","Jamaica"),
"Japonia":("JP","Japan"),"Jemen":("YE","Yemen"),"Jordania":("JO","Jordan"),"Kambodża":("KH","Cambodia"),"Kamerun":("CM","Cameroon"),
"Kanada":("CA","Canada"),"Katar":("QA","Qatar"),"Kazachstan":("KZ","Kazakhstan"),"Kenia":("KE","Kenya"),"Kirgistan":("KG","Kyrgyzstan"),
"Kiribati":("KI","Kiribati"),"Kolumbia":("CO","Colombia"),"Komory":("KM","Comoros"),"Kongo":("CG","Republic of the Congo"),
"Korea Południowa":("KR","South Korea"),"Korea Północna":("KP","North Korea"),"Kostaryka":("CR","Costa Rica"),"Kuba":("CU","Cuba"),
"Kuwejt":("KW","Kuwait"),"Laos":("LA","Laos"),"Lesotho":("LS","Lesotho"),"Liban":("LB","Lebanon"),"Liberia":("LR","Liberia"),
"Libia":("LY","Libya"),"Liechtenstein":("LI","Liechtenstein"),"Litwa":("LT","Lithuania"),"Luksemburg":("LU","Luxembourg"),"Łotwa":("LV","Latvia"),
"Macedonia Północna":("MK","North Macedonia"),"Madagaskar":("MG","Madagascar"),"Malawi":("MW","Malawi"),"Malediwy":("MV","Maldives"),
"Malezja":("MY","Malaysia"),"Mali":("ML","Mali"),"Malta":("MT","Malta"),"Maroko":("MA","Morocco"),"Mauretania":("MR","Mauritania"),
"Mauritius":("MU","Mauritius"),"Meksyk":("MX","Mexico"),"Mikronezja":("FM","Micronesia"),"Mjanma":("MM","Myanmar"),"Mołdawia":("MD","Moldova"),
"Monako":("MC","Monaco"),"Mongolia":("MN","Mongolia"),"Mozambik":("MZ","Mozambique"),"Namibia":("NA","Namibia"),"Naoero":("NR","Nauru"),
"Nepal":("NP","Nepal"),"Niemcy":("DE","Germany"),"Niger":("NE","Niger"),"Nigeria":("NG","Nigeria"),"Nikaragua":("NI","Nicaragua"),
"Norwegia":("NO","Norway"),"Nowa Zelandia":("NZ","New Zealand"),"Oman":("OM","Oman"),"Pakistan":("PK","Pakistan"),"Palau":("PW","Palau"),
"Panama":("PA","Panama"),"Papua-Nowa Gwinea":("PG","Papua New Guinea"),"Paragwaj":("PY","Paraguay"),"Peru":("PE","Peru"),"Polska":("PL","Poland"),
"Południowa Afryka":("ZA","South Africa"),"Portugalia":("PT","Portugal"),"Republika Środkowoafrykańska":("CF","Central African Republic"),
"Republika Zielonego Przylądka":("CV","Cabo Verde"),"Rosja":("RU","Russia"),"Rumunia":("RO","Romania"),"Rwanda":("RW","Rwanda"),
"Salwador":("SV","El Salvador"),"Samoa":("WS","Samoa"),"San Marino":("SM","San Marino"),"Senegal":("SN","Senegal"),"Serbia":("RS","Serbia"),
"Seszele":("SC","Seychelles"),"Sierra Leone":("SL","Sierra Leone"),"Singapur":("SG","Singapore"),"Słowacja":("SK","Slovakia"),"Słowenia":("SI","Slovenia"),
"Somalia":("SO","Somalia"),"Sri Lanka":("LK","Sri Lanka"),"Stany Zjednoczone":("US","United States"),"Sudan":("SD","Sudan"),
"Sudan Południowy":("SS","South Sudan"),"Surinam":("SR","Suriname"),"Syria":("SY","Syria"),"Szwajcaria":("CH","Switzerland"),"Szwecja":("SE","Sweden"),
"Tadżykistan":("TJ","Tajikistan"),"Tajlandia":("TH","Thailand"),"Tanzania":("TZ","Tanzania"),"Timor Wschodni":("TL","Timor-Leste"),"Togo":("TG","Togo"),
"Tonga":("TO","Tonga"),"Tunezja":("TN","Tunisia"),"Turcja":("TR","Turkey"),"Turkmenistan":("TM","Turkmenistan"),"Tuvalu":("TV","Tuvalu"),
"Uganda":("UG","Uganda"),"Ukraina":("UA","Ukraine"),"Urugwaj":("UY","Uruguay"),"Uzbekistan":("UZ","Uzbekistan"),"Vanuatu":("VU","Vanuatu"),
"Watykan":("VA","Vatican City"),"Wenezuela":("VE","Venezuela"),"Węgry":("HU","Hungary"),"Wielka Brytania":("GB","United Kingdom"),
"Wietnam":("VN","Vietnam"),"Włochy":("IT","Italy"),"Zambia":("ZM","Zambia"),"Zimbabwe":("ZW","Zimbabwe"),
"Zjednoczone Emiraty Arabskie":("AE","United Arab Emirates"),
}

CODES.update({
    "Antigua i Barbuda": ("AG", "Antigua and Barbuda"), "Barbados": ("BB", "Barbados"),
    "Bośnia i Hercegowina": ("BA", "Bosnia and Herzegovina"), "Kosowo": ("XK", "Kosovo"), "Niue": ("NU", "Niue"),
    "Saint Kitts i Nevis": ("KN", "Saint Kitts and Nevis"),
    "Saint Lucia": ("LC", "Saint Lucia"), "Saint Vincent i Grenadyny": ("VC", "Saint Vincent and the Grenadines"),
    "Trynidad i Tobago": ("TT", "Trinidad and Tobago"), "Wybrzeże Kości Słoniowej": ("CI", "Côte d'Ivoire"),
    "Wyspy Cooka": ("CK", "Cook Islands"), "Wyspy Marshalla": ("MH", "Marshall Islands"), "Wyspy Salomona": ("SB", "Solomon Islands"),
    "Wyspy Świętego Tomasza i Książęca": ("ST", "São Tomé and Príncipe"),
})

CAPITAL_EN = {
"Kabul":"Kabul","Tirana":"Tirana","Algier":"Algiers","Andora":"Andorra la Vella","Luanda":"Luanda","Saint John’s":"Saint John's","Rijad":"Riyadh",
"Buenos Aires":"Buenos Aires","Erywań":"Yerevan","Canberra":"Canberra","Wiedeń":"Vienna","Baku":"Baku","Nassau":"Nassau","Manama":"Manama",
"Dhaka":"Dhaka","Bridgetown":"Bridgetown","Bruksela":"Brussels","Belmopan":"Belmopan","Porto-Novo":"Porto-Novo","Thimphu":"Thimphu","Mińsk":"Minsk",
"Sucre":"Sucre","Sarajewo":"Sarajevo","Gaborone":"Gaborone","Brasília":"Brasília","Bandar Seri Begawan":"Bandar Seri Begawan","Sofia":"Sofia",
"Wagadugu":"Ouagadougou","Gitega":"Gitega","Santiago":"Santiago","Pekin":"Beijing","Zagrzeb":"Zagreb","Nikozja":"Nicosia","Ndżamena":"N'Djamena",
"Podgorica":"Podgorica","Praga":"Prague","Kopenhaga":"Copenhagen","Kinszasa":"Kinshasa","Roseau":"Roseau","Santo Domingo":"Santo Domingo",
"Dżibuti":"Djibouti","Kair":"Cairo","Quito":"Quito","Asmara":"Asmara","Tallinn":"Tallinn","Mbabane":"Mbabane","Addis Abeba":"Addis Ababa",
"Suva":"Suva","Manila":"Manila","Helsinki":"Helsinki","Paryż":"Paris","Libreville":"Libreville","Bandżul":"Banjul","Akra":"Accra","Ateny":"Athens",
"Saint George’s":"Saint George's","Tbilisi":"Tbilisi","Georgetown":"Georgetown","Gwatemala":"Guatemala City","Konakry":"Conakry","Bissau":"Bissau",
"Ciudad de la Paz":"Ciudad de la Paz","Port-au-Prince":"Port-au-Prince","Madryt":"Madrid","Amsterdam":"Amsterdam","Tegucigalpa":"Tegucigalpa",
"Nowe Delhi":"New Delhi","Dżakarta":"Jakarta","Bagdad":"Baghdad","Teheran":"Tehran","Dublin":"Dublin","Reykjavík":"Reykjavík","Jerozolima":"Jerusalem",
"Kingston":"Kingston","Tokio":"Tokyo","Sana":"Sanaa","Amman":"Amman","Phnom Penh":"Phnom Penh","Jaunde":"Yaoundé","Ottawa":"Ottawa","Doha":"Doha",
"Astana":"Astana","Nairobi":"Nairobi","Biszkek":"Bishkek","Bairiki":"Bairiki","Bogota":"Bogotá","Moroni":"Moroni","Brazzaville":"Brazzaville",
"Seul":"Seoul","Pjongjang":"Pyongyang","Prisztina":"Pristina","San José":"San José","Hawana":"Havana","Kuwejt":"Kuwait City","Wientian":"Vientiane",
"Maseru":"Maseru","Bejrut":"Beirut","Monrovia":"Monrovia","Trypolis":"Tripoli","Vaduz":"Vaduz","Wilno":"Vilnius","Luksemburg":"Luxembourg",
"Ryga":"Riga","Skopje":"Skopje","Antananarywa":"Antananarivo","Lilongwe":"Lilongwe","Male":"Malé","Kuala Lumpur":"Kuala Lumpur","Bamako":"Bamako",
"Valletta":"Valletta","Rabat":"Rabat","Nawakszut":"Nouakchott","Port Louis":"Port Louis","Meksyk":"Mexico City","Palikir":"Palikir",
"Naypyidaw":"Naypyidaw","Kiszyniów":"Chișinău","Monako":"Monaco","Ułan Bator":"Ulaanbaatar","Maputo":"Maputo","Windhuk":"Windhoek",
"Katmandu":"Kathmandu","Berlin":"Berlin","Niamey":"Niamey","Abudża":"Abuja","Managua":"Managua","Alofi":"Alofi","Oslo":"Oslo",
"Wellington":"Wellington","Maskat":"Muscat","Islamabad":"Islamabad","Ngerulmud":"Ngerulmud","Panama":"Panama City","Port Moresby":"Port Moresby",
"Asunción":"Asunción","Lima":"Lima","Warszawa":"Warsaw","Pretoria":"Pretoria","Lizbona":"Lisbon","Bangi":"Bangui","Praia":"Praia","Moskwa":"Moscow",
"Bukareszt":"Bucharest","Kigali":"Kigali","Basseterre":"Basseterre","Castries":"Castries","Kingstown":"Kingstown","San Salvador":"San Salvador",
"Apia":"Apia","San Marino":"San Marino","Dakar":"Dakar","Belgrad":"Belgrade","Victoria":"Victoria","Freetown":"Freetown","Singapur":"Singapore",
"Bratysława":"Bratislava","Lublana":"Ljubljana","Mogadiszu":"Mogadishu","Sri Dźajawardanapura Kotte":"Sri Jayawardenepura Kotte",
"Waszyngton":"Washington, D.C.","Chartum":"Khartoum","Dżuba":"Juba","Paramaribo":"Paramaribo","Damaszek":"Damascus","Berno":"Bern",
"Sztokholm":"Stockholm","Duszanbe":"Dushanbe","Bangkok":"Bangkok","Dodoma":"Dodoma","Dili":"Dili","Lomé":"Lomé","Nuku’alofa":"Nukuʻalofa",
"Port-of-Spain":"Port of Spain","Tunis":"Tunis","Ankara":"Ankara","Aszchabad":"Ashgabat","Vaiaku":"Vaiaku","Kampala":"Kampala","Kijów":"Kyiv",
"Montevideo":"Montevideo","Taszkent":"Tashkent","Port Vila":"Port Vila","Watykan":"Vatican City","Caracas":"Caracas","Budapeszt":"Budapest",
"Londyn":"London","Hanoi":"Hanoi","Rzym":"Rome","Jamusukro":"Yamoussoukro","Avarua":"Avarua","Delap":"Delap","Honiara":"Honiara",
"São Tomé":"São Tomé","Lusaka":"Lusaka","Harare":"Harare","Abu Zabi":"Abu Dhabi","Yaren":"Yaren",
}


def entries(lines: list[str]) -> dict[str, dict]:
    found: dict[str, dict] = {}
    current = None
    field = None
    for line in lines:
        header = HEADER.match(line)
        if header:
            name = re.sub(r"\s*zob\. aktualizację.*$", "", header.group(1)).strip().rstrip("*")
            current = {"continent": header.group(2)}
            found[name] = current
            field = None
        elif current is None:
            continue
        elif re.match(r"^[a-ząćęłńóśźż]+\.", line):
            field = line.split(".")[0]
            current[field] = current.get(field, "") + line.split(".", 1)[1]
        elif line.startswith("  ") and field:
            current[field] += " " + line.strip()
        elif not line.strip():
            field = None
    return found


def main() -> int:
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    text = subprocess.run(["pdftotext", "-layout", sys.argv[1], "-"], capture_output=True, text=True, check=True).stdout
    lines = text.split("\n")
    start = next(i for i, line in enumerate(lines) if re.match(r"^Afganistan\s{3,}Azja", line))
    end = next(i for i, line in enumerate(lines) if i > start and "Terytoria niesamodzielne" in line)
    updates = next(i for i, line in enumerate(lines) if i > end and "Aktualizacja nr 1" in line)

    states = entries(lines[start:end])
    revised = entries(lines[updates:])
    states["Gwinea Równikowa"] = revised["Gwinea Równikowa"]
    states.pop("Nauru")
    states["Naoero"] = revised["Naoero"]

    countries = []
    for name, entry in states.items():
        code, english = CODES[name]
        capital = re.split(r",|;| ndm\.| albo", entry["stol"].strip())[0].strip().rstrip("*")
        people = [part.strip() for part in entry["obyw"].split(",")] if entry.get("obyw") else []
        countries.append(
            {
                "code": code,
                "english": english,
                "polish": [name, *ALSO_ACCEPTED.get(name, [])],
                "man": people[0] if people else None,
                "woman": people[1] if people else None,
                "capital": capital,
                "capitalEnglish": CAPITAL_EN[capital],
            }
        )
    countries.sort(key=lambda country: country["english"])
    ASSET.write_text(json.dumps(countries, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    with_people = sum(1 for country in countries if country["man"])
    print(f"{len(countries)} countries, {with_people} with words for a man and a woman -> {ASSET}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
