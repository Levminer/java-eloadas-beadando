package hu.beadando.soap;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * SOAP menü: deviza és időszak kiválasztása, az MNB árfolyamok kiírása táblázatban és grafikonon.
 */
@Controller
public class SoapController {

	private final MnbService mnbService;

	public SoapController(MnbService mnbService) {
		this.mnbService = mnbService;
	}

	@GetMapping("/soap")
	public String soap(
			@RequestParam(required = false) String currency,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			Model model) {

		// Alapértelmezés: EUR, az elmúlt 30 nap
		LocalDate today = LocalDate.now();
		model.addAttribute("currency", currency != null ? currency : "EUR");
		model.addAttribute("startDate", startDate != null ? startDate : today.minusDays(30));
		model.addAttribute("endDate", endDate != null ? endDate : today);
		model.addAttribute("today", today);

		try {
			model.addAttribute("currencies", mnbService.getCurrencies());
		} catch (MnbService.MnbException e) {
			model.addAttribute("currencies", List.of("EUR", "USD", "GBP", "CHF"));
			model.addAttribute("error", e.getMessage());
			return "soap";
		}

		// Űrlap elküldése nélkül csak az üres űrlapot mutatjuk
		if (currency == null || startDate == null || endDate == null) {
			return "soap";
		}

		if (startDate.isAfter(endDate)) {
			model.addAttribute("error", "A kezdő dátum nem lehet későbbi a befejező dátumnál.");
			return "soap";
		}

		try {
			List<ExchangeRate> rates = mnbService.getExchangeRates(currency, startDate, endDate);
			model.addAttribute("rates", rates);
			if (!rates.isEmpty()) {
				model.addAttribute("unit", rates.getFirst().unit());
				model.addAttribute("minRate", rates.stream().map(ExchangeRate::rate).min(Comparator.naturalOrder()).orElseThrow());
				model.addAttribute("maxRate", rates.stream().map(ExchangeRate::rate).max(Comparator.naturalOrder()).orElseThrow());
				model.addAttribute("change", rates.getLast().rate().subtract(rates.getFirst().rate()));
				// A grafikon adatai (Chart.js)
				model.addAttribute("chartLabels", rates.stream().map(r -> r.date().toString()).toList());
				model.addAttribute("chartData", rates.stream().map(ExchangeRate::rate).map(BigDecimal::doubleValue).toList());
			}
		} catch (MnbService.MnbException e) {
			model.addAttribute("error", e.getMessage());
		}

		return "soap";
	}

}
