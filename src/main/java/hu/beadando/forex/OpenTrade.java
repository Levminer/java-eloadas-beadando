package hu.beadando.forex;

import com.oanda.v20.trade.Trade;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Egy nyitott pozíció (trade) a számlán (Forex-Poz menü).
 *
 * @param units        a még nyitott mennyiség; pozitív: Long, negatív: Short
 * @param unrealizedPL nem realizált nyereség/veszteség a számla devizanemében
 */
public record OpenTrade(String id, String instrument, BigDecimal units, BigDecimal price, Instant openTime,
						BigDecimal unrealizedPL, BigDecimal marginUsed) {

	static OpenTrade from(Trade t) {
		return new OpenTrade(
				t.getId().toString(),
				t.getInstrument().toString(),
				t.getCurrentUnits().bigDecimalValue(),
				t.getPrice().bigDecimalValue(),
				Instant.parse(t.getOpenTime().toString()),
				t.getUnrealizedPL().bigDecimalValue(),
				t.getMarginUsed().bigDecimalValue());
	}

	/** Long (vétel) vagy Short (eladás) a pozíció. */
	public boolean isLong() {
		return units.signum() > 0;
	}

}
