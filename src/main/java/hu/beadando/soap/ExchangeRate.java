package hu.beadando.soap;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Egy MNB árfolyam adott napon: hány forint {@code unit} egység devizáért.
 */
public record ExchangeRate(LocalDate date, String currency, int unit, BigDecimal rate) {
}
