package hu.beadando.forex;

import com.oanda.v20.transaction.OrderCancelTransaction;
import com.oanda.v20.transaction.OrderFillTransaction;
import com.oanda.v20.transaction.TradeOpen;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Egy piaci áras megbízás eredménye (Forex-Nyit menü): vagy teljesült, vagy az Oanda elutasította.
 *
 * @param tradeId      a megnyitott pozíció (trade) azonosítója; üres, ha a megbízás csak egy meglévő
 *                     ellentétes pozíciót csökkentett vagy zárt (a számlán nincs hedging)
 * @param cancelReason az elutasítás oka magyarul, ha a megbízás nem teljesült
 */
public record OrderResult(boolean filled, String instrument, BigDecimal units, String tradeId,
						  BigDecimal price, Instant time, String cancelReason) {

	/** A gyakori elutasítási okok magyarul. */
	private static final Map<String, String> CANCEL_REASONS = Map.of(
			"MARKET_HALTED", "a piac jelenleg zárva van (hétvégén nem lehet kereskedni)",
			"INSTRUMENT_BID_HALTED", "az instrumentum kereskedése szünetel",
			"INSTRUMENT_ASK_HALTED", "az instrumentum kereskedése szünetel",
			"INSUFFICIENT_MARGIN", "nincs elég fedezet (margin) a számlán",
			"INSUFFICIENT_LIQUIDITY", "nincs elég likviditás a piacon",
			"POSITION_SIZE_EXCEEDED", "túl nagy a pozíció mérete",
			"ACCOUNT_POSITION_VALUE_LIMIT_EXCEEDED", "a számla pozícióértékének limitje túllépve",
			"OPEN_TRADES_ALLOWED_EXCEEDED", "elérte a nyitott pozíciók maximális számát",
			"ACCOUNT_LOCKED", "a számla zárolva van");

	static OrderResult filled(String instrument, OrderFillTransaction fill) {
		TradeOpen opened = fill.getTradeOpened();
		return new OrderResult(
				true,
				instrument,
				fill.getUnits().bigDecimalValue(),
				opened != null ? opened.getTradeID().toString() : null,
				opened != null ? opened.getPrice().bigDecimalValue() : fill.getPrice().bigDecimalValue(),
				Instant.parse(fill.getTime().toString()),
				null);
	}

	static OrderResult cancelled(String instrument, BigDecimal units, OrderCancelTransaction cancel) {
		String reason = cancel.getReason() != null ? cancel.getReason().name() : "ismeretlen";
		return new OrderResult(
				false,
				instrument,
				units,
				null,
				null,
				Instant.parse(cancel.getTime().toString()),
				CANCEL_REASONS.getOrDefault(reason, reason));
	}

	/** Long (vétel) vagy Short (eladás) a megbízás. */
	public boolean isLong() {
		return units.signum() > 0;
	}

}
