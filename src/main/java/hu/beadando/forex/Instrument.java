package hu.beadando.forex;

/**
 * Egy kereskedhető instrumentum (devizapár) adatai.
 *
 * @param name             Oanda név, pl. EUR_USD
 * @param displayName      megjelenítési név, pl. EUR/USD
 * @param pipLocation      a pip helyiértéke 10 hatványaként, pl. -4 → 0,0001
 * @param displayPrecision ennyi tizedesjeggyel jegyzik az árat
 */
public record Instrument(String name, String displayName, int pipLocation, int displayPrecision) {

	static Instrument from(com.oanda.v20.primitives.Instrument i) {
		return new Instrument(
				i.getName().toString(),
				i.getDisplayName(),
				i.getPipLocation().intValue(),
				i.getDisplayPrecision().intValue());
	}

}
