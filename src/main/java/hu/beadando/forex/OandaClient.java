package hu.beadando.forex;

import com.oanda.v20.Context;
import com.oanda.v20.ContextBuilder;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.account.AccountInstrumentsRequest;
import com.oanda.v20.instrument.CandlestickGranularity;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.primitives.InstrumentName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Oanda kliens a hivatalos v20 Java könyvtárral (https://github.com/oanda/v20-java),
 * fxTrade Practice (demo) számlához. Az összes Forex menü ezen keresztül éri el az Oanda szervert.
 */
@Service
public class OandaClient {

	/** A választható fő devizapárok. */
	private static final List<String> INSTRUMENTS = List.of(
			"EUR_USD", "GBP_USD", "USD_JPY", "USD_CHF", "AUD_USD", "USD_CAD",
			"NZD_USD", "EUR_GBP", "EUR_JPY", "EUR_HUF", "USD_HUF");

	/** A választható időfelbontások (granularity) és magyar megnevezésük. */
	public static final Map<String, String> GRANULARITIES = new LinkedHashMap<>();

	static {
		GRANULARITIES.put("M1", "M1 – 1 perces");
		GRANULARITIES.put("M5", "M5 – 5 perces");
		GRANULARITIES.put("M15", "M15 – 15 perces");
		GRANULARITIES.put("M30", "M30 – 30 perces");
		GRANULARITIES.put("H1", "H1 – 1 órás");
		GRANULARITIES.put("H4", "H4 – 4 órás");
		GRANULARITIES.put("D", "D – napi");
		GRANULARITIES.put("W", "W – heti");
		GRANULARITIES.put("M", "M – havi");
	}

	private final Context ctx;
	private final AccountID accountId;
	private final boolean configured;

	private List<Instrument> instruments;

	public OandaClient(
			@Value("${oanda.api-url}") String apiUrl,
			@Value("${oanda.account-id}") String accountId,
			@Value("${oanda.token}") String token) {
		this.accountId = new AccountID(accountId);
		this.configured = !token.isBlank();
		this.ctx = new ContextBuilder(apiUrl)
				.setToken(token)
				.setApplication("beadando")
				.build();
	}

	/** Számlainformációk (Forex-account menü). */
	public Account getAccount() {
		return call(() -> Account.from(ctx.account.summary(accountId).getAccount()));
	}

	/**
	 * A lenyíló listákban választható instrumentumok (a fő devizapárok), ebben a sorrendben.
	 * Az adataikat (név, pip, tizedesjegyek) egyszer kérdezzük le az Oandától.
	 */
	public synchronized List<Instrument> getInstruments() {
		if (instruments == null) {
			instruments = call(() -> {
				AccountInstrumentsRequest request = new AccountInstrumentsRequest(accountId).setInstruments(INSTRUMENTS);
				return ctx.account.instruments(request).getInstruments().stream()
						.map(Instrument::from)
						.sorted(Comparator.comparing(i -> INSTRUMENTS.indexOf(i.name())))
						.toList();
			});
		}
		return instruments;
	}

	/** Egy instrumentum aktuális ára (Forex-AktÁr menü). */
	public Price getPrice(String instrumentName) {
		Instrument instrument = findInstrument(instrumentName);
		return call(() -> {
			ClientPrice price = ctx.pricing.get(accountId, List.of(instrument.name())).getPrices().getFirst();
			return new Price(
					instrument,
					price.getBids().getFirst().getPrice().bigDecimalValue(),
					price.getAsks().getFirst().getPrice().bigDecimalValue(),
					Instant.parse(price.getTime().toString()),
					Boolean.TRUE.equals(price.getTradeable()));
		});
	}

	/** Az utolsó {@code count} historikus ár (gyertya), a legfrissebb elöl (Forex-HistÁr menü). */
	public List<Candle> getCandles(String instrumentName, String granularity, int count) {
		Instrument instrument = findInstrument(instrumentName);
		if (!GRANULARITIES.containsKey(granularity)) {
			throw new OandaException("Ismeretlen granularity: " + granularity, null);
		}
		return call(() -> {
			InstrumentCandlesRequest request = new InstrumentCandlesRequest(new InstrumentName(instrument.name()))
					.setGranularity(CandlestickGranularity.valueOf(granularity))
					.setCount((long) count);
			return ctx.instrument.candles(request).getCandles().stream()
					.map(Candle::from)
					.sorted(Comparator.comparing(Candle::time).reversed())
					.toList();
		});
	}

	public Instrument findInstrument(String name) {
		return getInstruments().stream()
				.filter(i -> i.name().equals(name))
				.findFirst()
				.orElseThrow(() -> new OandaException("Ismeretlen instrumentum: " + name, null));
	}

	/** Egy v20 hívás, ami a könyvtár kivételeit dobhatja. */
	@FunctionalInterface
	private interface OandaCall<T> {
		T run() throws RequestException, ExecuteException;
	}

	/** Az Oanda hívások közös hibakezelése: érthető üzenet a felhasználónak. */
	private <T> T call(OandaCall<T> request) {
		if (!configured) {
			throw new OandaException("Nincs beállítva az Oanda API token (oanda.token / OANDA_TOKEN).", null);
		}
		try {
			return request.run();
		} catch (RequestException e) {
			throw new OandaException("Oanda hiba (" + e.getStatus() + "): " + e.getErrorMessage(), e);
		} catch (ExecuteException e) {
			throw new OandaException("Nem sikerült elérni az Oanda szervert: " + e.getMessage(), e);
		}
	}

	/** Hiba az Oanda API hívása közben. */
	public static class OandaException extends RuntimeException {
		public OandaException(String message, Throwable cause) {
			super(message, cause);
		}
	}

}
