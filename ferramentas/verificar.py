#!/usr/bin/env python3
"""Indicadores lexicais de treino; nao substituem revisao de projeto."""
import re
import sys
from pathlib import Path

raiz = Path(__file__).resolve().parents[1]
arquivos = sorted((raiz / 'src/main/java').rglob('*.java'))
textos = {p: p.read_text(encoding='utf-8') for p in arquivos}
fachada = next(p for p in arquivos if p.name == 'PedidoService.java')
modelos = {p:t for p,t in textos.items() if p != fachada}
abreviacoes = r'\b(?:cli|clis|prod|prods|ped|peds|qtd|vlr|vlrUnit|vldCpf|seqCli|seqProd|seqPed|cid|est|end|desc|sub|reg|txt|obj|objItem|dig)\b'
padroes = {
    '01': r'^ {12,}(?:if|for|while)\s*\(',
    '02': r'\belse\b',
    '03': r'^\s*private\s+(?:String|long|int|double|boolean)\s+\w+\s*[;=]',
    '04': r'\b(?:List|Map|ArrayList|LinkedHashMap|Iterator)\b',
    '05': r'\.[a-zA-Z]\w*\([^;\n]*?\)\.[a-zA-Z]\w*\(',
    '06': abreviacoes,
    '09': r'\bpublic\s+\w+\s+(?:get|set)\w+\(',
}
medidas = {}
for regra, padrao in padroes.items():
    fonte = modelos if regra in ('03','09') else textos
    medidas[regra] = sum(len(re.findall(padrao,t,re.M)) for t in fonte.values())
linhas = {p:len(t.splitlines()) for p,t in textos.items()}
medidas['07'] = sum(n > 50 for n in linhas.values())
campos = {p:len(re.findall(r'^\s*private\s+[\w<>?, ]+\s+\w+\s*(?:=|;)',t,re.M)) for p,t in textos.items()}
medidas['08'] = sum(n > 2 for n in campos.values())
if len(sys.argv) == 2:
    regra = sys.argv[1].zfill(2)
    if regra not in medidas:
        raise SystemExit('Use 01 a 09, ou sem argumento para medir.')
    print(f'Regra {regra}: {medidas[regra]} indicadores restantes')
    raise SystemExit(1 if medidas[regra] else 0)
print('Classes:',len(arquivos))
for p,n in linhas.items():
    print(f'{p.stem}: {n} linhas, {campos[p]} atributos')
for regra in sorted(medidas):
    print(f'Regra {regra}: {medidas[regra]}')
