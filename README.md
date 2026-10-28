# Controle IR

Controle remoto universal para TVs usando o emissor infravermelho do celular (ex.: RedMagic 11 Pro).

- Códigos do [IR-DB](https://github.com/probonopd/irdb), embutidos no APK (funciona offline).
- Escolha a marca → escolha o modelo ou toque em **Não sei o modelo** para enviar, a cada tecla, os códigos de todos os controles de TV daquela marca.
- Aba **AC**: controle de ar-condicionado (liga/desliga, temperatura, modo, ventilação e vaivém) com os protocolos Gree, Midea/Coolix e LG. O IR-DB quase não tem códigos de AC, e ACs modernos enviam o estado completo a cada toque, então esses protocolos são gerados pelo app (`AcProtocols.kt`).
- O APK é compilado pelo GitHub Actions (`.github/workflows/build.yml`) e publicado em *Releases*.

Gerar o banco localmente: `python3 tools/build_db.py <irdb>/codes app/src/main/assets`.
