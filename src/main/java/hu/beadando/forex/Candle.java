package hu.beadando.forex;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickData;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Egy historikus ár (gyertya) a megadott időszakra, középárakkal (Forex-HistÁr menü).
 *
 * @param complete lezárult-e már az időszak (az utolsó gyertya gyakran még folyamatban van)
 */
public record Candle(Instant time, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
					 long volume, boolean complete) {

	static Candle from(Candlestick c) {
		CandlestickData mid = c.getMid();
		return new Candle(
				Instant.parse(c.getTime().toString()),
				mid.getO().bigDecimalValue(),
				mid.getH().bigDecimalValue(),
				mid.getL().bigDecimalValue(),
				mid.getC().bigDecimalValue(),
				c.getVolume(),
				Boolean.TRUE.equals(c.getComplete()));
	}

	/** Nőtt (1), csökkent (-1) vagy nem változott (0) az ár az időszak alatt. */
	public int direction() {
		return close.compareTo(open);
	}

}
