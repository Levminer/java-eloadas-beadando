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

	/** Forex-AktÁr: a kiválasztott instrumentum aktuális ára. */
	@GetMapping("/aktar")
	public String aktar(@RequestParam(required = false) String instrument, Model model) {
		model.addAttribute("instrument", instrument != null ? instrument : "EUR_USD");
		try {
			model.addAttribute("instruments", oanda.getInstruments());
			if (instrument != null) {
				model.addAttribute("price", oanda.getPrice(instrument));
			}
		} catch (OandaClient.OandaException e) {
			model.addAttribute("error", e.getMessage());
		}
		return "forex/aktar";
	}

	/** Forex-HistÁr: a kiválasztott instrumentum utolsó 10 historikus ára a megadott időfelbontással. */
	@GetMapping("/histar")
	public String histar(@RequestParam(required = false) String instrument,
						 @RequestParam(required = false) String granularity,
						 Model model) {
		model.addAttribute("instrument", instrument != null ? instrument : "EUR_USD");
		model.addAttribute("granularity", granularity != null ? granularity : "D");
		model.addAttribute("granularities", OandaClient.GRANULARITIES);
		try {
			model.addAttribute("instruments", oanda.getInstruments());
			if (instrument != null && granularity != null) {
				List<Candle> candles = oanda.getCandles(instrument, granularity, 10);
				model.addAttribute("candles", candles);
				model.addAttribute("selected", oanda.findInstrument(instrument));
				// A grafikon időrendben (a legrégebbi elöl). Az Oanda az időszak kezdetét adja meg,
				// a napi gyertya pl. New York-i 17:00-kor (budapesti 22/23 óra) kezdődik, ezért kell az óra is.
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM.dd. HH:mm").withZone(BUDAPEST);
				List<Candle> chronological = candles.reversed();
				model.addAttribute("chartLabels", chronological.stream().map(c -> formatter.format(c.time())).toList());
				model.addAttribute("chartData", chronological.stream().map(c -> c.close().doubleValue()).toList());
			}
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

}
