# Java alkalmazások – Előadás – Beadandó

Böngészőben futó Spring Boot alkalmazás, amely az **MNB SOAP árfolyam-szolgáltatását** és az **Oanda v20 Forex API-t** használja.

**Készítette:** Fődi Kinga (EW67RO) - Lőrik Levente (AICYHB)

## Futtatás

Java 25 szükséges. A repó gyökerében lévő JAR fájl elindítása:

```
java -jar beadando.jar
```

Ezután az alkalmazás a böngészőben érhető el: <http://localhost:8080>

## Menüpontok

| Menü | Leírás |
|---|---|
| Főoldal | Az alkalmazás bemutatása, a menüpontok áttekintése |
| SOAP – MNB árfolyamok | Az MNB SOAP szolgáltatásából lekérdezett árfolyamok: deviza és időszak választás, táblázat és Chart.js grafikon |
| Forex-account | Az Oanda demo számla adatai (egyenleg, NAV, P/L, fedezet) |
| Forex-AktÁr | A kiválasztott instrumentum aktuális vételi/eladási ára |
| Forex-HistÁr | A kiválasztott instrumentum utolsó 10 historikus ára a megadott granularity-vel, táblázatban és grafikonon |
| Forex-Nyit | Pozíció nyitása piaci áron: pozitív mennyiség Long, negatív Short |
| Forex-Poz | A nyitott pozíciók táblázatban |
| Forex-Zár | Pozíció zárása a trade azonosítója alapján, piaci áron |

## Felhasznált technológiák

- **Spring Boot 4** (Spring MVC, Thymeleaf), **Java 25**, Maven
- **SOAP kliens:** JAX-WS (`jaxws-maven-plugin` wsimport) az MNB WSDL-jéből (`src/main/resources/wsdl`, a JAR-ba csomagolva) generált osztályokkal
- **Oanda:** a hivatalos [v20 Java könyvtár](https://github.com/oanda/v20-java), fxTrade Practice (demo) számla
- **Grafikonok:** [Chart.js](https://www.chartjs.org/)
- **Sablon:** [Editorial by HTML5 UP](https://html5up.net/editorial) (CCA 3.0 licenc)

## Fordítás

```
mvnw.cmd package
```

A kész JAR a `target/beadando.jar` helyen jön létre.

Az Oanda API token az `application.properties` fájl `oanda.token` beállításában található.
Helyette megadható az `OANDA_TOKEN` környezeti változóval is, vagy a futtatási mappában lévő `.env` fájlban (`OANDA_TOKEN=...`).

A JAR manifestje `Add-Opens: java.base/java.lang` bejegyzést tartalmaz.
Erre azért van szükség, mert a v20 könyvtár az Oanda hibaválaszait reflexióval alakítja kivétellé, és Java 25 alatt ezt csak így teheti meg.
