package hu.beadando.forex;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Egy instrumentum aktuális ára (Forex-AktÁr menü).
 *
 * @param bid       vételi ár (ezen adhatunk el)
 * @param ask       eladási ár (ezen vehetünk)
 * @param tradeable éppen kereskedhető-e (hétvégén a piac zárva van)
 */
public record Price(Instrument instrument, BigDecimal bid, BigDecimal ask, Instant time, boolean tradeable) {

	/** Középár: (bid + ask) / 2 */
	public BigDecimal mid() {
		return bid.add(ask).divide(BigDecimal.TWO, instrument.displayPrecision() + 1, RoundingMode.HALF_UP);
	}

	/** A vételi és eladási ár különbsége pipben. */
	public BigDecimal spreadPips() {
		return ask.subtract(bid).scaleByPowerOfTen(-instrument.pipLocation()).setScale(1, RoundingMode.HALF_UP);
	}

}
