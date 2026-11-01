package hu.beadando.forex;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Forex menük: Oanda demo számla kezelése.
 */
@Controller
@RequestMapping("/forex")
public class ForexController {

	private static final ZoneId BUDAPEST = ZoneId.of("Europe/Budapest");

	private final OandaClient oanda;

	public ForexController(OandaClient oanda) {
		this.oanda = oanda;
	}

	/** Forex-account: számlainformációk kiírása. */
	@GetMapping("/account")
	public String account(Model model) {
		try {
			model.addAttribute("account", oanda.getAccount());
		} catch (OandaClient.OandaException e) {
			model.addAttribute("error", e.getMessage());
		}
		return "forex/account";
	}

	/** Forex-AktÁr: a kiválasztott instrumentum aktuális ára (első megnyitáskor az EUR_USD-é). */
	@GetMapping("/aktar")
	public String aktar(@RequestParam(defaultValue = "EUR_USD") String instrument, Model model) {
		model.addAttribute("instrument", instrument);
		try {
			model.addAttribute("instruments", oanda.getInstruments());
			model.addAttribute("price", oanda.getPrice(instrument));
		} catch (OandaClient.OandaException e) {
			model.addAttribute("error", e.getMessage());
		}
		return "forex/aktar";
	}

	/**
	 * Forex-HistÁr: a kiválasztott instrumentum utolsó 10 historikus ára a megadott időfelbontással
	 * (első megnyitáskor EUR_USD, napi).
	 */
	@GetMapping("/histar")
	public String histar(@RequestParam(defaultValue = "EUR_USD") String instrument,
						 @RequestParam(defaultValue = "D") String granularity,
						 Model model) {
		model.addAttribute("instrument", instrument);
		model.addAttribute("granularity", granularity);
		model.addAttribute("granularities", OandaClient.GRANULARITIES);
		try {
			model.addAttribute("instruments", oanda.getInstruments());
			List<Candle> candles = oanda.getCandles(instrument, granularity, 10);
			model.addAttribute("candles", candles);
			model.addAttribute("selected", oanda.findInstrument(instrument));
			// A grafikon időrendben (a legrégebbi elöl). Az Oanda az időszak kezdetét adja meg,
			// a napi gyertya pl. New York-i 17:00-kor (budapesti 22/23 óra) kezdődik, ezért kell az óra is.
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM.dd. HH:mm").withZone(BUDAPEST);
			List<Candle> chronological = candles.reversed();
			model.addAttribute("chartLabels", chronological.stream().map(c -> formatter.format(c.time())).toList());
			model.addAttribute("chartData", chronological.stream().map(c -> c.close().doubleValue()).toList());
		} catch (OandaClient.OandaException e) {
			model.addAttribute("error", e.getMessage());
		}
		return "forex/histar";
	}

	/** Forex-Nyit: űrlap pozíció nyitásához. */
	@GetMapping("/nyit")
	public String nyit(Model model) {
		model.addAttribute("maxUnits", OandaClient.MAX_UNITS);
		if (!model.containsAttribute("instrument")) {
			model.addAttribute("instrument", "EUR_USD");
		}
		if (!model.containsAttribute("units")) {
			model.addAttribute("units", "1000");
		}
		try {
			model.addAttribute("instruments", oanda.getInstruments());
		} catch (OandaClient.OandaException e) {
			model.addAttribute("error", e.getMessage());
		}
		return "forex/nyit";
	}

	/**
	 * Forex-Nyit: pozíció nyitása piaci áron. Az eredményt átirányítás után jelenítjük meg
	 * (POST/Redirect/GET), így az oldal frissítése nem nyit újabb pozíciót.
	 */
	@PostMapping("/nyit")
	public String nyitPost(@RequestParam String instrument, @RequestParam String units,
						   RedirectAttributes redirect) {
		redirect.addFlashAttribute("instrument", instrument);
		redirect.addFlashAttribute("units", units);
		try {
			long parsedUnits;
			try {
				parsedUnits = Long.parseLong(units.trim());
			} catch (NumberFormatException e) {
				throw new OandaClient.OandaException("A mennyiség csak egész szám lehet.", e);
			}
			redirect.addFlashAttribute("result", oanda.openMarketOrder(instrument, parsedUnits));
		} catch (OandaClient.OandaException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/forex/nyit";
	}

	/** Forex-Poz: a nyitott pozíciók táblázatban, a számla összesítő adataival. */
	@GetMapping("/poz")
	public String poz(Model model) {
		try {
			model.addAttribute("account", oanda.getAccount());
			model.addAttribute("trades", oanda.getOpenTrades());
		} catch (OandaClient.OandaException e) {
			model.addAttribute("error", e.getMessage());
		}
		return "forex/poz";
	}

	/**
	 * Forex-Zár: űrlap egy pozíció zárásához. A Forex-Poz oldal „Zárás” gombja a tradeId-t
	 * előre kitöltve adja át; segítségként a nyitott pozíciók listáját is megjelenítjük.
	 */
	@GetMapping("/zar")
	public String zar(@RequestParam(required = false) String tradeId, Model model) {
		if (!model.containsAttribute("tradeId")) {
			model.addAttribute("tradeId", tradeId);
		}
		try {
			model.addAttribute("currency", oanda.getAccount().currency());
			model.addAttribute("trades", oanda.getOpenTrades());
		} catch (OandaClient.OandaException e) {
			model.addAttribute("error", e.getMessage());
		}
		return "forex/zar";
	}

	/** Forex-Zár: a megadott pozíció zárása piaci áron (POST/Redirect/GET). */
	@PostMapping("/zar")
	public String zarPost(@RequestParam String tradeId, RedirectAttributes redirect) {
		redirect.addFlashAttribute("tradeId", tradeId);
		try {
			long parsedId;
			try {
				parsedId = Long.parseLong(tradeId.trim());
			} catch (NumberFormatException e) {
				throw new OandaClient.OandaException("A trade azonosító csak egész szám lehet.", e);
			}
			redirect.addFlashAttribute("result", oanda.closeTrade(parsedId));
		} catch (OandaClient.OandaException e) {
			redirect.addFlashAttribute("closeError", e.getMessage());
		}
		return "redirect:/forex/zar";
	}

}
