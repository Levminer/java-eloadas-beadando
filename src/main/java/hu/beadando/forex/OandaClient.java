package hu.beadando.forex;

import com.oanda.v20.Context;
import com.oanda.v20.ContextBuilder;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.AccountID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Oanda kliens a hivatalos v20 Java könyvtárral (https://github.com/oanda/v20-java),
 * fxTrade Practice (demo) számlához. Az összes Forex menü ezen keresztül éri el az Oanda szervert.
 */
@Service
public class OandaClient {

	private final Context ctx;
	private final AccountID accountId;
	private final boolean configured;

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
