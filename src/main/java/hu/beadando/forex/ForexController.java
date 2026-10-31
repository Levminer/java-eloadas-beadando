package hu.beadando.forex;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Forex menük: Oanda demo számla kezelése.
 */
@Controller
@RequestMapping("/forex")
public class ForexController {

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

}
