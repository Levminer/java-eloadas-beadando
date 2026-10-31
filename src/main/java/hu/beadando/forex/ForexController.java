package hu.beadando.forex;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

}
