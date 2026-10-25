#!/usr/bin/env python3
"""Converte o IR-DB (github.com/probonopd/irdb) em assets compactos para o app.

Uso: build_db.py <irdb/codes> <app/src/main/assets>
Gera brands.tsv (marca, nº de modelos) e codes.tsv.gz
(marca, categoria, arquivo, função, protocolo, device, subdevice, function).
"""
import csv, gzip, io, os, re, sys

# Nome no IR-DB -> nome canônico entendido por IrEncoder.kt
PROTOCOLS = {
    "nec": "NEC1", "nec1": "NEC1", "nec2": "NEC2", "necx1": "NECX1", "necx2": "NECX2",
    "nec1-f16": "NEC1-F16", "nec2-f16": "NEC2-F16",
    "rc5": "RC5", "rc6": "RC6", "mce": "MCE",
    "sony12": "SONY12", "sony15": "SONY15", "sony20": "SONY20",
    "jvc": "JVC", "jvc{2}": "JVC2",
    "mitsubishi": "MITSUBISHI", "aiwa": "AIWA", "panasonic": "PANASONIC",
    "denon": "DENON", "denon{1}": "DENON", "denon{2}": "DENON",
    "sharp": "SHARP", "sharp{1}": "SHARP", "sharp{2}": "SHARP",
    "pioneer": "PIONEER", "proton": "PROTON", "recs80": "RECS80",
    "rca": "RCA", "rca-38": "RCA38", "panasonic_old": "PANASONIC_OLD",
    "fujitsu": "FUJITSU", "akai": "AKAI", "emerson": "EMERSON",
    "thomson": "THOMSON", "konka": "KONKA",
}


def to_int(v):
    v = v.strip()
    try:
        return int(v)
    except ValueError:
        return None


def main(src, dst):
    os.makedirs(dst, exist_ok=True)
    rows, brands = [], {}
    for brand in sorted(os.listdir(src), key=str.lower):
        bdir = os.path.join(src, brand)
        if not os.path.isdir(bdir):
            continue
        for cat in sorted(os.listdir(bdir), key=str.lower):
            cdir = os.path.join(bdir, cat)
            if not os.path.isdir(cdir):
                continue
            for fname in sorted(os.listdir(cdir)):
                if not fname.endswith(".csv"):
                    continue
                with open(os.path.join(cdir, fname), encoding="utf-8", errors="replace") as f:
                    text = f.read()
                # alguns arquivos têm o cabeçalho colado no fim da última linha
                text = text.replace("functionname,protocol,device,subdevice,function", "\n")
                got = []
                for r in csv.reader(io.StringIO(text)):
                    if len(r) != 5:
                        continue
                    name, proto, d, s, fn = r
                    proto = PROTOCOLS.get(proto.strip().lower())
                    d, s, fn = to_int(d), to_int(s), to_int(fn)
                    name = re.sub(r"[\t\r\n]", " ", name).strip()
                    if not proto or d is None or s is None or fn is None or not name:
                        continue
                    got.append((brand, cat, fname[:-4], name, proto, str(d), str(s), str(fn)))
                if got:
                    rows.extend(got)
                    brands[brand] = brands.get(brand, 0) + 1
    with gzip.open(os.path.join(dst, "codes.tsv.gz"), "wt", encoding="utf-8", compresslevel=9) as f:
        for r in rows:
            f.write("\t".join(r) + "\n")
    with open(os.path.join(dst, "brands.tsv"), "w", encoding="utf-8") as f:
        for b, n in brands.items():
            f.write(f"{b}\t{n}\n")
    print(f"{len(brands)} marcas, {sum(brands.values())} modelos, {len(rows)} códigos")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
