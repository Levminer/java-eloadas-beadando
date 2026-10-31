package hu.beadando.forex;

import com.oanda.v20.account.AccountSummary;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Az Oanda számla összesítő adatai a megjelenítéshez.
 * A v20 könyvtár AccountSummary objektumából készül.
 */
public record Account(
		String id,
		String alias,
		String currency,
		BigDecimal balance,
		BigDecimal nav,
		BigDecimal unrealizedPL,
		BigDecimal pl,
		BigDecimal marginRate,
		BigDecimal marginUsed,
		BigDecimal marginAvailable,
		long openTradeCount,
		long openPositionCount,
		long pendingOrderCount,
		Instant createdTime) {

	static Account from(AccountSummary s) {
		return new Account(
				s.getId().toString(),
				s.getAlias(),
				s.getCurrency().toString(),
				s.getBalance().bigDecimalValue(),
				s.getNAV().bigDecimalValue(),
				s.getUnrealizedPL().bigDecimalValue(),
				s.getPl().bigDecimalValue(),
				s.getMarginRate().bigDecimalValue(),
				s.getMarginUsed().bigDecimalValue(),
				s.getMarginAvailable().bigDecimalValue(),
				s.getOpenTradeCount(),
				s.getOpenPositionCount(),
				s.getPendingOrderCount(),
				Instant.parse(s.getCreatedTime().toString()));
	}

}
