package hu.beadando.forex;

import com.oanda.v20.transaction.OrderCancelTransaction;
import com.oanda.v20.transaction.OrderFillTransaction;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Egy pozíció (trade) piaci áras zárásának eredménye (Forex-Zár menü).
 *
 * @param units        a záró megbízás mennyisége: Long pozíció zárásakor negatív (eladás), Short zárásakor pozitív
 * @param realizedPL   a zárással realizált nyereség/veszteség a számla devizanemében
 * @param cancelReason az elutasítás oka magyarul, ha a zárás nem teljesült
 */
public record CloseResult(boolean closed, String tradeId, String instrument, BigDecimal units, BigDecimal price,
						  BigDecimal realizedPL, Instant time, String cancelReason) {

	static CloseResult closed(String tradeId, OrderFillTransaction fill) {
		return new CloseResult(
				true,
				tradeId,
				fill.getInstrument().toString(),
				fill.getUnits().bigDecimalValue(),
				fill.getPrice().bigDecimalValue(),
				fill.getPl().bigDecimalValue(),
				Instant.parse(fill.getTime().toString()),
				null);
	}

	static CloseResult cancelled(String tradeId, OrderCancelTransaction cancel) {
		return new CloseResult(
				false,
				tradeId,
				null,
				null,
				null,
				null,
				Instant.parse(cancel.getTime().toString()),
				OrderResult.cancelReason(cancel));
	}

	/** Long pozíciót zártunk-e (a záró megbízás eladás volt). */
	public boolean wasLong() {
		return units.signum() < 0;
	}

}
